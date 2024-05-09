package de.snap20lp.offlineplayers.depends;

import com.onarandombox.multiverseinventories.MultiverseInventories;
import com.onarandombox.multiverseinventories.profile.PlayerProfile;
import com.onarandombox.multiverseinventories.share.Sharables;
import de.snap20lp.offlineplayers.OfflinePlayers;
import de.snap20lp.offlineplayers.events.OfflinePlayerSpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

/**
 * This serves as a compatibility layer between OfflinePlayers and MultiverseInventories. Creating an instance of this
 * class without MultiverseInventories will cause errors.
 *
 * @author Sugaku
 */
public class MultiverseInventoriesFacade {

    /**
     * The active Multiverse-Inventories instance.
     */
    private static MultiverseInventories multiverseInventoriesAPI;

    /**
     * Creates a new MultiverseInventoriesFacade.
     */
    public MultiverseInventoriesFacade () {
        multiverseInventoriesAPI = (MultiverseInventories) Bukkit.getPluginManager().getPlugin("Multiverse-Inventories");
    }

    /**
     * Updates MortalMaker related events using the correct inventories from Multiverse Inventories.
     *
     * @param event The OfflinePlayerSpawnEvent to update with the correct inventories.
     */
    public void updateInventory (OfflinePlayerSpawnEvent event) {
        PlayerProfile profile = multiverseInventoriesAPI.getGroupManager().getGroupsForWorld(event.getLocation().getWorld().getName()).get(0).getGroupProfileContainer().getPlayerData(event.getOfflinePlayer().getOfflinePlayer().getPlayer());
        ItemStack[] inv = profile.get(Sharables.INVENTORY);
        ItemStack[] armor = profile.get(Sharables.ARMOR);
        ItemStack offHand = profile.get(Sharables.OFF_HAND);
        event.setInventory(inv);
        event.setArmor(armor);
        event.setOffHand(offHand);
    }
}
