package com.fuyu.starswillfall;

import com.fuyu.starswillfall.entity.ModEntities;
import com.fuyu.starswillfall.item.ModDataComponents;
import com.fuyu.starswillfall.item.ModItems;
import com.fuyu.starswillfall.particle.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(StarsWillFall.MOD_ID)
public final class StarsWillFall {
    public static final String MOD_ID = "starswillfall";

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARS_WILL_FALL_TAB =
            CREATIVE_TABS.register("stars_will_fall", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.starswillfall"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SCYTHE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SCYTHE.get());
                        output.accept(ModItems.GUARDA_SOL.get());
                    })
                    .build());

    public StarsWillFall(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
