package com.example.omegasnipers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the generated list of every Omega sniper ItemStack (lore name x real
 * TaCZ gun id). Regenerated whenever {@link #refresh()} is called, which
 * happens on server start and can be re-triggered with /omegasnipers reload.
 */
public final class OmegaSniperRegistry {

    private static volatile List<ItemStack> cachedStacks = List.of();

    private OmegaSniperRegistry() {
    }

    public static synchronized void refresh() {
        TaczGunIndex.refresh();
        List<ResourceLocation> baseGuns = TaczGunIndex.allIds();

        if (baseGuns.isEmpty()) {
            OmegaSnipersMod.LOGGER.warn(
                    "[OmegaSnipers] No TaCZ guns were found (base mod or addons). "
                            + "No Omega variants were generated. Make sure TaCZ's gun packs finished loading.");
            cachedStacks = List.of();
            return;
        }

        int count = Config.VARIANT_COUNT.get();
        List<OmegaLore.Entry> entries = OmegaLore.generate(count);

        List<ItemStack> built = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            ResourceLocation baseGun = baseGuns.get(i % baseGuns.size());
            ItemStack stack = OmegaGunFactory.build(baseGun, entries.get(i));
            if (!stack.isEmpty()) {
                built.add(stack);
            }
        }

        cachedStacks = Collections.unmodifiableList(built);
        OmegaSnipersMod.LOGGER.info(
                "[OmegaSnipers] Generated {} Omega sniper variant(s) across {} real TaCZ gun(s).",
                cachedStacks.size(), baseGuns.size());
    }

    /** Returns copies so callers (e.g. the creative tab) can't mutate the shared cache. */
    public static List<ItemStack> allStacksCopy() {
        List<ItemStack> source = cachedStacks;
        List<ItemStack> copies = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            copies.add(stack.copy());
        }
        return copies;
    }

    public static int count() {
        return cachedStacks.size();
    }

    /** Generates the list on first use if nothing has called refresh() yet (e.g. client-side creative tab). */
    public static void ensureGenerated() {
        if (cachedStacks.isEmpty()) {
            refresh();
        }
    }
}
