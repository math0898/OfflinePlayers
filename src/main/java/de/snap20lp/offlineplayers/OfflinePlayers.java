package de.snap20lp.offlineplayers;

import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerDespawnEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerHitEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class OfflinePlayers extends JavaPlugin implements Listener {

    private final double version = 1.0;
    private final HashMap<UUID, OfflinePlayer> offlinePlayerList = new HashMap<>();
    private final HashMap<Integer, OfflinePlayer> entityOfflinePlayerHashMap = new HashMap<>();

    public static OfflinePlayers getInstance() {
        return getPlugin(OfflinePlayers.class);
    }

    @Override
    public void onEnable() {
        System.out.println("OfflinePlayers starting in version " + getVersion());
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getOfflinePlayerList().values().forEach(offlinePlayer -> {
            offlinePlayer.despawnClone();
        });
    }

    @EventHandler
    public void on(PlayerJoinEvent playerJoinEvent) {
        if (getOfflinePlayerList().containsKey(playerJoinEvent.getPlayer().getUniqueId())) {

            OfflinePlayer clone = getOfflinePlayerList().get(playerJoinEvent.getPlayer().getUniqueId());
            OfflinePlayerDespawnEvent offlinePlayerDespawnEvent = new OfflinePlayerDespawnEvent(clone);
            Bukkit.getPluginManager().callEvent(offlinePlayerDespawnEvent);

            clone.despawnClone();
            playerJoinEvent.getPlayer().teleport(clone.getCloneEntity().getLocation());
            playerJoinEvent.getPlayer().addPotionEffects(clone.getCloneEntity().getActivePotionEffects());
            if (clone.isDead()) {
                playerJoinEvent.getPlayer().getInventory().clear();
                if (playerJoinEvent.getPlayer().getEquipment() != null) {
                    playerJoinEvent.getPlayer().getEquipment().clear();
                }
                playerJoinEvent.getPlayer().setLevel(0);
                playerJoinEvent.getPlayer().setExp(0.0f);
                playerJoinEvent.getPlayer().setHealth(0.0d);
            } else {
                playerJoinEvent.getPlayer().setHealth(clone.getCloneEntity().getHealth());
            }
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent playerQuitEvent) {
        Player quitPlayer = playerQuitEvent.getPlayer();
        OfflinePlayer offlinePlayer = new OfflinePlayer(quitPlayer, quitPlayer.getInventory().getContents(), quitPlayer.getEquipment() == null ? new ItemStack[]{} : quitPlayer.getEquipment().getArmorContents(), quitPlayer.getEquipment().getItemInMainHand(), quitPlayer.getEquipment().getItemInOffHand());
        OfflinePlayerSpawnEvent offlinePlayerSpawnEvent = new OfflinePlayerSpawnEvent(offlinePlayer);
        Bukkit.getPluginManager().callEvent(offlinePlayerSpawnEvent);
        getOfflinePlayerList().put(quitPlayer.getUniqueId(), offlinePlayer);
        getEntityOfflinePlayerHashMap().put(offlinePlayer.getCloneEntity().getEntityId(), offlinePlayer);

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
        if (getEntityOfflinePlayerHashMap().containsKey(entityTargetEvent.getEntity().getEntityId())) {
            entityTargetEvent.setCancelled(true);
        }
    }


    @EventHandler
    public void on(EntityDeathEvent event) {
        if (getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());

            OfflinePlayerDeathEvent offlinePlayerDeathEvent = new OfflinePlayerDeathEvent(offlinePlayer);
            Bukkit.getPluginManager().callEvent(offlinePlayerDeathEvent);

            event.getDrops().clear();
            for (ItemStack inventoryContent : offlinePlayer.getInventoryContents()) {
                if (inventoryContent != null) {
                    event.getDrops().add(inventoryContent);
                }
            }
            event.setDroppedExp(offlinePlayer.getPlayerExp());
            getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId()).despawnClone();
            getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId()).setDead(true);

        }
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity().getType() == EntityType.ZOMBIE) {
            if (getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId())) {
                Player damager = (Player) event.getDamager();
                OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
                if (!offlinePlayer.isHittable()) {
                    event.setCancelled(true);
                } else {
                    OfflinePlayerHitEvent offlinePlayerHitEvent = new OfflinePlayerHitEvent(offlinePlayer, damager);
                    Bukkit.getPluginManager().callEvent(offlinePlayerHitEvent);
                }
                damager.playSound(damager.getLocation(), Sound.ENTITY_PLAYER_HURT, 100, 1);
            }
        }
    }

}
