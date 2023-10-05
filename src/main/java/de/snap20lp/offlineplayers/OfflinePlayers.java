package de.snap20lp.offlineplayers;

import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerDespawnEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerHitEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import lombok.Getter;
import me.libraryaddict.disguise.events.UndisguiseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

@Getter
public class OfflinePlayers extends JavaPlugin implements Listener {

    private final double version = 2.7;
    private final HashMap<UUID, OfflinePlayer> offlinePlayerList = new HashMap<>();
    private final HashMap<Integer, OfflinePlayer> entityOfflinePlayerHashMap = new HashMap<>();
    Metrics metrics;
    public static OfflinePlayers getInstance() {
        return getPlugin(OfflinePlayers.class);
    }

    @Override
    public void onEnable() {
        this.metrics = new Metrics(this, 19973);

        Bukkit.getConsoleSender().sendMessage("§aOfflinePlayers starting in version " + getVersion());
        if (getServer().getPluginManager().getPlugin("LibsDisguises") == null || getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] ERROR: LibsDisguises is not activated! Please install LibsDisguises and ProtocolLib to use this plugin!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.saveDefaultConfig();
        try {
            EntityType.valueOf(getConfig().getString("OfflinePlayer.cloneRawEntity"));
            if(OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.useBlockEntity")) {
                try {
                BlockData block = Material.valueOf(getConfig().getString("OfflinePlayer.cloneEntity")).createBlockData().getMaterial().createBlockData();
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] ERROR: The cloneEntity in the config.yml is not a valid Block Material!");
                    Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Since you have 'useBlockEntity' enabled the cloneEntity needs to be an Block Material");
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
            } else {
                EntityType.valueOf(getConfig().getString("OfflinePlayer.cloneEntity"));
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] ERROR: The cloneEntities in the config.yml are not a valid EntityType!");
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Please change the cloneEntities in the config.yml to a valid EntityType!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.metrics.shutdown();

        if (getServer().getPluginManager().getPlugin("LibsDisguises") != null && getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            getOfflinePlayerList().values().forEach(OfflinePlayer::despawnClone);
        }
    }

    @EventHandler
    public void on(EntityResurrectEvent entityResurrectEvent) {
        if (getEntityOfflinePlayerHashMap().containsKey(entityResurrectEvent.getEntity().getEntityId())) {
            if (getEntityOfflinePlayerHashMap().get(entityResurrectEvent.getEntity().getEntityId()) == null) {
                return;
            }
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(entityResurrectEvent.getEntity().getEntityId());
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


            getOfflinePlayerList().remove(offlinePlayer.getOfflinePlayer().getUniqueId());
            getEntityOfflinePlayerHashMap().remove(entityResurrectEvent.getEntity().getEntityId());
            offlinePlayer.despawnClone();


            getOfflinePlayerList().put(newOfflinePlayer.getOfflinePlayer().getUniqueId(), newOfflinePlayer);
            getEntityOfflinePlayerHashMap().put(newOfflinePlayer.getCloneEntity().getEntityId(), newOfflinePlayer);

        }
    }




    @EventHandler
    public void on(PlayerJoinEvent playerJoinEvent) {

        if (getOfflinePlayerList().containsKey(playerJoinEvent.getPlayer().getUniqueId())) {
            if (getOfflinePlayerList().get(playerJoinEvent.getPlayer().getUniqueId()) == null) {
                return;
            }
            OfflinePlayer clone = getOfflinePlayerList().get(playerJoinEvent.getPlayer().getUniqueId());


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
                if (!getConfig().getBoolean("OfflinePlayer.cloneKeepItems")) {
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

            getOfflinePlayerList().remove(playerJoinEvent.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent playerQuitEvent) {
        Player quitPlayer = playerQuitEvent.getPlayer();

        if (getConfig().getStringList("OfflinePlayer.worldBlacklist").contains(quitPlayer.getWorld().getName())) {
            return;
        }
        if (getConfig().getStringList("OfflinePlayer.game-modeBlacklist").contains(quitPlayer.getGameMode().name())) {
            return;
        }
        if (getConfig().getBoolean("OfflinePlayer.permissions.enabled") && getConfig().getString("OfflinePlayer.permissions.permission") != null) {
            if (!quitPlayer.hasPermission(getConfig().getString("OfflinePlayer.permissions.permission"))) {
                return;
            }
        }


        OfflinePlayer offlinePlayer = new OfflinePlayer(quitPlayer, new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getContents())), quitPlayer.getEquipment() == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getArmorContents())), quitPlayer.getEquipment().getItemInMainHand(), quitPlayer.getEquipment().getItemInOffHand());
        OfflinePlayerSpawnEvent offlinePlayerSpawnEvent = new OfflinePlayerSpawnEvent(offlinePlayer);
        Bukkit.getPluginManager().callEvent(offlinePlayerSpawnEvent);
        getOfflinePlayerList().put(quitPlayer.getUniqueId(), offlinePlayer);
        getEntityOfflinePlayerHashMap().put(offlinePlayer.getCloneEntity().getEntityId(), offlinePlayer);
    }

    @EventHandler
    public void on(UndisguiseEvent undisguiseEvent) {
        if(getEntityOfflinePlayerHashMap().containsKey(undisguiseEvent.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(undisguiseEvent.getEntity().getEntityId());
            offlinePlayer.replaceCloneStats((LivingEntity) undisguiseEvent.getEntity());
        }
    }

    @EventHandler
    public void on(PlayerDropItemEvent event) {
        if (getConfig().getBoolean("OfflinePlayer.cloneItemPickup")) {
            event.getItemDrop().getNearbyEntities(6, 6, 6).forEach(entity -> {
                if (getEntityOfflinePlayerHashMap().containsKey(entity.getEntityId())) {
                    OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(entity.getEntityId());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {

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
        if (getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
            if (offlinePlayer.getCloneEntity().getLocation().getBlock().getType() != Material.LAVA && offlinePlayer.getCloneEntity().getLocation().getBlock().getType() != Material.FIRE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(EntityTargetEvent entityTargetEvent) {
        if (entityTargetEvent.getTarget() == null)
            return;
        if (getEntityOfflinePlayerHashMap().containsKey(entityTargetEvent.getTarget().getEntityId())) {
            entityTargetEvent.setCancelled(true);
            entityTargetEvent.setTarget(null);
        }
    }

    @EventHandler
    public void on(EntityDeathEvent event) {
        OfflinePlayer offlinePlayer = null;
        for (OfflinePlayer value : getOfflinePlayerList().values()) {
            if(value.getCloneEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
                offlinePlayer = value;
                break;
            }
        }
        if (offlinePlayer != null) {
      OfflinePlayerDeathEvent offlinePlayerDeathEvent = new OfflinePlayerDeathEvent(offlinePlayer);
            Bukkit.getPluginManager().callEvent(offlinePlayerDeathEvent);

            event.getDrops().clear();

            if (!getConfig().getBoolean("OfflinePlayer.cloneKeepItems")) {

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
    public void on(EntityDamageEvent event) {
        if (event.getEntity().getType() == EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneRawEntity")) && getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
            if (!offlinePlayer.isCloneIsHittable()) {
                event.setCancelled(true);
                return;
            }
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_HURT, 100, 1);
        }
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent event) {
        if (getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId()) && event.getDamager() instanceof Player damager) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
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
