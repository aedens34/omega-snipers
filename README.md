# Omega Snipers (TaCZ Addon)

A Forge 1.20.1 TaCZ addon that adds **120+ overpowered "Omega" sniper
variants** to the creative inventory, each with:

- A unique lore name and tier (`Omega Wraithfang MK-IV`, `Omega Voidreaper
  MK-IX`, ...), colour-coded by rarity in its tooltip.
- **Guaranteed hit / lock-on**: firing one cancels the normal travelling
  bullet and instantly hits the nearest valid target (players by default,
  optionally mobs too) within a configurable range - no walls, distance, or
  travel time involved, by default. "Hits from anywhere."
- **Auto ammo refill**: the magazine (and dummy ammo, if the base gun uses
  it) tops itself back up after every shot. Effectively infinite ammo.
- **A pre-installed, locked scope attachment** out of the box, with every
  other attachment slot left free for you to fill with anything from TaCZ
  or any addon.
- Fully configurable damage, range, one-shot-kill toggle, friendly fire,
  line-of-sight requirement, and more.

## Important: how this stays compatible with every TaCZ addon

This mod does **not** invent 120+ brand-new 3D gun models - nobody can
generate original, licensed 3D art as code, and copying real guns' models
would be both impossible to fetch here and a copyright problem. Instead,
every Omega variant is built **on top of a real, already-registered TaCZ
gun id**, using TaCZ's own `GunItemBuilder` (the exact same builder every
TaCZ gun item is made from). The pool of base gun ids is read live from
`TimelessAPI.getAllCommonGunIndex()` - TaCZ's own registry of every gun the
base mod *and every installed addon* has added.

That means:
- Every Omega gun renders, animates, reloads, and sounds exactly like a
  real, correctly-configured TaCZ weapon, because it *is* one under the
  hood - it just carries a fancy name, maxed ammo, and the lock-on behaviour.
- It automatically works with **any** TaCZ addon you have installed, with
  zero addon-specific code: more installed guns just means more variety in
  which real gun each Omega name is paired with.
- If you only have base TaCZ installed (a handful of guns), you still get
  120 *named* Omega variants - they just reuse those few base guns more
  than once, each under a different name/tier/lock-on stats.

## What's in here

```
build.gradle, gradle.properties, settings.gradle   - ForgeGradle build config
src/main/resources/META-INF/mods.toml              - mod metadata, requires TaCZ
src/main/resources/assets/omegasnipers/lang/en_us.json
src/main/java/.../OmegaSnipersMod.java              - entry point
src/main/java/.../Config.java                       - every tunable (damage, range, one-shot-kill, etc.)
src/main/java/.../TaczGunIndex.java                 - reads TaCZ's live gun list (base + every addon)
src/main/java/.../OmegaLore.java                    - generates 120+ unique lore names/tiers
src/main/java/.../OmegaGunFactory.java               - builds one Omega ItemStack (name, ammo, attachment, marker tag)
src/main/java/.../OmegaSniperRegistry.java           - pairs lore entries with real gun ids, caches the final list
src/main/java/.../OmegaCreativeTab.java              - the "Omega Snipers" creative inventory tab
src/main/java/.../OmegaCombatHandler.java            - the lock-on / guaranteed-hit / ammo-refill logic
src/main/java/.../OmegaCommands.java                 - /omegasnipers reload|list|give
```

## 1. Get a Forge 1.20.1 dev environment

1. Download the **Forge 1.20.1 MDK** from https://files.minecraftforge.net/
   (pick 1.20.1; 47.3.0 is what `gradle.properties` assumes - change
   `forge_version` if you grab a different build).
2. Unzip it, e.g. into `omega-mod/`.
3. Copy everything from this project into `omega-mod/`, **overwriting**
   `build.gradle`, `gradle.properties`, `settings.gradle`, and `src/`. Keep
   the MDK's `gradlew`, `gradlew.bat`, and `gradle/` folder.

## 2. Pin the TaCZ dependency

