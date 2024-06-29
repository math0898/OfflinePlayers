package de.snap20lp.offlineplayers;

import com.github.retrooper.packetevents.protocol.player.UserProfile;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class OfflinePlayer implements Listener {

    private final org.bukkit.OfflinePlayer offlinePlayer;
    private final double currentHP;
    private final ArrayList<ItemStack> savedInventoryContents;
    private final ArrayList<ItemStack> addedItems = new ArrayList<>();
    private final ArrayList<ItemStack> savedArmorContents;
    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final int playerExp;
    private final String customName;
    private final int despawnTimerSeconds = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.cloneDe-spawnTimer.timer");
    private Location spawnLocation;
    private int despawnTask = 0, updateTask = 0;
    private int currentSeconds = 0;
    private boolean isHidden = false;

    /**
     * This is the id of the clone entity. It is used to update the cloneEntity reference.
     */
    private UUID cloneEntityId;

    private static final boolean cloneIsHittable = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneIsHittable");
    private static final boolean cloneHasAI = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneHasAI");

    private boolean isDead = false;
    private static final boolean despawnTimerEnabled = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneDe-spawnTimer.enabled");
    private static final boolean isBlockEntity = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.useBlockEntity");

    private static final int distance = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.cloneSpawnDistance");

    private static final boolean canProvoke = OfflinePlayers.getInstance().getConfig().getBoolean("OfflinePlayer.cloneCanProvoke");

    private static final int cloneUpdateTimer = OfflinePlayers.getInstance().getConfig().getInt("OfflinePlayer.cloneUpdateTimer");

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
        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneName");
        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
        this.customName = customName;
//        spawnClone();
//        despawnClone();
        isHidden = true;
//        startTimers();
    }

    public OfflinePlayer(org.bukkit.OfflinePlayer player, int currentSeconds, Location spawnLocation, int playerExp, double currentHP, ArrayList<ItemStack> savedInventoryContents, ArrayList<ItemStack> savedArmorContents, ItemStack mainHand, ItemStack offHand) {
        this.offlinePlayer = player;
        this.mainHand = mainHand;
        this.spawnLocation = spawnLocation;
        this.offHand = offHand;
        this.savedInventoryContents = savedInventoryContents;
        this.savedArmorContents = savedArmorContents;
        this.playerExp = playerExp;
        this.currentHP = currentHP;
        if (currentHP <= 0) isDead = true;
        this.currentSeconds = currentSeconds;
        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneName");
        customName = customName.replaceAll("%PLAYER_NAME", player.getName());
        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
        this.customName = customName;
        spawnClone();
        despawnClone();
        isHidden = true;
        startTimers();
    }

    public TargetedDisguise getDisguisedEntity() {
        return disguisedEntity;
    }

    public ItemStack getMainHand() {
        return mainHand;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

    public org.bukkit.OfflinePlayer getOfflinePlayer() {
        return this.offlinePlayer;
    }

    public int getCurrentSeconds() {
        return currentSeconds;
    }

    public LivingEntity getCloneEntity() {
        return cloneEntity;
    }

    public int getPlayerExp() {
        return playerExp;
    }

    public double getCurrentHP() {
        return currentHP;
    }

    public ArrayList<ItemStack> getSavedInventoryContents() {
        return savedInventoryContents;
    }

    public ArrayList<ItemStack> getSavedArmorContents() {
        return savedArmorContents;
    }

    public boolean isCloneHasAI() {
        return cloneHasAI;
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean val) {
        isDead = val;
    }

    public ArrayList<ItemStack> getAddedItems() {
        return addedItems;
    }

    public void setHidden(boolean val) {
        isHidden = val;
    }

    public boolean isCloneIsHittable() {
        return cloneIsHittable;
    }

    public void setSpawnLocation(Location val) {
        this.spawnLocation = val;
    }

    // todo: Remove used for benchmarking performance.
    static BigInteger sum = BigInteger.ZERO;
    static BigInteger runs = BigInteger.ONE;

    public void startTimers() {
        updateTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(OfflinePlayers.getInstance(), () -> {
//            long startTime = System.nanoTime(); // todo: Remove used for benchmarking performance.
            if (isDead) return;
            AtomicBoolean isNearby = new AtomicBoolean(false);
            int entityID = cloneEntity.getEntityId();
            int disguiseID = disguisedEntity.getEntity().getEntityId();
            cloneEntity.getNearbyEntities(distance, distance, distance).forEach(entity -> {
                if (entity.getEntityId() != entityID && entity.getEntityId() != disguiseID) {
                    if (entity.getType() == EntityType.PLAYER && !cloneEntityId.equals(offlinePlayer.getUniqueId()))
                        isNearby.set(true);
                    else if (entity instanceof Mob mob && canProvoke)
                        if (mob.getTarget() == null && mob.hasLineOfSight(cloneEntity)) mob.setTarget(cloneEntity);
                }
            });

            if (!isNearby.get()) {
//                OfflinePlayer offlinePlayerClone = new OfflinePlayer(offlinePlayer, currentSeconds, cloneEntity.getLocation(), playerExp, cloneEntity.getHealth(), savedInventoryContents, savedArmorContents,
//                        /*cloneEntity.getEquipment().getItemInMainHand()*/ new ItemStack(Material.AIR, 1), cloneEntity.getEquipment().getItemInOffHand());
//                offlinePlayerClone.replaceCloneStats(cloneEntity);

                despawnClone();
//                Map<UUID, OfflinePlayer> offlinePlayerList = CloneManager.getInstance().getOfflinePlayerList();
//                Map<Integer, OfflinePlayer> entityList = CloneManager.getInstance().getEntityOfflinePlayerHashMap();
//                offlinePlayerList.remove(offlinePlayer.getUniqueId());
//                entityList.remove(entityID);
//                cancelUpdateTask();
//                cancelDespawnTask();
//                spawnClone();
//                despawnClone();
                isHidden = true;
//                startTimers();
//                offlinePlayerList.put(offlinePlayer.getUniqueId(), offlinePlayerClone);
//                entityList.put(offlinePlayerClone.getDisguisedEntity().getEntity().getEntityId(), offlinePlayerClone);
            } else if (isNearby.get() && isHidden) {
                spawnClone();
                isHidden = false;
            }

//            BigInteger time = BigInteger.valueOf(System.nanoTime() - startTime); // todo: Remove used for benchmarking performance.
//            runs = runs.add(BigInteger.ONE); // todo: Remove used for benchmarking performance.
//            sum = sum.add(time); // todo: Remove used for benchmarking performance.
//            System.out.println("Finished: " + time.divide(BigInteger.valueOf(10000)) + " centimilliseconds"); // todo: Remove used for benchmarking performance.
//            System.out.println("Average: " + (sum.divide(runs.multiply(BigInteger.valueOf(10000)))) + " centimilliseconds"); // todo: Remove used for benchmarking performance.
        }, 10, cloneUpdateTimer);

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
                        String customName = OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneName");
                        customName = customName.replaceAll("%PLAYER_NAME", offlinePlayer.getName());
                        customName = customName.replaceAll("%DESPAWN_TIMER%", String.valueOf(despawnTimerSeconds - currentSeconds));
                        cloneEntity.setCustomName(customName);
                        disguisedEntity.getWatcher().setCustomName(customName);
                    }
                }
            }, 20, 20);
        }
    }

    public void cancelDespawnTask() {
        Bukkit.getScheduler().cancelTask(despawnTask);
    }

    public void cancelUpdateTask() {
        Bukkit.getScheduler().cancelTask(updateTask);
    }

    public void replaceCloneStats(LivingEntity entity) {
        cloneEntity.teleport(entity.getLocation());
        cloneEntity.setHealth(entity.getHealth());
        cloneEntity.setFireTicks(entity.getFireTicks());
        cloneEntity.setFallDistance(entity.getFallDistance());
        cloneEntity.setVelocity(entity.getVelocity());
        cloneEntity.setArrowsInBody(entity.getArrowsInBody());
        cloneEntity.setFreezeTicks(entity.getFreezeTicks());
        cloneEntity.setVisualFire(entity.isVisualFire());
    }

    public void spawnClone() {
        Entity clone;
        EntityType entityType;
        if (cloneEntity != null) if (cloneEntity.isValid()) return;
        try {
            clone = spawnLocation.getWorld().spawnEntity(cloneEntity != null ? cloneEntity.getLocation() : spawnLocation, EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneRawEntity")));
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§4[OfflinePlayers] Could not spawn clone entity! Please check your config.yml! | Exception: " + e.getMessage());
            return;
        }

        if (clone instanceof LivingEntity) {
            ((LivingEntity) clone).setCanPickupItems(false);
            if (clone instanceof Ageable) ((Ageable) clone).setAdult();
            clone.setSilent(true);

            ((LivingEntity) clone).setAI(cloneHasAI);
            if (cloneHasAI) ((LivingEntity) clone).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false, false));
            clone.setGravity(true);
            ((LivingEntity) clone).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(cloneEntity == null ? currentHP : cloneEntity.getHealth());
            ((LivingEntity) clone).setHealth(cloneEntity == null ? currentHP : cloneEntity.getHealth());
            ((LivingEntity) clone).getEquipment().setArmorContents(cloneEntity == null ? savedArmorContents.toArray(new ItemStack[0]) : cloneEntity.getEquipment().getArmorContents());
            ((LivingEntity) clone).getEquipment().setItemInMainHand(cloneEntity == null ? mainHand : cloneEntity.getEquipment().getItemInMainHand());
            ((LivingEntity) clone).getEquipment().setItemInOffHand(cloneEntity == null ? offHand : cloneEntity.getEquipment().getItemInOffHand());
            org.bukkit.entity.Player player = offlinePlayer.getPlayer();
            if (player != null)
                player.getActivePotionEffects().forEach(potionEffect -> ((LivingEntity) clone).addPotionEffect(potionEffect));
            clone.setInvulnerable(!cloneIsHittable);
            clone.setCustomName(customName);
            if (cloneEntity != null) cloneEntity.remove();
            if (cloneEntityId != null) {
                Entity e = Bukkit.getEntity(cloneEntityId);
                if (e != null) e.remove();
            }
            if (disguisedEntity != null) disguisedEntity.removeDisguise();
            this.cloneEntity = (LivingEntity) clone;
            cloneEntityId = cloneEntity.getUniqueId();

            TargetedDisguise targetedDisguise;

            if (isBlockEntity) {
                targetedDisguise = new MiscDisguise(DisguiseType.getType(EntityType.FALLING_BLOCK), Material.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity")), 0);
            } else {
                entityType = EntityType.valueOf(OfflinePlayers.getInstance().getConfig().getString("OfflinePlayer.cloneEntity"));
                if (entityType == EntityType.PLAYER) {
                    if (player == null)
                        targetedDisguise = new PlayerDisguise(new UserProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName()), new UserProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName()));
                    else
                        targetedDisguise = new me.libraryaddict.disguise.disguisetypes.PlayerDisguise(player.getName());
                } else {
                    targetedDisguise = new MobDisguise(DisguiseType.getType(entityType));
                }
            }

            targetedDisguise.setCustomDisguiseName(true);
            targetedDisguise.setExpires(Long.MAX_VALUE);
            targetedDisguise.getWatcher().setCustomName(customName);
            targetedDisguise.getWatcher().setYawLocked(true);
            targetedDisguise.getWatcher().setPitchLocked(true);
            targetedDisguise.getWatcher().setYawLock(spawnLocation.getYaw());
            targetedDisguise.getWatcher().setPitchLock(spawnLocation.getPitch());
            me.libraryaddict.disguise.DisguiseAPI.disguiseToAll(clone, targetedDisguise);
            disguisedEntity = targetedDisguise;
        }
    }

    public void despawnClone() {
        if (cloneEntity != null) {
            DisguiseAPI.undisguiseToAll(cloneEntity);
            disguisedEntity.removeDisguise();
            cloneEntity.remove();
            if (cloneEntityId != null) {
                Entity e = Bukkit.getEntity(cloneEntityId);
                if (e != null) e.remove();
            }
        }
    }

}
