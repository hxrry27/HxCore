package dev.hxrry.hxcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

// End Goal Target to help plan out the API
// HxCommand.create("prefix")
//     .executes(sender -> showPrefix(sender))
//     .sub("reload",
//          perm("hxprefix.reload", OP),
//          sender -> reload(sender))
//     .sub("set",
//          arg("colour", styleConfig.getColours()),
//          perm("hxprefix.colour", ALL),
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

    public void register(JavaPlugin plugin) {
        //just a stubby boy for now
    }
}
