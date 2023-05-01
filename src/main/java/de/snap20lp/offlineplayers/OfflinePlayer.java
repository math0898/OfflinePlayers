package de.snap20lp.offlineplayers;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.ArmorStandTrait;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
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
    private final int despawnTimerSeconds = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.de-spawnTimer.timer");
    private int currentSeconds = 0;
    private int despawnTask = 0;
    @Setter
    private boolean isHittable = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.isHittable"), hasAI = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasAI"), nameAlwaysVisible = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.nameAlwaysVisible"), isDead = false, hasGravity = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.hasGravity"), despawnTimerEnabled = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.de-spawnTimer.enabled");
    private LivingEntity cloneEntity;
    private NPC cloneNPCEntity;
    private boolean isUsingNPC;

    public OfflinePlayer(Player player, ArrayList<ItemStack> savedInventoryContents, ArrayList<ItemStack> savedArmorContents, ItemStack mainHand, ItemStack offHand) {
        this.offlinePlayer = player;
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
                    if (cloneEntity.isValid() || (isUsingNPC && cloneNPCEntity.getEntity() != null && cloneNPCEntity.getEntity().isValid())) {
                        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.name");
                        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
                        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
                        if(isUsingNPC) {
                            cloneNPCEntity.getEntity().setCustomName(customName);
                            cloneNPCEntity.setName(customName);
                        } else {
                            cloneEntity.setCustomName(customName);
                        }
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
        if(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity").equals("PLAYER")) {
            if(!OfflinePlayers.getInstance().isCitizensEnabled()) {
                Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Citizens is not enabled! You can't use PLAYER as clone entity!");
                return;
            }
            isUsingNPC = true;
            NPC npc = OfflinePlayers.getInstance().getInMemoryNPCRegistry().createNPC(EntityType.PLAYER,customName);
            npc.getOrAddTrait(SkinTrait.class).setSkinName(offlinePlayer.getName());
            npc.setProtected(false);
            npc.spawn(offlinePlayer.getPlayer().getLocation());
            cloneNPCEntity = npc;
            clone = npc.getEntity();
        } else {
            try {
                clone = offlinePlayer.getPlayer().getWorld().spawnEntity(offlinePlayer.getPlayer().getLocation(), EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity")));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Could not spawn clone entity! Please check your config.yml! | Exception: " + e.getMessage());
                return;
            }
        }
        if (clone instanceof LivingEntity || clone instanceof NPC) {
            assert clone instanceof LivingEntity;
            ((LivingEntity) clone).setCanPickupItems(false);
            if (clone instanceof Ageable)
                ((Ageable) clone).setAdult();
            clone.setSilent(true);
            ((LivingEntity) clone).setAI(false);
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

            clone.setCustomNameVisible(nameAlwaysVisible);
            clone.setCustomName(customName);
            this.cloneEntity = (LivingEntity) clone;
        }
    }

    public void despawnClone() {
        if (cloneEntity != null) {
            if(isUsingNPC)
                cloneNPCEntity.despawn();
            else
                cloneEntity.remove();
        }
    }

}
