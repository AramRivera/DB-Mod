package com.dragonblock.common.network.packet;

import com.dragonblock.common.data.Appearance;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Character creator: the race (only once) and the look (any time). Colors must come from the race's
 * palettes and the hair style from its list, anything else is rejected.
 */
public class C2SSelectRacePacket {
    private final String raceName;
    private final int skin, eyes, hair, accent;
    private final String hairStyle;

    public C2SSelectRacePacket(String raceName, int skin, int eyes, int hair, int accent, String hairStyle) {
        this.raceName = raceName;
        this.skin = skin;
        this.eyes = eyes;
        this.hair = hair;
        this.accent = accent;
        this.hairStyle = hairStyle;
    }

    public C2SSelectRacePacket(FriendlyByteBuf buf) {
        this.raceName = buf.readUtf(32);
        this.skin = buf.readInt();
        this.eyes = buf.readInt();
        this.hair = buf.readInt();
        this.accent = buf.readInt();
        this.hairStyle = buf.readUtf(32);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(raceName, 32);
        buf.writeInt(skin);
        buf.writeInt(eyes);
        buf.writeInt(hair);
        buf.writeInt(accent);
        buf.writeUtf(hairStyle, 32);
    }

    private boolean validLook(Race race, Transformation.HairStyle style) {
        return Appearance.inPalette(Appearance.skinPalette(race), skin)
                && Appearance.inPalette(Appearance.accentPalette(race), accent)
                && Appearance.inPalette(Appearance.EYES, eyes)
                && Appearance.inPalette(Appearance.HAIR, hair)
                && Appearance.hairStyles(race).contains(style);
    }

    public void handle(CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PlayerDBCData data = PlayerDBCData.get(player);
            try {
                Race newRace = data.isRaceChosen() ? data.getRace() : Race.valueOf(raceName); // race is permanent
                Transformation.HairStyle style = Transformation.HairStyle.valueOf(hairStyle);
                if (!validLook(newRace, style)) return;
                data.setLook(skin, eyes, hair, accent, style);
                if (data.isRaceChosen()) {
                    data.saveToPlayer(player);
                    ModMessages.syncToTracking(player);
                    return;
                }
                data.setRace(newRace);
                data.setRaceChosen(true);
                data.setTransformation(Transformation.BASE);
                data.setSelectedForm(Transformation.BASE);
                data.saveToPlayer(player);
                ModMessages.syncToTracking(player);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("Raza elegida: " + newRace.getDisplayName()), false);
            } catch (IllegalArgumentException ignored) {}
        });
        context.setPacketHandled(true);
    }
}
