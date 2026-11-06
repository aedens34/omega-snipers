package com.example.omegasnipers;

import com.tacz.guns.api.TimelessAPI;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mirrors TimelessAPI's live gun index (base TaCZ gun pack + every installed
 * addon's gun pack). Omega variants are built ON TOP of whatever real gun ids
 * this returns, which is what guarantees they always have a correct model,
 * animation set and sound - and automatically "supports" any addon without a
 * single line of addon-specific code, because it never hardcodes a gun id.
 *
 * Handles both plausible return shapes of TimelessAPI.getAllCommonGunIndex()
 * (a Map<ResourceLocation, CommonGunIndex>, or a Collection of entries) via
 * instanceof, since the exact return type isn't pinned down in TaCZ's public
 * docs and can differ slightly between builds.
 */
public final class TaczGunIndex {

    private static volatile List<ResourceLocation> cachedIds = List.of();

    private TaczGunIndex() {
    }

    public static synchronized void refresh() {
        List<ResourceLocation> ids = fetchAllGunIds();
        cachedIds = ids;
        OmegaSnipersMod.LOGGER.info("[OmegaSnipers] Found {} real TaCZ gun(s) to build Omega variants on top of.", ids.size());
    }

    public static List<ResourceLocation> allIds() {
        return cachedIds;
    }

    private static List<ResourceLocation> fetchAllGunIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        try {
            Object result = TimelessAPI.getAllCommonGunIndex();

            if (result instanceof Map<?, ?> map) {
                for (Object key : map.keySet()) {
                    if (key instanceof ResourceLocation rl) {
                        ids.add(rl);
                    }
                }
            } else if (result instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item instanceof Map.Entry<?, ?> entry && entry.getKey() instanceof ResourceLocation rl) {
                        ids.add(rl);
                    } else if (item instanceof ResourceLocation rl) {
                        ids.add(rl);
                    }
                }
            } else if (result != null) {
                OmegaSnipersMod.LOGGER.warn(
                        "[OmegaSnipers] TimelessAPI.getAllCommonGunIndex() returned an unrecognised type: {}.",
                        result.getClass());
            }
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.error("[OmegaSnipers] Failed to read TaCZ's gun index.", t);
        }
        return ids;
    }
}
