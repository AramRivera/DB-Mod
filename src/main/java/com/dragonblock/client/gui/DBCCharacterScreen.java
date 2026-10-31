package com.dragonblock.client.gui;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SActionPacket;
import com.dragonblock.common.network.packet.C2SSelectRacePacket;
import com.dragonblock.common.network.packet.C2SUpgradeStatPacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.List;

/**
 * Character sheet: a live 3D preview of the player on the left (hair, aura and tail included), and
 * race selection or the Attributes / Forms tabs on the right. Everything eases in and reacts to hover.
 */
public class DBCCharacterScreen extends Screen {
    private static final int W = 420;
    private static final int H = 250;
    private static final int PREVIEW_W = 130;
    private static final int CARD_H = 30;
    private static final String[] STATS = {"STR", "DEX", "CON", "WIL", "MND"};
    private static final String[] STAT_NAMES = {"Fuerza", "Destreza", "Constitución", "Voluntad", "Mente"};
    private static final String[] STAT_HINTS = {
            "Daño cuerpo a cuerpo", "Velocidad y evasión", "Vida máxima", "Ki máximo y daño de Ki", "TP y maestría"};

    private enum Tab { STATS, FORMS }

    private Tab tab = Tab.STATS;
    private Race pendingRace = Race.HUMAN;
    private int formScroll = 0;
    private int lastSyncHash = 0;
    private final long openedAt = Util.getMillis();
    private long tabChangedAt = 0;
    private float tabUnderline = 0;
    /** Displayed stat values ease toward the real ones, so upgrades animate. */
    private final float[] shownStats = new float[5];

    public DBCCharacterScreen() {
        super(Component.literal("Ficha de personaje"));
    }

    private PlayerDBCData data() {
        return PlayerDBCData.get(Minecraft.getInstance().player);
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2 + (int) (12 * (1 - openProgress())); }
    private int contentLeft() { return left() + PREVIEW_W + 10; }
    private int contentWidth() { return W - PREVIEW_W - 20; }

