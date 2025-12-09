package com.momosoftworks.irradiated.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class VaporParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final boolean hasGravity;
    private boolean collidedY;
    private float maxAlpha;

    protected VaporParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, boolean hasGravity) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.alpha = 0.0F;
        this.maxAlpha = (float)(Math.random() / 3.0D + 0.2D);
        float size = 0.3F + (float)(Math.random() / 2.5D);
        this.setSize(size, size);
        this.scale(size / 10.0F);
        this.lifetime = 40 + (int)(Math.random() * 20.0D - 10.0D);
        this.hasPhysics = true;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasGravity = hasGravity;
        this.gravity = hasGravity ? 0.04F : -0.04F;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().options.particles().get().getId() < 2) {
            this.remove();
            return;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.yd -= 0.04D * this.gravity;
            this.move(this.xd * (this.onGround ? 1.0D : 0.2D), this.yd, this.zd * (this.onGround ? 1.0D : 0.2D));
            this.xd *= 0.99D;
            this.yd *= 0.99D;
            this.zd *= 0.99D;
        }

        this.setSpriteFromAge(this.spriteSet);
        
        if (this.hasGravity) {
            if (this.alpha < this.maxAlpha) {
                this.alpha += 0.02F;
            } else if (this.age > 32) {
                this.alpha -= 0.02F;
            }

            if (this.alpha < 0.035F && this.age > 10) {
                this.remove();
            }
        } else {
            if (this.age < 10) {
                this.alpha += 0.07F;
            } else if (this.age > this.lifetime - this.alpha / 0.02F) {
                this.alpha -= 0.02F;
            }

            if (this.alpha < 0.07F && this.age > 10) {
                this.remove();
            }
        }
    }

    @Override
    public void move(double x, double y, double z) {
        double d0 = x;
        double d1 = y;
        if (this.hasPhysics && (x != 0.0D || y != 0.0D || z != 0.0D)) {
            // Simplified collision detection for particles - check if we hit a solid block
            AABB newBB = this.getBoundingBox().move(x, y, z);
            if (!this.level.noCollision(newBB)) {
                // If we would collide, stop movement
                x = 0.0D;
                y = 0.0D;
                z = 0.0D;
            }
        }

        if (x != 0.0D || y != 0.0D || z != 0.0D) {
            this.setBoundingBox(this.getBoundingBox().move(x, this.collidedY ? 0.0D : y, z));
            AABB axisalignedbb = this.getBoundingBox();
            this.x = (axisalignedbb.minX + axisalignedbb.maxX) / 2.0D;
            this.y = axisalignedbb.minY + (this.hasGravity ? 0.2D : 0.0D);
            this.z = (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D;
        }

        if (Math.abs(y) >= 1.0E-5D && Math.abs(y) < 1.0E-5D) {
            this.collidedY = true;
        }

        this.onGround = y != y && d1 < 0.0D;
        if (d0 != x) {
            this.xd = 0.0D;
        }

        if (z != z) {
            this.zd = 0.0D;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MistFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public MistFactory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            int particleSetting = Minecraft.getInstance().options.particles().get().getId();
            return particleSetting >= 2 ? new VaporParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite, true) : null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SteamFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public SteamFactory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            int particleSetting = Minecraft.getInstance().options.particles().get().getId();
            return particleSetting >= 1 ? new VaporParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite, false) : null;
        }
    }
}
