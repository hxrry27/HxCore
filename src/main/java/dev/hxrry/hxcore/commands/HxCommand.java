package dev.hxrry.hxcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

// End Goal Target to help plan out the API
// HxCommand.create("prefix")
//     .executes(sender -> showPrefix(sender))
//     .sub("reload",
//          perm("hxprefix.reload", OP),
//          sender -> reload(sender))
//     .sub("set",
//          perm("hxprefix.colour", ALL),
//          arg("colour", styleConfig.getColours()),
//          (sender, colour) -> setColour(sender, colour))
//     .register(plugin);

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

    public record Arg(String name, List<String> completions) {}

    public record Perm(String node, PermDefault def) {}

    public record Sub(String name, Perm perm, Arg arg, Consumer<CommandSender> action, BiConsumer<CommandSender, String> argAction) {}

    public HxCommand sub(String name, Perm perm, Consumer<CommandSender> action) {
        subs.add(new Sub(name, perm, null, action, null));
        return this;
    }

    public HxCommand sub(String name,  Perm perm, Arg arg, BiConsumer<CommandSender, String> action) {
        subs.add(new Sub(name, perm, arg, null, action));
        return this;
    }

    public static Arg arg(String name, List<String> completions) {
        return new Arg(name, completions);
    }
    
    public enum PermDefault { OP, ALL}
    
    public static Perm perm(String name, PermDefault def) {
        return new Perm(name, def);
    }

    @SuppressWarnings("null")
	public void register(JavaPlugin plugin) {
        
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            
            @SuppressWarnings("null")
			var root = Commands.literal(name);

            if (action != null) {
                root.executes(ctx -> {
                    action.accept(ctx.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                });
            }

            for (Sub sub : subs) {
                @SuppressWarnings("null")
				var subNode = Commands.literal(sub.name())
                    .requires(source -> source.getSender().hasPermission(sub.perm().node()));
                
                if (sub.arg() == null) {
                    subNode.executes(ctx -> {
                        sub.action().accept(ctx.getSource().getSender());
                        return Command.SINGLE_SUCCESS;
                    });
                } else {
                    @SuppressWarnings("null")
					var argNode = Commands.argument(sub.arg().name(), StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String option : sub.arg().completions()) {
                                builder.suggest(option);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            sub.argAction().accept(
                                ctx.getSource().getSender(),
                                StringArgumentType.getString(ctx, sub.arg().name()));
                            return Command.SINGLE_SUCCESS;
                        });

                    subNode.then(argNode);
                }
                root.then(subNode);
            }

            event.registrar().register(root.build());
        });
    }
}
