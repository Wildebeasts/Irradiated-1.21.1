package com.momosoftworks.irradiated.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.common.radiation.DynamicRadiationHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class RadiationCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rad")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                    .executes(RadiationCommand::setRadiation)))
            .then(Commands.literal("add")
                .then(Commands.argument("levels", IntegerArgumentType.integer(1, 100))
                    .executes(RadiationCommand::addRadiation)))
            .then(Commands.literal("reduce")
                .then(Commands.argument("levels", IntegerArgumentType.integer(1, 100))
                    .executes(RadiationCommand::reduceRadiation)))
            .then(Commands.literal("clear").executes(RadiationCommand::clearRadiation))
            .then(Commands.literal("get").executes(RadiationCommand::getRadiation))
            .then(Commands.literal("exposure").executes(RadiationCommand::getExposure));

        dispatcher.register(root);
    }

    private static int setRadiation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int level = IntegerArgumentType.getInteger(ctx, "level");

            RadiationAPI.setRadiationLevel(player, level, 20 * 60 * 60); // 1 hour duration
            
            if (level == 0) {
                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Radiation cleared"), true);
            } else {
                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Radiation set to level " + level), true);
            }
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }

    private static int addRadiation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int levels = IntegerArgumentType.getInteger(ctx, "levels");

            RadiationAPI.addRadiation(player, levels, 20 * 60 * 60); // 1 hour duration
            int newLevel = RadiationAPI.getRadiationLevel(player);
            
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Added " + levels + " radiation levels. Current level: " + newLevel), true);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }

    private static int reduceRadiation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int levels = IntegerArgumentType.getInteger(ctx, "levels");

            int oldLevel = RadiationAPI.getRadiationLevel(player);
            RadiationAPI.reduceRadiation(player, levels);
            int newLevel = RadiationAPI.getRadiationLevel(player);
            
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Reduced radiation by " + levels + " levels. " + oldLevel + " -> " + newLevel), true);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }

    private static int clearRadiation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            RadiationAPI.clearRadiation(player);
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Radiation cleared"), true);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }

    private static int getRadiation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int level = RadiationAPI.getRadiationLevel(player);
            int duration = RadiationAPI.getRadiationDuration(player);
            
            if (level == 0) {
                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("No radiation"), false);
            } else {
                int minutes = duration / (20 * 60);
                int seconds = (duration % (20 * 60)) / 20;
                ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Radiation level: " + level + " (Duration: " + minutes + "m " + seconds + "s)"), false);
            }
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }
    
    private static int getExposure(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            float exposure = DynamicRadiationHandler.getPlayerRadiationExposure(player);
            int radiationLevel = RadiationAPI.getRadiationLevel(player);
            
            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                String.format("Dynamic radiation exposure: %.2f (Radiation level: %d)", exposure, radiationLevel)), false);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("This command must be run by a player."));
            return 0;
        }
    }
}


