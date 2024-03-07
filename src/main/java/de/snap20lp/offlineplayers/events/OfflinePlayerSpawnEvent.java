package de.snap20lp.offlineplayers.events;

import de.snap20lp.offlineplayers.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class OfflinePlayerSpawnEvent extends Event { // todo: Does this really need the OfflinePlayer or would a reference to the active {@link org.bukkit.entity.Player} work?
    private static final HandlerList HANDLERS = new HandlerList();
    private final OfflinePlayer offlinePlayer;

    /**
     * The resulting location after this OfflinePlayerSpawnEvent.
     */
    private Location location;

    /**
     * The inventory the player will have.
     */
    private ItemStack[] inventory;

    /**
     * The armor that the player will have.
     */
    private ItemStack[] armor;

    /**
     * The item in the offHand that the player will have.
     */
    private ItemStack offHand;

    /**
     * Creates a new OfflinePlayerSpawnEvent with the given parameters.
     *
     * @param offlinePlayer The temporary OfflinePlayer object created.
     * @param location      The pending location for this clone.
     * @param inventory     The pending inventory for the clone.
     * @param armor         Any pending armor that the clone may have.
     * @param offHand       The item in the player's offHand.
     */
    public OfflinePlayerSpawnEvent (OfflinePlayer offlinePlayer, Location location, ItemStack[] inventory, ItemStack[] armor, ItemStack offHand) {
        this.offlinePlayer = offlinePlayer;
        this.location = location;
        this.inventory = inventory;
        this.armor = armor;
        this.offHand = offHand;

    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    /**
     * Accessor method for the location that the clone will be spawned at after this event.
     *
     * @return The location the clone will be spawned at after the event executes.
     */
    public Location getLocation () {
        return location;
    }


    /**
     * Sets a new location to spawn the clone at.
     *
     * @param location The location to spawn the clone at.
     */
    public void setLocation (Location location) {
        this.location = location;
    }

    /**
     * Sets the inventory that the player will have.
     *
     * @param val The new inventory value.
     */
    public void setInventory (ItemStack[] val) {
        inventory = val;
    }

    /**
     * Accessor method for the set inventory value.
     *
     * @return The inventory value.
     */
    public ItemStack[] getInventory () {
        return inventory;
    }

    /**
     * Sets the armor that the player will have.
     *
     * @param val The new armor value.
     */
    public void setArmor (ItemStack[] val) {
        armor = val;
    }

    /**
     * Accessor method for the player will have.
     *
     * @return The armor the player will have.
     */
    public ItemStack[] getArmor () {
        return armor;
    }

    /**
     * Sets the offhand item of the player.
     *
     * @param val The value of the player's offhand.
     */
    public void setOffHand (ItemStack val) {
        offHand = val;
    }

    /**
     * Accessor method for what the player will have in their offhand.
     *
     * @return The player's offhand contents.
     */
    public ItemStack getOffHand () {
        return offHand;
    }
}