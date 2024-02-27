package de.snap20lp.offlineplayers;

import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerDespawnEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerHitEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import me.libraryaddict.disguise.events.UndisguiseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The CloneManager manages creating, saving, loading, and removing clones. Follows the singleton instance model.
 *
 * @author Sugaku
 */
public class CloneManager implements Listener { // todo: Perhaps refactor events into an event manager.

    /**
     * The active CloneManager instance.
     */
    private static CloneManager singleton = null;

    private final HashMap<UUID, OfflinePlayer> offlinePlayerList = new HashMap<>();

    private final HashMap<Integer, OfflinePlayer> entityOfflinePlayerHashMap = new HashMap<>();

    /**
     * Creates a new CloneManager by loading, if present, persistent clones from the drive.
     */
    public CloneManager () { // todo: Might be cleaner to use a custom constructor.
        FileConfiguration save = new YamlConfiguration();
        try {
            save.load("./plugins/OfflinePlayers/clones.yml");
        } catch (IOException | InvalidConfigurationException exception) {
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Failed to load clones from disk. Hopefully first run: " + exception.getMessage());
            return;
        }
        for (String s : save.getKeys(false)) {
            UUID uuid = UUID.fromString(s);
            int currentSeconds = save.getInt(s + ".current-seconds", 0);
            Location location = save.getLocation(s + ".location", new Location(null, 0, 0,0));
            int playerXp = save.getInt(s + ".player-xp", 0);
            double currentHP = save.getDouble(s + ".hp", 0);
            ArrayList<ItemStack> inventory = new ArrayList<>();
            ConfigurationSection section = save.getConfigurationSection(s + ".inventory");
            if (section != null)
                for (String i : section.getKeys(false))
                    inventory.add(save.getItemStack(s + ".inventory." + i, new ItemStack(Material.AIR, 1)));
            ArrayList<ItemStack> armor = new ArrayList<>();
            section = save.getConfigurationSection(s + ".armor");
            if (section != null)
                for (String i : save.getConfigurationSection(s + ".armor").getKeys(false))
                    armor.add(save.getItemStack(s + ".armor." + i, new ItemStack(Material.AIR, 1)));
            ItemStack mainHand = save.getItemStack(s + ".main-hand", new ItemStack(Material.AIR, 1));
            ItemStack offHand = save.getItemStack(s + ".off-hand", new ItemStack(Material.AIR, 1));
            OfflinePlayer player = new OfflinePlayer(Bukkit.getOfflinePlayer(uuid), currentSeconds, location, playerXp, currentHP, inventory, armor, mainHand, offHand);
            offlinePlayerList.put(uuid, player);
            entityOfflinePlayerHashMap.put(player.getCloneEntity().getEntityId(), player);
        }
    }

    /**
     * Accessor method for the active CloneManager instance.
     *
     * @return The active CloneManager instance.
     */
    public static CloneManager getInstance () {
        if (singleton == null) singleton = new CloneManager();
        return singleton;
    }

    public HashMap<UUID, OfflinePlayer> getOfflinePlayerList () { // todo: Consider changing.
        return offlinePlayerList;
    }

    public HashMap<Integer, OfflinePlayer> getEntityOfflinePlayerHashMap () { // todo: Consider changing.
        return entityOfflinePlayerHashMap;
    }

    /**
     * Saves all current clones to the disk.
     */
    public void save () { // Todo: Might be cleaner to make each save themselves.
        FileConfiguration save = new YamlConfiguration();
        for (UUID s : getOfflinePlayerList().keySet()) {
            String uuid = s.toString();
            OfflinePlayer player = getOfflinePlayerList().get(s);
            save.set(uuid + ".current-seconds", player.getCurrentSeconds());
            save.set(uuid + ".location", player.getCloneEntity().getLocation());
            save.set(uuid + ".player-xp", player.getPlayerExp());
            save.set(uuid + ".hp", player.getCurrentHP());
            ArrayList<ItemStack> inventory = player.getSavedInventoryContents();
            for (ItemStack i : inventory)
                save.set(uuid + ".inventory." + inventory.indexOf(i), i);
            ArrayList<ItemStack> armor = player.getSavedArmorContents();
            for (ItemStack i : armor)
                save.set(uuid + ".armor." + armor.indexOf(i), i);
            save.set(uuid + ".main-hand", player.getMainHand());
            save.set(uuid + ".off-hand", player.getOffHand());
        }
        try {
            save.save("./plugins/OfflinePlayers/clones.yml");
        } catch (IOException ioException) {
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Failed to save clone data:" + ioException.getMessage());
        }
    }

