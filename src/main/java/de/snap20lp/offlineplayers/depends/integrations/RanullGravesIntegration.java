package de.snap20lp.offlineplayers.depends.integrations;

import de.snap20lp.offlineplayers.events.OfflinePlayerDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * The RanullGravesIntegration exists to let clones spawn graves when they die using Ranull's Graves plugin.
 *
 * @author Sugaku
 */
public class RanullGravesIntegration implements Listener {

    /**
     * Attempts to create the RanullGravesIntegration module.
     */
    public RanullGravesIntegration () {
        if (Bukkit.getPluginManager().isPluginEnabled("Graves")) {

        } else {

        }
    }

    /**
     * Called whenever a clone dies.
     *
     * @param event The clone death event.
     */
    @EventHandler
    public void onDeath (OfflinePlayerDeathEvent event) {

    }
}
