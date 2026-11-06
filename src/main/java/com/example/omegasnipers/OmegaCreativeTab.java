package com.example.omegasnipers;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * A dedicated "Omega Snipers" tab in the creative inventory, populated with
 * every generated Omega gun. displayItems is lazy (runs each time the tab is
 * opened), so it always reflects whatever TaCZ addons are currently loaded.
 */
public final class OmegaCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OmegaSnipersMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> OMEGA_SNIPERS_TAB = CREATIVE_MODE_TABS.register(
            "omega_snipers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.omegasnipers.omega_snipers"))
                    .icon(() -> new ItemStack(Items.NETHER_STAR))
                    .displayItems((parameters, output) -> {
                        OmegaSniperRegistry.ensureGenerated();
                        for (ItemStack stack : OmegaSniperRegistry.allStacksCopy()) {
                            output.accept(stack);
                        }
                    })
                    .build());

    private OmegaCreativeTab() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
