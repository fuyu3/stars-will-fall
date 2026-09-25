package com.fuyu.starswillfall.item;

import com.fuyu.starswillfall.StarsWillFall;
import com.fuyu.starswillfall.guardasol.GuardaSolTuning;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StarsWillFall.MOD_ID);

    /**
     * Dano total: 1 (jogador) + 3 (tier diamante) + 5 = 9.
     * A classe SwordItem mantém o golpe varrido e o comportamento de espada.
     */
    public static final DeferredItem<ScytheItem> SCYTHE = ITEMS.register("scythe", () -> new ScytheItem(
            Tiers.DIAMOND,
            new Item.Properties().attributes(SwordItem.createAttributes(Tiers.DIAMOND, 5, -2.4F))
    ));

    /**
     * Guarda-sol com duas formas (aberto/espada). Nasce aberto; os atributos da forma fechada são
     * trocados em GuardaSolItem ao abrir/fechar. Números em {@link GuardaSolTuning}.
     */
    public static final DeferredItem<GuardaSolItem> GUARDA_SOL = ITEMS.register("guarda_sol", () -> new GuardaSolItem(
            new Item.Properties()
                    .durability(GuardaSolTuning.MAX_DURABILITY)
                    .rarity(Rarity.RARE)
                    .attributes(GuardaSolItem.OPEN_ATTRIBUTES)
    ));

    private ModItems() {
    }
}
