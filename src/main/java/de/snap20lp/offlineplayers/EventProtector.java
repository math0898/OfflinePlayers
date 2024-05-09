package de.snap20lp.offlineplayers;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.profile.PlayerProfile;
import com.onarandombox.multiverseinventories.share.Sharables;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import de.snap20lp.offlineplayers.depends.APIManager;
import de.snap20lp.offlineplayers.depends.MultiverseInventoriesFacade;
import de.snap20lp.offlineplayers.depends.TownyFacade;
import de.snap20lp.offlineplayers.depends.WorldGuardFacade;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    public void onCloneSpawn(OfflinePlayerSpawnEvent event) {
        WorldGuardFacade wg = APIManager.getInstance().getWorldGuard();
        if (wg == null) return;
        if (wg.testFlag(event.getLocation())) {
            Location spawn = null;
            if (isBedEnabled) spawn = event.getOfflinePlayer().getOfflinePlayer().getBedSpawnLocation();
            TownyFacade towny = APIManager.getInstance().getTownyFacade();
            if (towny != null && spawn == null) spawn = towny.grabTownSpawn(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
            if (spawn == null) spawn = Bukkit.getWorld(destinationWorld).getSpawnLocation();
            event.setLocation(spawn);
            MultiverseInventoriesFacade multiverseInventoriesFacade = APIManager.getInstance().getMultiverseInventoriesFacade();
            if (multiverseInventoriesFacade != null) multiverseInventoriesFacade.updateInventory(event);
        }
    }
}
