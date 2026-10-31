package com.dragonblock.common.block;

import com.dragonblock.common.entity.ShenlongEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2COpenWishScreenPacket;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class DragonBallBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.5, 0.0, 4.5, 11.5, 7.0, 11.5);
    private final int stars;

    public DragonBallBlock(int stars) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .noOcclusion()
                .lightLevel(state -> 8)
                .strength(1.5f, 6.0f)
                .sound(SoundType.AMETHYST));
        this.stars = stars;
    }

    public int getStars() {
        return stars;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult res = tryTriggerRitual(level, pos, player);
        return res == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return tryTriggerRitual(level, pos, player);
    }

    private InteractionResult tryTriggerRitual(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Search in a 9x5x9 area for all 7 Dragon Balls
        Map<Integer, BlockPos> ballsFound = new HashMap<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -2, -4), pos.offset(4, 2, 4))) {
            BlockState checkState = level.getBlockState(p);
            if (checkState.getBlock() instanceof DragonBallBlock dbBlock) {
                ballsFound.put(dbBlock.getStars(), p.immutable());
            }
        }

        if (ballsFound.size() == 7) {
            // Check if Shenlong is already present nearby
            AABB searchBox = new AABB(pos).inflate(16.0);
            List<ShenlongEntity> existing = level.getEntitiesOfClass(ShenlongEntity.class, searchBox);
            if (!existing.isEmpty()) {
                ShenlongEntity shenlong = existing.get(0);
                if (player instanceof ServerPlayer sp) {
                    ModMessages.sendToPlayer(new S2COpenWishScreenPacket(shenlong.getId()), sp);
                }
                return InteractionResult.SUCCESS;
            }

            // Calculate center
            double avgX = 0, avgY = 0, avgZ = 0;
            for (BlockPos bp : ballsFound.values()) {
                avgX += bp.getX() + 0.5;
                avgY += bp.getY() + 0.5;
                avgZ += bp.getZ() + 0.5;
            }
            avgX /= 7.0;
            avgY /= 7.0;
            avgZ /= 7.0;

            ServerLevel serverLevel = (ServerLevel) level;

            // Visual Lightning
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(avgX, avgY, avgZ);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }

            // Thunderstorm weather effect
            serverLevel.setWeatherParameters(0, 6000, true, true);

            // Roar & thunder sound
            level.playSound(null, avgX, avgY, avgZ, ModSounds.TRANSFORM.get(), SoundSource.WEATHER, 3.0f, 0.6f);
            level.playSound(null, avgX, avgY, avgZ, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.WEATHER, 2.5f, 0.8f);

            // Spawn Shenlong Entity
            ShenlongEntity shenlong = new ShenlongEntity(ModEntities.SHENLONG.get(), level);
            shenlong.moveTo(avgX, avgY + 2.5, avgZ, 0, 0);
            shenlong.setBallPositions(new ArrayList<>(ballsFound.values()));
            level.addFreshEntity(shenlong);

            // Broadcast message
            for (Player p : level.players()) {
                if (p.distanceToSqr(avgX, avgY, avgZ) < 64.0 * 64.0) {
                    p.displayClientMessage(
                            Component.literal("§a§l[SHENLONG] §e¡¡SAL DE AHÍ SHENLONG Y CUMPLE MI DESEO!!")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            false
                    );
                    p.displayClientMessage(
                            Component.literal("§a§l[SHENLONG] §2¡Habla mortal! ¡Te concederé un solo deseo!")
                                    .withStyle(ChatFormatting.GREEN),
                            false
                    );
                }
            }

            // Open Wish Screen for the summoner
            if (player instanceof ServerPlayer sp) {
                ModMessages.sendToPlayer(new S2COpenWishScreenPacket(shenlong.getId()), sp);
            }

            return InteractionResult.SUCCESS;
        } else {
            // Missing balls
            List<Integer> missing = new ArrayList<>();
            for (int i = 1; i <= 7; i++) {
                if (!ballsFound.containsKey(i)) missing.add(i);
            }

            player.displayClientMessage(
                    Component.literal("§6[Esferas del Dragón] §eSe han reunido §a" + ballsFound.size() + "/7 §eesferas en el altar.")
                            .append(Component.literal(" §7(Faltan: §c" + missing + "§7 estrellas)").withStyle(ChatFormatting.GRAY)),
                    true
            );
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double py = pos.getY() + 0.25 + random.nextDouble() * 0.3;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.WAX_OFF, px, py, pz, 0, 0.02, 0);
        }
    }
}
