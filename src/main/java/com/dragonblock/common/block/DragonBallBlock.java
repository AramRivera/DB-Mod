package com.dragonblock.common.block;

import com.dragonblock.common.entity.ShenlongEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dragon ball set down in the world. The seven laid out in a circle call the eternal dragon. Placed
 * balls can't be broken (only the ball capsule takes them back) and are remembered so the radar finds them.
 */
public class DragonBallBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 7, 12);
    private final int stars;

    public DragonBallBlock(int stars) {
        // Once set down a ball can't be mined, blown up or pushed: only the ball capsule picks it up
        super(BlockBehaviour.Properties.of().strength(-1f, 3600000f).sound(SoundType.GLASS).lightLevel(s -> 9).noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) ShenlongEntity.trySummon(server, pos, sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (level instanceof ServerLevel server && !old.is(this)) DragonBallTracker.get(server).put(stars, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moved) {
        if (level instanceof ServerLevel server && !now.is(this)) DragonBallTracker.get(server).remove(stars, pos);
        super.onRemove(state, level, pos, now, moved);
    }

    /** A faint shimmer, like the stars inside are alive. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4,
                pos.getY() + 0.3 + random.nextDouble() * 0.3, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
    }
}
