package com.dragonblock.common.entity;

import com.dragonblock.common.block.DragonBallBlock;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2COpenWishPacket;
import com.dragonblock.common.registry.ModBlocks;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The eternal dragon. Using one of seven gathered balls starts the ritual: the sky goes dark, lightning
 * falls, a column of light bursts from the balls and the dragon flows out of it, coiling up into the sky.
 * The summoner picks one wish; the dragon departs as light and the balls fly off to far corners of the
 * world. All timing lives here on the server; ShenlongRenderer turns the synced state into the show.
 */
public class ShenlongEntity extends Entity {
    public enum State { RISING, WAITING, LEAVING }

    /** Ticks of each phase the client draws against. */
    public static final int RISE_TICKS = 220, LEAVE_TICKS = 120, WAIT_LIMIT = 20 * 120;

    public enum Wish {
        STRENGTH("Quiero ser más fuerte", "Ganas una gran cantidad de TP."),
        RICHES("Quiero riquezas", "Diamantes, esmeraldas y oro."),
        SENZU("Quiero semillas del ermitaño", "Ocho semillas que lo curan todo."),
        MASTERY("Quiero dominar mi poder", "+40% de maestría en todas tus formas desbloqueadas."),
        REBIRTH("Quiero renacer", "Vuelves a elegir raza y apariencia.");

        public final String line, effect;

        Wish(String line, String effect) {
            this.line = line;
            this.effect = effect;
        }
    }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ShenlongEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE_START = SynchedEntityData.defineId(ShenlongEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SUMMONER = SynchedEntityData.defineId(ShenlongEntity.class, EntityDataSerializers.INT);

    // Server only
    private UUID summoner;
    private final Map<Integer, BlockPos> balls = new HashMap<>();
    private final List<Integer> toScatter = new ArrayList<>();

    public ShenlongEntity(EntityType<? extends ShenlongEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, 0);
        builder.define(STATE_START, 0);
        builder.define(SUMMONER, -1);
    }

    public State getState() { return State.values()[Math.min(2, Math.max(0, entityData.get(STATE)))]; }
    /** Ticks since the current phase began. */
    public int stateAge() { return tickCount - entityData.get(STATE_START); }
    public int getSummonerId() { return entityData.get(SUMMONER); }

    private void setState(State state) {
        entityData.set(STATE, state.ordinal());
        entityData.set(STATE_START, tickCount);
    }

