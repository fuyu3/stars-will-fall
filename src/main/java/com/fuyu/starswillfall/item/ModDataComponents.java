package com.fuyu.starswillfall.item;

import com.fuyu.starswillfall.StarsWillFall;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, StarsWillFall.MOD_ID);

    /** true = guarda-sol fechado (forma de espada). Ausente/false = guarda-sol aberto. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> GUARDA_SOL_CLOSED =
            DATA_COMPONENTS.registerComponentType("guarda_sol_closed", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /** true = foice no modo impulso estelar. Ausente/false = modo bumerangue. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCYTHE_DASH_MODE =
            DATA_COMPONENTS.registerComponentType("scythe_dash_mode", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    private ModDataComponents() {
    }
}
