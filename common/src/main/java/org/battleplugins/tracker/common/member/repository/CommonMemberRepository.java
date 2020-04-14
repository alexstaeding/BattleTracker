package org.battleplugins.tracker.common.member.repository;

import org.anvilpowered.anvil.api.datastore.DataStoreContext;
import org.anvilpowered.anvil.base.datastore.BaseRepository;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.model.member.Member;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class CommonMemberRepository<
    TKey,
    TDataStore>
    extends BaseRepository<TKey, Member<TKey>, TDataStore>
    implements MemberRepository<TKey, TDataStore> {

    protected CommonMemberRepository(DataStoreContext<TKey, TDataStore> dataStoreContext) {
        super(dataStoreContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Member<TKey>> getTClass() {
        return (Class<Member<TKey>>) getDataStoreContext().getEntityClassUnsafe("member");
    }

    @Override
    public CompletableFuture<Optional<Member<TKey>>> getOneOrGenerateForUser(UUID userUUID) {
        return getOneForUser(userUUID).thenApplyAsync(optionalMember -> {
            if (optionalMember.isPresent()) {
                return optionalMember;
            }
            // if not present, generate a new one
            Member<TKey> member = generateEmpty();
            member.setUserUUID(userUUID);
            return insertOne(member).join();
        });
    }


}
