package de.snap20lp.offlineplayers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@Getter
public class OfflinePlayer implements Listener {

    private final org.bukkit.OfflinePlayer offlinePlayer;
    private final double currentHP;
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final int playerExp;
    private final String customName;

    private int currentSeconds = 0;
    private int despawnTask = 0;
    private final int despawnTimerSeconds = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.de-spawnTimer.timer");

    @Setter
    private boolean isHittable = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.isHittable"), hasAI = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasAI"), nameAlwaysVisible = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.nameAlwaysVisible"), isDead = false, hasGravity = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasGravity"),despawnTimerEnabled = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.de-spawnTimer.enabled");
    private LivingEntity cloneEntity;

    public OfflinePlayer(Player player, ItemStack[] inventoryContents, ItemStack[] armorContents, ItemStack mainHand, ItemStack offHand) {
        this.offlinePlayer = player;
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
        this.playerExp = player.getTotalExperience();
        this.currentHP = player.getHealth();
        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds-currentSeconds));
        this.customName = customName;

        spawnClone();

        if(despawnTimerEnabled) {
            despawnTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(OfflinePlayers.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if(currentSeconds >= despawnTimerSeconds-1) {
                        despawnClone();
                        cancelTask();
                        return;
                    }
                    currentSeconds++;
                    if(cloneEntity.isValid()) {
                        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
                        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
                        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds-currentSeconds));
                        cloneEntity.setCustomName(customName);
                    }
                }
            },20,20);
        }
    }

    private void cancelTask() {
        Bukkit.getScheduler().cancelTask(despawnTask);
    }


    public void spawnClone() {



        Zombie clone = (Zombie) offlinePlayer.getPlayer().getWorld().spawnEntity(offlinePlayer.getPlayer().getLocation(), EntityType.ZOMBIE);
        clone.setCanPickupItems(false);
        clone.setAdult();
        clone.setSilent(true);
        clone.setAI(hasAI);
        clone.setGravity(hasGravity);
        clone.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentHP);
        clone.setHealth(currentHP);
        clone.getEquipment().setArmorContents(armorContents);
        clone.getEquipment().setItemInMainHand(mainHand);
        clone.getEquipment().setItemInOffHand(offHand);
        clone.setCustomNameVisible(nameAlwaysVisible);
        clone.setCustomName(customName);
        this.cloneEntity = clone;
    }

    public void despawnClone() {
        cloneEntity.remove();
    }

}
