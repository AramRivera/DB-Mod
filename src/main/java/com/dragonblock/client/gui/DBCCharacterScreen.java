package com.dragonblock.client.gui;

import com.dragonblock.client.overlay.TechniqueBar;
import com.dragonblock.common.data.Appearance;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.data.Transformation.HairStyle;
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
 * Character sheet. Before the race is chosen it is the character creator (race, skin, eyes, hair,
 * clothes) with a live 3D preview; afterwards it has Attributes, Forms, Techniques and Appearance tabs.
 * Everything eases in and reacts to hover.
 */
public class DBCCharacterScreen extends Screen {
    private static final int W = 440;
    private static final int H = 262;
    private static final int PREVIEW_W = 130;
    private static final int CARD_H = 30;
    private static final int TECH_H = 26;
    private static final int SWATCH = 13;
    private static final String[] STATS = {"STR", "DEX", "CON", "WIL", "MND"};
    private static final String[] STAT_NAMES = {"Fuerza", "Destreza", "Constitución", "Voluntad", "Mente"};
    private static final String[] STAT_HINTS = {
            "Daño cuerpo a cuerpo", "Velocidad y evasión", "Vida máxima", "Ki máximo y daño de Ki", "TP y maestría"};

    private enum Tab {
        STATS("Atributos"), FORMS("Formas"), TECHNIQUES("Técnicas"), LOOK("Apariencia");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab tab = Tab.STATS;
    private int formScroll = 0;
    private int techScroll = 0;
    private int lastSyncHash = 0;
    private final long openedAt = Util.getMillis();
    private long tabChangedAt = 0;
    private float tabUnderline = 0;
    private final float[] shownStats = new float[5];

    // Creator state; previewed live on the local player's data, restored if you leave without saving
    private Race pendingRace;
    private int skin, eyes, hair, accent;
    private HairStyle hairStyle;
    private int[] savedLook;
    private Race savedRace;
    private HairStyle savedStyle;
    private boolean saved = false;

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
    private boolean creating() { return !data().isRaceChosen(); }
    private boolean editingLook() { return creating() || tab == Tab.LOOK; }

