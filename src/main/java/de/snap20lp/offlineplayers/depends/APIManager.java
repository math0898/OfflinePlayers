package de.snap20lp.offlineplayers.depends;

import de.snap20lp.offlineplayers.OfflinePlayers;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * The APIManager attempts to isolate all API loading from the main OfflinePlayer's plugin class. These are then further
 * isolated into individual facades.
 *
 * @author Sugaku
 */
public class APIManager {

    /**
     * The active APIManager instance.
     */
    private static APIManager instance;

    /**
     * The WorldGuardFacade that is contained in the APIManager.
     */
    private WorldGuardFacade worldGuard;

    /**
     * An accessor method to the active APIManager instance.
     *
     * @return The active APIManager instance.
     */
    public static APIManager getInstance () {
        if (instance == null) instance = new APIManager();
        return instance;
    }

    /**
     * Called when onLoad() is issued executed for OfflinePlayers.
     */
    public void delegatedOnLoad () {
        try {
            worldGuard = new WorldGuardFacade();
        } catch (NoClassDefFoundError error) {
            worldGuard = null;
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "WorldGuard not found. Plugin will run normally.");
        }
    }

    /**
     * An accessor method for the WorldGuardFacade.
     *
     * @return The WorldGuardFacade that was created by the APIManager.
     */
    @Nullable
    public WorldGuardFacade getWorldGuard () {
        return worldGuard;
    }
}
