package com.dragonblock.common.entity;

import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.S2COpenTravelPacket;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.world.Planet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The capsule spaceship. Right click to board and pick a planet; it lifts off, jumps through hyperspace
 * and sets down at the destination with you inside. Sneak and hit it (empty) to pack it back into a capsule.
 */
public class SpaceshipEntity extends Entity {
    public enum State { IDLE, LIFTOFF, LANDING }

    public static final int LIFTOFF_TICKS = 50, LANDING_TICKS = 70;
    private static final double LAND_HEIGHT = 30;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(SpaceshipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE_START = SynchedEntityData.defineId(SpaceshipEntity.class, EntityDataSerializers.INT);

    private Planet destination;
    private double groundY;

    public SpaceshipEntity(EntityType<? extends SpaceshipEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, 0);
        builder.define(STATE_START, 0);
    }

    public State getState() { return State.values()[Math.min(2, Math.max(0, entityData.get(STATE)))]; }
    public int stateAge() { return (int) level().getGameTime() - entityData.get(STATE_START); }

    private void setState(State s) {
        entityData.set(STATE, s.ordinal());
        entityData.set(STATE_START, (int) level().getGameTime());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getState() != State.IDLE || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            if (!hasPassenger(sp) && !sp.startRiding(this)) return InteractionResult.PASS;
            ModMessages.sendToPlayer(new S2COpenTravelPacket(getId()), sp);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** From the travel screen: only the pilot, only while parked, never to the planet it already is on. */
    public void launch(ServerPlayer pilot, Planet planet) {
        if (getState() != State.IDLE || !hasPassenger(pilot) || Planet.of(level().dimension()) == planet) return;
        if (com.dragonblock.common.data.PlayerDBCData.get(pilot).isDead()) {
            // No escaping the afterlife by ship: only the judge sends souls back
            pilot.displayClientMessage(Component.literal("Las almas no pueden pilotar naves. Habla con el Gran Juez."), true);
            return;
        }
        destination = planet;
        groundY = getY();
        setState(State.LIFTOFF);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 3f, 0.5f);
        pilot.displayClientMessage(Component.literal("Rumbo a " + planet.getDisplayName() + "..."), true);
    }

    @Override
    public void tick() {
        super.tick();
        // The ship flies, not you: whatever height it drops never counts as your fall
        for (Entity passenger : getPassengers()) passenger.resetFallDistance();
        int age = stateAge();
        switch (getState()) {
            case LIFTOFF -> {
                // Slow at first, then it rockets up
                double speed = 0.02 + age * age * 0.0009;
                setPos(getX(), getY() + speed, getZ());
                exhaust(level(), 3);
                if (!level().isClientSide && age % 10 == 0) {
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.5f, 0.6f);
                }
                if (!level().isClientSide && age >= LIFTOFF_TICKS) travel();
            }
            case LANDING -> {
                // Eases down onto the landing spot
                float t = Math.min(1f, age / (float) LANDING_TICKS);
                double y = groundY + LAND_HEIGHT * (1 - t) * (1 - t);
                setPos(getX(), y, getZ());
                if (t < 0.9f) exhaust(level(), 2);
                if (!level().isClientSide && t >= 1f) {
                    setState(State.IDLE);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.6f, 0.6f);
                }
            }
            case IDLE -> {
                // Parked ships rest on the ground (a ship saved mid-flight settles back down)
                if (!onGround()) {
                    setDeltaMovement(0, Math.max(-0.6, getDeltaMovement().y - 0.04), 0);
                    move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
                }
            }
        }
    }

    private void exhaust(Level level, int count) {
        if (!level.isClientSide) return;
        for (int i = 0; i < count; i++) {
            level.addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 1.5, getY(), getZ() + (random.nextDouble() - 0.5) * 1.5, 0, -0.3, 0);
            level.addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 2, getY() - 0.3, getZ() + (random.nextDouble() - 0.5) * 2, 0, -0.1, 0);
        }
    }

    /** Hyperspace: a new ship appears high over the landing spot of the destination with the pilot aboard. */
    private void travel() {
        ServerLevel target = ((ServerLevel) level()).getServer().getLevel(destination.getDimension());
        if (target == null || !(getFirstPassenger() instanceof ServerPlayer pilot)) {
            setState(State.IDLE);
            return;
        }
        BlockPos land = destination.landing(target);
        SpaceshipEntity arrived = new SpaceshipEntity(ModEntities.SPACESHIP.get(), target);
        arrived.groundY = land.getY();
        arrived.setPos(land.getX() + 0.5, land.getY() + LAND_HEIGHT, land.getZ() + 0.5);
        arrived.setYRot(getYRot());
        target.addFreshEntity(arrived);
        arrived.setState(State.LANDING);

        pilot.stopRiding();
        pilot.teleportTo(target, arrived.getX(), arrived.getY(), arrived.getZ(), pilot.getYRot(), pilot.getXRot());
        pilot.startRiding(arrived, true);
        pilot.resetFallDistance();
        pilot.displayClientMessage(Component.literal("Llegando a " + destination.getDisplayName()), true);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return true;
        if (source.getEntity() instanceof Player p && p.isShiftKeyDown() && getPassengers().isEmpty() && getState() == State.IDLE) {
            if (!p.getAbilities().instabuild) spawnAtLocation(new ItemStack(ModItems.SHIP_CAPSULE.get()));
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dims, float scale) {
        return new Vec3(0, 0.9, 0); // seated inside the hull, eyes level with the window
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        passenger.resetFallDistance();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
