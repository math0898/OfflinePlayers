package de.snap20lp.offlineplayers;

import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerDespawnEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerHitEvent;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@Getter
public class OfflinePlayers extends JavaPlugin implements Listener {

    private final double version = 1.6;
    private boolean isCitizensEnabled = false;
    private final HashMap<UUID, OfflinePlayer> offlinePlayerList = new HashMap<>();
    private final HashMap<Integer, OfflinePlayer> entityOfflinePlayerHashMap = new HashMap<>();
    private NPCRegistry inMemoryNPCRegistry;
    public static OfflinePlayers getInstance() {
        return getPlugin(OfflinePlayers.class);
    }

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("§aOfflinePlayers starting in version " + getVersion());
        if(getServer().getPluginManager().getPlugin("Citizens") == null) {
            Bukkit.getConsoleSender().sendMessage("§e[OfflinePlayers] WARNING: Citizens is not installed! EntityType PLAYER is not available!");
        } else {
            inMemoryNPCRegistry = CitizensAPI.createInMemoryNPCRegistry("OfflinePlayersRegistry");
            isCitizensEnabled = true;
        }
        this.saveDefaultConfig();
        try {
            EntityType.valueOf(getConfig().getString("OfflinePlayer.cloneEntity"));
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] ERROR: The cloneEntity in the config.yml is not a valid EntityType!");
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Please change the cloneEntity in the config.yml to a valid EntityType!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getOfflinePlayerList().values().forEach(OfflinePlayer::despawnClone);
        inMemoryNPCRegistry.deregisterAll();
    }

    @EventHandler
    public void on(EntityResurrectEvent entityResurrectEvent) {
        if (getEntityOfflinePlayerHashMap().containsKey(entityResurrectEvent.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(entityResurrectEvent.getEntity().getEntityId());
            if (offlinePlayer.getCloneEntity().getEquipment().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING || offlinePlayer.getOffHand().getType() != Material.TOTEM_OF_UNDYING) {
                entityResurrectEvent.setCancelled(true);
                return;
            }
                ArrayList<ItemStack> savedInventoryContentsTemp = offlinePlayer.getSavedInventoryContents();
            for (ItemStack itemStack : savedInventoryContentsTemp) {
                if (itemStack != null && itemStack.getType() == Material.TOTEM_OF_UNDYING) {
                    offlinePlayer.getSavedInventoryContents().remove(itemStack);
                    break;
                }
            }
            if (offlinePlayer.getCloneEntity().getEquipment().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
                offlinePlayer.getCloneEntity().getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
                offlinePlayer.getOffHand().setType(Material.AIR);
                return;
            }
            if (offlinePlayer.getCloneEntity().getEquipment().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING) {
                offlinePlayer.getCloneEntity().getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                offlinePlayer.getMainHand().setType(Material.AIR);
            }
        }
    }


    @EventHandler
    public void on(PlayerJoinEvent playerJoinEvent) {
        NPC npc = OfflinePlayers.getInstance().getInMemoryNPCRegistry().createNPC(EntityType.PLAYER,playerJoinEvent.getPlayer().getName());
        npc.spawn(playerJoinEvent.getPlayer().getLocation());
        npc.despawn();
        if (getOfflinePlayerList().containsKey(playerJoinEvent.getPlayer().getUniqueId())) {

            OfflinePlayer clone = getOfflinePlayerList().get(playerJoinEvent.getPlayer().getUniqueId());
            OfflinePlayerDespawnEvent offlinePlayerDespawnEvent = new OfflinePlayerDespawnEvent(clone);
            Bukkit.getPluginManager().callEvent(offlinePlayerDespawnEvent);

            playerJoinEvent.getPlayer().teleport(clone.getCloneEntity().getLocation());
            playerJoinEvent.getPlayer().addPotionEffects(clone.getCloneEntity().getActivePotionEffects());

            //Bug fix 1.2 | Thank you for the support :)
            playerJoinEvent.getPlayer().getInventory().setItemInMainHand(clone.getCloneEntity().getEquipment().getItemInMainHand());
            playerJoinEvent.getPlayer().getInventory().setItemInOffHand(clone.getCloneEntity().getEquipment().getItemInOffHand());

            if (clone.getCloneEntity().hasPotionEffect(PotionEffectType.SLOW) && clone.isHasAI()) {
                playerJoinEvent.getPlayer().removePotionEffect(PotionEffectType.SLOW);
            }

            if (clone.isDead()) {
                if (playerJoinEvent.getPlayer().getEquipment() != null) {
                    playerJoinEvent.getPlayer().getEquipment().clear();
                }
                playerJoinEvent.getPlayer().setLevel(0);
                playerJoinEvent.getPlayer().setExp(0.0f);
                playerJoinEvent.getPlayer().setHealth(0.0d);
            } else {
                playerJoinEvent.getPlayer().setHealth(clone.getCloneEntity().getHealth());
            }
            clone.despawnClone();
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent playerQuitEvent) {
        Player quitPlayer = playerQuitEvent.getPlayer();
        if(getConfig().getStringList("OfflinePlayer.worldBlacklist").contains(quitPlayer.getWorld().getName())) {
            return;
        }
        if(getConfig().getBoolean("OfflinePlayer.permissions.enabled") && getConfig().getString("OfflinePlayer.permissions.permission") != null) {
            if(!quitPlayer.hasPermission(getConfig().getString("OfflinePlayer.permissions.permission"))) {
                return;
            }
        }

        if (quitPlayer.getGameMode() == GameMode.CREATIVE && !getConfig().getBoolean("OfflinePlayer.spawnOnCreative")) {
            return;
        }
        OfflinePlayer offlinePlayer = new OfflinePlayer(quitPlayer, new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getContents())), quitPlayer.getEquipment() == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(quitPlayer.getInventory().getArmorContents())), quitPlayer.getEquipment().getItemInMainHand(), quitPlayer.getEquipment().getItemInOffHand());
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

            for (ItemStack inventoryContent : offlinePlayer.getSavedInventoryContents()) {
                if (inventoryContent != null) {
                    event.getDrops().add(inventoryContent);
                }
            }
            event.setDroppedExp(offlinePlayer.getPlayerExp());
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId()).despawnClone(), 25);
            getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId()).setDead(true);

        }
    }

    @EventHandler
    public void on(EntityDamageEvent event) {
        if (event.getEntity().getType() == EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity")) && getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId())) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
            if (!offlinePlayer.isHittable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity")) && getEntityOfflinePlayerHashMap().containsKey(event.getEntity().getEntityId()) && event.getDamager() instanceof Player damager) {
            OfflinePlayer offlinePlayer = getEntityOfflinePlayerHashMap().get(event.getEntity().getEntityId());
            if (!offlinePlayer.isHittable()) {
                event.setCancelled(true);
            } else {
                OfflinePlayerHitEvent offlinePlayerHitEvent = new OfflinePlayerHitEvent(offlinePlayer, damager);
                Bukkit.getPluginManager().callEvent(offlinePlayerHitEvent);
                damager.playSound(damager.getLocation(), Sound.ENTITY_PLAYER_HURT, 100, 1);
            }
        }
    }

}
