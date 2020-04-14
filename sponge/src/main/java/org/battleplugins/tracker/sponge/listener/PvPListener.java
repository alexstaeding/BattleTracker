package org.battleplugins.tracker.sponge.listener;

import com.google.inject.Inject;
import org.battleplugins.api.entity.living.player.OfflinePlayer;
import org.battleplugins.api.sponge.entity.living.player.SpongePlayer;
import org.battleplugins.tracker.member.MemberManager;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.tracking.recap.DamageInfo;
import org.battleplugins.tracker.tracking.recap.Recap;
import org.battleplugins.tracker.tracking.recap.RecapManager;
import org.battleplugins.tracker.util.TrackerUtil;
import org.spongepowered.api.data.manipulator.mutable.entity.TameableData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Main listener for PvP tracking in Sponge.
 *
 * @author Redned
 */
public class PvPListener {

    @Inject
    private MemberManager memberManager;

    /**
     * Event called when one player is killed by another
     *
     * @param event the event being called
     */
    @Listener
    public void onDeath(
        DestructEntityEvent.Death event,
        @Getter("getTargetEntity") Player killed,
        @Root EntityDamageSource killerSource
    ) {
        Player killer;
        ItemStack weapon;
        Entity source = killerSource instanceof IndirectEntityDamageSource
            ? ((IndirectEntityDamageSource) killerSource).getIndirectSource()
            : killerSource.getSource();
        if (source instanceof Player) {
            killer = (Player) source;
            weapon = killer.getItemInHand(HandTypes.MAIN_HAND).get();
        } else if (source instanceof Projectile) {
            Projectile projectile = (Projectile) source;
            if (projectile.getShooter() instanceof Player) {
                killer = (Player) proj.getShooter();
                weapon = killer.getItemInHand(HandTypes.MAIN_HAND).get();
            }
        } else if (source instanceof IndirectEntityDamageSource) {

        }

        if (source.get(TameableData.class).isPresent()) {
            Optional<UUID> opOwnerUUID = damager.get(TameableData.class).get().owner().get();
            if (opOwnerUUID.isPresent()) {
                UUID uuid = opOwnerUUID.get();
                if (plugin.getServer().getOfflinePlayer(uuid).map(OfflinePlayer::isOnline).orElse(false)) {
                    killer = ((SpongePlayer) plugin.getServer().getPlayer(uuid).get()).getHandle();
                    // Use a bone to show the case was a wolf
                    weapon = ItemStack.builder().itemType(ItemTypes.BONE).quantity(1).build();
                }
            }
        }

        if (killer == null)
            return;

        // Check the killers world just incase for some reason the
        // killed player was teleported to another world
        if (plugin.getConfigManager().getPvPConfig().getNode("ignoredWorlds").getCollectionValue(String.class).contains(killer.getWorld().getName()))
            return;

        TrackerUtil.updatePvPStats(plugin.getServer().getOfflinePlayer(killed.getUniqueId().toString()).get(),
            plugin.getServer().getOfflinePlayer(killer.getUniqueId().toString()).get());

        MemberRepository pvpTracker = plugin.getTrackerManager().getPvPInterface();
        if (pvpTracker.getDeathMessageManager().isDefaultMessagesOverriden())
            event.setMessageCancelled(true);

        pvpTracker.getDeathMessageManager().sendItemMessage(killer.getName(), killed.getName(), weapon.getType().getName().toLowerCase());
        pvpTracker.getRecapManager().getDeathRecaps().get(killed.getName()).setVisible(true);
    }

    /**
     * Event called when a player takes damage from another player
     *
     * @param event the event being called
     */
    @Listener
    public void onEntityDamage(DamageEntityEvent event) {
        if (!(event.getTargetEntity() instanceof Player) || !(event.getCause().first(EntityDamageSource.class).isPresent()))
            return;

        EntityDamageSource source = event.getCause().first(EntityDamageSource.class).get();
        if (!(getTrueDamager(source) instanceof Player)) {
            return;
        }

        Player spongePlayer = (Player) event.getTargetEntity();
        org.battleplugins.api.entity.living.player.Player player = plugin.getServer().getPlayer(spongePlayer.getName()).get();
        MemberRepository pvpTracker = plugin.getTrackerManager().getPvPInterface();

        RecapManager recapManager = pvpTracker.getRecapManager();
        Recap recap = recapManager.getDeathRecaps().computeIfAbsent(player.getName(), (value) -> new Recap(player));
        if (recap.isVisible()) {
            recap = recapManager.getDeathRecaps().compute(player.getName(), (key, value) -> new Recap(player));
        }

        recap.getLastDamages().add(new DamageInfo(spongePlayer.getName(), event.getFinalDamage()));
    }

    private Entity getTrueDamager(EntityDamageSource source) {
        Entity damager = source.getSource();
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Entity) {
                return (Entity) proj.getShooter();
            }
        }

        if (damager.get(TameableData.class).isPresent()) {
            Optional<UUID> opOwnerUUID = damager.get(TameableData.class).get().owner().get();
            if (opOwnerUUID.isPresent()) {
                UUID uuid = opOwnerUUID.get();
                if (plugin.getServer().getOfflinePlayer(uuid).map(OfflinePlayer::isOnline).orElse(false)) {
                    return ((SpongePlayer) plugin.getServer().getPlayer(uuid).get()).getHandle();
                }
            }
        }

        return damager;
    }
}
