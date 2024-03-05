package de.snap20lp.offlineplayers;

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
    List<String> whitelist;

    /**
     * Creates a new MortalMaker and loads config file values.
     */
    public MortalMaker () {
        FileConfiguration config = OfflinePlayers.getInstance().getConfig();
        whitelist = config.getStringList("OfflinePlayer.mortal-maker.world-whitelist");
    }

    /**
     * Called whenever a clone is spawned because a player is logging off.
     *
     * @param event The OfflinePlayerSpawnEvent.
     */
    @EventHandler
    public void onCloneSpawn (OfflinePlayerSpawnEvent event) {
        if (whitelist.contains(event.getLocation().getWorld().getName())) {
            Location bedSpawn = event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation();
            if (bedSpawn != null ) {
                event.setLocation(event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation());
                return;
            }
            // todo: Towny Spawn.
            Location spawn = Bukkit.getWorld("world").getSpawnLocation();
            event.setLocation(spawn);
        }
    }
}