TaCZ isn't on Maven Central, so the build pulls a specific CurseForge file
via [Curse Maven](https://www.cursemaven.com/).

1. Open the 1.20.1 TaCZ file you want to build against at
   https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero/files
2. Get its `curse.maven:timeless-and-classics-zero-XXXXXXX:YYYYYYY`
   coordinate from cursemaven.com.
3. Paste it into `gradle.properties` as `tacz_curse_maven_coordinate`. A
   starting value is already filled in but **check it's still current**
   (CurseForge file IDs are specific to one release).

You only need the base TaCZ jar as a Gradle dependency - addons only need
to be present at *runtime*, since this mod talks to them indirectly through
TaCZ's own API, never addon code directly.

## 3. Build

```
cd omega-mod
./gradlew build          # Windows: gradlew.bat build
```

Needs JDK 17. The jar lands at `build/libs/omegasnipers-1.0.0.jar`.

### If it doesn't compile

Every TaCZ class/method here (`TimelessAPI`, `IGun`, `GunItemBuilder`,
`AttachmentItemBuilder`, `FireMode`, `CommonGunIndex`, `GunShootEvent`) is
taken from TaCZ's own published API docs/examples, with one soft spot:

- **`GunItemBuilder`'s exact package**
  (`com.tacz.guns.api.item.builder.GunItemBuilder`, used in
  `OmegaGunFactory.java`) is inferred from TaCZ's class-naming pattern, not
  confirmed letter-for-letter. If that one import fails, open the project
  in an IDE (the `tacz` dependency includes sources), delete the bad
  import, and let "organize imports" find the real package for
  `GunItemBuilder` and `AttachmentItemBuilder` - two clicks.
- `GunShootEvent` is assumed to expose `getShooter()` and `getLogicalSide()`
  (both documented) but **not** the gun ItemStack directly, so
  `OmegaCombatHandler` reads the stack from the shooter's main/off hand
  instead. If your TaCZ build's `GunShootEvent` *does* have a
  `getGunItemStack()`-style accessor, feel free to swap to that for
  perfect accuracy (the current main/off-hand fallback already covers the
  overwhelming majority of real play, so this is a nice-to-have, not a
  blocker).
- `TaczGunIndex.fetchAllGunIds()` is written defensively (handles either a
  `Map` or a collection-of-entries return shape) since that exact return
  type isn't nailed down in TaCZ's docs - this one should just work. The
  server-start log line `[OmegaSnipers] Found N real TaCZ gun(s)...`
  confirms it worked.

## 4. Install & configure

Drop `omegasnipers-1.0.0.jar` into `mods/` alongside TaCZ (and any TaCZ
addons - picked up automatically).

Open the creative inventory, find the **Omega Snipers** tab (nether-star
icon), and every generated variant is right there, ready to use. In
survival, give one with:

```
/omegasnipers give <player> [index]
/omegasnipers list
/omegasnipers reload
```

Config lives at `world/serverconfig/omegasnipers-server.toml`
(or `saves/<world>/serverconfig/...` singleplayer) after the first launch:

| Option | Default | What it does |
|---|---|---|
| `variantCount` | `120` | How many Omega guns to generate |
| `baseDamage` / `damagePerTier` | `12` / `4` | Guaranteed-hit damage scaling by tier (1-10) |
| `oneShotKill` | `false` | `true` = every hit is lethal, armor/absorption irrelevant |
| `pvpDamageMultiplier` | `1.0` | Extra multiplier, players only (ignored if `oneShotKill`) |
| `lockRange` | `512` | Max blocks an Omega gun will search for a target |
| `requireLineOfSight` | `false` | `false` = true "hits from anywhere" through walls |
| `targetPlayers` / `targetMobs` | `true` / `true` | What it's allowed to lock onto |
| `friendlyFireSameTeam` | `true` | Set `false` to protect scoreboard teammates |
| `infiniteAmmo` | `true` | Auto-refill magazine after every shot |
| `equipDefaultAttachment` | `true` | Spawn with a locked-on default scope |
| `spawnVisualBeam` | `true` | Particle tracer from shooter to target on hit |

Restart the server/world to apply config changes.

## A note on balance

Every number above is in a config file specifically so you can tune this
down from "instant-kill through walls" to something saner (e.g.
`requireLineOfSight = true`, `oneShotKill = false`, a shorter `lockRange`)
if you want it as a strong-but-fair endgame weapon rather than a pure
creative/admin toy. Defaults are deliberately extreme since that's what was
asked for.
