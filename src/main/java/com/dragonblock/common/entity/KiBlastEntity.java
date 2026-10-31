package com.dragonblock.common.entity;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * Every ki technique that travels: blasts, beams, piercing rays, discs and giant spheres. The technique
 * is synced so each client draws it the right way; piercing kinds pass through what they hit.
 */
public class KiBlastEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TECH = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    /** Aura color of the shooter's form, drawn as a halo; 0 in base form. */
    private static final EntityDataAccessor<Integer> HALO = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    /** Ticks left in a clash with another technique, 0 when flying freely. */
    private static final EntityDataAccessor<Integer> CLASH = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    public static final int CLASH_TICKS = 50;
    /** Largest size a technique can reach (a full Genkidama in the top forms is about 7.3). */
    public static final float MAX_SIZE = 8f;

    /** Client-only: tail end of the beam. Held at the shooter's hands while they fire, then it catches up. */
    public Vec3 origin;
    public Vec3 prevOrigin;

    private float damage = 8.0f;
    private float explosionPower = 1.5f;
    private final Set<Integer> pierced = new HashSet<>();
    // Server-only clash state
    private int clashPartner = -1;
    private Vec3 frozenVelocity = Vec3.ZERO;
    private float clashBoost = 1f;

    public KiBlastEntity(EntityType<? extends KiBlastEntity> entityType, Level level) {
        super(entityType, level);
    }

    public KiBlastEntity(Level level, LivingEntity shooter, float damage, float explosionPower, int color, float size, Technique tech) {
        super(ModEntities.KI_BLAST.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
        this.entityData.set(COLOR, color);
        this.entityData.set(SIZE, Math.min(MAX_SIZE, size));
        this.entityData.set(TECH, tech.ordinal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0x9AB8FF);
        builder.define(SIZE, 0.35f);
        builder.define(TECH, 0);
        builder.define(HALO, 0);
        builder.define(CLASH, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    public int getColor() { return entityData.get(COLOR); }
    public int getHalo() { return entityData.get(HALO); }
    public void setHalo(int rgb) { entityData.set(HALO, rgb); }
    public boolean isClashing() { return entityData.get(CLASH) > 0; }
    /** 0..1 progress through the current clash. */
    public float clashProgress() { return 1f - entityData.get(CLASH) / (float) CLASH_TICKS; }
    public float getSize() { return entityData.get(SIZE); }

    public Technique getTechnique() {
        Technique[] all = Technique.values();
        int i = entityData.get(TECH);
        return i >= 0 && i < all.length ? all[i] : Technique.KI_BLAST;
    }

    public boolean isBeam() {
        Technique.Kind kind = getTechnique().getKind();
        return kind == Technique.Kind.BEAM || kind == Technique.Kind.PIERCE;
    }

    private int lifetime() {
        return getTechnique().getKind() == Technique.Kind.BOMB ? 220 : 100;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Explosion", explosionPower);
        tag.putInt("Color", getColor());
        tag.putFloat("Size", getSize());
        tag.putString("Technique", getTechnique().name());
        tag.putInt("Halo", getHalo());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        explosionPower = tag.getFloat("Explosion");
        entityData.set(COLOR, tag.getInt("Color"));
        entityData.set(SIZE, Math.max(0.05f, Math.min(MAX_SIZE, tag.getFloat("Size"))));
        entityData.set(HALO, tag.getInt("Halo"));
        try {
            entityData.set(TECH, Technique.valueOf(tag.getString("Technique")).ordinal());
        } catch (IllegalArgumentException e) {
            entityData.set(TECH, 0);
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KI_BLAST_ITEM.get();
    }

    @Override
    public void tick() {
        if (isClashing()) {
            // Locked in place against the other technique: no movement, no collisions
            xo = getX();
            yo = getY();
            zo = getZ();
            if (level().isClientSide) clientTick();
            else tickClash();
            return;
        }
        super.tick();

        if (this.level().isClientSide) {
            clientTick();
        } else if (canClash()) {
            findClash();
            if (isRemoved() || isClashing()) return;
        }
        if (!this.level().isClientSide && getTechnique().getKind() == Technique.Kind.BOMB) {
            // A giant sphere hits whatever it rolls over, not only what its small hitbox touches
            float r = getSize();
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r),
                    e -> e != getOwner() && e.isAlive() && e.distanceTo(this) < r + e.getBbWidth())) {
                onHitEntity(new EntityHitResult(e));
                if (isRemoved()) return;
            }
        }

        if (this.tickCount > lifetime()) {
            if (!level().isClientSide && getTechnique().getKind() == Technique.Kind.BOMB) explode();
            else this.discard();
        }
    }

    // ------------------------------------------------------------------ clashes

    /** Beams, spheres and blasts collide with other fighters' techniques; piercing rays and discs cut through. */
    private boolean canClash() {
        Technique.Kind kind = getTechnique().getKind();
        return kind != Technique.Kind.PIERCE && kind != Technique.Kind.DISC;
    }

    private float clashPower() {
        return damage * clashBoost;
    }

    private void findClash() {
        Entity owner = getOwner();
        for (KiBlastEntity other : level().getEntitiesOfClass(KiBlastEntity.class, getBoundingBox().inflate(getSize() + 1.5),
                o -> o != this && !o.isRemoved() && o.canClash() && !o.isClashing() && o.getOwner() != owner)) {
            if (other.distanceTo(this) > (getSize() + other.getSize()) * 0.9f + 0.6f) continue;
            if (getTechnique().getKind() == Technique.Kind.BLAST || other.getTechnique().getKind() == Technique.Kind.BLAST) {
                resolveClash(other); // small shots just knock each other out
            } else {
                beginClash(other);
                other.beginClash(this);
                for (KiBlastEntity side : new KiBlastEntity[]{this, other}) {
                    if (side.getOwner() instanceof ServerPlayer p) {
                        p.displayClientMessage(Component.literal("¡Choque de ki! Mantén la tecla de técnica para empujar"), true);
                    }
                }
                level().playSound(null, getX(), getY(), getZ(), ModSounds.KI_EXPLODE.get(), SoundSource.PLAYERS, 2.5f, 0.5f);
            }
            return;
        }
    }

    private void beginClash(KiBlastEntity other) {
        clashPartner = other.getId();
        frozenVelocity = getDeltaMovement();
        setDeltaMovement(Vec3.ZERO);
        entityData.set(CLASH, CLASH_TICKS);
    }

    private void tickClash() {
        int left = entityData.get(CLASH) - 1;
        // Struggle: an owner still holding the technique key pours ki into the clash
        if (getOwner() instanceof ServerPlayer p) {
            PlayerDBCData data = PlayerDBCData.get(p);
            if (data.isTechHeld() && data.consumeKi(0.6f)) clashBoost += 0.02f;
        }
        if (level() instanceof ServerLevel sl && (left % 3) == 0) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 6, getSize(), getSize(), getSize(), 0.4);
        }
        Entity partner = level().getEntity(clashPartner);
        if (!(partner instanceof KiBlastEntity other) || other.isRemoved() || !other.isClashing()) {
            release(); // the other side vanished: carry on
            return;
        }
        entityData.set(CLASH, Math.max(1, left));
        // The lower id settles it, so both sides don't resolve the same clash
        if (left <= 0 && getId() < other.getId()) resolveClash(other);
    }

    private void release() {
        entityData.set(CLASH, 0);
        clashPartner = -1;
        if (frozenVelocity.lengthSqr() > 0) setDeltaMovement(frozenVelocity);
        hasImpulse = true;
    }

    /** The stronger technique (with a little luck) wins: the loser blows up, the winner goes on weakened. */
    private void resolveClash(KiBlastEntity other) {
        float mine = clashPower() * (0.9f + random.nextFloat() * 0.2f);
        float theirs = other.clashPower() * (0.9f + random.nextFloat() * 0.2f);
        KiBlastEntity winner = mine >= theirs ? this : other;
        KiBlastEntity loser = winner == this ? other : this;
        float ratio = Math.min(mine, theirs) / Math.max(1e-3f, Math.max(mine, theirs));
        winner.damage *= Math.max(0.3f, 1f - ratio * 0.6f);
        winner.entityData.set(SIZE, Math.min(MAX_SIZE, winner.getSize() * 1.15f));
        winner.release();
        if (level() instanceof ServerLevel sl) {
            Vec3 mid = position().add(other.position()).scale(0.5);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
        }
        loser.release();
        loser.explode();
    }

    // ------------------------------------------------------------------ client

    private void clientTick() {
        Technique tech = getTechnique();
        Vec3 here = position();
        prevOrigin = origin;
        if (origin == null) origin = prevOrigin = here;
        if (isBeam()) {
            // While the shooter still holds the firing pose the beam stays attached to their hands
            Entity owner = getOwner();
            if (owner != null && (tickCount <= tech.getSustain() || isClashing())) origin = handAnchor(owner, tech);
            else origin = origin.add(here.subtract(origin).scale(tech == Technique.DEATH_BEAM ? 0.6 : 0.3));
        }

        int c = getColor();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.2f);
        float spread = getSize() * 0.8f;
        int count = tech.getKind() == Technique.Kind.BOMB ? 5 : 2;
        for (int i = 0; i < count; i++) {
            this.level().addParticle(dust,
                    this.getX() + (this.random.nextDouble() - 0.5) * spread,
                    this.getY() + (this.random.nextDouble() - 0.5) * spread,
                    this.getZ() + (this.random.nextDouble() - 0.5) * spread,
                    0.0, 0.0, 0.0);
        }
    }

    /** Where a beam leaves the body: in front of the chest, or of the forehead for overhead stances. */
    public static Vec3 handAnchor(Entity owner, Technique tech) {
        Vec3 look = owner.getLookAngle();
        double down = switch (tech.getStance()) {
            case OVERHEAD, FOREHEAD -> 0.05;
            case FINGER, ONE_PALM -> 0.3;
            default -> 0.4;
        };
        return owner.getEyePosition().add(look.scale(0.8)).add(0, -down * owner.getBbHeight() / 1.8, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (level().isClientSide || hit == getOwner()) return;
        Technique.Kind kind = getTechnique().getKind();
        boolean passesThrough = kind == Technique.Kind.PIERCE || kind == Technique.Kind.DISC;
        if (passesThrough && !pierced.add(hit.getId())) return;

        if (hit instanceof LivingEntity target) {
            target.hurt(this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity l ? l : null), this.damage);
        }
        if (passesThrough) {
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, hit.getX(), hit.getY(0.5), hit.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
            return;
        }
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) return;
        if (getTechnique().getKind() == Technique.Kind.DISC) {
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.3);
            discard();
            return;
        }
        explode();
    }

    private void explode() {
        if (explosionPower > 0.05f) {
            // The shooter is never hurt by their own blast
            Entity owner = this.getOwner();
            ExplosionDamageCalculator sparesOwner = new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    return entity != owner && super.shouldDamageEntity(explosion, entity);
                }
            };
            this.level().explode(this, null, sparesOwner, this.getX(), this.getY(), this.getZ(),
                    this.explosionPower, false, Level.ExplosionInteraction.MOB);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.KI_EXPLODE.get(), SoundSource.PLAYERS, 1.5f, explosionPower > 5 ? 0.6f : 1.0f);
        }
        this.discard();
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getExplosionPower() {
        return explosionPower;
    }

    public void setExplosionPower(float explosionPower) {
        this.explosionPower = explosionPower;
    }
}
