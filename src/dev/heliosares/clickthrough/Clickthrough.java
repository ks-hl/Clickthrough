package dev.heliosares.clickthrough;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class Clickthrough extends JavaPlugin implements Listener {
    private String update;

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();

        Set<Material> filter;
        if (getConfig().contains("containers")) {
            filter = new HashSet<>();
            for (String container : getConfig().getStringList("containers")) {
                try {
                    Material material = Material.valueOf(container.toUpperCase());
                    if (!material.isBlock()) {
                        getLogger().warning(material + " is not a block and cannot be used with Clickthrough.");
                        continue;
                    }
                    filter.add(material);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid material type in containers list: " + container + ", ensure you use values from https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html");
                }
            }
        } else {
            filter = Set.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        }

        Bukkit.getPluginManager().registerEvents(new MyListener(this, filter), this);

        if (getConfig().getBoolean("check-for-updates", true)) {
            Bukkit.getPluginManager().registerEvents(this, this);
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                String newVersion;
                try {
                    newVersion = UpdateChecker.getVersion(112466);
                } catch (IOException e) {
                    print(e);
                    return;
                }
                if (newVersion != null) {
                    int compare = UpdateChecker.compareVersions(Clickthrough.this.getDescription().getVersion(), newVersion);
                    if (compare <= 0) {
                        update = null;
                    } else {
                        boolean newUpdate = update == null;
                        update = newVersion;
                        if (newUpdate) {
                            Bukkit.getOnlinePlayers().forEach(this::tellAboutUpdate);
                            tellAboutUpdate(Bukkit.getConsoleSender());
                        }
                    }
                }
            }, 60, 15 * 60000L);
        }
    }

    private void tellAboutUpdate(CommandSender sender) {
        if (update == null) return;
        if (!sender.hasPermission("clickthrough.admin")) return;
        sender.sendMessage(String.format("""
                §fThere is a new update of §aClickthrough§f available!
                §fCurrent Version: §a%s §fNew Version: §a%s
                §f§nhttps://www.spigotmc.org/resources/clickthrough.112466/""", getDescription().getVersion(), update));
    }

    @EventHandler
    public void on(PlayerJoinEvent e) {
        if (!getConfig().getBoolean("check-for-updates", true)) return;
        getServer().getScheduler().runTaskLater(this, () -> tellAboutUpdate(e.getPlayer()), 40);
    }

    public void print(Throwable t) {
        getLogger().log(Level.WARNING, t.getMessage(), t);
    }
}
