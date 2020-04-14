package org.battleplugins.tracker.common.member.repository;

import com.google.inject.Inject;
import org.anvilpowered.anvil.api.datastore.DataStoreContext;
import org.anvilpowered.anvil.base.datastore.BaseMongoRepository;
import org.battleplugins.tracker.member.repository.MongoMemberRepository;
import org.battleplugins.tracker.model.member.Member;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CommonMongoMemberRepository
    extends CommonMemberRepository<ObjectId, Datastore>
    implements BaseMongoRepository<Member<ObjectId>>,
    MongoMemberRepository {

    @Inject
    public CommonMongoMemberRepository(DataStoreContext<ObjectId, Datastore> dataStoreContext) {
        super(dataStoreContext);
    }

    @Override
    public Query<Member<ObjectId>> asQuery(UUID userUUID) {
        return asQuery().field("userUUID").equal(userUUID);
    }

    @Override
    public CompletableFuture<Optional<Member<ObjectId>>> getOneForUser(UUID userUUID) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(asQuery(userUUID).get()));
    }

    @Override
    public CompletableFuture<Boolean> setKillsForUser(UUID userUUID, float rating) {
        return incrementKills(asQuery(userUUID), rating);
    }

    @Override
    public CompletableFuture<Boolean> setDeathsForUser(UUID userUUID, float rating) {
        return incrementDeaths(asQuery(userUUID), rating);
    }

    @Override
    public CompletableFuture<Boolean> incrementKills(Query<Member<ObjectId>> query, float rating) {
        return update(query, inc("kills").set("rating", rating));
    }

    @Override
    public CompletableFuture<Boolean> incrementDeaths(Query<Member<ObjectId>> query, float rating) {
        return update(query, inc("deaths").set("rating", rating));
    }
}
