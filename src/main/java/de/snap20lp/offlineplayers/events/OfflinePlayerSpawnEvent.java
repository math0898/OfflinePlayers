package de.snap20lp.offlineplayers.events;

import de.snap20lp.offlineplayers.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OfflinePlayerSpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final OfflinePlayer offlinePlayer;

    private Location location;

    public OfflinePlayerSpawnEvent (OfflinePlayer offlinePlayer, Location location) {
        this.offlinePlayer = offlinePlayer;
        this.location = location;
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
}