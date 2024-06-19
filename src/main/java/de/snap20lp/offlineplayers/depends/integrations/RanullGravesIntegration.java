package de.snap20lp.offlineplayers.depends.integrations;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.GraveManager;
import com.ranull.graves.type.Grave;
import de.snap20lp.offlineplayers.OfflinePlayers;
import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * The RanullGravesIntegration exists to let clones spawn graves when they die using Ranull's Graves plugin.
 *
 * @author Sugaku
 */
public class RanullGravesIntegration implements Listener {

    /**
     * The GraveManager instance we need to create new graves.
     */
    private final GraveManager graveManager;

    /**
     * Attempts to create the RanullGravesIntegration module.
     */
    public RanullGravesIntegration () {
        if (Bukkit.getPluginManager().isPluginEnabled("Graves")) {
            graveManager = ((Graves) Bukkit.getPluginManager().getPlugin("Graves")).getGraveManager();
            OfflinePlayers.getInstance().getLogger().log(Level.INFO, "We have located Ranull's Graves. OfflinePlayers will spawn graves on death.");
        } else {
            graveManager = null;
            throw new NoClassDefFoundError();
        }
    }

    /**
     * Called whenever a clone dies.
     *
     * @param event The clone death event.
     */
    @EventHandler
    public void onDeath (OfflinePlayerDeathEvent event) {
        if (graveManager == null) return;
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack i : event.getOfflinePlayer().getAddedItems())
            drops.add(i);
        for (ItemStack i : event.getOfflinePlayer().getSavedInventoryContents())
            drops.add(i);
        Grave grave = graveManager.createGrave(event.getOfflinePlayer().getCloneEntity(), drops);
        grave.setOwnerUUID(event.getOfflinePlayer().getOfflinePlayer().getUniqueId());
        grave.setOwnerName(event.getOfflinePlayer().getOfflinePlayer().getName());
    }
}
