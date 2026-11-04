package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.FormController;
import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** One-shot actions. {@code arg}: form ordinal, technique ordinal or bar slot, depending on the action. */
public class C2SActionPacket {
    public enum Action { REVERT, DASH, SELECT_FORM, TOGGLE_TAIL, SELECT_TECHNIQUE, LEARN_TECHNIQUE, TOGGLE_EQUIP, OPEN_ACCESSORIES, LIGHT, HEAVY }

    private final Action action;
    private final int arg;

    public C2SActionPacket(Action action, int arg) {
        this.action = action;
        this.arg = arg;
    }

    public C2SActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.arg = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(arg);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PlayerDBCData data = PlayerDBCData.get(player);

            switch (action) {
                case LIGHT, HEAVY -> {
                    com.dragonblock.common.combat.Melee.request(player, data, action == Action.HEAVY);
                    return; // the move syncs itself through the animation packet
                }
                case REVERT -> FormController.revert(player, data);
                case DASH -> KiAttacks.dash(player, data);
                case SELECT_FORM -> {
                    Transformation[] forms = Transformation.values();
                    if (arg < 0 || arg >= forms.length) return;
                    Transformation form = forms[arg];
                    if (!form.isAvailableTo(data.getRace())) return;
                    data.setSelectedForm(form);
                }
                case TOGGLE_TAIL -> {
                    if (!data.getRace().hasSaiyanTail()) return;
                    data.setTailOut(!data.isTailOut());
                    if (!data.isTailOut() && data.getTransformation().needsTail()) {
                        FormController.revert(player, data);
                    }
                    player.displayClientMessage(Component.literal(
                            data.isTailOut() ? "Cola suelta." : "Cola enrollada en la cintura."), true);
                }
                case SELECT_TECHNIQUE -> {
                    // Switching drops whatever was being charged
                    data.setTechCharge(0f);
                    data.setSelectedSlot(arg);
                }
                case LEARN_TECHNIQUE -> {
                    Technique t = technique(arg);
                    if (t == null || data.hasLearned(t)) return;
                    if (data.getLevel() < t.getMinLevel()) return;
                    if (data.getTrainingPoints() < t.getTpCost()) return;
                    data.addTrainingPoints(-t.getTpCost());
                    data.learn(t);
                    if (data.getEquipped().size() < Technique.MAX_EQUIPPED) data.toggleEquipped(t);
                    player.displayClientMessage(Component.literal("Técnica aprendida: " + t.getDisplayName()), true);
                }
                case OPEN_ACCESSORIES -> {
                    player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, p) -> com.dragonblock.common.menu.AccessoryMenu.open(id, inventory, player),
                            Component.literal("Accesorios")));
                    return;
                }
                case TOGGLE_EQUIP -> {
                    Technique t = technique(arg);
                    if (t == null || !data.hasLearned(t)) return;
                    data.setTechCharge(0f);
                    data.toggleEquipped(t);
                }
            }
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        });
        context.setPacketHandled(true);
    }

    private static Technique technique(int ordinal) {
        Technique[] all = Technique.values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
