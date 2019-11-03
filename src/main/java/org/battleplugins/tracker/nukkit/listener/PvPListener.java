package org.battleplugins.tracker.nukkit.listener;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.passive.EntityTameable;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.item.Item;

import mc.alk.mc.MCPlayer;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.tracking.TrackerInterface;
import org.battleplugins.tracker.tracking.recap.DamageInfo;
import org.battleplugins.tracker.tracking.recap.Recap;
import org.battleplugins.tracker.tracking.recap.RecapManager;
import org.battleplugins.tracker.util.TrackerUtil;

/**
 * Main listener for PvP tracking in Nukkit.
 *
 * @author Redned
 */
public class PvPListener implements Listener {

    private BattleTracker tracker;

    public PvPListener(BattleTracker tracker) {
        this.tracker = tracker;
    }

    /**
     * Event called when one player is killed by another
     *
     * @param event the event being called
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player killed = event.getEntity();

        EntityDamageEvent lastDamageCause = killed.getLastDamageCause();
        Player killer = null;
        Item weapon = null;
        if (lastDamageCause instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
            if (damager instanceof Player) {
                killer = (Player) damager;
                weapon = killer.getInventory().getItemInHand();
            }

            if (damager instanceof EntityProjectile) {
                EntityProjectile proj = (EntityProjectile) damager;
                if (proj.shootingEntity instanceof Player) {
                    killer = (Player) proj.shootingEntity;
                    weapon = killer.getInventory().getItemInHand();
                }
            }

            if (damager instanceof EntityTameable && ((EntityTameable) damager).isTamed()) {
                killer = ((EntityTameable) damager).getOwner();
                // Use a bone to show the case was a wolf
                Item bone = new Item(Item.BONE);
                bone.setCustomName(damager.getName() == null ? "Wolf" : damager.getName());
                weapon = bone;
            }
        }

        if (killer == null)
            return;

        // Check the killers world just incase for some reason the
        // killed player was teleported to another world
        if (tracker.getConfigManager().getPvPConfig().getStringList("ignoredWorlds").contains(killer.getLevel().getName()))
            return;

        TrackerUtil.updatePvPStats(tracker.getPlatform().getOfflinePlayer(killed.getUniqueId().toString()),
                tracker.getPlatform().getOfflinePlayer(killer.getUniqueId().toString()));

        TrackerInterface pvpTracker = tracker.getTrackerManager().getPvPInterface();
        if (pvpTracker.getDeathMessageManager().shouldOverrideDefaultMessages())
            event.setDeathMessage("");

        pvpTracker.getDeathMessageManager().sendItemMessage(killer.getName(), killed.getName(), weapon.getName().toLowerCase());
        pvpTracker.getRecapManager().getDeathRecaps().get(killed.getName()).setVisible(true);
    }

    /**
     * Event called when a player takes damage from another player
     *
     * @param event the event being called
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(getTrueDamager(event) instanceof Player))
            return;

        MCPlayer player = tracker.getPlatform().getPlayer(event.getEntity().getName());
        TrackerInterface pvpTracker = tracker.getTrackerManager().getPvPInterface();

        RecapManager recapManager = pvpTracker.getRecapManager();
        Recap recap = recapManager.getDeathRecaps().computeIfAbsent(player.getName(), (value) -> new Recap(player));
        if (recap.isVisible()) {
            recap = recapManager.getDeathRecaps().compute(player.getName(), (key, value) -> new Recap(player));
        }

        recap.getLastDamages().add(new DamageInfo(event.getEntity().getName(), (double) event.getDamage()));
    }

    private Entity getTrueDamager(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof EntityProjectile) {
            EntityProjectile proj = (EntityProjectile) damager;
            return proj.shootingEntity;
        }

        if (damager instanceof EntityTameable && ((EntityTameable) damager).isTamed()) {
            return ((EntityTameable) damager).getOwner();

        }

        return damager;
    }
}