    /** 0..1 ease-out over the first 220 ms after opening. */
    private float openProgress() {
        float t = Mth.clamp((Util.getMillis() - openedAt) / 220f, 0f, 1f);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    @Override
    protected void init() {
        PlayerDBCData d = data();
        int[] base = baseStats(d);
        for (int i = 0; i < 5; i++) shownStats[i] = base[i];
        if (savedLook == null) {
            savedRace = d.getRace();
            savedStyle = d.getHairStyle();
            savedLook = new int[]{d.getSkinColor(), d.getEyeColor(), d.getHairColor(), d.getAccentColor()};
            loadLookFrom(d);
        }
        rebuild();
    }

    private void loadLookFrom(PlayerDBCData d) {
        pendingRace = d.getRace();
        skin = d.getSkinColor();
        eyes = d.getEyeColor();
        hair = d.getHairColor();
        accent = d.getAccentColor();
        hairStyle = d.getHairStyle();
    }

    /** Shows the pending look on the local player so the preview (and the world) updates as you click. */
    private void previewLook() {
        PlayerDBCData d = data();
        if (creating()) d.setRace(pendingRace);
        d.setLook(skin, eyes, hair, accent, hairStyle);
    }

    private void pickRace(Race race) {
        pendingRace = race;
        skin = Appearance.defaultSkin(race);
        eyes = Appearance.defaultEyes(race);
        hair = Appearance.defaultHair(race);
        accent = Appearance.defaultAccent(race);
        hairStyle = Appearance.defaultHairStyle(race);
        previewLook();
        rebuild();
    }

    private static int[] baseStats(PlayerDBCData d) {
        return new int[]{d.getStr(), d.getDex(), d.getCon(), d.getWil(), d.getMnd()};
    }

    private void rebuild() {
        clearWidgets();
        if (minecraft == null || minecraft.player == null) return;
        PlayerDBCData data = data();
        int accentRgb = UiTheme.ORANGE;
        int cl = contentLeft(), t = top(), cw = contentWidth();

        if (editingLook()) {
            addRenderableWidget(new FlatButton(cl + cw - 120, t + H - 26, 120, 18,
                    Component.literal(creating() ? "Crear personaje" : "Guardar apariencia"), accentRgb, b -> {
                ModMessages.sendToServer(new C2SSelectRacePacket(pendingRace.name(), skin, eyes, hair, accent, hairStyle.name()));
                saved = true;
                if (creating()) onClose();
            }));
            if (creating()) return;
        }

        int tx = cl;
        for (Tab each : Tab.values()) {
            addRenderableWidget(new FlatButton(tx, t + 26, 70, 16, Component.literal(each.label), accentRgb, b -> switchTab(each)));
            tx += 74;
        }

        if (tab == Tab.STATS) {
            int y = t + 108;
            for (String stat : STATS) {
                FlatButton plus = new FlatButton(cl + cw - 24, y + 1, 24, 18, Component.literal("+"), accentRgb,
                        b -> ModMessages.sendToServer(new C2SUpgradeStatPacket(stat)));
                plus.active = data.getTrainingPoints() >= data.getUpgradeCost(stat);
                addRenderableWidget(plus);
                y += 26;
            }
        }
        if (data.getRace().hasSaiyanTail()) {
            addRenderableWidget(new FlatButton(left() + 10, t + H - 26, PREVIEW_W - 20, 16,
                    Component.literal(data.isTailOut() ? "Enrollar cola" : "Soltar cola"), accentRgb,
                    b -> ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.TOGGLE_TAIL, 0))));
        }
    }

    private void switchTab(Tab next) {
        if (tab == next) return;
        if (tab == Tab.LOOK && !saved) restoreLook();
        tab = next;
        saved = false;
        tabChangedAt = Util.getMillis();
        if (tab == Tab.LOOK) loadLookFrom(data());
        rebuild();
    }

    private void restoreLook() {
        PlayerDBCData d = data();
        if (!d.isRaceChosen()) d.setRace(savedRace);
        d.setLook(savedLook[0], savedLook[1], savedLook[2], savedLook[3], savedStyle);
    }

    @Override
    public void removed() {
        // Leaving without saving puts the old look back (the server never heard of the preview)
        if (!saved && (creating() || tab == Tab.LOOK)) restoreLook();
        super.removed();
    }

    // Rebuild when the server syncs (TP spent, form changed...) so enabled states stay correct
    @Override
    public void tick() {
        PlayerDBCData d = data();
        int hash = d.getTrainingPoints() * 31 + d.getLevel() * 7 + d.getTransformation().ordinal()
                + (d.isTailOut() ? 1 : 0) + (d.isRaceChosen() ? 2 : 0) + d.getEquipped().hashCode();
        if (hash != lastSyncHash) {
            lastSyncHash = hash;
            rebuild();
        }
        if (editingLook()) previewLook(); // a server sync may have overwritten the preview
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
        int accent = 0xFF000000 | UiTheme.ORANGE;

        // Panel
        g.fill(l, t, l + W, t + H, UiTheme.PANEL);
        g.renderOutline(l, t, W, H, UiTheme.BORDER);
        UiTheme.header(g, l, t, W, openProgress());
        String title = creating() ? "CREACIÓN DE PERSONAJE" : "FICHA DE PERSONAJE";
        g.drawString(font, title, l + 24, t + 9, UiTheme.TEXT_HEADER, false);
        String name = minecraft.player.getGameProfile().getName();
        g.drawString(font, name, l + W - 10 - font.width(name), t + 9, UiTheme.GOLD, false);

        renderPreview(g, data, mouseX, mouseY, l, t, accent);

        if (creating()) {
            renderCreator(g, mouseX, mouseY, contentLeft(), t + 26);
        } else {
            renderTabUnderline(g, accent);
            switch (tab) {
                case STATS -> renderStats(g, data, mouseX, mouseY, accent);
                case FORMS -> renderForms(g, data, mouseX, mouseY, accent);
                case TECHNIQUES -> renderTechniques(g, data, mouseX, mouseY);
                case LOOK -> renderLookEditor(g, mouseX, mouseY, contentLeft() + tabSlide(), t + 54);
            }
        }

        for (Renderable r : renderables) r.render(g, mouseX, mouseY, partialTick);
        if (!creating() && tab == Tab.FORMS) renderFormTooltip(g, data, mouseX, mouseY);
        if (!creating() && tab == Tab.TECHNIQUES) renderTechTooltip(g, data, mouseX, mouseY);
    }

