package com.example.omegasnipers;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Builds one finished Omega sniper ItemStack from a lore entry + a real TaCZ
 * gun id. The item is 100% a normal TaCZ gun as far as TaCZ itself is
 * concerned (built through the same GunItemBuilder every other TaCZ gun item
 * uses) - it just carries a fancy name/lore, maxed-out ammo, a locked-on
 * default attachment, and a hidden NBT marker that OmegaCombatHandler reads
 * to apply the overpowered lock-on behaviour.
 */
public final class OmegaGunFactory {

    /** Root NBT compound key this mod stores its data under on the gun ItemStack. */
    public static final String NBT_ROOT = "OmegaSnipers";
    public static final String NBT_TIER = "Tier";
    public static final String NBT_BASE_GUN = "BaseGun";

    private OmegaGunFactory() {
    }

    public static ItemStack build(ResourceLocation baseGunId, OmegaLore.Entry entry) {
        int magazineSize = lookUpMagazineSize(baseGunId);

        ItemStack stack;
        try {
            GunItemBuilder builder = GunItemBuilder.create()
                    .setId(baseGunId)
                    .setFireMode(FireMode.SEMI)
                    .setAmmoCount(magazineSize)
                    .setAmmoInBarrel(true);
            stack = builder.build();
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.error("[OmegaSnipers] Failed building Omega variant for base gun " + baseGunId, t);
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            return stack;
        }

        applyCosmetics(stack, entry);
        applyMarker(stack, baseGunId, entry.tier());
        applyGunplayDefaults(stack, magazineSize);

        if (Config.EQUIP_DEFAULT_ATTACHMENT.get()) {
            tryEquipDefaultScope(stack);
        }

        return stack;
    }

    private static void applyCosmetics(ItemStack stack, OmegaLore.Entry entry) {
        ChatFormatting tierColor = tierColor(entry.tier());
        stack.setHoverName(Component.literal(entry.displayName())
                .withStyle(tierColor, ChatFormatting.BOLD));

        CompoundTag display = stack.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal(entry.loreLine())
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal("Tier " + entry.tier() + "/10")
                        .withStyle(tierColor))));
        display.put("Lore", lore);
    }

    private static void applyMarker(ItemStack stack, ResourceLocation baseGunId, int tier) {
        CompoundTag root = stack.getOrCreateTagElement(NBT_ROOT);
        root.putInt(NBT_TIER, tier);
        root.putString(NBT_BASE_GUN, baseGunId.toString());
    }

    private static void applyGunplayDefaults(ItemStack stack, int magazineSize) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return;
        }
        try {
            gun.setCurrentAmmoCount(stack, magazineSize);
            gun.setBulletInBarrel(stack, true);
            if (gun.hasMaxDummyAmmo(stack)) {
                int maxDummy = gun.getMaxDummyAmmoAmount(stack);
                if (maxDummy > 0) {
                    gun.setDummyAmmoAmount(stack, maxDummy);
                }
            }
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.debug("[OmegaSnipers] Could not fully prime ammo on generation.", t);
        }
    }

    /**
     * Best-effort only: installs TaCZ's own confirmed default scope id
     * (tacz:sight_sro_dot) and locks attachments so it doesn't fall off.
     * Deliberately does NOT try to guess addon-specific attachment ids -
     * that's what keeps this safe to run with any combination of addons.
     * Players remain free to swap any other slot with anything their
     * installed addons provide.
     */
    private static void tryEquipDefaultScope(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return;
        }
        try {
            ItemStack scope = AttachmentItemBuilder.create()
                    .setId(new ResourceLocation("tacz", "sight_sro_dot"))
                    .build();
            if (!scope.isEmpty() && gun.allowAttachment(stack, scope)) {
                gun.installAttachment(stack, scope);
            }
            gun.setAttachmentLock(stack, true);
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.debug("[OmegaSnipers] Could not pre-install default scope.", t);
        }
    }

    /** Public so OmegaCombatHandler can re-derive a gun's magazine size for the infinite-ammo refill. */
    public static int lookUpMagazineSize(ResourceLocation gunId) {
        try {
            Optional<CommonGunIndex> indexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (indexOpt.isPresent()) {
                int amount = indexOpt.get().getGunData().getAmmoAmount();
                if (amount > 0) {
                    return amount;
                }
            }
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.debug("[OmegaSnipers] Could not read magazine size for {}", gunId, t);
        }
        return 10;
    }

    public static ChatFormatting tierColor(int tier) {
        return switch (Math.max(1, Math.min(10, tier))) {
            case 1, 2, 3 -> ChatFormatting.GREEN;
            case 4, 5, 6 -> ChatFormatting.AQUA;
            case 7, 8 -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GOLD;
        };
    }

    /** Reads the Omega tier NBT marker back off a stack, or -1 if it isn't an Omega gun. */
    public static int readTier(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_ROOT)) {
            return -1;
        }
        CompoundTag root = stack.getTag().getCompound(NBT_ROOT);
        if (!root.contains(NBT_TIER)) {
            return -1;
        }
        return root.getInt(NBT_TIER);
    }

    public static boolean isOmegaGun(ItemStack stack) {
        return readTier(stack) >= 0;
    }

    /** Reads the real TaCZ gun id this Omega variant was built from, or null if this isn't an Omega gun. */
    public static ResourceLocation readBaseGunId(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_ROOT)) {
            return null;
        }
        CompoundTag root = stack.getTag().getCompound(NBT_ROOT);
        if (!root.contains(NBT_BASE_GUN)) {
            return null;
        }
        return ResourceLocation.tryParse(root.getString(NBT_BASE_GUN));
    }
}
