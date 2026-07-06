package dev.hxrry.hxcore.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class HxCommand {

    private final String name;
    private Consumer<CommandSender> action;
    private final List<Sub> subs = new ArrayList<>();

    private HxCommand(String name) {
        this.name = name;
    }

    public static HxCommand create(String name) {
        return new HxCommand(name);
    }

    public HxCommand executes(Consumer<CommandSender> action) {
        this.action = action;
        return this;
    }

    public record Arg(String name, Function<CommandSender, List<String>> completions, boolean greedy) {}

    public record Perm(String node, PermDefault def) {}

    public record Args(Map<String, String> values) {
        public String get(String name) {
            return values.get(name);
        }
    }

    public record Sub(String name, Perm perm, List<Arg> args,
                      Consumer<CommandSender> action,
                      BiConsumer<CommandSender, String> argAction,
                      BiConsumer<CommandSender, Args> argsAction) {}

    public HxCommand sub(String name, Perm perm, Consumer<CommandSender> action) {
        subs.add(new Sub(name, perm, null, action, null, null));
        return this;
    }

    public HxCommand sub(String name, Perm perm, Arg arg, BiConsumer<CommandSender, String> action) {
        subs.add(new Sub(name, perm, List.of(arg), null, action, null));
        return this;
    }

    public HxCommand sub(String name, Perm perm, List<Arg> args, BiConsumer<CommandSender, Args> action) {
        // greedy swallows the rest of the line, so nothing can come after it
        for (int i = 0; i < args.size() - 1; i++) {
            if (args.get(i).greedy()) {
                throw new IllegalArgumentException("greedy arg '" + args.get(i).name() + "' must be the last arg");
            }
        }
        subs.add(new Sub(name, perm, args, null, null, action));
        return this;
    }

    // fixed completions, same list for everyone
    public static Arg arg(String name, List<String> completions) {
        return new Arg(name, sender -> completions, false);
    }

    // dynamic completions, asked per-player while they type
    public static Arg arg(String name, Function<CommandSender, List<String>> completions) {
        return new Arg(name, completions, false);
    }

    // swallows the rest of the line (spaces included), no completions. last slot only.
    public static Arg greedyArg(String name) {
        return new Arg(name, sender -> List.of(), true);
    }

    public static List<Arg> args(Arg... args) {
        return List.of(args);
    }

    public enum PermDefault { OP, ALL }

    public static Perm perm(String name, PermDefault def) {
        return new Perm(name, def);
    }

    @SuppressWarnings("null")
    public void register(JavaPlugin plugin) {

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {

            var root = Commands.literal(name);

            if (action != null) {
                root.executes(ctx -> {
                    action.accept(ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                });
            }

            for (Sub sub : subs) {
                registerPermission(plugin, sub.perm());

                var subNode = Commands.literal(sub.name())
                    .requires(source -> source.getSender().hasPermission(sub.perm().node()));

                if (sub.args() == null || sub.args().isEmpty()) {
                    // no blanks: action hangs straight off the word
                    subNode.executes(ctx -> {
                        sub.action().accept(ctx.getSource().getSender());
                        return Command.SINGLE_SUCCESS;
                    });
                } else if (sub.argAction() != null) {
                    // one blank: unwrap sender + the single typed value
                    Arg arg = sub.args().get(0);
                    var argNode = argNode(arg)
                        .executes(ctx -> {
                            sub.argAction().accept(
                                ctx.getSource().getSender(),
                                StringArgumentType.getString(ctx, arg.name()));
                            return Command.SINGLE_SUCCESS;
                        });
                    subNode.then(argNode);
                } else {
                    List<Arg> args = sub.args();
                    var node = argNode(args.get(args.size() - 1))
                        .executes(ctx -> {
                            Map<String, String> values = new HashMap<>();
                            for (Arg a : args) {
                                values.put(a.name(), StringArgumentType.getString(ctx, a.name()));
                            }
                            sub.argsAction().accept(ctx.getSource().getSender(), new Args(values));
                            return Command.SINGLE_SUCCESS;
                        });
                    for (int i = args.size() - 2; i >= 0; i--) {
                        var outer = argNode(args.get(i));
                        outer.then(node);
                        node = outer;
                    }
                    subNode.then(node);
                }

                root.then(subNode);
            }

            event.registrar().register(root.build());
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> argNode(Arg arg) {
        var shape = arg.greedy() ? StringArgumentType.greedyString() : StringArgumentType.word();
        return Commands.argument(arg.name(), shape)
            .suggests((ctx, builder) -> {
                for (String option : arg.completions().apply(ctx.getSource().getSender())) {
                    builder.suggest(option);
                }
                return builder.buildFuture();
            });
    }

    private void registerPermission(JavaPlugin plugin, Perm perm) {
        PermissionDefault bukkitDefault;
        if (perm.def() == PermDefault.OP) {
            bukkitDefault = PermissionDefault.OP;
        } else {
            bukkitDefault = PermissionDefault.TRUE;
        }

        var pluginManager = plugin.getServer().getPluginManager();
        if (pluginManager.getPermission(perm.node()) == null) {
            pluginManager.addPermission(new Permission(perm.node(), bukkitDefault));
        }
    }
}
