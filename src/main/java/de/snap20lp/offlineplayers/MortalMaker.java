package de.snap20lp.offlineplayers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
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
    public void onCloneSpawn (OfflinePlayerSpawnEvent event) {
        if (whitelist.contains(event.getLocation().getWorld().getName())) {
            if (isBedEnabled) {
                Location bedSpawn = event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation();
                if (bedSpawn != null)
                    if (!whitelist.contains(bedSpawn.getWorld().getName())) {
                        event.setLocation(event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation());
                        return;
                    }
            }
            TownyAPI api = OfflinePlayers.getTownyAPI();
            if (api != null) {
                Town town = api.getTown(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
                if (town != null) {
                    try {
                        Location townSpawn = town.getSpawn();
                        event.setLocation(townSpawn);
                        return;
                    } catch (TownyException ignored) { }
                }
            }
            Location spawn = Bukkit.getWorld(destinationWorld).getSpawnLocation();
            event.setLocation(spawn);
        }
    }
}