    @EventHandler
    public void on(EntityResurrectEvent entityResurrectEvent) {
        if (entityOfflinePlayerHashMap.containsKey(entityResurrectEvent.getEntity().getEntityId())) {
            if (entityOfflinePlayerHashMap.get(entityResurrectEvent.getEntity().getEntityId()) == null) {
                return;
            }
            OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(entityResurrectEvent.getEntity().getEntityId());
            if (offlinePlayer.getMainHand().getType() != Material.TOTEM_OF_UNDYING && offlinePlayer.getOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                entityResurrectEvent.setCancelled(true);
                return;
            }

            entityResurrectEvent.getEntity().getWorld().playSound(entityResurrectEvent.getEntity().getLocation(), Sound.ITEM_TOTEM_USE, 100, 1);

            if (offlinePlayer.getMainHand().getType() == Material.TOTEM_OF_UNDYING)
                offlinePlayer.getMainHand().setType(Material.AIR);
            else if (offlinePlayer.getOffHand().getType() == Material.TOTEM_OF_UNDYING)
                offlinePlayer.getOffHand().setType(Material.AIR);

            OfflinePlayer newOfflinePlayer = new OfflinePlayer(offlinePlayer.getOfflinePlayer(), offlinePlayer.getCurrentSeconds(), offlinePlayer.getCloneEntity().getLocation(), offlinePlayer.getPlayerExp(), offlinePlayer.getCurrentHP(), offlinePlayer.getSavedInventoryContents(), offlinePlayer.getSavedArmorContents(), offlinePlayer.getMainHand(), offlinePlayer.getOffHand());


            offlinePlayerList.remove(offlinePlayer.getOfflinePlayer().getUniqueId());
            entityOfflinePlayerHashMap.remove(entityResurrectEvent.getEntity().getEntityId());
            offlinePlayer.despawnClone();


            offlinePlayerList.put(newOfflinePlayer.getOfflinePlayer().getUniqueId(), newOfflinePlayer);
            entityOfflinePlayerHashMap.put(newOfflinePlayer.getCloneEntity().getEntityId(), newOfflinePlayer);

        }
    }