    /** The ritual is a one-time event: never written to the world (the balls stay where they are). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // ------------------------------------------------------------------ summoning

    /** A player used a dragon ball: if all seven lie close together, the dragon comes. */
    public static void trySummon(ServerLevel level, BlockPos used, ServerPlayer player) {
        for (ShenlongEntity existing : level.getEntitiesOfClass(ShenlongEntity.class, player.getBoundingBox().inflate(160))) {
            // Closed the wish screen by accident: using a ball again reopens it
            if (existing.getState() == State.WAITING && player.getUUID().equals(existing.summoner)) {
                ModMessages.sendToPlayer(new S2COpenWishPacket(existing.getId()), player);
            } else {
                player.displayClientMessage(Component.literal("El dragón ya ha sido invocado.").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        Map<Integer, BlockPos> found = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(used.offset(-4, -2, -4), used.offset(4, 2, 4))) {
            if (level.getBlockState(pos).getBlock() instanceof DragonBallBlock ball) found.putIfAbsent(ball.getStars(), pos.immutable());
        }
        if (found.size() < 7) {
            player.displayClientMessage(Component.literal("Las esferas laten débilmente... faltan " + (7 - found.size()) + " (" + found.size() + "/7 juntas).")
                    .withStyle(ChatFormatting.GOLD), true);
            return;
        }
        Vec3 center = Vec3.ZERO;
        for (BlockPos p : found.values()) center = center.add(Vec3.atBottomCenterOf(p));
        center = center.scale(1 / 7.0);
        ShenlongEntity dragon = new ShenlongEntity(ModEntities.SHENLONG.get(), level);
        dragon.setPos(center);
        dragon.summoner = player.getUUID();
        dragon.balls.putAll(found);
        dragon.entityData.set(SUMMONER, player.getId());
        level.addFreshEntity(dragon);
        dragon.say("Las siete esferas brillan al unísono...", ChatFormatting.GOLD);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel server)) return;
        int age = stateAge();
        switch (getState()) {
            case RISING -> {
                if (age % 25 == 0 && age < 180) sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.6f + random.nextFloat() * 0.3f);
                if (age == 60) sound(SoundEvents.BEACON_POWER_SELECT, 4f, 0.5f);
                if (age == 90) sound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.6f);
                if (age >= RISE_TICKS) {
                    setState(State.WAITING);
                    say("Has reunido las siete esferas. Habla: te concederé un deseo.", ChatFormatting.GREEN);
                    sound(SoundEvents.ENDER_DRAGON_AMBIENT, 4f, 0.55f);
                    if (server.getPlayerByUUID(summoner) instanceof ServerPlayer p) ModMessages.sendToPlayer(new S2COpenWishPacket(getId()), p);
                }
            }
            case WAITING -> {
                ServerPlayer p = summoner == null ? null : (ServerPlayer) server.getPlayerByUUID(summoner);
                if (age > WAIT_LIMIT || (p == null && age > 200)) {
                    say("Si no tienes deseo alguno, me marcho.", ChatFormatting.GREEN);
                    leave();
                }
            }
            case LEAVING -> {
                if (age == 50) sound(SoundEvents.ENDER_DRAGON_FLAP, 4f, 0.5f);
                // Place one scattered ball per tick so world generation is spread out
                if (age >= 70 && !toScatter.isEmpty()) scatter(server, toScatter.remove(0));
                if (age >= LEAVE_TICKS && toScatter.isEmpty()) discard();
            }
        }
    }

    // ------------------------------------------------------------------ wishes

    /** From the wish screen; only the summoner, only while the dragon waits. */
    public void grant(ServerPlayer player, Wish wish) {
        if (getState() != State.WAITING || !player.getUUID().equals(summoner) || player.distanceTo(this) > 160) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        switch (wish) {
            case STRENGTH -> data.addTrainingPoints(1500 + data.getLevel() * 25);
            case RICHES -> {
                give(player, new ItemStack(Items.DIAMOND, 16));
                give(player, new ItemStack(Items.EMERALD, 32));
                give(player, new ItemStack(Items.GOLD_INGOT, 24));
            }
            case SENZU -> give(player, new ItemStack(ModItems.SENZU_BEAN.get(), 8));
            case MASTERY -> {
                for (Transformation f : Transformation.values()) {
                    if (f != Transformation.BASE && data.isUnlocked(f)) data.setMastery(f, data.getMastery(f) + 40f);
                }
            }
            case REBIRTH -> {
                data.setTransformation(Transformation.BASE);
                data.setSelectedForm(Transformation.BASE);
                data.setRaceChosen(false);
                player.displayClientMessage(Component.literal("Abre tu ficha (V) para renacer.").withStyle(ChatFormatting.AQUA), false);
            }
        }
        data.saveToPlayer(player);
        ModMessages.syncToTracking(player);
        say("Tu deseo ha sido concedido. Hasta que las esferas vuelvan a reunirse...", ChatFormatting.GREEN);
        leave();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void leave() {
        setState(State.LEAVING);
        sound(SoundEvents.BEACON_DEACTIVATE, 4f, 0.5f);
        for (Map.Entry<Integer, BlockPos> e : balls.entrySet()) {
            if (level().getBlockState(e.getValue()).getBlock() instanceof DragonBallBlock) {
                level().removeBlock(e.getValue(), false);
                toScatter.add(e.getKey());
            }
        }
    }

    /** Sends one ball far away: somewhere 400 to 1500 blocks off, on the surface. */
    private void scatter(ServerLevel level, int stars) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, dist = 400 + random.nextDouble() * 1100;
            int x = (int) (getX() + Math.cos(angle) * dist), z = (int) (getZ() + Math.sin(angle) * dist);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) continue;
            level.getChunk(x >> 4, z >> 4); // generate it if nobody has been there
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (y <= level.getMinBuildHeight() || !level.getFluidState(pos.below()).isEmpty()) continue;
            level.setBlockAndUpdate(pos, ModBlocks.DRAGON_BALLS.get(stars - 1).get().defaultBlockState());
            return;
        }
        // Nowhere suitable: it falls right here instead of being lost
        level.setBlockAndUpdate(blockPosition(), ModBlocks.DRAGON_BALLS.get(stars - 1).get().defaultBlockState());
    }

    // ------------------------------------------------------------------ helpers

    private void say(String line, ChatFormatting color) {
        Component msg = Component.literal("[Dragón Eterno] ").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD)
                .append(Component.literal(line).withStyle(color));
        for (ServerPlayer p : ((ServerLevel) level()).players()) if (p.distanceTo(this) < 200) p.sendSystemMessage(msg);
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY() + 20, getZ(), sound, SoundSource.AMBIENT, volume, pitch);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 400 * 400;
    }
}
