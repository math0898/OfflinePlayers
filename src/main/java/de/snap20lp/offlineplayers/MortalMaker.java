package de.snap20lp.offlineplayers;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.profile.PlayerProfile;
import com.onarandombox.multiverseinventories.share.Sharables;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Mortal maker prevents players from logging off in creative worlds to save themselves from losing their inventory.
 *
 * @author Sugaku
 */
public class MortalMaker implements Listener {

    /**
     * A list of worlds which the MortalMaker looks at.
     */
    private final List<String> whitelist;

    /**
     * Whether bed spawns are allowed or not.
     */
    private final boolean isBedEnabled;

    /**
     * The world to teleport players to the spawn of.
     */
    private final String destinationWorld;

    /**
     * Creates a new MortalMaker and loads config file values.
     */
    public MortalMaker () {
        FileConfiguration config = OfflinePlayers.getInstance().getConfig();
        whitelist = config.getStringList("OfflinePlayer.mortal-maker.world-whitelist");
        isBedEnabled = config.getBoolean("OfflinePlayer.mortal-maker.is-bed-enabled", false);
        destinationWorld = config.getString("OfflinePlayer.mortal-maker.destination-world", "world");
    }

    /**
     * Called whenever a clone is spawned because a player is logging off.
     *
     * @param event The OfflinePlayerSpawnEvent.
     */
    @EventHandler
    public void onCloneSpawn (OfflinePlayerSpawnEvent event) { // todo: This method requires a lot of cognitive load to understand. Simplify by breaking up into more readable chunks.
        if (whitelist.contains(event.getLocation().getWorld().getName())) {
            Location spawn = null;
            if (isBedEnabled) {
                spawn = event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation();
                if (spawn != null)
                    if (whitelist.contains(spawn.getWorld().getName()))
                        spawn = null;

            }
            TownyAPI api = OfflinePlayers.getTownyAPI();
            if (api != null && spawn != null) {
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
                PlayerProfile profile = multiInvAPI.getGroupManager().getGroup(spawn.getWorld().getName()).getGroupProfileContainer().getPlayerData(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
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
