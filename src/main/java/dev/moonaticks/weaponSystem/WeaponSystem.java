package dev.moonaticks.weaponSystem;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeaponSystem extends JavaPlugin {
    public static NamespacedKey TWO_HANDED_KEY;
    public static NamespacedKey OFF_HANDED_KEY;
    public static WeaponSystem plugin;
    public static FileConfiguration config;
    @Override
    public void onEnable() {
        plugin = this;
        // Plugin startup logic
        TWO_HANDED_KEY = new NamespacedKey(this, "two_handed");
        OFF_HANDED_KEY = new NamespacedKey(this, "off_handed");

        saveDefaultConfig();
        config = getConfig();
        WeaponListener weaponListener = new WeaponListener();
        weaponListener.updateCooldownCache();
        Bukkit.getPluginManager().registerEvents(weaponListener, plugin);
        this.getCommand("weapontag").setExecutor(new WeaponTagCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    WeaponSystem getPlugin() {
        return plugin;
    }
}
