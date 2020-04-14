package org.battleplugins.tracker.tracking;

import lombok.AllArgsConstructor;

import org.battleplugins.api.entity.living.player.Player;
import org.battleplugins.api.event.Subscribe;
import org.battleplugins.api.event.player.PlayerInteractBlockEvent;
import org.battleplugins.api.event.player.PlayerJoinEvent;
import org.battleplugins.api.event.player.PlayerQuitEvent;
import org.battleplugins.api.world.block.entity.Sign;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.sign.LeaderboardSign;
import org.battleplugins.tracker.model.member.Member;
import org.battleplugins.tracker.util.TrackerUtil;

import java.util.Map;
import java.util.Optional;

/**
 * Miscellaneous listener for BattleTracker in Bukkit.
 *
 * @author Redned
 */
@AllArgsConstructor
public class TrackerListener {

    private BattleTracker plugin;

    /**
     * Event called when player joins
     *
     * @param event the event being called
     */
    @Subscribe
    public void onJoin(PlayerJoinEvent event) {
        MemberRepository pvpInterface = plugin.getTrackerManager().getPvPInterface();
        MemberRepository pveInterface = plugin.getTrackerManager().getPvEInterface();

        Player player = event.getPlayer();
        if (!pvpInterface.hasRecord(player) && plugin.getTrackerManager().isTrackingPvP()) {
            pvpInterface.createNewRecord(player);
        }

        if (!pveInterface.hasRecord(player) && plugin.getTrackerManager().isTrackingPvE()) {
            pveInterface.createNewRecord(player);
        }
    }

    /**
     * Event called when player quits
     *
     * @param event the event being called
     */
    @Subscribe
    public void onQuit(PlayerQuitEvent event) {
        for (Map.Entry<String, MemberRepository> interfaces : plugin.getTrackerManager().getInterfaces().entrySet()) {
            interfaces.getValue().save(event.getPlayer());
        }

        for (MemberRepository memberRepository : plugin.getTrackerManager().getInterfaces().values()) {
            if (!memberRepository.getRecapManager().getDeathRecaps().containsKey(event.getPlayer().getName()))
                continue;

            memberRepository.getRecapManager().getDeathRecaps().remove(event.getPlayer().getName());
        }
    }

    /**
     * Event called when a sign is clicked
     *
     * @param event the event being called
     */
    @Subscribe
    public void onSignInteract(PlayerInteractBlockEvent event) {
        if (!event.getBlock().getType().getIdentifier().getKey().contains("sign")) {
            return;
        }

        Sign sign = (Sign) event.getBlock().getWorld().getBlockEntityAt(event.getBlock().getLocation()).get();
        LeaderboardSign leaderboardSign = plugin.getSignManager().getSigns().get(sign.getLocation());
        if (leaderboardSign == null) {
            return; // not a BattleTracker sign
        }

        Optional<MemberRepository> tracker = plugin.getTrackerManager().getInterface(leaderboardSign.getTrackerName());
        if (!tracker.isPresent()) {
            plugin.getLogger().debug("A tracker could not be found for " + leaderboardSign.getTrackerName() + "!");
            return;
        }

        Member member = tracker.get().getOrCreateRecord(event.getPlayer());
        String[] lines = plugin.getSignManager().getPersonalFormat().clone();
        for (String line : lines) {
            TrackerUtil.replaceRecordValues(line, member);
        }
        sign.sendSignUpdate(event.getPlayer(), lines);
    }
}
