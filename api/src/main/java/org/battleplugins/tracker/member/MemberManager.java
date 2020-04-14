package org.battleplugins.tracker.member;

import org.anvilpowered.anvil.api.datastore.Manager;
import org.battleplugins.tracker.member.repository.MemberRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MemberManager
    extends Manager<MemberRepository<?, ?>> {

    @Override
    default String getDefaultIdentifierSingularUpper() {
        return "Member";
    }

    @Override
    default String getDefaultIdentifierPluralUpper() {
        return "Members";
    }

    @Override
    default String getDefaultIdentifierSingularLower() {
        return "member";
    }

    @Override
    default String getDefaultIdentifierPluralLower() {
        return "members";
    }

    CompletableFuture<Void> kill(UUID killerUUID, UUID killedUUID, boolean tie);

    CompletableFuture<Void> kill(UUID killedUUID);
}
