package dev.moonaticks.weaponSystem;

import io.papermc.paper.event.player.PlayerPickItemEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponListener implements Listener {
    private static final Map<UUID, ItemStack> lastAttack = new HashMap<>();
    private static final Map<UUID, Boolean> blockAttack = new ConcurrentHashMap<>();
    //Двуручное
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if ((event.getWhoClicked() instanceof Player player)) {
            Inventory inventory = event.getView().getTopInventory();
            Inventory clickedInv = event.getClickedInventory();
            if (event.getSlot() == 40 && clickedInv != inventory) {
                ItemStack cursorItem = event.getCursor();
                if (isTwoHanded(cursorItem)) {
                    event.setCancelled(true);
                }
                if(isTwoHanded(player)) {
                    event.setCancelled(true);
                }
            }
            //выкинуть
            int heldSlot = player.getInventory().getHeldItemSlot();
            if (event.getSlot() == heldSlot) {
                ItemStack newItem = event.getCursor();
                if(newItem.getType().equals(Material.AIR)) return;
                if (isTwoHanded(newItem)) {
                    returnOrDropOffHandItem(player);
                }
            }

            // Проверяем, что действие — перемещение между рукой и инвентарём (кнопка F)
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack clickedItem = event.getCurrentItem(); // Предмет, который пытаются переместить

                // Если предмет двухручный — отменяем и сбрасываем оффхенд
                if (clickedItem != null && (isTwoHanded(clickedItem) || isTwoHanded(player))) {
                    event.setCancelled(true); // Отменяем перемещение
                    returnOrDropOffHandItem(player); // Вызываем ваш метод
                }
            }
        }

    }
    @EventHandler
    public void onItemShuffle(PlayerSwapHandItemsEvent event) {
        if (isTwoHanded(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem != null && isTwoHanded(newItem)) {
            returnOrDropOffHandItem(player);
        }
    }
    @EventHandler
    public void onItemPickUp(PlayerPickItemEvent event) {
        Bukkit.getScheduler().runTaskLater(WeaponSystem.plugin, () -> {
            Player player = event.getPlayer();
            if (isTwoHanded(player)) {
                returnOrDropOffHandItem(player);
            }
        }, 1);
    }


    boolean isTwoHanded(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        // Проверяем тег
        return stack.getPersistentDataContainer().has(WeaponSystem.TWO_HANDED_KEY, PersistentDataType.BOOLEAN);
    }
    boolean isTwoHanded(ItemStack stack) {
        // Проверяем тег
        return stack.getPersistentDataContainer().has(WeaponSystem.TWO_HANDED_KEY, PersistentDataType.BOOLEAN);
    }
    boolean isOffHanded(ItemStack stack) {
        // Проверяем тег
        return stack.getPersistentDataContainer().has(WeaponSystem.OFF_HANDED_KEY, PersistentDataType.BOOLEAN);
    }
    public static void returnOrDropOffHandItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack offHandItem = inv.getItemInOffHand(); // Получаем предмет из оффхенда

        // Если в оффхенде пусто - ничего не делаем
        if (offHandItem.getType().isAir()) {
            return;
        }

        // Пытаемся добавить предмет в инвентарь
        if (inv.firstEmpty() != -1) { // Если есть свободные слоты
            inv.addItem(offHandItem); // Добавляем предмет в инвентарь
        } else { // Если нет места
            player.getWorld().dropItemNaturally(player.getLocation(), offHandItem); // Выкидываем предмет
        }

        // Очищаем оффхенд
        inv.setItemInOffHand(null);
    }
    //оружие левой руки
    @EventHandler
    void AditionalAttack(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        ItemStack weapon = event.getItem();
        EquipmentSlot slot = event.getHand();
        Player player = event.getPlayer();
        //проверки предмета
        if(weapon == null) return;
        if(!isOffHanded(weapon)) return;
        if(isTwoHanded(weapon)) return;
        if(isTwoHanded(player)) return;
        if(slot != EquipmentSlot.OFF_HAND) return;
        //полкчение аттрибутов
        AttributeInstance attribute_range = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if(attribute_range == null) return;

        ItemMeta meta = weapon.getItemMeta();
        meta.getAttributeModifiers(Attribute.ENTITY_INTERACTION_RANGE);
        double range = attribute_range.getValue() + getEntityInteractionRangeModifier(weapon);
        double damage = getAttackDamageModifier(player, weapon);
        double knockback = Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_KNOCKBACK)).getBaseValue() + getKnockbackModifier(weapon);

        recordAttack(player);
        coolDownBar(player, weapon);
        player.swingHand(slot);

        UUID playerId = player.getUniqueId();
        if(!blockAttack.containsKey(playerId)) {
            LivingEntity livingEntity = raycastToMob(player, range);
            if(livingEntity == null) return;

            applyAttack(player, livingEntity, weapon, damage, knockback);
            blockAttack.put(playerId, true);
            Bukkit.getScheduler().runTaskLater(WeaponSystem.plugin, () ->
                    blockAttack.remove(playerId), 5);
        }
    }
    public static double getEntityInteractionRangeModifier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0.0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasAttributeModifiers()) {
            return 0.0;
        }

        var modifiers = meta.getAttributeModifiers(Attribute.ENTITY_INTERACTION_RANGE);
        if (modifiers == null || modifiers.isEmpty()) {
            return 0.0;
        }

        double totalModifier = 0.0;
        for (AttributeModifier modifier : modifiers) {
            totalModifier += modifier.getAmount();
        }

        return totalModifier;
    }
    public static double getAttackDamageModifier(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0.0;
        }
        AttributeInstance attributeInstance = player.getAttribute(Attribute.ATTACK_DAMAGE);
        double totalDamage;
        if(attributeInstance != null) {
            totalDamage = attributeInstance.getBaseValue();
            boolean hasModifier = attributeInstance.getModifiers().stream()
                    .anyMatch(mod -> mod.getKey().equals(Key.key("minecraft:effect.strength")));
            if(hasModifier) {
                totalDamage += Objects.requireNonNull(attributeInstance.getModifier(Key.key("minecraft:effect.strength"))).getAmount();
            }
        } else {
            totalDamage = 1;
        }

        ItemMeta meta = item.getItemMeta();
        var modifiers = meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);

        if(modifiers != null && !modifiers.isEmpty()) {
            for (AttributeModifier modifier : modifiers) {
                totalDamage += modifier.getAmount();
            }
        } else {
            totalDamage += getToolDamage(item);
        }
        // Учитываем энчант "Острота" (Sharpness)
        if (meta.hasEnchant(Enchantment.SHARPNESS)) {
            int sharpnessLevel = meta.getEnchantLevel(Enchantment.SHARPNESS);
            totalDamage += 1.0 + (sharpnessLevel - 1) * 0.5; // Формула из Minecraft: 1 + (level * 0.5)
        }
        return totalDamage;
    }
    public static double getToolDamage(ItemStack item) {
        switch (item.getType()) {
            // ЛОПАТЫ
            case WOODEN_SHOVEL -> {
                return 2.5;
            }
            case STONE_SHOVEL -> {
                return 3.5;
            }
            case GOLDEN_SHOVEL -> {
                return 2.5;
            }
            case IRON_SHOVEL -> {
                return 4.5;
            }
            case DIAMOND_SHOVEL -> {
                return 5.5;
            }
            case NETHERITE_SHOVEL -> {
                return 6.5;
            }
            //МЕЧИ
            case WOODEN_SWORD -> {
                return 4.0;
            }
            case STONE_SWORD, DIAMOND_PICKAXE -> {
                return 5.0;
            }
            case GOLDEN_SWORD -> {
                return 4.0;
            }
            case IRON_SWORD, NETHERITE_PICKAXE -> {
                return 6.0;
            }
            case DIAMOND_SWORD, GOLDEN_AXE -> {
                return 7.0;
            }
            case NETHERITE_SWORD -> {
                return 8.0;
            }
            // КИРКИ
            case WOODEN_PICKAXE -> {
                return 2.0;
            }
            case STONE_PICKAXE -> {
                return 3.0;
            }
            case GOLDEN_PICKAXE -> {
                return 2.0;
            }
            case IRON_PICKAXE -> {
                return 4.0;
            }

            // ТОПОРЫ
            case WOODEN_AXE -> {
                return 7.0;
            }
            case STONE_AXE -> {
                return 9.0;
            }
            case IRON_AXE -> {
                return 9.0;
            }
            case DIAMOND_AXE -> {
                return 9.0;
            }
            case NETHERITE_AXE -> {
                return 10.0;
            }

            // МОТЫГИ
            case WOODEN_HOE -> {
                return 1.0;
            }
            case STONE_HOE -> {
                return 1.0;
            }
            case GOLDEN_HOE -> {
                return 1.0;
            }
            case IRON_HOE -> {
                return 1.0;
            }
            case DIAMOND_HOE -> {
                return 1.0;
            }
            case NETHERITE_HOE -> {
                return 1.0;
            }
            default -> {
                return 0.0;
            }
        }
    }
    public static double getSmiteDamageModifier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0.0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0.0;
        }

        double smiteDamage = 0.0;

        // Учитываем энчант "Небесная кара" (Smite)
        if (meta.hasEnchant(Enchantment.SMITE)) {
            int smiteLevel = meta.getEnchantLevel(Enchantment.SMITE);
            smiteDamage += 2.5 * smiteLevel; // Формула: 2.5 × уровень (Smite V даёт +12.5 урона)
        }

        return smiteDamage;
    }
    public static double getBaneOfArthropodsDamageModifier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0.0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0.0;
        }

        double baneDamage = 0.0;

        // Учитываем энчант "Бич членистоногих" (Bane of Arthropods)
        if (meta.hasEnchant(Enchantment.BANE_OF_ARTHROPODS)) {
            int baneLevel = meta.getEnchantLevel(Enchantment.BANE_OF_ARTHROPODS);
            baneDamage += 2.5 * baneLevel; // Формула: 2.5 × уровень (Bane V даёт +12.5 урона)
        }

        return baneDamage;
    }
    public static double getKnockbackModifier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1.0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1.0;
        }

        double knockback = 1.0;

        // 1. Учитываем атрибуты предмета (например, от модификаторов NBT)
        if (meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(Attribute.ATTACK_KNOCKBACK);
            if (modifiers != null) {
                for (AttributeModifier modifier : modifiers) {
                    knockback += modifier.getAmount();
                }
            }
        }

        // 2. Учитываем зачарование "Отдача" (Knockback)
        if (meta.hasEnchant(Enchantment.KNOCKBACK)) {
            int knockbackLevel = meta.getEnchantLevel(Enchantment.KNOCKBACK);
            knockback += knockbackLevel * 0.5; // Каждый уровень даёт +0.5 к отдаче
        }
        return knockback;
    }
    private static boolean isUndead(EntityType type) {
        // Список нежити (уязвимой к Smite)
        return switch (type) {
            case SKELETON, ZOMBIE, WITHER_SKELETON, ZOMBIFIED_PIGLIN, DROWNED, PHANTOM, WITHER, ZOMBIE_VILLAGER,
                 SKELETON_HORSE, ZOMBIE_HORSE, STRAY, HUSK -> true;
            default -> false;
        };
    }
    private static boolean isArthropod(EntityType type) {
        // Список членистоногих (уязвимых к Bane of Arthropods)
        return switch (type) {
            case SPIDER, CAVE_SPIDER, SILVERFISH, ENDERMITE, BEE -> true;
            default -> false;
        };
    }
    public static void applyFireAspect(ItemStack weapon, LivingEntity target) {
        // Проверяем валидность входных данных
        if (weapon == null || target == null || !weapon.hasItemMeta()) {
            return;
        }

        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) {
            return;
        }

        // Проверяем наличие зачарования Fire Aspect
        if (meta.hasEnchant(Enchantment.FIRE_ASPECT)) {
            int fireAspectLevel = meta.getEnchantLevel(Enchantment.FIRE_ASPECT);
            int fireTicks = calculateFireTicks(fireAspectLevel);

            // Применяем поджигание с учетом иммунитета
            if (!target.isInvulnerable()) {
                target.setFireTicks(fireTicks);
            }
        }
    }
    private static int calculateFireTicks(int fireAspectLevel) {
        // Fire Aspect I: 80 тиков (4 секунды)
        // Fire Aspect II: 160 тиков (8 секунд)
        return 80 * fireAspectLevel;
    }
    private boolean isCriticalHit(Player player) {
        return !player.isFlying() &&
                !player.isSprinting() &&
                player.getFallDistance() > 0.0F &&
                !player.isClimbing() &&
                !player.isSwimming() &&
                !player.hasPotionEffect(PotionEffectType.BLINDNESS);
    }
    public LivingEntity raycastToMob(Player player, double maxDistance) {
        // Получаем начальную точку (глаза игрока)
        Location start = player.getEyeLocation();
        // Получаем направление взгляда
        Vector direction = start.getDirection();

        // Создаем рейкаст
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                start,
                direction,
                maxDistance,
                0.025, // Разграничивающий размер хитбокса
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            return (LivingEntity) rayTrace.getHitEntity();
        }
        return null;
    }
    public static double getLootBonusModifier(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return 1.0; // Нет бонуса
        }

        ItemMeta meta = weapon.getItemMeta();
        double modifier = 1.0;

        // Учет зачарования "Добыча" (Looting)
        if (meta.hasEnchant(Enchantment.LOOTING)) {
            int lootingLevel = meta.getEnchantLevel(Enchantment.LOOTING);
            modifier += 0.15 * lootingLevel; // +15% за уровень
        }

        return Math.max(modifier, 1.0); // Минимум 1.0 (без бонуса)
    }
    private void applyAttack(Player player, LivingEntity target, ItemStack weapon,
                             double damage, double knockback) {

        // 1. Проверка на валидность цели
        if (target.isDead() || target.isInvulnerable() || target.getHealth() <= 0) {
            return;
        }

        // 2. Учет зачарований оружия
        double enchantDamageBonus = 0.0d;
        if(isUndead(target.getType())) {
            enchantDamageBonus += getSmiteDamageModifier(weapon);
        }
        if(isArthropod(target.getType())) {
            enchantDamageBonus += getBaneOfArthropodsDamageModifier(weapon);
        }

        double totalDamage = damage + enchantDamageBonus;

        // 4. Проверка на критический удар (игрок в прыжке + полное восстановление атаки)
        boolean isCritical = isCriticalHit(player);

        if (isCritical) {
            totalDamage *= 1.5; // +50% урона за крит (ванильное значение)
            player.spawnParticle(Particle.CRIT, target.getEyeLocation(), 10);
        }
        // В методе applyAttack используем так:
        if(perPlayerCooldown.containsKey(player.getUniqueId())) {
            totalDamage *= perPlayerCooldown.get(player.getUniqueId());
        }
        // 7. Нанесение урона
        target.damage(totalDamage, player);
        UUID uuid = target.getUniqueId();
        lastAttack.put(uuid, weapon);
        Bukkit.getScheduler().runTaskLater(WeaponSystem.plugin, () -> lastAttack.remove(uuid), 10);
        // 4. Применение огненного аспекта
        applyFireAspect(weapon, target);

        // 6. Применение отдачи
        Vector knockbackVector = target.getLocation()
                .toVector()
                .subtract(player.getLocation().toVector()) // target - player = направление ОТ игрока
                .normalize()
                .multiply(knockback * 0.5);
        target.setVelocity(knockbackVector);

        // 7. Эффекты при попадании
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.0f);
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
        // 8. Обновление статистики
        if (weapon != null && weapon.getType() != Material.AIR) {
            weapon.damage(1, target);
            player.updateInventory();
        }
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ItemStack weapon;
        Entity entity = event.getEntity();

        weapon = lastAttack.get(entity.getUniqueId());
        double lootModifier = getLootBonusModifier(weapon);
        // Применение модификатора
        modifyDrops(event.getDrops(), lootModifier);
    }
    private void modifyDrops(List<ItemStack> drops, double modifier) {
        for (ItemStack drop : new ArrayList<>(drops)) {
            // Увеличение количества (с округлением вверх)
            if (Math.random() < modifier - 1) {
                ItemStack bonus = drop.clone();
                bonus.setAmount(1);
                drops.add(bonus);
            }
        }
    }
    // КУЛДАУН СИСТЕМА
    private static final Map<UUID, Integer> lastAttackTicks = new HashMap<>();
    private static final Map<UUID, Double> perPlayerCooldown = new HashMap<>();
    public static void recordAttack(Player player) {
        int currentTime = Bukkit.getCurrentTick();
        UUID playerId = player.getUniqueId();
        lastAttackTicks.put(playerId, currentTime);
    }
    public static double calculateCurrentCooldown(Player player, ItemStack item) {
        UUID playerId = player.getUniqueId();
        int currentTime = Bukkit.getCurrentTick();
        // Получаем базовый кулдаун оружия (в секундах)
        double AttackSpeed = getAttackSpeed(player, item);
        if (AttackSpeed <= 0) { // Защита от нуля и отрицательных значений
            return 1.0; // Кулдаун считается восстановленным
        }
        double cooldownInSeconds = 1.0 / AttackSpeed;
        // Если игрок еще не атаковал
        if (!lastAttackTicks.containsKey(playerId)) {
            return 1.0; // Кулдаун полностью восстановлен
        }

        // Рассчитываем прошедшее время в секундах
        long lastAttackTime = lastAttackTicks.get(playerId);
        long timePassedTick = currentTime - lastAttackTime;
        double timePassedSeconds = (double) timePassedTick / 20;

            // Вычисляем прогресс восстановления (0.0 - 1.0)
            double cooldownProgress = timePassedSeconds / cooldownInSeconds;

        return Math.min(cooldownProgress, 1.0); // Не может быть больше 1.0
    }
    public static double getAttackSpeed(Player player, ItemStack item) {
        double baseAttackSpeed = Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).getBaseValue();
        AttributeInstance attributeInstance = player.getAttribute(Attribute.ATTACK_SPEED);
        if(attributeInstance != null) {
            double playerAttackSpeed = attributeInstance.getBaseValue();
            boolean hasModifier = attributeInstance.getModifiers().stream()
                    .anyMatch(mod -> mod.getKey().equals(Key.key("minecraft:effect.haste")));
            if(hasModifier) {
                playerAttackSpeed += Objects.requireNonNull(attributeInstance.getModifier(Key.key("minecraft:effect.haste"))).getAmount();
            }
            baseAttackSpeed = playerAttackSpeed;
        }
        if (item == null || !item.hasItemMeta()) {
            return baseAttackSpeed;
        }
        // Добавляем модификаторы из атрибутов предмета
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            var modifiers = meta.getAttributeModifiers(Attribute.ATTACK_SPEED);
            if (modifiers != null) {
                for (AttributeModifier modifier : modifiers) {
                    baseAttackSpeed += modifier.getAmount();
                }
            } else {
                baseAttackSpeed = getBaseAttackSpeed(item.getType());
            }
        } else {
            // Получаем базовую скорость атаки для типа предмета
            baseAttackSpeed = getBaseAttackSpeed(item.getType());
        }
        return Math.max(baseAttackSpeed, 0.1); // Минимальная скорость атаки
    }
    private static double getBaseAttackSpeed(Material material) {
        // Базовые значения скорости атаки (в ударах в секунду)
        return switch (material) {
            // Мечи
            case WOODEN_SWORD, GOLDEN_SWORD, STONE_SWORD,
                 IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> 1.6;

            // Топоры
            case WOODEN_AXE, GOLDEN_AXE, STONE_AXE -> 0.8;
            case IRON_AXE -> 0.9;
            case DIAMOND_AXE, NETHERITE_AXE -> 1.0;

            // Кирки
            case WOODEN_PICKAXE, GOLDEN_PICKAXE, STONE_PICKAXE,
                 IRON_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE -> 1.2;

            // Лопаты
            case WOODEN_SHOVEL, GOLDEN_SHOVEL, STONE_SHOVEL,
                 IRON_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL -> 1.0;

            // Мотыги
            case WOODEN_HOE, GOLDEN_HOE -> 1.0;
            case STONE_HOE -> 2.0;
            case IRON_HOE -> 3.0;
            case DIAMOND_HOE, NETHERITE_HOE -> 4.0;

            // Руки и прочее
            default -> 4.0;
        };
    }
    void coolDownBar(Player player, ItemStack weapon) {
        Bukkit.getScheduler().runTaskLater(WeaponSystem.plugin, () -> {
            double cooldown = calculateCurrentCooldown(player, weapon);
            showCooldownIndicator(player, cooldown);
            perPlayerCooldown.put(player.getUniqueId(), cooldown);
            if (cooldown < 1.0) {
                coolDownBar(player, weapon);
            }
        }, 2L);
    }

    //ВИЗУАЛ КУЛДАУНА
    // Кэш для хранения настроек cooldown
    private static class CooldownConfigCache {
        public final String[] frames = new String[4];
        public NamedTextColor color;
    }
    private static CooldownConfigCache cooldownCache = null;
    private void showCooldownIndicator(Player player, double cooldownProgress) {
        // Определяем текущий кадр анимации (1-4)
        int frameIndex;
        if (cooldownProgress >= 0.75) {
            frameIndex = 3; // frames[3] соответствует 4 кадру
        } else if (cooldownProgress >= 0.5) {
            frameIndex = 2;
        } else if (cooldownProgress >= 0.25) {
            frameIndex = 1;
        } else {
            frameIndex = 0;
        }

        // Получаем символ кадра из кэша
        Title title = getTitle(frameIndex);
        player.showTitle(title);
    }

    private static @NotNull Title getTitle(int frameIndex) {
        String frameSymbol = cooldownCache.frames[frameIndex];

        // Создаем и отправляем сообщение
        // Создание заголовка с пустым title и указанным subtitle
        // Заголовок (title) — пустой
        // Подзаголовок (subtitle)
        // Появление, длительность, исчезновение
        return Title.title(
                Component.empty(), // Заголовок (title) — пустой
                Component.text(frameSymbol).color(cooldownCache.color),          // Подзаголовок (subtitle)
                Title.Times.times(Ticks.duration(0), Ticks.duration(20), Ticks.duration(3)) // Появление, длительность, исчезновение
        );
    }

    public void updateCooldownCache() {
        ConfigurationSection cooldownSection = WeaponSystem.config.getConfigurationSection("cooldown");
        CooldownConfigCache newCache = new CooldownConfigCache();

        // Устанавливаем значения по умолчанию
        String[] defaultFrames = {"◉", "◎", "◌", "○"};
        NamedTextColor defaultColor = NamedTextColor.YELLOW;

        // Заполняем кадры
        for (int i = 0; i < 4; i++) {
            if (cooldownSection != null) {
                newCache.frames[i] = cooldownSection.getString((i+1) + "_frame", defaultFrames[i]);
            } else {
                newCache.frames[i] = defaultFrames[i];
            }
        }

        // Устанавливаем цвет
        try {
            String colorStr = cooldownSection != null ?
                    cooldownSection.getString("color", "YELLOW") : "YELLOW";
            newCache.color = NamedTextColor.NAMES.value(colorStr.toLowerCase());
        } catch (IllegalArgumentException e) {
            newCache.color = defaultColor;
        }

        cooldownCache = newCache;
    }
    @EventHandler
    void OnPlayerQuit(PlayerQuitEvent event) {
        blockAttack.remove(event.getPlayer().getUniqueId());
    }
}
