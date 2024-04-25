package de.snap20lp.offlineplayers.depends;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A Facade sits right between the dependency and the OfflinePlayers plugin. Instantiating one when a dependency is not
 * found will cause a bug.
 *
 * @author Sugaku
 */
public class WorldGuardFacade {

    /**
     * This is the instance of the custom flag that we need in order to query its value.
     */
    private final StateFlag BAN_OFFLINE_PLAYERS;

    /**
     * Attempts to create the WorldGuardFacade. THIS MAY THROW NoClassDefFound!
     */
    public WorldGuardFacade () {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        StateFlag flag = new StateFlag("ban-offline-players", false);
        registry.register(flag);
        BAN_OFFLINE_PLAYERS = flag;
    }

    /**
     * Tests the given location with the BAN_OFFLINE_PLAYERS flag.
     *
     * @param location The location to test.
     */
    public boolean testFlag (Location location) {
        World w = location.getWorld();
        if (w == null) return false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        return set.testState(null, BAN_OFFLINE_PLAYERS);
    }
}
