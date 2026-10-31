package com.dragonblock.common.entity;

import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class KiBlastEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BEAM = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);

    /** Client-only: where the projectile first appeared, the beam is drawn from here. */
    public Vec3 origin;

    private float damage = 8.0f;
    private float explosionPower = 1.5f;

    public KiBlastEntity(EntityType<? extends KiBlastEntity> entityType, Level level) {
        super(entityType, level);
    }

    public KiBlastEntity(Level level, LivingEntity shooter, float damage, float explosionPower, int color, float size, boolean beam) {
        super(ModEntities.KI_BLAST.get(), shooter, level);
        this.damage = damage;
        this.explosionPower = explosionPower;
        this.entityData.set(COLOR, color);
        this.entityData.set(SIZE, size);
        this.entityData.set(BEAM, beam);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0x9AB8FF);
        builder.define(SIZE, 0.35f);
        builder.define(BEAM, false);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    public int getColor() { return entityData.get(COLOR); }
    public float getSize() { return entityData.get(SIZE); }
    public boolean isBeam() { return entityData.get(BEAM); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Explosion", explosionPower);
        tag.putInt("Color", getColor());
        tag.putFloat("Size", getSize());
        tag.putBoolean("Beam", isBeam());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        explosionPower = tag.getFloat("Explosion");
        entityData.set(COLOR, tag.getInt("Color"));
        entityData.set(SIZE, tag.getFloat("Size"));
        entityData.set(BEAM, tag.getBoolean("Beam"));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KI_BLAST_ITEM.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (origin == null) origin = this.position();
            int c = getColor();
            DustParticleOptions dust = new DustParticleOptions(
                    new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 1.2f);
            float spread = getSize() * 0.8f;
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(dust,
                        this.getX() + (this.random.nextDouble() - 0.5) * spread,
                        this.getY() + (this.random.nextDouble() - 0.5) * spread,
                        this.getZ() + (this.random.nextDouble() - 0.5) * spread,
                        0.0, 0.0, 0.0);
            }
        }

        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            if (result.getEntity() instanceof LivingEntity target) {
                target.hurt(this.damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity l ? l : null), this.damage);
            }
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            explode();
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
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
                    ModSounds.KI_EXPLODE.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
            this.discard();
        }
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
