package de.snap20lp.offlineplayers.depends;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * The TownyFacade is used to separate this plugin from specific Towny implementations. Without Towny present, it is
 * unsafe to instantiate this class.
 *
 * @author Sugaku
 */
public class TownyFacade {

    /**
     * The active Towny api instance.
     */
    private static TownyAPI townyAPI;

    /**
     * Creates a new TownyFacade by grabbing the TownyAPI.
     */
    public TownyFacade () {
        townyAPI = TownyAPI.getInstance();
    }

    /**
     * Grabs the town spawn of the given player from Towny.
     *
     * @param player The player to grab the town spawn of.
     */
    public Location grabTownSpawn (Player player) {
        Town town = townyAPI.getTown(player);
        if (town != null) {
            try {
                return town.getSpawn();
            } catch (TownyException ignored) {
                return null;
            }
        }
        else return null;
    }
}
