package de.snap20lp.offlineplayers.events;

import de.snap20lp.offlineplayers.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OfflinePlayerSpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final OfflinePlayer offlinePlayer;

    public OfflinePlayerSpawnEvent(OfflinePlayer offlinePlayer) {
        this.offlinePlayer = offlinePlayer;
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

}