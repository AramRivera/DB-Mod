package com.dragonblock.common.network.packet;

import com.dragonblock.common.entity.SpaceshipEntity;
import com.dragonblock.common.world.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** The planet the pilot picked; only honored for the ship the sender is actually sitting in. */
public class C2STravelPacket {
    private final int shipId, planet;

    public C2STravelPacket(int shipId, int planet) {
        this.shipId = shipId;
        this.planet = planet;
    }

    public C2STravelPacket(FriendlyByteBuf buf) {
        this.shipId = buf.readVarInt();
        this.planet = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(shipId);
        buf.writeVarInt(planet);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            Planet[] planets = Planet.values();
            if (player == null || planet < 0 || planet >= planets.length) return;
            if (player.getVehicle() instanceof SpaceshipEntity ship && ship.getId() == shipId) ship.launch(player, planets[planet]);
        });
        context.setPacketHandled(true);
    }
}
