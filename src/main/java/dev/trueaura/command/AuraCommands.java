package dev.trueaura.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.trueaura.TrueAura;
import dev.trueaura.aura.AuraEffect;
import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.AuraShape;
import dev.trueaura.network.AuraNetwork;
import dev.trueaura.network.AuraSnapshot;
import dev.trueaura.particle.AuraParticleCatalog;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Commands run on the server thread; only the target branch requires operator permission. */
@Mod.EventBusSubscriber(modid = TrueAura.MOD_ID)
public final class AuraCommands {
    private static final List<String> COLORS = List.of("gold", "red", "orange", "yellow", "green", "aqua",
        "blue", "purple", "pink", "white", "black", "gray", "light_purple", "dark_purple", "FFB30F");
    @FunctionalInterface
    private interface Targets { Collection<ServerPlayer> get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException; }
    private static final Targets SELF = context -> List.of(context.getSource().getPlayerOrException());

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = actions(literal("aura"), SELF);
        root.executes(context -> {
            context.getSource().sendSuccess(() -> message("help",
                "/aura [self] on|off|toggle|status|reset|set ...; /aura player <targets> ... (OP)"), false);
            return 1;
        });
        root.then(actions(literal("self"), SELF));
        root.then(literal("player").requires(source -> source.hasPermission(2))
            .then(actions(argument("targets", EntityArgument.players()), context -> EntityArgument.getPlayers(context, "targets"))));
        dispatcher.register(root);
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T actions(T root, Targets targets) {
        for (String action : List.of("on", "off", "toggle", "reset")) {
            root.then(literal(action).executes(context -> change(context, targets, old -> switch (action) {
                case "on" -> new AuraSnapshot(old.settings(), true);
                case "off" -> new AuraSnapshot(old.settings(), false);
                case "toggle" -> new AuraSnapshot(old.settings(), !old.enabled());
                default -> new AuraSnapshot(AuraSettings.DEFAULT, old.enabled());
            })));
        }
        root.then(literal("status").executes(context -> status(context, targets)));
        var set = literal("set");
        var color = literal("color");
        for (String channel : List.of("main", "highlight", "base")) {
            color.then(literal(channel).then(argument("color", StringArgumentType.string())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(COLORS, builder))
                .executes(context -> {
                    int value = parseColor(StringArgumentType.getString(context, "color"));
                    return settings(context, targets, old -> old.withColors(
                        channel.equals("main") ? value : old.mainColor(),
                        channel.equals("highlight") ? value : old.highlightColor(),
                        channel.equals("base") ? value : old.baseColor()));
                })));
        }
        set.then(color);
        for (String field : List.of("intensity", "size", "speed")) {
            double min = field.equals("size") ? .5 : .1;
            double max = field.equals("speed") ? 3 : dev.trueaura.aura.AuraLimits.HARD_MAX;
            set.then(literal(field).then(argument("value", DoubleArgumentType.doubleArg(min, max))
                .executes(context -> {
                    double value = DoubleArgumentType.getDouble(context, "value");
                    var limits = dev.trueaura.config.AuraServerConfig.limits();
                    double allowed = field.equals("size") ? limits.maxScale() : field.equals("intensity") ? limits.maxIntensity() : 3;
                    if (value > allowed) throw new SimpleCommandExceptionType(message("limit",
                        "Maximum %s in this world: %s (trueaura-server.toml)", field, allowed)).create();
                    return settings(context, targets, old -> old.withAppearance(
                        field.equals("intensity") ? value : old.intensity(),
                        field.equals("size") ? value : old.scale(), field.equals("speed") ? value : old.speed(), old.shape()));
                })));
        }
        for (boolean appearing : new boolean[]{true, false}) {
            var mode = literal(appearing ? "appear" : "disappear");
            for (String choice : appearing ? List.of("instant", "fade", "rise") : List.of("instant", "fade")) {
                mode.then(literal(choice).executes(context -> settings(context, targets, old -> old.withAnimation(
                    appearing ? (choice.equals("instant") ? old.animation().withAppear(true) : old.animation().withRise(choice.equals("rise"))) : old.animation().withDisappear(choice.equals("instant"))))));
            }
            set.then(mode);
            set.then(literal(appearing ? "fade_in" : "fade_out").then(argument("seconds", DoubleArgumentType.doubleArg(.05, 5))
                .executes(context -> settings(context, targets, old -> {
                    int ticks = (int) Math.round(DoubleArgumentType.getDouble(context, "seconds") * 20);
                    return old.withAnimation(old.animation().withDurations(appearing ? ticks : old.animation().appearTicks(),
                        appearing ? old.animation().disappearTicks() : ticks));
                }))));
        }
        set.then(literal("motion").then(argument("strength", DoubleArgumentType.doubleArg(0, 2))
            .executes(context -> settings(context, targets, old -> old.withAnimation(old.animation().withMotion(DoubleArgumentType.getDouble(context, "strength")))))));
        set.then(literal("shape").then(argument("shape", StringArgumentType.word())
            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                Arrays.stream(AuraShape.values()).map(shape -> shape.name().toLowerCase(Locale.ROOT)), builder))
            .executes(context -> {
                AuraShape shape;
                try { shape = AuraShape.valueOf(StringArgumentType.getString(context, "shape").toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException ex) { throw error("shape", "Unknown aura shape. Use Tab to see the available shapes."); }
                return settings(context, targets, old -> old.withShape(shape));
            })));
        for (boolean opening : new boolean[]{true, false}) {
            set.then(literal(opening ? "open_effect" : "close_effect")
                .then(argument("effect", ResourceLocationArgument.id())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(AuraParticleCatalog.entries(""), builder))
                    .executes(context -> effect(context, targets, opening, null))
                    .then(argument("parameters", StringArgumentType.greedyString())
                        .executes(context -> effect(context, targets, opening, StringArgumentType.getString(context, "parameters"))))));
        }
        set.then(literal("copy_shape").then(argument("source", EntityArgument.player())
            .executes(context -> {
                var source = AuraNetwork.current(EntityArgument.getPlayer(context, "source")).settings();
                return settings(context, targets, old -> old.withShape(source.shape()).withCustom(source.custom()));
            })));
        root.then(set);
        return root;
    }

    static int parseColor(String value) throws CommandSyntaxException {
        Integer named = switch (value.toLowerCase(Locale.ROOT)) {
            case "orange" -> 0xFF8800;
            case "yellow" -> 0xFFFF55;
            case "purple" -> 0xAA00FF;
            case "pink" -> 0xFF66CC;
            default -> {
                ChatFormatting formatting = ChatFormatting.getByName(value);
                yield formatting != null && formatting.isColor() ? formatting.getColor() : null;
            }
        };
        if (named != null) return named;
        var hex = AuraSettings.parseHex(value);
        if (hex.isPresent()) return hex.getAsInt();
        throw error("color", "Unknown color. Use a name such as gold/blue/pink or six hex digits such as FFB30F.");
    }

    private static int effect(CommandContext<CommandSourceStack> context, Targets targets, boolean opening, String parameters)
        throws CommandSyntaxException {
        String id = ResourceLocationArgument.getId(context, "effect").toString();
        String args = parameters == null ? AuraParticleCatalog.defaultParameters(id) : parameters.trim();
        AuraEffect effect = new AuraEffect(id, args);
        if (!effect.id().equals(id) || !effect.parameters().equals(args)) {
            throw error("effect_invalid", "Invalid or overly long particle arguments.");
        }
        String problem = AuraParticleCatalog.problem(effect);
        if (!problem.isEmpty()) {
            throw new SimpleCommandExceptionType(message("effect_error", "Cannot use this effect: %s (%s)", id, problem)).create();
        }
        return settings(context, targets, old -> old.withEffect(opening, effect));
    }

    private static int settings(CommandContext<CommandSourceStack> context, Targets targets, UnaryOperator<AuraSettings> edit)
        throws CommandSyntaxException {
        return change(context, targets, old -> new AuraSnapshot(edit.apply(old.settings()), old.enabled()));
    }

    private static int change(CommandContext<CommandSourceStack> context, Targets targets, UnaryOperator<AuraSnapshot> edit)
        throws CommandSyntaxException {
        Collection<ServerPlayer> players = targets.get(context);
        for (ServerPlayer player : players) AuraNetwork.applyCommand(player, edit);
        context.getSource().sendSuccess(() -> message("changed", "Updated aura for %s player(s).", players.size()), true);
        return players.size();
    }

    private static int status(CommandContext<CommandSourceStack> context, Targets targets) throws CommandSyntaxException {
        Collection<ServerPlayer> players = targets.get(context);
        for (ServerPlayer player : players) {
            AuraSnapshot snapshot = AuraNetwork.current(player);
            AuraSettings s = snapshot.settings();
            context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "%s: %s | %s | intensity %.2f, size %.2f, speed %.2f | main %s, highlight %s, base %s | open %s %s | close %s %s | appear %s %.2fs, disappear %s %.2fs, motion %.2f",
                player.getGameProfile().getName(), snapshot.enabled() ? "ON" : "OFF", (s.custom() == null ? s.shape().name().toLowerCase(Locale.ROOT) : s.custom().id()),
                s.intensity(), s.scale(), s.speed(), AuraSettings.formatHex(s.mainColor()), AuraSettings.formatHex(s.highlightColor()),
                AuraSettings.formatHex(s.baseColor()), s.openEffect().id(), s.openEffect().parameters(),
                s.closeEffect().id(), s.closeEffect().parameters(), s.animation().appearMode(), s.animation().appearTicks() / 20.0,
                s.animation().instantDisappear() ? "instant" : "fade", s.animation().disappearTicks() / 20.0, s.animation().motionStrength())), false);
        }
        return players.size();
    }

    private static Component message(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("command.trueaura." + key, fallback, args);
    }
    private static CommandSyntaxException error(String key, String fallback) {
        return new SimpleCommandExceptionType(message(key, fallback)).create();
    }
    private AuraCommands() {}
}
