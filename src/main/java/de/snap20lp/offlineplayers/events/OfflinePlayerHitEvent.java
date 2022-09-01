package de.snap20lp.offlineplayers.events;

import de.snap20lp.offlineplayers.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OfflinePlayerHitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final OfflinePlayer offlinePlayer;
    private final Player damager;

    public OfflinePlayerHitEvent(OfflinePlayer offlinePlayer, Player damager) {
        this.offlinePlayer = offlinePlayer;
        this.damager = damager;
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

    public Player getDamager() {
        return damager;
    }
}
