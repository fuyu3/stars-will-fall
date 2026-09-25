package com.fuyu.starswillfall.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Estrelinha que cintila, desacelera e some. A imagem vem de
 * assets/starswillfall/textures/particle/star.png (listada em particles/star.json); ela é
 * pintada de amarelo-claro aqui, então use uma textura branca/clara.
 */
public class StarParticle extends TextureSheetParticle {
    protected StarParticle(ClientLevel level, double x, double y, double z,
                           double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.friction = 0.9F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 14 + this.random.nextInt(12);
        this.quadSize = 0.08F + this.random.nextFloat() * 0.08F;
        this.setColor(1.0F, 0.95F, 0.6F);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - life * life;
        // Cintilar: o tamanho pulsa enquanto a estrela vive.
        this.quadSize *= 0.95F + 0.1F * (float) Math.abs(Math.sin(this.age * 1.3D));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 15728880; // brilho máximo, como as partículas de end rod
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public StarParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed) {
            return new StarParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
