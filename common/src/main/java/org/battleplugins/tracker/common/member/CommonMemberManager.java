package org.battleplugins.tracker.common.member;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.data.registry.Registry;
import org.anvilpowered.anvil.base.datastore.BaseManager;
import org.battleplugins.tracker.common.tracking.stat.calculator.RatingCalculator;
import org.battleplugins.tracker.member.MemberManager;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.model.member.Member;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommonMemberManager
    extends BaseManager<MemberRepository<?, ?>>
    implements MemberManager {

    @Inject
    private RatingCalculator ratingCalculator;

    @Inject
    public CommonMemberManager(Registry registry) {
        super(registry);
    }

    @Override
    public CompletableFuture<Void> kill(UUID killerUUID, UUID killedUUID, boolean tie) {
        return getPrimaryComponent().getOneForUser(killerUUID).thenAcceptBothAsync(
            getPrimaryComponent().getOneForUser(killedUUID),
            (optionalKiller, optionalKilled) -> {
                // ensure we have both members from db
                if (!optionalKilled.isPresent() || !optionalKiller.isPresent()) {
                    System.err.println(String.format(
                        "Failed to get records for killer %s and killed %s",
                        killerUUID, killedUUID)
                    );
                    return;
                }
                Member<?> killer = optionalKiller.get();
                Member<?> killed = optionalKilled.get();
                // save properties in case of failed db update
                int killerKills = killer.getKills();
                float killerRating = killer.getRating();
                // calculate new ratings
                ratingCalculator.updateRating(killer, killed, tie);
                // update killer
                if (!getPrimaryComponent().setKillsForUser(
                    killerUUID, killer.getKills(), killer.getRating()).join()) {
                    System.err.println(String.format(
                        "Failed to update killer %s. Will skip update for killed %s",
                        killerUUID, killedUUID)
                    );
                    return;
                }
                // update killed
                if (!getPrimaryComponent().setDeathsForUser(
                    killerUUID, killed.getDeaths(), killed.getRating()).join()) {
                    // revert killer if failed
                    if (!getPrimaryComponent().setKillsForUser(
                        killerUUID, killerKills, killerRating).join()) {
                        System.err.println(String.format(
                            "Failed revert killer %s after failed update to killed %s",
                            killerUUID, killedUUID)
                        );
                    }
                }
            }
        );
    }

    @Override
    public CompletableFuture<Void> kill(UUID killedUUID) {
        return null;
    }
}
