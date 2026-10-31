package com.dragonblock.common.event;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.combat.FormController;
import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.command.DevCommand;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.item.TrainingWeightsItem;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DragonBlockEvents {
    private static final ResourceLocation CON_HEALTH_MODIFIER = id("con_health_bonus");
    private static final ResourceLocation DEX_SPEED_MODIFIER = id("dex_speed_bonus");
    private static final ResourceLocation FORM_SCALE_MODIFIER = id("form_scale");
    private static final ResourceLocation GUARD_SLOW_MODIFIER = id("guard_slow");
    private static final long ZENKAI_COOLDOWN_TICKS = 20L * 60 * 10;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, path);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DevCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDBCData.get(player).saveToPlayer(player);
            PlayerDBCData.removePlayer(player.getUUID());
        }
    }

    /** A player coming into view needs the other player's data to render their aura, hair and tail. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            ModMessages.sendToPlayer(ModMessages.createSync(target), viewer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            data.setCurrentKi(data.getMaxKi());
            data.setTransformation(Transformation.BASE);
            data.setFlying(false);
            data.setChargingKi(false);
            data.saveToPlayer(player);
            ModMessages.syncToTracking(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (original.getPersistentData().contains("DragonBlockData")) {
            newPlayer.getPersistentData().put("DragonBlockData", original.getPersistentData().getCompound("DragonBlockData"));
        }
        PlayerDBCData.removePlayer(original.getUUID());
        if (newPlayer instanceof ServerPlayer serverPlayer) {
            PlayerDBCData data = PlayerDBCData.get(serverPlayer);
            data.saveToPlayer(serverPlayer);
            ModMessages.syncToTracking(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        // Attacker
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            if (event.getSource().getDirectEntity() == player) {
                event.setAmount(event.getAmount() + data.getEffectiveStr() * 0.4f);
            }

            float tp = Math.max(1f, event.getAmount() * 0.4f + data.getMnd() / 5.0f);
            if (data.getRace() == Race.HALF_SAIYAN) tp *= 1.25f;
            if (player.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof TrainingWeightsItem)) tp *= 1.5f;
            data.addTrainingPoints((int) tp);
            data.addMastery(data.getTransformation(), 0.15f);
            data.saveToPlayer(player);
            ModMessages.syncToPlayer(player);
        }

        // Defender
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDBCData data = PlayerDBCData.get(player);

            if (data.dashIframes > 0) {
                event.setCanceled(true);
                return;
            }

            float amount = event.getAmount();
            amount *= 1f - Math.min(0.6f, data.getEffectiveDex() * 0.012f);
            if (data.getRace() == Race.MAJIN) amount *= 0.85f;

            // Guard: 65% blocked, paid with Ki
            if (data.isGuarding() && data.consumeKi(amount * 1.5f)) {
                amount *= 0.35f;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
            event.setAmount(amount);

            long now = player.level().getGameTime();
            boolean nearDeath = player.getHealth() - amount <= player.getMaxHealth() * 0.15f;
            if (data.getRace().hasSaiyanTail() && nearDeath && now - data.getLastZenkaiTime() > ZENKAI_COOLDOWN_TICKS) {
                data.setLastZenkaiTime(now);
                int bonus = 50 + data.getLevel() * 2;
                data.addTrainingPoints(bonus);
                player.heal(player.getMaxHealth() * 0.3f);
                player.displayClientMessage(Component.literal("Zenkai: tu cuerpo se sobrepone al límite (+" + bonus + " TP)"), true);
            }
            data.saveToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        PlayerDBCData data = PlayerDBCData.get(player);

        if (player.level().isClientSide) {
            clientParticles(player, data);
            return;
        }
        ServerPlayer sp = (ServerPlayer) player;

        if (data.isChargingKi()) {
            data.setCurrentKi(data.getCurrentKi() + 1.2f + data.getEffectiveWil() * 0.1f);
            if (player.tickCount % 20 == 0) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.KI_CHARGE.get(), SoundSource.PLAYERS, 0.4f, 1.0f);
            }
        } else if (!data.isWaveHeld()) {
            data.setCurrentKi(data.getCurrentKi() + 0.15f + (data.getRace() == Race.HUMAN ? 0.15f : 0.05f));
        }

        FormController.tick(sp, data);
        KiAttacks.tick(sp, data);
        tickFlight(sp, data);

        if (data.getRace() == Race.NAMEKIAN && player.getHealth() < player.getMaxHealth() && player.tickCount % 40 == 0) {
            player.heal(1.0f);
        }

        data.saveToPlayer(player);

        if (player.tickCount % 20 == 0) updatePlayerAttributes(player, data);

        boolean active = data.isChargingKi() || data.isTransformHeld() || data.isWaveHeld();
        if (player.tickCount % (active ? 4 : 20) == 0) ModMessages.syncToTracking(sp);
    }

    private static void tickFlight(ServerPlayer player, PlayerDBCData data) {
        if (!data.isFlying()) return;
        float cost = data.getRace() == Race.ARCOSIAN ? 0.06f : 0.12f;
        if (player.isSprinting()) cost *= 2.5f;

        if (data.consumeKi(cost)) {
            // Faster flight with DEX, much faster while sprinting
            float speed = 0.05f + Math.min(0.1f, data.getEffectiveDex() * 0.0004f);
            if (player.isSprinting()) speed *= 1.8f;
            if (Math.abs(player.getAbilities().getFlyingSpeed() - speed) > 0.001f) {
                player.getAbilities().setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
            return;
        }

        data.setFlying(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
        ModMessages.syncToTracking(player);
        player.displayClientMessage(Component.literal("Ki insuficiente para mantener el vuelo."), true);
    }

    private static void clientParticles(Player player, PlayerDBCData data) {
        if (data.isFlying() && player.tickCount % 4 == 0 && player.getAbilities().flying) {
            player.level().addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() - 0.2, player.getZ(), 0, -0.05, 0);
        }
        if ((data.isChargingKi() || data.isTransformHeld()) && player.onGround() && player.tickCount % 3 == 0) {
            // Debris kicked up by the aura
            double a = player.getRandom().nextDouble() * Math.PI * 2;
            player.level().addParticle(ParticleTypes.POOF, player.getX() + Math.cos(a) * 1.2, player.getY() + 0.05,
                    player.getZ() + Math.sin(a) * 1.2, Math.cos(a) * 0.1, 0.02, Math.sin(a) * 0.1);
        }
    }

    private static void updatePlayerAttributes(Player player, PlayerDBCData data) {
        setModifier(player, Attributes.MAX_HEALTH, CON_HEALTH_MODIFIER,
                Math.max(0, (data.getEffectiveCon() - 5) * 2.0), AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.MOVEMENT_SPEED, DEX_SPEED_MODIFIER,
                Math.min(0.15, Math.max(0, (data.getEffectiveDex() - 5) * 0.003)), AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.SCALE, FORM_SCALE_MODIFIER,
                data.getTransformation().getScale() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.MOVEMENT_SPEED, GUARD_SLOW_MODIFIER,
                data.isGuarding() ? -0.5 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double value,
                                    AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (value == 0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, op));
        }
    }
}