    @EventHandler
    public void on(PlayerJoinEvent playerJoinEvent) {

        if (offlinePlayerList.containsKey(playerJoinEvent.getPlayer().getUniqueId())) {
            if (offlinePlayerList.get(playerJoinEvent.getPlayer().getUniqueId()) == null) {
                return;
            }
            OfflinePlayer clone = offlinePlayerList.get(playerJoinEvent.getPlayer().getUniqueId());


            OfflinePlayerDespawnEvent offlinePlayerDespawnEvent = new OfflinePlayerDespawnEvent(clone);
            Bukkit.getPluginManager().callEvent(offlinePlayerDespawnEvent);
            playerJoinEvent.getPlayer().teleport(clone.getCloneEntity().getLocation());
            playerJoinEvent.getPlayer().getActivePotionEffects().forEach(potionEffect -> playerJoinEvent.getPlayer().removePotionEffect(potionEffect.getType()));
            playerJoinEvent.getPlayer().addPotionEffects(clone.getCloneEntity().getActivePotionEffects());

            playerJoinEvent.getPlayer().getInventory().setItemInMainHand(clone.getCloneEntity().getEquipment().getItemInMainHand());
            playerJoinEvent.getPlayer().getInventory().setItemInOffHand(clone.getCloneEntity().getEquipment().getItemInOffHand());
            if (clone.getCloneEntity().hasPotionEffect(PotionEffectType.SLOW) && clone.isCloneHasAI()) {
                playerJoinEvent.getPlayer().removePotionEffect(PotionEffectType.SLOW);
            }
            if (clone.isDead()) {
                if (!OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneKeepItems")) {
                    if (playerJoinEvent.getPlayer().getEquipment() != null) {
                        playerJoinEvent.getPlayer().getEquipment().clear();
                    }
                }
                playerJoinEvent.getPlayer().setLevel(0);
                playerJoinEvent.getPlayer().setExp(0.0f);
                playerJoinEvent.getPlayer().setHealth(0.0d);
            } else {
                playerJoinEvent.getPlayer().setFireTicks(clone.getCloneEntity().getFireTicks());
                playerJoinEvent.getPlayer().setHealth(clone.getCloneEntity().getHealth());
                clone.getAddedItems().forEach(itemStack -> {
                    if (itemStack != null) {
                        playerJoinEvent.getPlayer().getInventory().addItem(itemStack);
                    }
                });
            }

            clone.despawnClone();

            offlinePlayerList.remove(playerJoinEvent.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent playerQuitEvent) {
        Player quitPlayer = playerQuitEvent.getPlayer();
        FileConfiguration config = OfflinePlayers.getInstance().getConfig();
        if (config.getStringList("OfflinePlayer.worldBlacklist").contains(quitPlayer.getWorld().getName())) return;
        if (config.getStringList("OfflinePlayer.game-modeBlacklist").contains(quitPlayer.getGameMode().name())) return;
        if (config.getBoolean("OfflinePlayer.permissions.enabled") && config.getString("OfflinePlayer.permissions.permission") != null)
            if (!quitPlayer.hasPermission(config.getString("OfflinePlayer.permissions.permission", "offlineplayer.clone")))
                return;

        OfflinePlayer offlinePlayer = new OfflinePlayer(quitPlayer, new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getContents())), quitPlayer.getEquipment() == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getArmorContents())), quitPlayer.getEquipment().getItemInMainHand(), quitPlayer.getEquipment().getItemInOffHand());
        OfflinePlayerSpawnEvent offlinePlayerSpawnEvent = new OfflinePlayerSpawnEvent(offlinePlayer);
        Bukkit.getPluginManager().callEvent(offlinePlayerSpawnEvent);
        offlinePlayerList.put(quitPlayer.getUniqueId(), offlinePlayer);
        entityOfflinePlayerHashMap.put(offlinePlayer.getCloneEntity().getEntityId(), offlinePlayer);
    }

    @EventHandler
    public void on(UndisguiseEvent undisguiseEvent) {
        if(entityOfflinePlayerHashMap.containsKey(undisguiseEvent.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(undisguiseEvent.getEntity().getEntityId());
            offlinePlayer.replaceCloneStats((LivingEntity) undisguiseEvent.getEntity());
        }
    }

    @EventHandler
    public void on(PlayerDropItemEvent event) {
        if (OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneItemPickup")) {
            event.getItemDrop().getNearbyEntities(6, 6, 6).forEach(entity -> {
                if (entityOfflinePlayerHashMap.containsKey(entity.getEntityId())) {
                    OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(entity.getEntityId());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(OfflinePlayers.getInstance(), () -> {

                        if(!event.getItemDrop().getNearbyEntities(1, 1, 1).contains(offlinePlayer.getCloneEntity())){
                            return;
                        }

                        offlinePlayer.getAddedItems().add(event.getItemDrop().getItemStack());
                        offlinePlayer.getCloneEntity().getWorld().playSound(offlinePlayer.getCloneEntity().getLocation(), Sound.ENTITY_ITEM_PICKUP, 100,2);
                        event.getItemDrop().remove();

                    }, 50L);
                }
            });
        }
    }

    @EventHandler
    public void on(EntityCombustEvent event) {
        if (entityOfflinePlayerHashMap.containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(event.getEntity().getEntityId());
            if (offlinePlayer.getCloneEntity().getLocation().getBlock().getType() != Material.LAVA && offlinePlayer.getCloneEntity().getLocation().getBlock().getType() != Material.FIRE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(EntityTargetEvent entityTargetEvent) {
        if (entityTargetEvent.getTarget() == null)
            return;
        if (entityOfflinePlayerHashMap.containsKey(entityTargetEvent.getTarget().getEntityId())) {
            entityTargetEvent.setCancelled(true);
            entityTargetEvent.setTarget(null);
        }
    }

    @EventHandler
    public void on(EntityDeathEvent event) {
        OfflinePlayer offlinePlayer = null;
        for (OfflinePlayer value : offlinePlayerList.values()) {
            if(value.getCloneEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
                offlinePlayer = value;
                break;
            }
        }
        if (offlinePlayer != null) {
            OfflinePlayerDeathEvent offlinePlayerDeathEvent = new OfflinePlayerDeathEvent(offlinePlayer);
            Bukkit.getPluginManager().callEvent(offlinePlayerDeathEvent);

            event.getDrops().clear();

            if (!OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneKeepItems")) {

                for (ItemStack inventoryContent : offlinePlayer.getSavedInventoryContents()) {
                    if (inventoryContent != null) {
                        event.getDrops().add(inventoryContent);
                    }
                }

                offlinePlayer.getAddedItems().forEach(itemStack -> {
                    if (itemStack != null) {
                        event.getDrops().add(itemStack);
                    }
                });

            }

            event.setDroppedExp(offlinePlayer.getPlayerExp());
            offlinePlayer.setHidden(true);
            offlinePlayer.despawnClone();
            offlinePlayer.setDead(true);
        }
    }

    @EventHandler
    public void on (EntityDamageEvent event) {
        if (event.getEntity().getType() == EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneRawEntity")) && entityOfflinePlayerHashMap.containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(event.getEntity().getEntityId());
            if (!offlinePlayer.isCloneIsHittable()) {
                event.setCancelled(true);
                return;
            }
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_HURT, 100, 1);
        }
    }

    @EventHandler
    public void on (EntityDamageByEntityEvent event) {
        if (entityOfflinePlayerHashMap.containsKey(event.getEntity().getEntityId()) && event.getDamager() instanceof Player damager) {
            OfflinePlayer offlinePlayer = entityOfflinePlayerHashMap.get(event.getEntity().getEntityId());
            if (!offlinePlayer.isCloneIsHittable()) {
                event.setCancelled(true);
            } else {
                OfflinePlayerHitEvent offlinePlayerHitEvent = new OfflinePlayerHitEvent(offlinePlayer, damager);
                Bukkit.getPluginManager().callEvent(offlinePlayerHitEvent);
                damager.playSound(damager.getLocation(), Sound.ENTITY_PLAYER_HURT, 100, 1);
            }
        }
    }
}
