package com.example.omegasnipers;

import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The core "overpowered sniper" behaviour. When an Omega gun is fired:
 *  1. The normal TaCZ projectile is cancelled - no travel time, no physical
 *     bullet to dodge or block. That's the "hits from anywhere" lock-on.
 *  2. The nearest valid target (player, optionally also hostile mobs) within
 *     lockRange is found and hurt directly, with tier-scaled damage.
 *  3. If infiniteAmmo is on, the magazine is topped back up immediately.
 *  4. A short particle "tracer" and a sound play at both ends for feedback.
 *
 * Every other property of the weapon (reload animation, recoil, sound,
 * model) still comes straight from the real TaCZ gun it was built on top of,
 * so it keeps looking/feeling like a real gun - it just never misses.
 */
public class OmegaCombatHandler {

    @SubscribeEvent
    public void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) {
            return;
        }

        Entity shooterEntity = event.getShooter();
        if (!(shooterEntity instanceof LivingEntity shooter)) {
            return;
        }

        ItemStack gunStack = findOmegaStack(shooter);
        if (gunStack == null) {
            return;
        }

        // Cancel the normal physical bullet - we resolve the hit ourselves, instantly.
        event.setCanceled(true);

        int tier = OmegaGunFactory.readTier(gunStack);
        if (tier < 0) {
            return;
        }

        LivingEntity target = findTarget(shooter);

        Level level = shooter.level();
        level.playSound(null, shooter.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.2F, 0.6F);

        if (target != null) {
            resolveHit(shooter, target, tier);
        }

        if (Config.INFINITE_AMMO.get()) {
            refillAmmo(gunStack);
        }
    }

    @Nullable
    private ItemStack findOmegaStack(LivingEntity shooter) {
        ItemStack main = shooter.getItemInHand(InteractionHand.MAIN_HAND);
        if (OmegaGunFactory.isOmegaGun(main)) {
            return main;
        }
        ItemStack off = shooter.getItemInHand(InteractionHand.OFF_HAND);
        if (OmegaGunFactory.isOmegaGun(off)) {
            return off;
        }
        return null;
    }

    @Nullable
    private LivingEntity findTarget(LivingEntity shooter) {
        double range = Config.LOCK_RANGE.get();
        boolean needLineOfSight = Config.REQUIRE_LINE_OF_SIGHT.get();
        boolean allowFriendlyFire = Config.FRIENDLY_FIRE_SAME_TEAM.get();

        Level level = shooter.level();
        AABB searchBox = shooter.getBoundingBox().inflate(range);

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;

        if (Config.TARGET_PLAYERS.get()) {
            for (Player candidate : level.players()) {
                if (!isValidTarget(shooter, candidate, range, needLineOfSight, allowFriendlyFire)) {
                    continue;
                }
                double distSq = shooter.distanceToSqr(candidate);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = candidate;
                }
            }
        }

        if (best == null && Config.TARGET_MOBS.get()) {
            List<Monster> monsters = level.getEntitiesOfClass(Monster.class, searchBox,
                    m -> m.isAlive() && m != shooter);
            for (Monster candidate : monsters) {
                if (needLineOfSight && !hasLineOfSight(shooter, candidate)) {
                    continue;
                }
                double distSq = shooter.distanceToSqr(candidate);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = candidate;
                }
            }
        }

        return best;
    }

    private boolean isValidTarget(LivingEntity shooter, Player candidate, double range,
                                   boolean needLineOfSight, boolean allowFriendlyFire) {
        if (candidate == shooter || !candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        if (shooter.distanceToSqr(candidate) > range * range) {
            return false;
        }
        if (!allowFriendlyFire && shooter instanceof Player shooterPlayer) {
            PlayerTeam shooterTeam = shooterPlayer.getTeam();
            if (shooterTeam != null && shooterTeam.equals(candidate.getTeam())) {
                return false;
            }
        }
        if (needLineOfSight && !hasLineOfSight(shooter, candidate)) {
            return false;
        }
        return true;
    }

    private boolean hasLineOfSight(LivingEntity from, LivingEntity to) {
        Level level = from.level();
        Vec3 eyeFrom = new Vec3(from.getX(), from.getEyeY(), from.getZ());
        Vec3 eyeTo = new Vec3(to.getX(), to.getEyeY(), to.getZ());
        ClipContext ctx = new ClipContext(eyeFrom, eyeTo, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from);
        HitResult result = level.clip(ctx);
        return result.getType() == HitResult.Type.MISS;
    }

    private void resolveHit(LivingEntity shooter, LivingEntity target, int tier) {
        Level level = shooter.level();

        float damage;
        if (Config.ONE_SHOT_KILL.get()) {
            damage = 10000.0F; // functionally guaranteed lethal even through armor/absorption/resistance
        } else {
            double base = Config.BASE_DAMAGE.get() + (tier - 1) * Config.DAMAGE_PER_TIER.get();
            if (target instanceof Player) {
                base *= Config.PVP_DAMAGE_MULTIPLIER.get();
            }
            damage = (float) base;
        }

        DamageSource damageSource = shooter instanceof Player shooterPlayer
                ? level.damageSources().playerAttack(shooterPlayer)
                : level.damageSources().mobAttack(shooter);

        target.hurt(damageSource, damage);

        if (Config.SPAWN_VISUAL_BEAM.get() && level instanceof ServerLevel serverLevel) {
            spawnTracer(serverLevel, shooter, target);
        }

        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4F, 1.8F);
    }

    private void spawnTracer(ServerLevel serverLevel, LivingEntity from, LivingEntity to) {
        Vec3 start = new Vec3(from.getX(), from.getEyeY(), from.getZ());
        Vec3 end = new Vec3(to.getX(), to.getEyeY(), to.getZ());
        Vec3 delta = end.subtract(start);
        int steps = 24;
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    point.x, point.y, point.z, 1, 0, 0, 0, 0.0);
        }
    }

    private void refillAmmo(ItemStack gunStack) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) {
            return;
        }
        try {
            ResourceLocation baseGunId = OmegaGunFactory.readBaseGunId(gunStack);
            int magazineSize = baseGunId != null
                    ? OmegaGunFactory.lookUpMagazineSize(baseGunId)
                    : gun.getCurrentAmmoCount(gunStack);
            gun.setCurrentAmmoCount(gunStack, magazineSize);
            gun.setBulletInBarrel(gunStack, true);
            if (gun.hasMaxDummyAmmo(gunStack)) {
                int maxDummy = gun.getMaxDummyAmmoAmount(gunStack);
                if (maxDummy > 0) {
                    gun.setDummyAmmoAmount(gunStack, maxDummy);
                }
            }
        } catch (Throwable t) {
            OmegaSnipersMod.LOGGER.debug("[OmegaSnipers] Ammo refill failed.", t);
        }
    }
}
