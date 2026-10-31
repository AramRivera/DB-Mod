package com.dragonblock.common.entity;

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
import net.minecraft.server.level.ServerLevel;
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

    /** Client-only: tail end of the beam. Held at the shooter's hands while they fire, then it catches up. */
    public Vec3 origin;
    public Vec3 prevOrigin;

    private float damage = 8.0f;
    private float explosionPower = 1.5f;
    private final Set<Integer> pierced = new HashSet<>();

    public KiBlastEntity(EntityType<? extends KiBlastEntity> entityType, Level level) {
        super(entityType, level);
    }

    public KiBlastEntity(Level level, LivingEntity shooter, float damage, float explosionPower, int color, float size, Technique tech) {
        super(ModEntities.KI_BLAST.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
        this.entityData.set(COLOR, color);
        this.entityData.set(SIZE, size);
        this.entityData.set(TECH, tech.ordinal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0x9AB8FF);
        builder.define(SIZE, 0.35f);
        builder.define(TECH, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    public int getColor() { return entityData.get(COLOR); }
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        explosionPower = tag.getFloat("Explosion");
        entityData.set(COLOR, tag.getInt("Color"));
        entityData.set(SIZE, tag.getFloat("Size"));
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
        super.tick();

        if (this.level().isClientSide) {
            clientTick();
        } else if (getTechnique().getKind() == Technique.Kind.BOMB) {
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

    private void clientTick() {
        Technique tech = getTechnique();
        Vec3 here = position();
        prevOrigin = origin;
        if (origin == null) origin = prevOrigin = here;
        if (isBeam()) {
            // While the shooter still holds the firing pose the beam stays attached to their hands
            Entity owner = getOwner();
            if (owner != null && tickCount <= tech.getSustain()) origin = handAnchor(owner, tech);
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
