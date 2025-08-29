package dev.hxrry.hxcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * scheduler utility for easy async/sync operations
 */
public class Scheduler {
    private static Plugin plugin;
    private static BukkitScheduler scheduler;
    
    /**
     * initialize with plugin instance
     */
    public static void init(@NotNull Plugin pluginInstance) {
        plugin = pluginInstance;
        scheduler = Bukkit.getScheduler();
    }
    
    /**
     * run task on main thread
     */
    public static BukkitTask runTask(@NotNull Runnable task) {
        return scheduler.runTask(plugin, task);
    }
    
    /**
     * run task on main thread after delay
     */
    public static BukkitTask runTaskLater(@NotNull Runnable task, long delayTicks) {
        return scheduler.runTaskLater(plugin, task, delayTicks);
    }
    
    /**
     * run repeating task on main thread
     */
    public static BukkitTask runTaskTimer(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
    }
    
    /**
     * run task asynchronously
     */
    public static BukkitTask runTaskAsync(@NotNull Runnable task) {
        return scheduler.runTaskAsynchronously(plugin, task);
    }
    
    /**
     * run task asynchronously after delay
     */
    public static BukkitTask runTaskLaterAsync(@NotNull Runnable task, long delayTicks) {
        return scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
    }
    
    /**
     * run repeating task asynchronously
     */
    public static BukkitTask runTaskTimerAsync(@NotNull Runnable task, long delayTicks, long periodTicks) {
        return scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }
    
    /**
     * create executor for completable futures that runs on main thread
     */
    public static Executor getMainThreadExecutor() {
        return task -> runTask(task);
    }
    
    /**
     * create executor for completable futures that runs async
     */
    public static Executor getAsyncExecutor() {
        return task -> runTaskAsync(task);
    }
    
    /**
     * run async then sync
     */
    public static <T> CompletableFuture<T> supplyAsync(@NotNull java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, getAsyncExecutor());
    }
    
    /**
     * run on main thread as future
     */
    public static <T> CompletableFuture<T> supplySync(@NotNull java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, getMainThreadExecutor());
    }
}