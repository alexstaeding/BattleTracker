package org.battleplugins.tracker.member.repository;

import org.battleplugins.tracker.model.member.Member;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MongoMemberRepository
    extends MemberRepository<ObjectId, Datastore> {

    Query<Member<ObjectId>> asQuery(UUID userUUID);

    CompletableFuture<Boolean> incrementKills(Query<Member<ObjectId>> query, float rating);
    CompletableFuture<Boolean> incrementDeaths(Query<Member<ObjectId>> query, float rating);
}
