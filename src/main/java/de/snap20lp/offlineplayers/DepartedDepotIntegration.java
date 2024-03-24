package de.snap20lp.offlineplayers;

import com.gitlab.redstonerevive.departeddepot.DepartedDepot;
import com.gitlab.redstonerevive.departeddepot.Depot;
import com.gitlab.redstonerevive.departeddepot.DepotManager;
import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Level;

/**
 * The DepartedDepotIntegration exists to let clones spawn depots when they die.
 *
 * @author Sugaku
 */
public class DepartedDepotIntegration implements Listener {

    /**
     * The reference to the DepartedDepot plugin.
     */
    private final DepartedDepot departedDepot;

    /**
     * Attempts to create the DepartedDepotIntegration module.
     */
    public DepartedDepotIntegration () {
        if (Bukkit.getPluginManager().isPluginEnabled("DepartedDepot")) {
            departedDepot = (DepartedDepot) Bukkit.getPluginManager().getPlugin("DepartedDepot");
        } else {
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "We could not locate DepartedDepot. Clones will not spawn graves. Everything else should be fine.");
            departedDepot = null;
        }
    }

    /**
     * Called whenever a clone dies.
     *
     * @param event The clone death event.
     */
    @EventHandler
    public void onDeath (OfflinePlayerDeathEvent event) {
        if (departedDepot == null) return;
        OfflinePlayer player = event.getOfflinePlayer();
        List<ItemStack> items = player.getSavedInventoryContents();
        DepotManager.getInstance().addDepot(new Depot(player.getCloneEntity().getLocation(),
                player.getOfflinePlayer().getUniqueId(),
                player.getPlayerExp(),
                items.toArray(new ItemStack[0])));
        player.getSavedInventoryContents().clear();
    }
}
