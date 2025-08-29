# HxCore

Core utility library for Paper/Bukkit plugins, providing essential services for ValeSMP's custom plugins.

## Overview

HxCore is a shared library that handles common functionality across multiple Minecraft plugins, including database management, caching, configuration, and text formatting.

## Tech Stack

- **Database**: HikariCP connection pooling with SQLite/PostgreSQL support
- **Caching**: Caffeine high-performance cache
- **Text**: MiniMessage, legacy (&), and hex color support
- **Config**: YAML with automatic migration and versioning
- **Async**: CompletableFuture-based async operations

## Features

- Async database operations with connection pooling
- In-memory caching with TTL and size limits
- Multi-file configuration management with auto-updates
- Advanced text formatting and color parsing
- Simplified scheduler and logging utilities
- Transaction support with auto-rollback

## Usage

```java
public class MyPlugin extends JavaPlugin {
    private HxCore core;
    
    @Override
    public void onEnable() {
        core = new HxCore(this);
        if (!core.initialize()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }
    
    @Override
    public void onDisable() {
        if (core != null) core.shutdown();
    }
}
