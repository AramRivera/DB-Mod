package com.dragonblock.client.gui;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SActionPacket;
import com.dragonblock.common.network.packet.C2SSelectRacePacket;
import com.dragonblock.common.network.packet.C2SUpgradeStatPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/** Character sheet: race selection on first open, then Attributes / Forms tabs. */
public class DBCCharacterScreen extends Screen {
    private static final int W = 330;
    private static final int H = 230;
    private static final int ROW_H = 22;
    private static final String[] STATS = {"STR", "DEX", "CON", "WIL", "MND"};
    private static final String[] STAT_NAMES = {"Fuerza", "Destreza", "Constitución", "Voluntad", "Mente"};
    private static final String[] STAT_HINTS = {
            "Daño cuerpo a cuerpo", "Velocidad y evasión", "Salud máxima", "Ki máximo y daño de ki", "Ganancia de TP y maestría"};

    private enum Tab { STATS, FORMS }

    private Tab tab = Tab.STATS;
    private Race pendingRace = Race.HUMAN;
    private int formScroll = 0;
    private int lastSyncHash = 0;

    public DBCCharacterScreen() {
        super(Component.literal("Ficha de personaje"));
    }

    private PlayerDBCData data() {
        return PlayerDBCData.get(Minecraft.getInstance().player);
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        if (minecraft == null || minecraft.player == null) return;
        PlayerDBCData data = data();
        int accent = data.getTransformation().getAuraColor();
        int l = left(), t = top();

        if (!data.isRaceChosen()) {
            addRenderableWidget(new FlatButton(l + W - 108, t + H - 26, 96, 16,
                    Component.literal("Confirmar raza"), accent, b -> {
                ModMessages.sendToServer(new C2SSelectRacePacket(pendingRace.name()));
                onClose();
            }));
            return;
        }

        addRenderableWidget(new FlatButton(l + 10, t + 24, 80, 14, Component.literal("Atributos"), accent,
                b -> { tab = Tab.STATS; rebuild(); })).active = tab != Tab.STATS;
        addRenderableWidget(new FlatButton(l + 94, t + 24, 80, 14, Component.literal("Formas"), accent,
                b -> { tab = Tab.FORMS; rebuild(); })).active = tab != Tab.FORMS;

        if (tab == Tab.STATS) {
            int y = t + 104;
            for (String stat : STATS) {
                FlatButton plus = new FlatButton(l + W - 34, y, 22, 14, Component.literal("+"), accent,
                        b -> ModMessages.sendToServer(new C2SUpgradeStatPacket(stat)));
                plus.active = data.getTrainingPoints() >= data.getUpgradeCost(stat);
                addRenderableWidget(plus);
                y += 20;
            }
            if (data.getRace().hasSaiyanTail()) {
                addRenderableWidget(new FlatButton(l + W - 108, t + 24, 96, 14,
                        Component.literal(data.isTailOut() ? "Enrollar cola" : "Soltar cola"), accent,
                        b -> ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.TOGGLE_TAIL, 0))));
            }
        }
    }

    // Rebuild buttons when the server syncs (TP spent, form changed...), so enabled states stay correct
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
        g.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        if (minecraft == null || minecraft.player == null) return;
        PlayerDBCData data = data();
        int l = left(), t = top();
        int accent = 0xFF000000 | data.getTransformation().getAuraColor();

        g.fill(l, t, l + W, t + H, UiTheme.PANEL);
        g.renderOutline(l, t, W, H, UiTheme.BORDER);
        g.fill(l, t, l + W, t + 2, accent);
        g.drawString(font, title.getString().toUpperCase(), l + 10, t + 9, UiTheme.TEXT_HEADER, false);
        String name = minecraft.player.getGameProfile().getName();
        g.drawString(font, name, l + W - 10 - font.width(name), t + 9, UiTheme.TEXT_DIM, false);

        if (!data.isRaceChosen()) {
            renderRaceSelect(g, mouseX, mouseY, l, t);
        } else if (tab == Tab.STATS) {
            renderStats(g, data, l, t, accent);
        } else {
            renderForms(g, data, mouseX, mouseY, l, t, accent);
        }

        for (Renderable r : renderables) r.render(g, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------------ race selection

    private void renderRaceSelect(GuiGraphics g, int mouseX, int mouseY, int l, int t) {
        g.drawString(font, "Elige tu raza. La elección es permanente.", l + 10, t + 26, UiTheme.TEXT_DIM, false);
        int y = t + 42;
        for (Race race : Race.values()) {
            boolean selected = race == pendingRace;
            boolean hover = inside(mouseX, mouseY, l + 10, y, 120, ROW_H - 2);
            g.fill(l + 10, y, l + 130, y + ROW_H - 2, selected ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            if (selected) g.fill(l + 10, y, l + 12, y + ROW_H - 2, 0xFF000000 | race.getFeatureColor() | 0x404040);
            g.drawString(font, race.getDisplayName(), l + 18, y + 6, selected ? UiTheme.TEXT_HEADER : UiTheme.TEXT, false);
            y += ROW_H;
        }

        int px = l + 140, py = t + 42, pw = W - 150;
        g.fill(px, py, px + pw, py + 6 * ROW_H - 2, UiTheme.PANEL_ALT);
        g.drawString(font, pendingRace.getDisplayName().toUpperCase(), px + 8, py + 8, UiTheme.TEXT_HEADER, false);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(pendingRace.getDescription()), pw - 16);
        int ly = py + 22;
        for (var line : lines) {
            g.drawString(font, line, px + 8, ly, UiTheme.TEXT, false);
            ly += 10;
        }
        ly += 6;
        float[] mods = {pendingRace.getStrModifier(), pendingRace.getDexModifier(), pendingRace.getConModifier(),
                pendingRace.getWilModifier(), pendingRace.getMndModifier()};
        for (int i = 0; i < STATS.length; i++) {
            g.drawString(font, STAT_NAMES[i], px + 8, ly, UiTheme.TEXT_DIM, false);
            String m = "x" + String.format("%.1f", mods[i]);
            int c = mods[i] > 1f ? UiTheme.GOOD : mods[i] < 1f ? UiTheme.BAD : UiTheme.TEXT;
            g.drawString(font, m, px + pw - 8 - font.width(m), ly, c, false);
            ly += 11;
        }
        long forms = Arrays.stream(Transformation.values()).filter(f -> f != Transformation.BASE && f.isAvailableTo(pendingRace)).count();
        g.drawString(font, forms + " transformaciones disponibles", px + 8, ly + 4, UiTheme.TEXT_DIM, false);
    }

    // ------------------------------------------------------------------ stats tab

    private void renderStats(GuiGraphics g, PlayerDBCData data, int l, int t, int accent) {
        int y = t + 46;
        infoRow(g, l + 10, y, "Raza", data.getRace().getDisplayName());
        infoRow(g, l + 170, y, "Nivel", String.valueOf(data.getLevel()));
        y += 13;
        infoRow(g, l + 10, y, "Forma", data.getTransformation().getDisplayName());
        infoRow(g, l + 170, y, "Poder", String.format("%,d", data.getBattlePower()));
        y += 13;
        infoRow(g, l + 10, y, "Ki", String.format("%.0f / %.0f", data.getCurrentKi(), data.getMaxKi()));
        infoRow(g, l + 170, y, "TP", String.format("%,d", data.getTrainingPoints()));

        g.fill(l + 10, t + 96, l + W - 10, t + 97, UiTheme.BORDER);

        int[] base = {data.getStr(), data.getDex(), data.getCon(), data.getWil(), data.getMnd()};
        int[] eff = {data.getEffectiveStr(), data.getEffectiveDex(), data.getEffectiveCon(), data.getEffectiveWil(), data.getEffectiveMnd()};
        int sy = t + 104;
        for (int i = 0; i < STATS.length; i++) {
            g.drawString(font, STAT_NAMES[i], l + 10, sy + 3, UiTheme.TEXT, false);
            g.drawString(font, STAT_HINTS[i], l + 88, sy + 3, UiTheme.TEXT_DIM, false);
            String value = String.valueOf(base[i]);
            g.drawString(font, value, l + 222 - font.width(value), sy + 3, UiTheme.TEXT_HEADER, false);
            if (eff[i] != base[i]) g.drawString(font, "→ " + eff[i], l + 226, sy + 3, accent & 0xFFFFFF, false);
            String cost = data.getUpgradeCost(STATS[i]) + " TP";
            g.drawString(font, cost, l + W - 40 - font.width(cost), sy + 3, UiTheme.TEXT_DIM, false);
            sy += 20;
        }
    }

    private void infoRow(GuiGraphics g, int x, int y, String label, String value) {
        g.drawString(font, label, x, y, UiTheme.TEXT_DIM, false);
        g.drawString(font, value, x + 42, y, UiTheme.TEXT_HEADER, false);
    }

    // ------------------------------------------------------------------ forms tab

    private List<Transformation> visibleForms() {
        Race race = data().getRace();
        return Arrays.stream(Transformation.values()).filter(f -> f.isAvailableTo(race)).toList();
    }

    private int formsTop() { return top() + 44; }
    private int formsVisibleRows() { return (H - 54) / ROW_H; }

    private void renderForms(GuiGraphics g, PlayerDBCData data, int mouseX, int mouseY, int l, int t, int accent) {
        g.drawString(font, "Selecciona una forma. Mantén [G] para transformarte, [H] para volver.",
                l + 10, t + H - 12, UiTheme.TEXT_DIM, false);
        List<Transformation> forms = visibleForms();
        int rows = formsVisibleRows();
        formScroll = Math.max(0, Math.min(formScroll, forms.size() - rows));
        int y = formsTop();

        for (int i = formScroll; i < Math.min(forms.size(), formScroll + rows); i++) {
            Transformation f = forms.get(i);
            String lock = data.getLockReason(f);
            boolean active = f == data.getTransformation();
            boolean selected = f == data.getSelectedForm();
            boolean hover = inside(mouseX, mouseY, l + 10, y, W - 20, ROW_H - 2);

            g.fill(l + 10, y, l + W - 10, y + ROW_H - 2, selected ? UiTheme.BUTTON_HOVER : hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_ALT);
            g.fill(l + 10, y, l + 12, y + ROW_H - 2, lock == null ? 0xFF000000 | f.getAuraColor() : UiTheme.BORDER);

            g.drawString(font, f.getDisplayName(), l + 18, y + 3, lock == null ? UiTheme.TEXT_HEADER : UiTheme.TEXT_DIM, false);
            String sub = f == Transformation.BASE ? "Sin multiplicador"
                    : "x" + trim(f.getStatMultiplier()) + "   " + (f.getKiDrainPerSecond() == 0 ? "sin consumo" : trim(f.getKiDrainPerSecond()) + " ki/s")
                    + "   nivel " + f.getMinLevel();
            g.drawString(font, sub, l + 18, y + 12, UiTheme.TEXT_DIM, false);

            String status = active ? "ACTIVA" : selected ? "SELECCIONADA" : lock != null ? lock : "";
            int statusColor = active ? accent & 0xFFFFFF : lock != null && !selected ? UiTheme.BAD : UiTheme.TEXT;
            g.drawString(font, status, l + W - 16 - font.width(status), y + 3, statusColor, false);

            if (f != Transformation.BASE) {
                int bx = l + W - 96, by = y + 14, bw = 80;
                float m = data.getMastery(f) / 100f;
                g.fill(bx, by, bx + bw, by + 3, UiTheme.TRACK);
                g.fill(bx, by, bx + (int) (bw * m), by + 3, 0xFF000000 | f.getAuraColor());
            }
            y += ROW_H;
        }

        // Scrollbar
        if (forms.size() > rows) {
            int trackH = rows * ROW_H - 2;
            int thumbH = Math.max(12, trackH * rows / forms.size());
            int thumbY = formsTop() + (trackH - thumbH) * formScroll / (forms.size() - rows);
            g.fill(l + W - 7, formsTop(), l + W - 5, formsTop() + trackH, UiTheme.TRACK);
            g.fill(l + W - 7, thumbY, l + W - 5, thumbY + thumbH, UiTheme.BORDER | 0xFF606060);
        }
    }

    private static String trim(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format("%.2f", v).replaceAll("0+$", "");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        PlayerDBCData data = data();
        int l = left(), t = top();

        if (!data.isRaceChosen()) {
            int y = t + 42;
            for (Race race : Race.values()) {
                if (inside(mouseX, mouseY, l + 10, y, 120, ROW_H - 2)) {
                    pendingRace = race;
                    return true;
                }
                y += ROW_H;
            }
        } else if (tab == Tab.FORMS) {
            List<Transformation> forms = visibleForms();
            int y = formsTop();
            for (int i = formScroll; i < Math.min(forms.size(), formScroll + formsVisibleRows()); i++) {
                if (inside(mouseX, mouseY, l + 10, y, W - 20, ROW_H - 2)) {
                    Transformation f = forms.get(i);
                    data.setSelectedForm(f); // instant feedback, the server confirms on sync
                    ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.SELECT_FORM, f.ordinal()));
                    return true;
                }
                y += ROW_H;
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
