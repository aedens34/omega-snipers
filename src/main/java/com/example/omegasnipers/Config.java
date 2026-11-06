package com.example.omegasnipers;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-side config (world/serverconfig/omegasnipers-server.toml).
 */
public class Config {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue VARIANT_COUNT;
    public static final ForgeConfigSpec.DoubleValue BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_PER_TIER;
    public static final ForgeConfigSpec.DoubleValue LOCK_RANGE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;
    public static final ForgeConfigSpec.BooleanValue TARGET_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue TARGET_MOBS;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE_SAME_TEAM;
    public static final ForgeConfigSpec.BooleanValue ONE_SHOT_KILL;
    public static final ForgeConfigSpec.BooleanValue INFINITE_AMMO;
    public static final ForgeConfigSpec.BooleanValue EQUIP_DEFAULT_ATTACHMENT;
    public static final ForgeConfigSpec.BooleanValue SPAWN_VISUAL_BEAM;
    public static final ForgeConfigSpec.DoubleValue PVP_DAMAGE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("omegaSnipers");

        VARIANT_COUNT = builder
                .comment(
                        "How many Omega sniper variants to generate and put in the creative tab.",
                        "There are 120 built-in lore names; each is paired with a real TaCZ gun id",
                        "(cycling through every gun your TaCZ + addons add) so the look/animation is",
                        "always a real, correctly-rendering weapon. Lower this if you only want a",
                        "handful. Raising it above 120 just repeats names with a (II), (III)... suffix.")
                .defineInRange("variantCount", 120, 1, 2000);

        BASE_DAMAGE = builder
                .comment("Base guaranteed-hit damage (in half-hearts) dealt by the lowest tier Omega sniper.")
                .defineInRange("baseDamage", 12.0D, 0.5D, 1000.0D);

        DAMAGE_PER_TIER = builder
                .comment("Extra damage added per tier above the lowest (tiers run 1-10, highest tier = rarest).")
                .defineInRange("damagePerTier", 4.0D, 0.0D, 1000.0D);

        LOCK_RANGE = builder
                .comment(
                        "Max distance (blocks) an Omega sniper will search for a target.",
                        "Set very high for a true 'hits from anywhere' feel on a normal-sized world.")
                .defineInRange("lockRange", 512.0D, 1.0D, 30000000.0D);

        REQUIRE_LINE_OF_SIGHT = builder
                .comment(
                        "If true, the target must be visible (no solid blocks between shooter and target).",
                        "If false (default, matches 'hits from anywhere'), walls/distance never block the shot.")
                .define("requireLineOfSight", false);

        TARGET_PLAYERS = builder
                .comment("If true, Omega snipers can lock onto other players.")
                .define("targetPlayers", true);

        TARGET_MOBS = builder
                .comment("If true, Omega snipers can also lock onto hostile mobs when no player is in range.")
                .define("targetMobs", true);

        FRIENDLY_FIRE_SAME_TEAM = builder
                .comment("If false, players on the shooter's scoreboard team are never targeted.")
                .define("friendlyFireSameTeam", true);

        ONE_SHOT_KILL = builder
                .comment(
                        "If true, every Omega sniper hit sets the target's health to 0 directly (true",
                        "one-shot-kill, ignores armor/absorption entirely). If false, normal damage math",
                        "from baseDamage/damagePerTier/pvpDamageMultiplier applies instead.")
                .define("oneShotKill", false);

        PVP_DAMAGE_MULTIPLIER = builder
                .comment("Extra multiplier applied only when the target is a player (ignored if oneShotKill is true).")
                .defineInRange("pvpDamageMultiplier", 1.0D, 0.01D, 1000.0D);

        INFINITE_AMMO = builder
                .comment("If true, every Omega sniper refills its magazine (and dummy ammo, if any) to full after each shot.")
                .define("infiniteAmmo", true);

        EQUIP_DEFAULT_ATTACHMENT = builder
                .comment(
                        "If true, newly generated Omega snipers spawn with a scope pre-installed and locked",
                        "on, so they always come 'kitted out'. Players can still swap other attachment",
                        "slots freely with anything from TaCZ or any addon - this mod never hardcodes",
                        "addon-specific attachment ids, which is what keeps it compatible with all of them.")
                .define("equipDefaultAttachment", true);

        SPAWN_VISUAL_BEAM = builder
                .comment("If true, a short particle beam is drawn from shooter to target on every guaranteed hit, for flair.")
                .define("spawnVisualBeam", true);

        builder.pop();

        SPEC = builder.build();
    }

    private Config() {
    }
}
