package de.snap20lp.offlineplayers;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.profile.PlayerProfile;
import com.onarandombox.multiverseinventories.share.Sharables;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * The Event Protector module prevents players from accidentally leaving their offline player in a WorldGuard protected
 * region. Frequently these regions are used for events, hence the name.
 *
 * @author Sugaku
 */
public class EventProtector implements Listener {

    /**
     * Whether bed spawns are allowed or not.
     */
    private final boolean isBedEnabled;

    /**
     * The world to teleport players to the spawn of.
     */
    private final String destinationWorld;

    /**
     * This is the instance of the custom flag that we need in order to query its value.
     */
    public static StateFlag BAN_OFFLINE_PLAYERS;

    /**
     * Creates a new MortalMaker and loads config file values.
     */
    public EventProtector () {
        FileConfiguration config = OfflinePlayers.getInstance().getConfig();
        isBedEnabled = config.getBoolean("OfflinePlayer.event-protector.is-bed-enabled", false);
        destinationWorld = config.getString("OfflinePlayer.event-protector.destination-world", "world");
    }

    /**
     * Called whenever a clone is spawned because a player is logging off.
     *
     * @param event The OfflinePlayerSpawnEvent.
     */
    @EventHandler
    public void onCloneSpawn (OfflinePlayerSpawnEvent event) {
        Location location = event.getLocation();
        World w = location.getWorld();
        if (w == null) return;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (set.testState(null, BAN_OFFLINE_PLAYERS)) {
            Location spawn = null;
            if (isBedEnabled) spawn = event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation();
            TownyAPI api = OfflinePlayers.getTownyAPI();
            if (api != null && spawn == null) {
                Town town = api.getTown(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
                if (town != null) {
                    try {
                        spawn = town.getSpawn();
                    } catch (TownyException ignored) { }
                }
            }
            if (spawn == null) spawn = Bukkit.getWorld(destinationWorld).getSpawnLocation();
            event.setLocation(spawn);
            MultiverseInventories multiInvAPI = OfflinePlayers.getMultiverseInventoriesAPI();
            if (multiInvAPI != null) {
                PlayerProfile profile = multiInvAPI.getGroupManager().getGroupsForWorld(spawn.getWorld().getName()).get(0).getGroupProfileContainer().getPlayerData(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
                ItemStack[] inv = profile.get(Sharables.INVENTORY);
                ItemStack[] armor = profile.get(Sharables.ARMOR);
                ItemStack offHand = profile.get(Sharables.OFF_HAND);
                event.setInventory(inv);
                event.setArmor(armor);
                event.setOffHand(offHand);
            }
        }
    }
}
