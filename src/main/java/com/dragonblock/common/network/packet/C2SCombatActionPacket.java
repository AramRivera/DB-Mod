package com.dragonblock.common.network.packet;

import com.dragonblock.common.combat.melee.MeleeCombatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server-authoritative combat packet. Carries the physical melee action intent,
 * defense actions, aerial chases, vanishes and the target entity ID (if locked on).
 */
public class C2SCombatActionPacket {
    public enum Action {
        LIGHT_ATTACK,
        HEAVY_ATTACK,
        GUARD_START,
        GUARD_END,
        DODGE,
        BOOST_CHASE,
        EVASIVE_VANISH,
        CANCEL_TRAINING,
        METEOR_SMASH
    }

    private final Action action;
    private final int targetId;

    public C2SCombatActionPacket(Action action, int targetId) {
        this.action = action;
        this.targetId = targetId;
    }

    public C2SCombatActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetId = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeVarInt(targetId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MeleeCombatManager.handleCombatAction(player, action, targetId);
        });
        context.setPacketHandled(true);
    }
}
