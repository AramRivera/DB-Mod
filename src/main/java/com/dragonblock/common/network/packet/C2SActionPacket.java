package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.FormController;
import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** One-shot actions. {@code arg} is only used by SELECT_FORM (form ordinal). */
public class C2SActionPacket {
    public enum Action { REVERT, DASH, SELECT_FORM, TOGGLE_TAIL }

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
            }
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        });
        context.setPacketHandled(true);
    }
}
