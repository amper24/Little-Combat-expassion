package dev.moonaticks.weaponSystem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class WeaponTagCommand implements CommandExecutor {

    private static final String PERMISSION_BASE = "weaponsystem.use";
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cКоманда только для игроков!");
            return true;
        }
        Player player = (Player) sender;
        // Проверка базового права
        if (!player.hasPermission(PERMISSION_BASE)) {
            player.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage("§cНужно держать предмет в руке!");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "twohanded":
                handleTwoHanded(player, item, args);
                break;
            case "offhanded":
                handleOffHanded(player, item, args);
                break;
            case "clear":
                handleClearTags(player, item);
                break;
            case "check":
                handleCheckTags(player, item);
                break;
            default:
                sendUsage(player);
        }

        return true;
    }

    private void handleTwoHanded(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
            meta.getPersistentDataContainer().remove(WeaponSystem.TWO_HANDED_KEY);
            player.sendMessage("§aТег 'twohanded' удален с предмета");
        } else {
            // Удаляем противоположный тег для избежания конфликтов
            meta.getPersistentDataContainer().set(WeaponSystem.TWO_HANDED_KEY, PersistentDataType.BOOLEAN, true);
            player.sendMessage("§aПредмет теперь считается двуручным");
        }
        item.setItemMeta(meta);
        player.updateInventory();
    }

    private void handleOffHanded(Player player, ItemStack item, String[] args) {
        ItemMeta meta = item.getItemMeta();
        if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
            meta.getPersistentDataContainer().remove(WeaponSystem.OFF_HANDED_KEY);
            player.sendMessage("§aТег 'offhanded' удален с предмета");
        } else {
            meta.getPersistentDataContainer().set(WeaponSystem.OFF_HANDED_KEY, PersistentDataType.BOOLEAN, true);
            // Удаляем противоположный тег
            meta.getPersistentDataContainer().remove(WeaponSystem.TWO_HANDED_KEY);
            player.sendMessage("§aПредмет теперь можно использовать в оффхенде");
        }
        item.setItemMeta(meta);
        player.updateInventory();
    }

    private void handleClearTags(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(WeaponSystem.TWO_HANDED_KEY);
        meta.getPersistentDataContainer().remove(WeaponSystem.OFF_HANDED_KEY);
        item.setItemMeta(meta);
        player.sendMessage("§aВсе теги оружия очищены");
        player.updateInventory();
    }

    private void handleCheckTags(Player player, ItemStack item) {
        boolean twoHanded = item.getPersistentDataContainer().has(WeaponSystem.TWO_HANDED_KEY, PersistentDataType.BOOLEAN);
        boolean offHanded = item.getPersistentDataContainer().has(WeaponSystem.OFF_HANDED_KEY, PersistentDataType.BOOLEAN);

        player.sendMessage("§6Информация о предмете:");
        player.sendMessage("§7- Двуручный: " + (twoHanded ? "§aДа" : "§cНет"));
        player.sendMessage("§7- Для оффхенда: " + (offHanded ? "§aДа" : "§cНет"));
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6Использование команды:");
        player.sendMessage("§e/weapontag twohanded §7- Сделать предмет двуручным");
        player.sendMessage("§e/weapontag twohanded remove §7- Удалить двуручность");
        player.sendMessage("§e/weapontag offhanded §7- Разрешить использовать в оффхенде");
        player.sendMessage("§e/weapontag offhanded remove §7- Запретить оффхенд");
        player.sendMessage("§e/weapontag clear §7- Очистить все теги");
        player.sendMessage("§e/weapontag check §7- Показать текущие теги");
    }
}