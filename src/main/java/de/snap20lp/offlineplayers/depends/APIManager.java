package de.snap20lp.offlineplayers.depends;

import de.snap20lp.offlineplayers.depends.integrations.DepartedDepotIntegration;
import de.snap20lp.offlineplayers.OfflinePlayers;
import de.snap20lp.offlineplayers.depends.integrations.RanullGravesIntegration;
import org.bukkit.Bukkit;

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
     * The MultiverseInventoriesFacade that is contained in the APIManager.
     */
    private MultiverseInventoriesFacade multiverseInventoriesFacade;

    /**
     * The TownyFacade that is contained in the APIManager.
     */
    private TownyFacade townyFacade;

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
     * Called when onLoad() is executed for OfflinePlayers.
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
     * Called when onEnable() is executed for OfflinePlayers.
     */
    public void delegatedOnEnabled () {
        System.out.println("Warn me!");
        try {
            townyFacade = new TownyFacade();
        } catch (NoClassDefFoundError error) {
            townyFacade = null;
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Towny not found. Plugin will run normally.");
        }
        try {
            multiverseInventoriesFacade = new MultiverseInventoriesFacade();
        } catch (NoClassDefFoundError error) {
            multiverseInventoriesFacade = null;
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Multiverse Inventories not found. Plugin will run normally.");
        }
        try {
            Bukkit.getPluginManager().registerEvents(new DepartedDepotIntegration(), OfflinePlayers.getInstance());
        } catch (NoClassDefFoundError error1) {
            OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Departed Depots not found. Attempting a graves fallback: Graves by Ranull.");
            try {
                Bukkit.getPluginManager().registerEvents(new RanullGravesIntegration(), OfflinePlayers.getInstance());
            } catch (NoClassDefFoundError error2) {
                OfflinePlayers.getInstance().getLogger().log(Level.WARNING, "Graves by Ranull not found. Plugin will run normally without graves.");
            }
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

    /**
     * An accessor method for the MultiverseInventoriesFacade.
     *
     * @return The MultiverseInventoriesFacade that was created by the APIManager.
     */
    @Nullable
    public MultiverseInventoriesFacade getMultiverseInventoriesFacade () {
        return multiverseInventoriesFacade;
    }

    /**
     * An accessor method for the MultiverseInventoriesFacade.
     *
     * @return The MultiverseInventoriesFacade that was created by the APIManager.
     */
    @Nullable
    public TownyFacade getTownyFacade () {
        return townyFacade;
    }
}
