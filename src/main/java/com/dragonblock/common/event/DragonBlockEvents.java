package com.dragonblock.common.event;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.command.DevCommand;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.item.TrainingWeightsItem;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
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
    private static final ResourceLocation CON_HEALTH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "con_health_bonus");
    private static final ResourceLocation DEX_SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "dex_speed_bonus");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DevCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            data.saveToPlayer(player);
            ModMessages.syncToPlayer(player);
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
            ModMessages.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (original.getPersistentData().contains("DragonBlockData")) {
            newPlayer.getPersistentData().put("DragonBlockData", original.getPersistentData().getCompound("DragonBlockData"));
        }
        if (newPlayer instanceof ServerPlayer serverPlayer) {
            ModMessages.syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Player attacking
        if (event.getSource().getEntity() instanceof Player player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            float bonusDamage = data.getEffectiveStr() * 0.4f;
            event.setAmount(event.getAmount() + bonusDamage);

            // Award TP for combat
            if (!player.level().isClientSide) {
                int baseTp = (int) Math.max(1, (event.getAmount() * 0.4f) + (data.getMnd() / 5.0f));
                if (data.getRace() == Race.HALF_SAIYAN) {
                    baseTp = (int) (baseTp * 1.25f);
                }

                // Check for training weights in inventory
                boolean hasWeights = player.getInventory().items.stream()
                        .anyMatch(stack -> stack.getItem() instanceof TrainingWeightsItem);
                if (hasWeights) {
                    baseTp = (int) (baseTp * 1.5f);
                }

                data.addTrainingPoints(baseTp);
                data.saveToPlayer(player);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModMessages.syncToPlayer(serverPlayer);
                }
            }
        }

        // Player taking damage
        if (event.getEntity() instanceof Player player) {
            PlayerDBCData data = PlayerDBCData.get(player);
            // Dexterity provides damage reduction
            float reduction = Math.min(0.6f, data.getEffectiveDex() * 0.012f);
            if (reduction > 0) {
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }

            // Saiyan Zenkai boost when very low HP
            if (data.getRace() == Race.SAIYAN && player.getHealth() - event.getAmount() <= 5.0f) {
                if (!player.level().isClientSide) {
                    data.addTrainingPoints(50);
                    data.saveToPlayer(player);
                    player.displayClientMessage(
                            Component.literal("§c¡¡Zenkai activado!! Tu sangre Saiyajin se fortalece ante el peligro mortal! (+50 TP)")
                                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            true
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        PlayerDBCData data = PlayerDBCData.get(player);

        // Ki Charging logic
        if (data.isChargingKi()) {
            float chargeRate = 0.5f + (data.getEffectiveWil() * 0.06f);
            data.setCurrentKi(data.getCurrentKi() + chargeRate);

            if (player.level().isClientSide) {
                // Client particles around player
                for (int i = 0; i < 4; i++) {
                    double px = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.2;
                    double py = player.getY() + player.getRandom().nextDouble() * 1.8;
                    double pz = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.2;
                    player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0.15, 0);
                }
            } else {
                if (player.tickCount % 20 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.KI_CHARGE.get(), SoundSource.PLAYERS, 0.4f, 1.0f);
                }
            }
        } else {
            // Passive Ki regeneration
            float passiveRegen = 0.04f + (data.getRace() == Race.HUMAN ? 0.05f : 0.02f);
            data.setCurrentKi(data.getCurrentKi() + passiveRegen);
        }

        // Transformation Ki drain
        Transformation form = data.getTransformation();
        if (form != Transformation.BASE) {
            float drain = form.getKiDrainPerSecond() / 20.0f;
            if (data.getCurrentKi() > drain) {
                data.setCurrentKi(data.getCurrentKi() - drain);

                if (player.level().isClientSide && player.tickCount % 3 == 0) {
                    double px = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                    double py = player.getY() + player.getRandom().nextDouble() * 1.8;
                    double pz = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.0;
                    player.level().addParticle(
                            form == Transformation.SUPER_SAIYAN ? ParticleTypes.WAX_OFF : ParticleTypes.FLAME,
                            px, py, pz, 0, 0.1, 0
                    );
                }
            } else {
                // Exhausted, revert form
                data.setCurrentKi(0);
                data.setTransformation(Transformation.BASE);
                if (!player.level().isClientSide) {
                    data.saveToPlayer(player);
                    if (player instanceof ServerPlayer sp) ModMessages.syncToPlayer(sp);
                    player.displayClientMessage(
                            Component.literal("§c¡Te has quedado sin Ki! Tu transformación se ha desactivado por agotamiento."),
                            true
                    );
                }
            }
        }

        // Flight (Bukujutsu) Ki drain
        if (data.isFlying()) {
            float flightCost = (data.getRace() == Race.ARCOSIAN ? 0.05f : 0.1f) / 20.0f;
            if (data.getCurrentKi() > flightCost) {
                data.setCurrentKi(data.getCurrentKi() - flightCost);
                if (player.level().isClientSide && player.tickCount % 4 == 0) {
                    player.level().addParticle(ParticleTypes.CLOUD,
                            player.getX(), player.getY() - 0.2, player.getZ(),
                            0, -0.05, 0);
                }
            } else {
                data.setFlying(false);
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                if (!player.level().isClientSide) {
                    data.saveToPlayer(player);
                    if (player instanceof ServerPlayer sp) ModMessages.syncToPlayer(sp);
                }
            }
        }

        // Namekian passive HP regen
        if (data.getRace() == Race.NAMEKIAN && player.getHealth() < player.getMaxHealth() && player.tickCount % 40 == 0) {
            player.heal(1.0f);
        }

        // Update attribute modifiers every 20 ticks (1s) on server
        if (!player.level().isClientSide && player.tickCount % 20 == 0) {
            updatePlayerAttributes(player, data);
            data.saveToPlayer(player);
            if (player instanceof ServerPlayer sp) {
                ModMessages.syncToPlayer(sp);
            }
        }
    }

    private static void updatePlayerAttributes(Player player, PlayerDBCData data) {
        // Constitution Max Health bonus
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double bonusHealth = Math.max(0, (data.getEffectiveCon() - 5) * 2.0);
            AttributeModifier mod = new AttributeModifier(
                    CON_HEALTH_MODIFIER, bonusHealth, AttributeModifier.Operation.ADD_VALUE
            );
            maxHp.addOrUpdateTransientModifier(mod);
        }

        // Dexterity Speed bonus
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            double bonusSpeed = Math.max(0, (data.getEffectiveDex() - 5) * 0.003);
            AttributeModifier mod = new AttributeModifier(
                    DEX_SPEED_MODIFIER, bonusSpeed, AttributeModifier.Operation.ADD_VALUE
            );
            speed.addOrUpdateTransientModifier(mod);
        }
    }
}
