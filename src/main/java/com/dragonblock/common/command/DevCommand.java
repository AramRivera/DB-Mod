package com.dragonblock.common.command;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.dragonblock.common.network.ModMessages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DevCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> devCommand = buildCommandTree("dev");
        LiteralArgumentBuilder<CommandSourceStack> dbcCommand = buildCommandTree("dbc");

        dispatcher.register(devCommand);
        dispatcher.register(dbcCommand);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String root) {
        return Commands.literal(root)
                .requires(source -> source.hasPermission(2) || source.getServer().isSingleplayer())
                // /dev help
                .then(Commands.literal("help").executes(ctx -> sendHelp(ctx.getSource())))

                // /dev tp <amount> (quick add)
                .then(Commands.literal("tp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> addTp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> addTp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setTp(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                )

                // /dev ki [full | set <amount>]
                .then(Commands.literal("ki")
                        .then(Commands.literal("full").executes(ctx -> fullKi(ctx.getSource())))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> setKi(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount"))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setKi(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                )

                // /dev stat <name> <value>
                .then(Commands.literal("stat")
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> setStat(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "stat"),
                                                IntegerArgumentType.getInteger(ctx, "value")
                                        ))))
                )

                // /dev race <raceName>
                .then(Commands.literal("race")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .executes(ctx -> setRace(ctx.getSource(), StringArgumentType.getString(ctx, "race")))))

                // /dev form <formName>
                .then(Commands.literal("form")
                        .then(Commands.argument("form", StringArgumentType.word())
                                .executes(ctx -> setForm(ctx.getSource(), StringArgumentType.getString(ctx, "form")))))

                // /dev max (sets 100 stats, 50,000 TP, full Ki)
                .then(Commands.literal("max").executes(ctx -> setMax(ctx.getSource())))

                // /dev reset (resets stats to base 5, 100 TP, base form)
                .then(Commands.literal("reset").executes(ctx -> resetStats(ctx.getSource())));
    }

    private static int sendHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== §eComandos Dragon Block Modo Dev §6==="), false);
        source.sendSuccess(() -> Component.literal("§a/dev tp <cantidad> §7- Añade puntos de entrenamiento (TP)"), false);
        source.sendSuccess(() -> Component.literal("§a/dev tp set <cantidad> §7- Establece tus TP"), false);
        source.sendSuccess(() -> Component.literal("§a/dev ki full §7- Rellena tu barra de Ki al 100%"), false);
        source.sendSuccess(() -> Component.literal("§a/dev ki <cantidad> §7- Establece tu Ki actual"), false);
        source.sendSuccess(() -> Component.literal("§a/dev stat <str|dex|con|wil|mnd|all> <valor> §7- Fija un atributo"), false);
        source.sendSuccess(() -> Component.literal("§a/dev race <human|saiyan|half_saiyan|namekian|arcosian|majin>"), false);
        source.sendSuccess(() -> Component.literal("§a/dev form <base|kaioken|ssj> §7- Cambia de transformación"), false);
        source.sendSuccess(() -> Component.literal("§a/dev max §7- Estadísticas a 100, 50k TP y Ki al máximo"), false);
        source.sendSuccess(() -> Component.literal("§a/dev reset §7- Restaura estadísticas al valor inicial"), false);
        return 1;
    }

    private static int addTp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.addTrainingPoints(amount);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §aSe añadieron §e" + String.format("%,d", amount) + " TP§a. Total: §e" + String.format("%,d", data.getTrainingPoints()) + " TP"), false);
        return 1;
    }

    private static int setTp(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setTrainingPoints(amount);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §aTP establecidos a: §e" + String.format("%,d", amount) + " TP"), false);
        return 1;
    }

    private static int fullKi(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setCurrentKi(data.getMaxKi());
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §b¡Ki restaurado al máximo! (" + String.format("%.0f", data.getMaxKi()) + " Ki)"), false);
        return 1;
    }

    private static int setKi(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setCurrentKi(amount);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §bKi establecido a: " + amount), false);
        return 1;
    }

    private static int setStat(CommandSourceStack source, String statName, int value) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        String name = statName.toUpperCase();

        switch (name) {
            case "STR" -> data.setStr(value);
            case "DEX" -> data.setDex(value);
            case "CON" -> data.setCon(value);
            case "WIL" -> data.setWil(value);
            case "MND" -> data.setMnd(value);
            case "ALL" -> {
                data.setStr(value);
                data.setDex(value);
                data.setCon(value);
                data.setWil(value);
                data.setMnd(value);
            }
            default -> {
                source.sendFailure(Component.literal("§cAtributo desconocido: '" + statName + "'. Usa STR, DEX, CON, WIL, MND o ALL."));
                return 0;
            }
        }

        data.setCurrentKi(Math.min(data.getCurrentKi(), data.getMaxKi()));
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §aAtributo §e" + name + " §aestablecido a §e" + value), false);
        return 1;
    }

    private static int setRace(CommandSourceStack source, String raceStr) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        try {
            Race race = Race.valueOf(raceStr.toUpperCase());
            PlayerDBCData data = PlayerDBCData.get(player);
            data.setRace(race);
            data.saveToPlayer(player);
            ModMessages.syncToPlayer(player);

            source.sendSuccess(() -> Component.literal("§6[DEV] §aRaza cambiada a: §e" + race.getDisplayName()), false);
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("§cRaza no válida. Opciones: HUMAN, SAIYAN, HALF_SAIYAN, NAMEKIAN, ARCOSIAN, MAJIN"));
            return 0;
        }
    }

    private static int setForm(CommandSourceStack source, String formStr) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        Transformation target;
        String upper = formStr.toUpperCase();
        if (upper.equals("SSJ") || upper.equals("SUPER_SAIYAN")) {
            target = Transformation.SUPER_SAIYAN;
        } else if (upper.equals("KAIOKEN") || upper.equals("KAIO_KEN")) {
            target = Transformation.KAIOKEN;
        } else if (upper.equals("BASE")) {
            target = Transformation.BASE;
        } else {
            source.sendFailure(Component.literal("§cForma no válida. Opciones: BASE, KAIOKEN, SSJ"));
            return 0;
        }

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setTransformation(target);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §aTransformación cambiada a: §e" + target.getDisplayName()), false);
        return 1;
    }

    private static int setMax(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setStr(100);
        data.setDex(100);
        data.setCon(100);
        data.setWil(100);
        data.setMnd(100);
        data.setTrainingPoints(50000);
        data.setCurrentKi(data.getMaxKi());
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §e¡Modo Dios activado! Stats a 100, 50,000 TP y Ki al máximo."), false);
        return 1;
    }

    private static int resetStats(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        PlayerDBCData data = PlayerDBCData.get(player);
        data.setRace(Race.HUMAN);
        data.setTransformation(Transformation.BASE);
        data.setStr(5);
        data.setDex(5);
        data.setCon(5);
        data.setWil(5);
        data.setMnd(5);
        data.setTrainingPoints(100);
        data.setCurrentKi(data.getMaxKi());
        data.setFlying(false);
        data.setChargingKi(false);
        data.saveToPlayer(player);
        ModMessages.syncToPlayer(player);

        source.sendSuccess(() -> Component.literal("§6[DEV] §eEstadísticas y datos restablecidos a valores iniciales."), false);
        return 1;
    }
}
