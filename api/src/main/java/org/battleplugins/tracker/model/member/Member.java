package org.battleplugins.tracker.model.member;

import org.anvilpowered.anvil.api.model.ObjectWithId;

import java.util.UUID;

/**
 * Stores and holds tracker data before it
 * is put into the database.
 *
 * @author Redned
 */
public interface Member<TKey> extends ObjectWithId<TKey> {

    UUID getUserUUID();
    void setUserUUID(UUID userUUID);

    int getKills();
    void setKills(int kills);

    int getDeaths();
    void setDeaths();

    int getTies();
    void setTies(int ties);

    int getMaxStreak();
    void setMaxStreak(int maxStreak);

    int getMaxRanking();
    void setMaxRanking(int maxRanking);

    float getRating();
    void setRating(float rating);

    int getMaxRating();
    void setMaxRating(int maxRating);

    int getMaxKdRatio();
    void setMaxKdRatio(int maxKdRatio);
}
