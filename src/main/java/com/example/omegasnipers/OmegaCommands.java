package com.example.omegasnipers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * /omegasnipers reload   - re-reads TaCZ's gun index and regenerates every Omega variant
 * /omegasnipers list     - prints how many Omega variants currently exist
 * /omegasnipers give <players> [index]   - gives the given (or a random) Omega sniper
 */
public final class OmegaCommands {

    private OmegaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("omegasnipers")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(OmegaCommands::reload))
                .then(Commands.literal("list")
                        .executes(OmegaCommands::list))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> give(ctx, -1))
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "index")))))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        OmegaSniperRegistry.refresh();
        int count = OmegaSniperRegistry.count();
        ctx.getSource().sendSuccess(() -> Component.literal("Regenerated " + count + " Omega sniper variant(s)."), true);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        OmegaSniperRegistry.ensureGenerated();
        int count = OmegaSniperRegistry.count();
        ctx.getSource().sendSuccess(() -> Component.literal(count + " Omega sniper variant(s) currently generated."), false);
        return count;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, int oneBasedIndex) {
        OmegaSniperRegistry.ensureGenerated();
        List<ItemStack> all = OmegaSniperRegistry.allStacksCopy();
        if (all.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(
                    "No Omega sniper variants have been generated - is TaCZ (and any addons) actually loaded?"));
            return 0;
        }

        ItemStack template;
        if (oneBasedIndex > 0) {
            int idx = Math.min(oneBasedIndex, all.size()) - 1;
            template = all.get(idx);
        } else {
            template = all.get((int) (Math.random() * all.size()));
        }

        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("No matching players."));
            return 0;
        }

        int given = 0;
        for (ServerPlayer player : targets) {
            ItemStack toGive = template.copy();
            if (!player.getInventory().add(toGive)) {
                player.drop(toGive, false);
            }
            given++;
        }

        ItemStack finalTemplate = template;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + finalTemplate.getHoverName().getString() + " to " + given + " player(s)."), true);
        return given;
    }
}
