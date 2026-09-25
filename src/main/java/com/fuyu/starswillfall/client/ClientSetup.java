package com.fuyu.starswillfall.client;

import com.fuyu.starswillfall.StarsWillFall;
import com.fuyu.starswillfall.entity.ModEntities;
import com.fuyu.starswillfall.item.ModItems;
import com.fuyu.starswillfall.particle.ModParticles;
import com.fuyu.starswillfall.item.GuardaSolItem;
import com.fuyu.starswillfall.item.ScytheItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Registros só do cliente: propriedades de item, renderizador da foice arremessada e partícula.
 *
 * <p>Propriedades de item que os modelos podem usar em "overrides":
 * <ul>
 *   <li>{@code starswillfall:closed}  = 1 quando o guarda-sol está fechado (espada);</li>
 *   <li>{@code starswillfall:gliding} = 1 enquanto o jogador está planando;</li>
 *   <li>{@code starswillfall:dash_mode} = 1 quando a foice está no modo impulso estelar.</li>
 * </ul>
 * Veja assets/starswillfall/models/item/guarda_sol.json.
 */
@EventBusSubscriber(modid = StarsWillFall.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.GUARDA_SOL.get(),
                    ResourceLocation.fromNamespaceAndPath(StarsWillFall.MOD_ID, "closed"),
                    (stack, level, entity, seed) -> GuardaSolItem.isClosed(stack) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.GUARDA_SOL.get(),
                    ResourceLocation.fromNamespaceAndPath(StarsWillFall.MOD_ID, "gliding"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem().is(stack.getItem()) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.SCYTHE.get(),
                    ResourceLocation.fromNamespaceAndPath(StarsWillFall.MOD_ID, "dash_mode"),
                    (stack, level, entity, seed) -> ScytheItem.isDashMode(stack) ? 1.0F : 0.0F);
        });
    }

    /** Desenho da foice arremessada. */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SCYTHE_PROJECTILE.get(), ScytheProjectileRenderer::new);
    }

    /** Partícula de estrela. */
    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.STAR.get(), StarParticle.Provider::new);
    }
}
