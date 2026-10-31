package com.dragonblock.common.event;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.combat.FormController;
import com.dragonblock.common.combat.FormEffects;
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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
            data.clearInput();
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
            data.setBody(data.getMaxBody());
            data.lastMirroredHealth = -1f;
            data.forcedForm = false;
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
                event.setAmount((event.getAmount() + data.getEffectiveStr() * 0.4f) * FormEffects.meleeMultiplier(player, data));
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
            var attacker = event.getSource().getEntity();
            if (FormEffects.tryDodge(player, data, attacker == null ? null : attacker.position())) {
                event.setCanceled(true);
                return;
            }

            float amount = event.getAmount();
            amount *= 1f - Math.min(0.6f, data.getEffectiveDex() * 0.012f);
            if (data.getRace() == Race.MAJIN) amount *= 0.85f;
            amount *= FormEffects.incomingMultiplier(data);

            // Guard: 65% blocked, paid with Ki
            if (data.isGuarding() && data.consumeKi(amount * 1.5f)) {
                amount *= 0.35f;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.8f);
            }
            event.setAmount(amount);

            long now = player.level().getGameTime();
            boolean nearDeath = data.getBody() - amount <= data.getMaxBody() * 0.15f;
            if (data.getRace().hasSaiyanTail() && nearDeath && now - data.getLastZenkaiTime() > ZENKAI_COOLDOWN_TICKS) {
                data.setLastZenkaiTime(now);
                int bonus = 50 + data.getLevel() * 2;
                data.addTrainingPoints(bonus);
                data.setBody(data.getBody() + data.getMaxBody() * 0.3f);
                player.displayClientMessage(Component.literal("Zenkai: tu cuerpo se sobrepone al límite (+" + bonus + " TP)"), true);
            }
            data.saveToPlayer(player);
        }
    }

    /**
     * After armor, convert vanilla damage into the body pool, then set the vanilla damage so the
     * hearts keep showing body / maxBody. Lowest priority so other mods' changes are already in.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        syncBodyWithVanilla(player, data);

        float before = player.getHealth();
        FormEffects.onDamaged(data, event.getAmount() / data.getMaxBody());
        data.setBody(data.getBody() - event.getAmount());
        float target = data.getBody() <= 0 ? 0f : mirroredHealth(player, data);
        float amount = data.getBody() <= 0 ? before + 1000f : Math.max(0f, before - target);
        event.setAmount(amount);
        data.lastMirroredHealth = Math.max(0f, before - amount);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);
    }

    private static float mirroredHealth(Player player, PlayerDBCData data) {
        // Never show 0 hearts while the body pool is still alive
        return Math.max(0.5f, player.getMaxHealth() * data.getBody() / data.getMaxBody());
    }

    /** Folds vanilla health changes (food regen, potions, /kill) into the pool, then rewrites vanilla health. */
    private static void syncBodyWithVanilla(Player player, PlayerDBCData data) {
        float maxBody = data.getMaxBody();
        // Transforming changes maxBody: keep the same fill ratio instead of the same absolute value
        if (data.lastMaxBody > 0 && Math.abs(maxBody - data.lastMaxBody) > 0.01f) {
            data.setBody(data.getBody() * maxBody / data.lastMaxBody);
        }
        data.lastMaxBody = maxBody;

        if (data.lastMirroredHealth >= 0) {
            float delta = player.getHealth() - data.lastMirroredHealth;
            if (Math.abs(delta) > 0.001f) data.setBody(data.getBody() + delta / player.getMaxHealth() * maxBody);
        }
        if (player.isDeadOrDying()) return;
        float mirrored = mirroredHealth(player, data);
        if (Math.abs(player.getHealth() - mirrored) > 0.001f) player.setHealth(mirrored);
        data.lastMirroredHealth = player.getHealth();
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
        } else if (!data.isTechHeld()) {
            data.setCurrentKi(data.getCurrentKi() + 0.15f + (data.getRace() == Race.HUMAN ? 0.15f : 0.05f));
        }

        FormController.tick(sp, data);
        FormEffects.tick(sp, data);
        KiAttacks.tick(sp, data);
        tickFlight(sp, data);

        if (data.getRace() == Race.NAMEKIAN && player.tickCount % 40 == 0) {
            data.setBody(data.getBody() + data.getMaxBody() * 0.02f);
        }
        syncBodyWithVanilla(player, data);

        data.saveToPlayer(player);

        if (player.tickCount % 20 == 0) updatePlayerAttributes(player, data);
        if (data.isGuarding() != data.guardApplied || data.isTransformHeld() != data.rootApplied) applyStanceSlow(player, data);
        tickScale(player, data);

        boolean active = data.isChargingKi() || data.isTransformHeld() || data.isTechHeld();
        if (player.tickCount % (active ? 4 : 20) == 0) ModMessages.syncToTracking(sp);
    }

    private static void tickFlight(ServerPlayer player, PlayerDBCData data) {
        if (!data.isFlying()) return;
        if (!data.getTransformation().canFly()) {
            stopFlying(player, data, "Tu cuerpo es demasiado grande para volar.");
            return;
        }
        if (!player.getAbilities().flying) return; // hovering off: walking or falling costs nothing
        float cost = data.getRace() == Race.ARCOSIAN ? 0.06f : 0.12f;
        if (player.isSprinting()) cost *= 2.5f;

        if (data.consumeKi(cost)) {
            // Faster flight with DEX, much faster while sprinting
            // Vanilla creative flight is 0.05; stay close to it so flying feels normal
            float speed = 0.05f + Math.min(0.03f, data.getEffectiveDex() * 0.00015f);
            if (player.isSprinting()) speed *= 1.6f;
            if (Math.abs(player.getAbilities().getFlyingSpeed() - speed) > 0.001f) {
                player.getAbilities().setFlyingSpeed(speed);
                player.onUpdateAbilities();
            }
            return;
        }

        stopFlying(player, data, "Ki insuficiente para mantener el vuelo.");
    }

    private static void stopFlying(ServerPlayer player, PlayerDBCData data, String reason) {
        data.setFlying(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();
        ModMessages.syncToTracking(player);
        player.displayClientMessage(Component.literal(reason), true);
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
        // Old saves had a CON health bonus; health now lives in the body pool
        setModifier(player, Attributes.MAX_HEALTH, CON_HEALTH_MODIFIER, 0, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.MOVEMENT_SPEED, DEX_SPEED_MODIFIER,
                dexSpeedBonus(data), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        applyStanceSlow(player, data);
    }

    /** Guarding halves speed; powering up roots you in place (you plant your feet to transform). */
    private static void applyStanceSlow(Player player, PlayerDBCData data) {
        data.guardApplied = data.isGuarding();
        data.rootApplied = data.isTransformHeld();
        setModifier(player, Attributes.MOVEMENT_SPEED, GUARD_SLOW_MODIFIER, stanceSlow(data),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static double stanceSlow(PlayerDBCData data) {
        return data.isTransformHeld() ? -0.85 : data.isGuarding() ? -0.5 : 0;
    }

    /** Up to +35% walk speed. The FOV effect is cancelled client-side so it never looks like a speed potion. */
    public static double dexSpeedBonus(PlayerDBCData data) {
        double dex = Math.min(0.35, Math.max(0, (data.getEffectiveDex() - 5) * 0.002));
        return (1 + dex) * FormEffects.speedMultiplier(data) - 1;
    }

    /** Eases body scale toward the form's scale over ~2 seconds, so giant forms grow (and shrink) visibly. */
    private static void tickScale(Player player, PlayerDBCData data) {
        float target = data.getTransformation().getScale();
        if (Math.abs(data.appliedScale - target) < 0.005f && player.tickCount % 20 != 0) return;
        float step = Math.max(0.02f, Math.abs(target - data.appliedScale) * 0.08f);
        data.appliedScale = data.appliedScale < target
                ? Math.min(target, data.appliedScale + step) : Math.max(target, data.appliedScale - step);
        setModifier(player, Attributes.SCALE, FORM_SCALE_MODIFIER, data.appliedScale - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
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