    /** 0..1 ease-out over the first 220 ms after opening. */
    private float openProgress() {
        float t = Mth.clamp((Util.getMillis() - openedAt) / 220f, 0f, 1f);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    @Override
    protected void init() {
        int[] base = baseStats(data());
        for (int i = 0; i < 5; i++) shownStats[i] = base[i];
        rebuild();
    }

    private static int[] baseStats(PlayerDBCData d) {
        return new int[]{d.getStr(), d.getDex(), d.getCon(), d.getWil(), d.getMnd()};
    }

    private void rebuild() {
        clearWidgets();
        if (minecraft == null || minecraft.player == null) return;
        PlayerDBCData data = data();
        int accent = data.getTransformation().getAuraColor();
        int cl = contentLeft(), t = top(), cw = contentWidth();

        if (!data.isRaceChosen()) {
            addRenderableWidget(new FlatButton(cl + cw - 110, t + H - 26, 110, 18,
                    Component.literal("Confirmar raza"), accent, b -> {
                ModMessages.sendToServer(new C2SSelectRacePacket(pendingRace.name()));
                onClose();
            }));
            return;
        }

        addRenderableWidget(new FlatButton(cl, t + 26, 80, 16, Component.literal("Atributos"), accent,
                b -> switchTab(Tab.STATS)));
        addRenderableWidget(new FlatButton(cl + 84, t + 26, 80, 16, Component.literal("Formas"), accent,
                b -> switchTab(Tab.FORMS)));

        if (tab == Tab.STATS) {
            int y = t + 108;
            for (String stat : STATS) {
                FlatButton plus = new FlatButton(cl + cw - 24, y + 1, 24, 18, Component.literal("+"), accent,
                        b -> ModMessages.sendToServer(new C2SUpgradeStatPacket(stat)));
                plus.active = data.getTrainingPoints() >= data.getUpgradeCost(stat);
                addRenderableWidget(plus);
                y += 26;
            }
        }
        if (data.getRace().hasSaiyanTail()) {
            addRenderableWidget(new FlatButton(left() + 10, t + H - 26, PREVIEW_W - 20, 16,
                    Component.literal(data.isTailOut() ? "Enrollar cola" : "Soltar cola"), accent,
                    b -> ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.TOGGLE_TAIL, 0))));
        }
    }

    private void switchTab(Tab next) {
        if (tab == next) return;
        tab = next;
        tabChangedAt = Util.getMillis();
        rebuild();
    }

    // Rebuild when the server syncs (TP spent, form changed...) so enabled states stay correct
    @Override
    public void tick() {
        PlayerDBCData d = data();
        int hash = d.getTrainingPoints() * 31 + d.getLevel() * 7 + d.getTransformation().ordinal()
                + (d.isTailOut() ? 1 : 0) + (d.isRaceChosen() ? 2 : 0);
        if (hash != lastSyncHash) {
            lastSyncHash = hash;
            rebuild();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, (int) (0xA0 * openProgress()) << 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null) return;
        PlayerDBCData data = data();
        int l = left(), t = top();
        int accentRgb = data.isRaceChosen() ? data.getTransformation().getAuraColor() : pendingRace.getFeatureColor() | 0x404040;
        int accent = 0xFF000000 | accentRgb;

        // Panel
        g.fill(l, t, l + W, t + H, UiTheme.PANEL);
        g.renderOutline(l, t, W, H, UiTheme.BORDER);
        g.fill(l, t, l + (int) (W * openProgress()), t + 2, accent);
        g.drawString(font, title.getString().toUpperCase(), l + 10, t + 9, UiTheme.TEXT_HEADER, false);
        String name = minecraft.player.getGameProfile().getName();
        g.drawString(font, name, l + W - 10 - font.width(name), t + 9, UiTheme.TEXT_DIM, false);

        renderPreview(g, data, mouseX, mouseY, l, t, accent);

        if (!data.isRaceChosen()) {
            renderRaceSelect(g, mouseX, mouseY, contentLeft(), t);
        } else {
            renderTabUnderline(g, accent);
            if (tab == Tab.STATS) renderStats(g, data, mouseX, mouseY, accent, partialTick);
            else renderForms(g, data, mouseX, mouseY, accent);
        }

        for (Renderable r : renderables) r.render(g, mouseX, mouseY, partialTick);
        if (data.isRaceChosen() && tab == Tab.FORMS) renderFormTooltip(g, data, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ 3D preview

    private void renderPreview(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int l, int t, int accent) {
        int px = l + 8, py = t + 24, pw = PREVIEW_W - 6, ph = H - 56;
        g.fill(px, py, px + pw, py + ph, UiTheme.PANEL_ALT);
        // Soft colored floor glow under the character
        for (int i = 0; i < 6; i++) {
            int a = 0x18 - i * 3;
            g.fill(px + 10 + i * 4, py + ph - 14 + i, px + pw - 10 - i * 4, py + ph - 13 + i, a << 24 | (accent & 0xFFFFFF));
        }
        g.enableScissor(px, py, px + pw, py + ph);
        int size = (int) (40 / Math.max(1f, minecraft.player.getScale()));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph - 8, size, 0.0625f,
                mouseX, mouseY, minecraft.player);
        g.disableScissor();

        String race = (data.isRaceChosen() ? data.getRace() : pendingRace).getDisplayName();
        g.drawCenteredString(font, race, px + pw / 2, py + ph + 4, UiTheme.TEXT_DIM);
    }

    private void renderTabUnderline(GuiGraphics g, int accent) {
        float target = tab == Tab.STATS ? 0 : 84;
        tabUnderline += (target - tabUnderline) * 0.3f;
        int x = contentLeft() + (int) tabUnderline;
        g.fill(x, top() + 43, x + 80, top() + 45, accent);
    }

    /** Content slides in briefly after switching tabs. */
    private int tabSlide() {
        float t = Mth.clamp((Util.getMillis() - tabChangedAt) / 160f, 0f, 1f);
        return (int) (10 * (1 - t) * (1 - t));
    }

    // ------------------------------------------------------------------ race selection

    private void renderRaceSelect(GuiGraphics g, int mouseX, int mouseY, int cl, int t) {
        int cw = contentWidth();
        g.drawString(font, "Elige tu raza. La elección es permanente.", cl, t + 28, UiTheme.TEXT_DIM, false);
        int cardW = (cw - 6) / 2, y = t + 42;
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            Race race = races[i];
            int x = cl + (i % 2) * (cardW + 6), cy = y + (i / 2) * 26;
            boolean selected = race == pendingRace;
            boolean hover = inside(mouseX, mouseY, x, cy, cardW, 22);
            g.fill(x, cy, x + cardW, cy + 22, selected ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            g.fill(x, cy, x + (selected ? 4 : 2), cy + 22, 0xFF000000 | race.getFeatureColor() | 0x303030);
            g.drawString(font, race.getDisplayName(), x + 10, cy + 7, selected ? UiTheme.TEXT_HEADER : UiTheme.TEXT, false);
        }

        int dy = y + 3 * 26 + 6;
        g.fill(cl, dy, cl + cw, t + H - 32, UiTheme.PANEL_ALT);
        g.drawString(font, pendingRace.getDisplayName().toUpperCase(), cl + 8, dy + 6, UiTheme.TEXT_HEADER, false);
        int ly = dy + 18;
        for (FormattedCharSequence line : font.split(Component.literal(pendingRace.getDescription()), cw - 16)) {
            g.drawString(font, line, cl + 8, ly, UiTheme.TEXT, false);
            ly += 10;
        }
        float[] mods = {pendingRace.getStrModifier(), pendingRace.getDexModifier(), pendingRace.getConModifier(),
                pendingRace.getWilModifier(), pendingRace.getMndModifier()};
        int bx = cl + 8, bw = (cw - 16 - 4 * 6) / 5;
        for (int i = 0; i < 5; i++) {
            int x = bx + i * (bw + 6);
            g.drawString(font, STATS[i], x, ly + 4, UiTheme.TEXT_DIM, false);
            g.fill(x, ly + 14, x + bw, ly + 17, UiTheme.TRACK);
            int c = mods[i] > 1f ? UiTheme.GOOD : mods[i] < 1f ? UiTheme.BAD : UiTheme.TEXT;
            g.fill(x, ly + 14, x + (int) (bw * Mth.clamp(mods[i] / 1.5f, 0f, 1f)), ly + 17, 0xFF000000 | c);
        }
        long forms = Arrays.stream(Transformation.values()).filter(f -> f != Transformation.BASE && f.isAvailableTo(pendingRace)).count();
        g.drawString(font, forms + " transformaciones disponibles", cl + 8, ly + 24, UiTheme.TEXT_DIM, false);
    }

    // ------------------------------------------------------------------ stats tab

    private void renderStats(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int accent, float partial) {
        int cl = contentLeft() + tabSlide(), cw = contentWidth(), t = top();
        int y = t + 52;
        int colW = cw / 3;
        statCard(g, cl, y, colW - 4, "Nivel", String.valueOf(data.getLevel()));
        statCard(g, cl + colW, y, colW - 4, "Poder", String.format("%,d", data.getBattlePower()));
        statCard(g, cl + colW * 2, y, colW - 4, "TP", String.format("%,d", data.getTrainingPoints()));
        y += 28;
        g.drawString(font, "Forma: " + data.getTransformation().getDisplayName(), cl, y, accent & 0xFFFFFF, false);
        g.drawString(font, String.format("Ki %.0f / %.0f", data.getCurrentKi(), data.getMaxKi()), cl + cw - 110, y, UiTheme.TEXT_DIM, false);

        int[] base = baseStats(data);
        int[] eff = {data.getEffectiveStr(), data.getEffectiveDex(), data.getEffectiveCon(), data.getEffectiveWil(), data.getEffectiveMnd()};
        int maxStat = Arrays.stream(base).max().orElse(1);
        int sy = t + 108;
        for (int i = 0; i < 5; i++) {
            shownStats[i] += (base[i] - shownStats[i]) * 0.25f;
            boolean hover = inside(mouseX, mouseY, cl, sy, cw - 28, 22);
            if (hover) g.fill(cl - 4, sy - 1, cl + cw - 28, sy + 21, UiTheme.ROW_HOVER);
            g.drawString(font, STAT_NAMES[i], cl, sy + 2, UiTheme.TEXT, false);
            g.drawString(font, hover ? STAT_HINTS[i] : data.getUpgradeCost(STATS[i]) + " TP", cl + 88, sy + 2, UiTheme.TEXT_DIM, false);
            String value = String.valueOf(Math.round(shownStats[i]));
            if (eff[i] != base[i]) value += "  → " + eff[i];
            g.drawString(font, value, cl + cw - 32 - font.width(value), sy + 2, UiTheme.TEXT_HEADER, false);
            int bw = cw - 32;
            g.fill(cl, sy + 13, cl + bw, sy + 16, UiTheme.TRACK);
            g.fill(cl, sy + 13, cl + (int) (bw * shownStats[i] / Math.max(10f, maxStat * 1.2f)), sy + 16, accent);
            sy += 26;
        }
    }

    private void statCard(GuiGraphics g, int x, int y, int w, String label, String value) {
        g.fill(x, y, x + w, y + 24, UiTheme.PANEL_ALT);
        g.drawString(font, label, x + 6, y + 4, UiTheme.TEXT_DIM, false);
        g.drawString(font, value, x + 6, y + 14, UiTheme.TEXT_HEADER, false);
    }

    // ------------------------------------------------------------------ forms tab

    private List<Transformation> visibleForms() {
        Race race = data().getRace();
        return Arrays.stream(Transformation.values()).filter(f -> f.isAvailableTo(race)).toList();
    }

    private int formsTop() { return top() + 52; }
    private int formsVisibleRows() { return (H - 52 - 22) / (CARD_H + 3); }

    private void renderForms(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int accent) {
        int cl = contentLeft() + tabSlide(), cw = contentWidth(), t = top();
        g.drawString(font, "Mantén [G] para transformarte · [H] para volver", cl, t + H - 14, UiTheme.TEXT_DIM, false);
        List<Transformation> forms = visibleForms();
        int rows = formsVisibleRows();
        formScroll = Mth.clamp(formScroll, 0, Math.max(0, forms.size() - rows));
        int y = formsTop();
        float pulse = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 300f);

        for (int i = formScroll; i < Math.min(forms.size(), formScroll + rows); i++) {
            Transformation f = forms.get(i);
            String lock = data.getLockReason(f);
            boolean active = f == data.getTransformation();
            boolean selected = f == data.getSelectedForm();
            boolean hover = inside(mouseX, mouseY, cl, y, cw - 8, CARD_H);
            int x = cl + (hover ? 2 : 0);
            int color = 0xFF000000 | f.getAuraColor();

            g.fill(x, y, x + cw - 8, y + CARD_H, selected ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            if (selected) g.renderOutline(x, y, cw - 8, CARD_H, (int) (0x60 + 0x9F * pulse) << 24 | (f.getAuraColor()));
            g.fill(x, y, x + 3, y + CARD_H, lock == null ? color : UiTheme.BORDER);
            // Color swatch with the hair color if the form changes it
            int swatch = f.getHairColor() != 0 ? f.getHairColor() : f.getAuraColor();
            g.fill(x + 8, y + 7, x + 24, y + 23, lock == null ? 0xFF000000 | swatch : 0xFF2A2E36);
            g.renderOutline(x + 8, y + 7, 16, 16, 0x40FFFFFF);

            g.drawString(font, f.getDisplayName(), x + 30, y + 5, lock == null ? UiTheme.TEXT_HEADER : UiTheme.TEXT_DIM, false);
            String sub = f == Transformation.BASE ? "x1"
                    : "x" + trim(f.getStatMultiplier()) + "  ·  " + (f.getKiDrainPerSecond() == 0 ? "sin consumo" : trim(f.getKiDrainPerSecond()) + " ki/s");
            g.drawString(font, sub, x + 30, y + 17, UiTheme.TEXT_DIM, false);

            String status = active ? "ACTIVA" : selected ? "ELEGIDA" : lock != null ? "BLOQUEADA" : "";
            int statusColor = active ? accent & 0xFFFFFF : lock != null ? UiTheme.BAD : UiTheme.TEXT;
            g.drawString(font, status, x + cw - 14 - font.width(status), y + 5, statusColor, false);
            if (f != Transformation.BASE) {
                int bx = x + cw - 94, by = y + 19, bw = 80;
                g.fill(bx, by, bx + bw, by + 3, UiTheme.TRACK);
                g.fill(bx, by, bx + (int) (bw * data.getMastery(f) / 100f), by + 3, color);
            }
            y += CARD_H + 3;
        }

        if (forms.size() > rows) {
            int trackH = rows * (CARD_H + 3) - 3;
            int thumbH = Math.max(14, trackH * rows / forms.size());
            int thumbY = formsTop() + (trackH - thumbH) * formScroll / Math.max(1, forms.size() - rows);
            g.fill(cl + cw - 4, formsTop(), cl + cw - 2, formsTop() + trackH, UiTheme.TRACK);
            g.fill(cl + cw - 4, thumbY, cl + cw - 2, thumbY + thumbH, 0xFF5A6070);
        }
    }

    /** Hovered form card: what it does, what it needs. */
    private void renderFormTooltip(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY) {
        Transformation f = formAt(mouseX, mouseY);
        if (f == null) return;
        String lock = data.getLockReason(f);
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.literal(f.getDisplayName()).withColor(f.getAuraColor()));
        lines.add(Component.literal(f.getEssence()).withColor(UiTheme.TEXT));
        if (f != Transformation.BASE) {
            lines.add(Component.literal("Nivel " + f.getMinLevel() + (f.getParent() != Transformation.BASE
                    ? " · domina " + f.getParent().getDisplayName() + " al " + (int) Transformation.UNLOCK_MASTERY + "%" : ""))
                    .withColor(UiTheme.TEXT_DIM));
            lines.add(Component.literal("Maestría " + (int) data.getMastery(f) + "%").withColor(UiTheme.TEXT_DIM));
        }
        if (lock != null) lines.add(Component.literal(lock).withColor(UiTheme.BAD));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private Transformation formAt(double mouseX, double mouseY) {
        List<Transformation> forms = visibleForms();
        int y = formsTop();
        for (int i = formScroll; i < Math.min(forms.size(), formScroll + formsVisibleRows()); i++) {
            if (inside(mouseX, mouseY, contentLeft(), y, contentWidth() - 8, CARD_H)) return forms.get(i);
            y += CARD_H + 3;
        }
        return null;
    }

    private static String trim(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format("%.2f", v).replaceAll("0+$", "");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        PlayerDBCData data = data();

        if (!data.isRaceChosen()) {
            int cw = contentWidth(), cardW = (cw - 6) / 2;
            Race[] races = Race.values();
            for (int i = 0; i < races.length; i++) {
                int x = contentLeft() + (i % 2) * (cardW + 6), cy = top() + 42 + (i / 2) * 26;
                if (inside(mouseX, mouseY, x, cy, cardW, 22)) {
                    pendingRace = races[i];
                    rebuild();
                    return true;
                }
            }
        } else if (tab == Tab.FORMS) {
            Transformation f = formAt(mouseX, mouseY);
            if (f != null) {
                data.setSelectedForm(f); // instant feedback, the server confirms on sync
                ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.SELECT_FORM, f.ordinal()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.FORMS) {
            formScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