    // ------------------------------------------------------------------ 3D preview

    private void renderPreview(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int l, int t, int accent) {
        int px = l + 8, py = t + 24, pw = PREVIEW_W - 6, ph = H - 56;
        g.fill(px, py, px + pw, py + ph, UiTheme.PANEL_ALT);
        // Soft orange floor glow under the character
        for (int i = 0; i < 6; i++) {
            int a = 0x20 - i * 4;
            g.fill(px + 10 + i * 4, py + ph - 14 + i, px + pw - 10 - i * 4, py + ph - 13 + i, a << 24 | (accent & 0xFFFFFF));
        }
        g.enableScissor(px, py, px + pw, py + ph);
        int size = (int) (40 / Math.max(1f, minecraft.player.getScale()));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph - 8, size, 0.0625f,
                mouseX, mouseY, minecraft.player);
        g.disableScissor();

        String race = (creating() ? pendingRace : data.getRace()).getDisplayName();
        g.drawCenteredString(font, race, px + pw / 2, py + ph + 4, UiTheme.TEXT_DIM);
    }

    private void renderTabUnderline(GuiGraphics g, int accent) {
        float target = tab.ordinal() * 74;
        tabUnderline += (target - tabUnderline) * 0.3f;
        int x = contentLeft() + (int) tabUnderline;
        g.fill(x, top() + 43, x + 70, top() + 45, accent);
    }

    /** Content slides in briefly after switching tabs. */
    private int tabSlide() {
        float t = Mth.clamp((Util.getMillis() - tabChangedAt) / 160f, 0f, 1f);
        return (int) (10 * (1 - t) * (1 - t));
    }

    // ------------------------------------------------------------------ character creator

    private int raceCardsBottom(int y) { return y + 3 * 22; }

    private void renderCreator(GuiGraphics g, int mouseX, int mouseY, int cl, int y) {
        int cw = contentWidth();
        int cardW = (cw - 6) / 2;
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            Race race = races[i];
            int x = cl + (i % 2) * (cardW + 6), cy = y + (i / 2) * 22;
            boolean selected = race == pendingRace;
            boolean hover = inside(mouseX, mouseY, x, cy, cardW, 19);
            g.fill(x, cy, x + cardW, cy + 19, selected ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            g.fill(x, cy, x + (selected ? 4 : 2), cy + 19, 0xFF000000 | (selected ? UiTheme.ORANGE : race.getFeatureColor() | 0x303030));
            g.drawString(font, race.getDisplayName(), x + 10, cy + 6, selected ? UiTheme.TEXT_HEADER : UiTheme.TEXT, false);
        }
        int ly = raceCardsBottom(y) + 2;
        for (FormattedCharSequence line : font.split(Component.literal(pendingRace.getDescription()), cw)) {
            g.drawString(font, line, cl, ly, UiTheme.TEXT_DIM, false);
            ly += 9;
        }
        renderLookEditor(g, mouseX, mouseY, cl, ly + 4);
    }

    /** Rows of swatches; returns nothing, clicks are resolved by {@link #lookClick}. */
    private void renderLookEditor(GuiGraphics g, int mouseX, int mouseY, int cl, int y) {
        for (LookRow row : lookRows(cl, y)) {
            g.drawString(font, row.label, cl, row.y + 3, UiTheme.TEXT, false);
            if (row.styles) {
                String text = "‹  " + Appearance.hairStyleName(hairStyle) + "  ›";
                boolean hover = inside(mouseX, mouseY, row.x, row.y, 110, SWATCH);
                g.fill(row.x, row.y, row.x + 110, row.y + SWATCH, hover ? UiTheme.BUTTON_HOVER : UiTheme.BUTTON);
                g.drawCenteredString(font, text, row.x + 55, row.y + 3, UiTheme.TEXT_HEADER);
                continue;
            }
            for (int i = 0; i < row.palette.length; i++) {
                int sx = row.x + i * (SWATCH + 3);
                boolean chosen = row.palette[i] == row.current;
                boolean hover = inside(mouseX, mouseY, sx, row.y, SWATCH, SWATCH);
                g.fill(sx - 1, row.y - 1, sx + SWATCH + 1, row.y + SWATCH + 1,
                        chosen ? 0xFF000000 | UiTheme.GOLD : hover ? 0xFFFFFFFF : UiTheme.BORDER);
                g.fill(sx, row.y, sx + SWATCH, row.y + SWATCH, 0xFF000000 | row.palette[i]);
            }
        }
    }

    private record LookRow(String label, int x, int y, int[] palette, int current, int kind, boolean styles) {}

    private List<LookRow> lookRows(int cl, int y) {
        int x = cl + 70;
        List<LookRow> rows = new java.util.ArrayList<>();
        rows.add(new LookRow(skinLabel(), x, y, Appearance.skinPalette(pendingRace), skin, 0, false));
        y += SWATCH + 5;
        rows.add(new LookRow("Ojos", x, y, Appearance.EYES, eyes, 1, false));
        y += SWATCH + 5;
        if (Appearance.hasHair(pendingRace)) {
            rows.add(new LookRow("Peinado", x, y, null, 0, 2, true));
            y += SWATCH + 5;
            rows.add(new LookRow("Color de pelo", x, y, Appearance.HAIR, hair, 3, false));
            y += SWATCH + 5;
        }
        rows.add(new LookRow(accentLabel(), x, y, Appearance.accentPalette(pendingRace), accent, 4, false));
        return rows;
    }

    private String skinLabel() {
        return pendingRace == Race.ARCOSIAN ? "Coraza" : "Piel";
    }

    private String accentLabel() {
        return switch (pendingRace) {
            case SAIYAN -> "Shorts";
            case NAMEKIAN -> "Manchas";
            case ARCOSIAN -> "Gemas";
            case MAJIN -> "Pantalón";
            default -> "Gi";
        };
    }

    private boolean lookClick(double mouseX, double mouseY, int cl, int y) {
        for (LookRow row : lookRows(cl, y)) {
            if (row.styles) {
                if (!inside(mouseX, mouseY, row.x, row.y, 110, SWATCH)) continue;
                List<HairStyle> styles = Appearance.hairStyles(pendingRace);
                int step = mouseX < row.x + 55 ? -1 : 1;
                hairStyle = styles.get(Math.floorMod(styles.indexOf(hairStyle) + step, styles.size()));
                previewLook();
                return true;
            }
            for (int i = 0; i < row.palette.length; i++) {
                if (!inside(mouseX, mouseY, row.x + i * (SWATCH + 3), row.y, SWATCH, SWATCH)) continue;
                int c = row.palette[i];
                switch (row.kind) {
                    case 0 -> skin = c;
                    case 1 -> eyes = c;
                    case 3 -> hair = c;
                    default -> accent = c;
                }
                previewLook();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ stats tab

    private void renderStats(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int accent) {
        int cl = contentLeft() + tabSlide(), cw = contentWidth(), t = top();
        int y = t + 52;
        int colW = cw / 3;
        statCard(g, cl, y, colW - 4, "Nivel", String.valueOf(data.getLevel()));
        statCard(g, cl + colW, y, colW - 4, "Poder", String.format("%,d", data.getBattlePower()));
        statCard(g, cl + colW * 2, y, colW - 4, "TP", String.format("%,d", data.getTrainingPoints()));
        y += 28;
        g.drawString(font, "Forma: " + data.getTransformation().getDisplayName(), cl, y, data.getTransformation().getAuraColor(), false);
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
            g.fillGradient(cl, sy + 13, cl + (int) (bw * shownStats[i] / Math.max(10f, maxStat * 1.2f)), sy + 16,
                    0xFF000000 | UiTheme.GOLD, accent);
            sy += 26;
        }
    }

    private void statCard(GuiGraphics g, int x, int y, int w, String label, String value) {
        g.fill(x, y, x + w, y + 24, UiTheme.PANEL_ALT);
        g.fill(x, y, x + 2, y + 24, 0xFF000000 | UiTheme.ORANGE);
        g.drawString(font, label, x + 6, y + 4, UiTheme.TEXT_DIM, false);
        g.drawString(font, value, x + 6, y + 14, UiTheme.TEXT_HEADER, false);
    }

    // ------------------------------------------------------------------ forms tab

    private List<Transformation> visibleForms() {
        Race race = data().getRace();
        return Arrays.stream(Transformation.values()).filter(f -> f.isAvailableTo(race)).toList();
    }

    private int listTop() { return top() + 52; }
    private int formsVisibleRows() { return (H - 52 - 22) / (CARD_H + 3); }

    private void renderForms(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int accent) {
        int cl = contentLeft() + tabSlide(), cw = contentWidth(), t = top();
        g.drawString(font, "Mantén [G] para transformarte · [H] para volver", cl, t + H - 14, UiTheme.TEXT_DIM, false);
        List<Transformation> forms = visibleForms();
        int rows = formsVisibleRows();
        formScroll = Mth.clamp(formScroll, 0, Math.max(0, forms.size() - rows));
        int y = listTop();
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
            int swatch = f.getHairColor() != 0 ? f.getHairColor() : f.getAuraColor();
            g.fill(x + 8, y + 7, x + 24, y + 23, lock == null ? 0xFF000000 | swatch : 0xFF2A2E36);
            g.renderOutline(x + 8, y + 7, 16, 16, 0x40FFFFFF);

            g.drawString(font, f.getDisplayName(), x + 30, y + 5, lock == null ? UiTheme.TEXT_HEADER : UiTheme.TEXT_DIM, false);
            String sub = f == Transformation.BASE ? "x1"
                    : "x" + trim(f.getStatMultiplier()) + "  ·  " + (f.getKiDrainPerSecond() == 0 ? "sin consumo" : trim(f.getKiDrainPerSecond()) + " ki/s");
            g.drawString(font, sub, x + 30, y + 17, UiTheme.TEXT_DIM, false);

            String status = active ? "ACTIVA" : selected ? "ELEGIDA" : lock != null ? "BLOQUEADA" : "";
            int statusColor = active ? UiTheme.GOLD : lock != null ? UiTheme.BAD : UiTheme.TEXT;
            g.drawString(font, status, x + cw - 14 - font.width(status), y + 5, statusColor, false);
            if (f != Transformation.BASE) {
                int bx = x + cw - 94, by = y + 19, bw = 80;
                g.fill(bx, by, bx + bw, by + 3, UiTheme.TRACK);
                g.fill(bx, by, bx + (int) (bw * data.getMastery(f) / 100f), by + 3, color);
            }
            y += CARD_H + 3;
        }
        scrollbar(g, cl + cw - 4, forms.size(), rows, formScroll, CARD_H + 3);
    }

    private void scrollbar(GuiGraphics g, int x, int total, int rows, int scroll, int rowH) {
        if (total <= rows) return;
        int trackH = rows * rowH - 3;
        int thumbH = Math.max(14, trackH * rows / total);
        int thumbY = listTop() + (trackH - thumbH) * scroll / Math.max(1, total - rows);
        g.fill(x, listTop(), x + 2, listTop() + trackH, UiTheme.TRACK);
        g.fill(x, thumbY, x + 2, thumbY + thumbH, 0xFF000000 | UiTheme.ORANGE);
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
        int y = listTop();
        for (int i = formScroll; i < Math.min(forms.size(), formScroll + formsVisibleRows()); i++) {
            if (inside(mouseX, mouseY, contentLeft(), y, contentWidth() - 8, CARD_H)) return forms.get(i);
            y += CARD_H + 3;
        }
        return null;
    }

    // ------------------------------------------------------------------ techniques tab

    private int techVisibleRows() { return (H - 52 - 22) / (TECH_H + 3); }

    private void renderTechniques(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY) {
        int cl = contentLeft() + tabSlide(), cw = contentWidth(), t = top();
        g.drawString(font, "Equipadas " + data.getEquipped().size() + "/" + Technique.MAX_EQUIPPED
                + " · Ctrl + rueda para elegir · [X] usar", cl, t + H - 14, UiTheme.TEXT_DIM, false);
        Technique[] all = Technique.values();
        int rows = techVisibleRows();
        techScroll = Mth.clamp(techScroll, 0, Math.max(0, all.length - rows));
        int y = listTop();
        for (int i = techScroll; i < Math.min(all.length, techScroll + rows); i++) {
            Technique tech = all[i];
            boolean learned = data.hasLearned(tech);
            boolean equipped = data.getEquipped().contains(tech);
            boolean hover = inside(mouseX, mouseY, cl, y, cw - 8, TECH_H);
            int x = cl + (hover ? 2 : 0);
            int color = tech.colorFor(data.getTransformation());

            g.fill(x, y, x + cw - 8, y + TECH_H, equipped ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            g.fill(x, y, x + 3, y + TECH_H, 0xFF000000 | (equipped ? UiTheme.ORANGE : learned ? color : 0x2A3040));
            g.fill(x + 7, y + 4, x + 25, y + 22, 0xFF0A0E1A);
            TechniqueBar.icon(g, tech, x + 8, y + 5);
            if (!learned) g.fill(x + 7, y + 4, x + 25, y + 22, 0x90000000);

            g.drawString(font, tech.getDisplayName(), x + 31, y + 4, learned ? UiTheme.TEXT_HEADER : UiTheme.TEXT, false);
            String sub = kindLabel(tech) + " · " + (int) tech.getKiCost() + " ki";
            g.drawString(font, sub, x + 31, y + 15, UiTheme.TEXT_DIM, false);

            String status;
            int statusColor;
            if (equipped) { status = "EQUIPADA"; statusColor = UiTheme.ORANGE; }
            else if (learned) { status = "Equipar"; statusColor = UiTheme.TEXT; }
            else if (data.getLevel() < tech.getMinLevel()) { status = "Nivel " + tech.getMinLevel(); statusColor = UiTheme.BAD; }
            else {
                status = "Aprender · " + tech.getTpCost() + " TP";
                statusColor = data.getTrainingPoints() >= tech.getTpCost() ? UiTheme.GOOD : UiTheme.BAD;
            }
            g.drawString(font, status, x + cw - 14 - font.width(status), y + 9, statusColor, false);
            y += TECH_H + 3;
        }
        scrollbar(g, cl + cw - 4, all.length, rows, techScroll, TECH_H + 3);
    }

    private static String kindLabel(Technique tech) {
        return switch (tech.getKind()) {
            case BLAST -> "Ráfaga";
            case BEAM -> "Onda · " + trim(tech.getChargeTicks() / 20f) + " s";
            case PIERCE -> "Perforante";
            case DISC -> "Disco cortante";
            case BOMB -> "Esfera colosal";
            case BALL -> "Esfera";
            case SHOCKWAVE -> "Onda expansiva";
            case FLASH -> "Ceguera";
            case TELEPORT -> "Movimiento";
        };
    }

    private Technique techAt(double mouseX, double mouseY) {
        Technique[] all = Technique.values();
        int y = listTop();
        for (int i = techScroll; i < Math.min(all.length, techScroll + techVisibleRows()); i++) {
            if (inside(mouseX, mouseY, contentLeft(), y, contentWidth() - 8, TECH_H)) return all[i];
            y += TECH_H + 3;
        }
        return null;
    }

    private void renderTechTooltip(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY) {
        Technique tech = techAt(mouseX, mouseY);
        if (tech == null) return;
        List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.literal(tech.getDisplayName()).withColor(tech.colorFor(data.getTransformation())));
        lines.add(Component.literal(tech.getDescription()).withColor(UiTheme.TEXT));
        String charge = tech.isInstant() ? "instantánea" : "carga " + trim(tech.getChargeTicks() / 20f) + " s";
        lines.add(Component.literal((tech.getDamage() > 0 ? "Daño x" + trim(tech.getDamage()) + " · " : "") + charge
                + " · recarga " + trim(tech.getCooldown() / 20f) + " s").withColor(UiTheme.TEXT_DIM));
        if (!data.hasLearned(tech)) {
            lines.add(Component.literal("Requiere nivel " + tech.getMinLevel() + " y " + tech.getTpCost() + " TP").withColor(UiTheme.GOLD));
        }
        g.renderTooltip(font, lines.stream().flatMap(c -> font.split(c, 220).stream()).toList(), mouseX, mouseY);
    }

    private static String trim(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format("%.2f", v).replaceAll("0+$", "");
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        PlayerDBCData data = data();

        if (creating()) {
            int cw = contentWidth(), cardW = (cw - 6) / 2;
            Race[] races = Race.values();
            int y = top() + 26;
            for (int i = 0; i < races.length; i++) {
                int x = contentLeft() + (i % 2) * (cardW + 6), cy = y + (i / 2) * 22;
                if (inside(mouseX, mouseY, x, cy, cardW, 19)) {
                    pickRace(races[i]);
                    return true;
                }
            }
            int ly = raceCardsBottom(y) + 2 + font.split(Component.literal(pendingRace.getDescription()), cw).size() * 9;
            return lookClick(mouseX, mouseY, contentLeft(), ly + 4);
        }
        switch (tab) {
            case FORMS -> {
                Transformation f = formAt(mouseX, mouseY);
                if (f != null) {
                    data.setSelectedForm(f); // instant feedback, the server confirms on sync
                    ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.SELECT_FORM, f.ordinal()));
                    return true;
                }
            }
            case TECHNIQUES -> {
                Technique tech = techAt(mouseX, mouseY);
                if (tech != null) {
                    C2SActionPacket.Action action = data.hasLearned(tech) ? C2SActionPacket.Action.TOGGLE_EQUIP : C2SActionPacket.Action.LEARN_TECHNIQUE;
                    ModMessages.sendToServer(new C2SActionPacket(action, tech.ordinal()));
                    return true;
                }
            }
            case LOOK -> {
                return lookClick(mouseX, mouseY, contentLeft(), top() + 54);
            }
            default -> {}
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!creating() && tab == Tab.FORMS) {
            formScroll -= (int) Math.signum(scrollY);
            return true;
        }
        if (!creating() && tab == Tab.TECHNIQUES) {
            techScroll -= (int) Math.signum(scrollY);
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
