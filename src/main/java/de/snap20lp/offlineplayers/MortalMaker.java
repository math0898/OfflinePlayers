package de.snap20lp.offlineplayers;

import de.snap20lp.offlineplayers.depends.APIManager;
import de.snap20lp.offlineplayers.depends.MultiverseInventoriesFacade;
import de.snap20lp.offlineplayers.depends.TownyFacade;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
            TownyFacade towny = APIManager.getInstance().getTownyFacade();
            if (towny != null && spawn == null) spawn = towny.grabTownSpawn(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
            if (spawn == null) spawn = Bukkit.getWorld(destinationWorld).getSpawnLocation();
            event.setLocation(spawn);
            MultiverseInventoriesFacade multiverseInventoriesFacade = APIManager.getInstance().getMultiverseInventoriesFacade();
            if (multiverseInventoriesFacade != null) multiverseInventoriesFacade.updateInventory(event);
        }
    }
}
