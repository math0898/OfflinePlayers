package de.snap20lp.offlineplayers;

import lombok.Getter;
import lombok.Setter;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.TargetedDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

@Getter
public class OfflinePlayer implements Listener {

    private final org.bukkit.OfflinePlayer offlinePlayer;
    private final double currentHP;
    private final ArrayList<ItemStack> savedInventoryContents;
    private final ArrayList<ItemStack> savedArmorContents;
    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final int playerExp;
    private final String customName;
    private Location spawnLocation;
    private int despawnTask = 0;
    private int currentSeconds = 0;
    private final int despawnTimerSeconds = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.de-spawnTimer.timer");
    @Setter
    private boolean isHittable = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.isHittable"), hasAI = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasAI"), isDead = false, hasGravity = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasGravity"), despawnTimerEnabled = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.de-spawnTimer.enabled");
    private LivingEntity cloneEntity;
    private TargetedDisguise disguisedEntity;

    public OfflinePlayer(Player player, ArrayList<ItemStack> savedInventoryContents, ArrayList<ItemStack> savedArmorContents, ItemStack mainHand, ItemStack offHand) {
        this.offlinePlayer = player;
        this.spawnLocation = player.getLocation();
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.savedInventoryContents = savedInventoryContents;
        this.savedArmorContents = savedArmorContents;
        this.playerExp = player.getTotalExperience();
        this.currentHP = player.getHealth();
        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
        this.customName = customName;
        spawnClone();
        startDespawnTimer();
    }

    public OfflinePlayer(org.bukkit.OfflinePlayer player,int currentSeconds,Location spawnLocation, int playerExp, double currentHP, ArrayList<ItemStack> savedInventoryContents, ArrayList<ItemStack> savedArmorContents, ItemStack mainHand, ItemStack offHand) {
        this.offlinePlayer = player;
        this.mainHand = mainHand;
        this.spawnLocation = spawnLocation;
        this.offHand = offHand;
        this.savedInventoryContents = savedInventoryContents;
        this.savedArmorContents = savedArmorContents;
        this.playerExp = playerExp;
        this.currentHP = currentHP;
        this.currentSeconds = currentSeconds;
        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
        this.customName = customName;
        spawnClone();
        startDespawnTimer();
    }

    private void startDespawnTimer() {
        if (despawnTimerEnabled) {
            despawnTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(OfflinePlayers.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (currentSeconds >= despawnTimerSeconds - 1) {
                        despawnClone();
                        cancelDespawnTask();
                        return;
                    }
                    currentSeconds++;
                    if (cloneEntity.isValid()) {
                        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
                        customName = customName.replaceAll("%PLAYER_NAME", offlinePlayer.getName());
                        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
                        cloneEntity.setCustomName(customName);
                        disguisedEntity.getWatcher().setCustomName(customName);
                    }
                }
            }, 20, 20);
        }
    }

    private void cancelDespawnTask() {
        Bukkit.getScheduler().cancelTask(despawnTask);
    }


    public void spawnClone() {
        Entity clone;
        EntityType entityType = EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity"));
        try {
            clone = offlinePlayer.getPlayer().getWorld().spawnEntity(spawnLocation, EntityType.FOX);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Could not spawn clone entity! Please check your config.yml! | Exception: " + e.getMessage());
            return;
        }

        if (clone instanceof LivingEntity) {
            ((LivingEntity) clone).setCanPickupItems(false);
            if (clone instanceof Ageable)
                ((Ageable) clone).setAdult();
            clone.setSilent(true);

            if (hasAI) {
                ((LivingEntity) clone).setAI(true);
                ((LivingEntity) clone).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false, false));
            }
            clone.setGravity(true);
            ((LivingEntity) clone).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentHP);
            ((LivingEntity) clone).setHealth(currentHP);
            ((LivingEntity) clone).getEquipment().setArmorContents(savedArmorContents.toArray(new ItemStack[0]));
            ((LivingEntity) clone).getEquipment().setItemInMainHand(mainHand);
            ((LivingEntity) clone).getEquipment().setItemInOffHand(offHand);
            offlinePlayer.getPlayer().getActivePotionEffects().forEach(potionEffect -> ((LivingEntity) clone).addPotionEffect(potionEffect));
            clone.setInvulnerable(!isHittable);
            clone.setCustomName(customName);
            this.cloneEntity = (LivingEntity) clone;

            TargetedDisguise targetedDisguise;

            if (entityType == EntityType.PLAYER) {
                targetedDisguise = new me.libraryaddict.disguise.disguisetypes.PlayerDisguise(offlinePlayer.getPlayer().getName());
            } else {
                targetedDisguise = new MobDisguise(DisguiseType.getType(entityType));
            }
            targetedDisguise.setCustomDisguiseName(true);
            targetedDisguise.getWatcher().setCustomName(customName);
            targetedDisguise.getWatcher().setYawLocked(true);
            targetedDisguise.getWatcher().setPitchLocked(true);
            targetedDisguise.getWatcher().setYawLock(offlinePlayer.getPlayer().getLocation().getYaw());
            targetedDisguise.getWatcher().setPitchLock(offlinePlayer.getPlayer().getLocation().getPitch());
            me.libraryaddict.disguise.DisguiseAPI.disguiseEntity(clone, targetedDisguise);
            disguisedEntity = targetedDisguise;
        }
    }

    public void despawnClone() {
        if (cloneEntity != null) {
            cloneEntity.remove();
        }
    }

}
