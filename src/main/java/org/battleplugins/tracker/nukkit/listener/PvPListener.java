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
import mc.alk.mc.MCServer;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.TrackerInterface;
import org.battleplugins.tracker.stat.StatType;
import org.battleplugins.tracker.stat.record.Record;

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
                    killer = (Player) damager;
                    weapon = killer.getInventory().getItemInHand();
                }
            }

            if (damager instanceof EntityTameable && ((EntityTameable) damager).isTamed()) {
                Player owner = ((EntityTameable) damager).getOwner();
                killer = (Player) damager;
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
        if (tracker.getPvPConfig().getStringList("ignoredWorlds").contains(killer.getLevel().getName()))
            return;

        updateStats(killed, killer);

        // TODO: Add death messages
    }

    public void updateStats(Player killed, Player killer) {
        TrackerInterface pvpTracker = tracker.getTrackerManager().getPvPInterface();
        Record killerRecord = pvpTracker.getRecord(MCServer.getOfflinePlayer(killer.getUniqueId()));
        Record killedRecord = pvpTracker.getRecord(MCServer.getOfflinePlayer(killed.getUniqueId()));

        if (killerRecord.isTracking())
            pvpTracker.incrementValue(StatType.KILLS, MCServer.getOfflinePlayer(killer.getUniqueId()));

        if (killedRecord.isTracking())
            pvpTracker.incrementValue(StatType.DEATHS, MCServer.getOfflinePlayer(killed.getUniqueId()));

        pvpTracker.updateRating(MCServer.getOfflinePlayer(killer.getUniqueId()), MCServer.getOfflinePlayer(killed.getUniqueId()), false);
    }
}