package org.battleplugins.tracker.common.tracking.stat.calculator;

import org.anvilpowered.anvil.api.misc.Named;
import org.battleplugins.tracker.model.member.Member;

/**
 * Interface for rating calculators
 *
 * @author alkarin_v, Redned
 */
public interface RatingCalculator extends Named {

    /**
     * Returns the name of the calculator
     *
     * @return the name of the calculator
     */
    @Override
    String getName();

    /**
     * Returns the default rating
     *
     * @return the default rating
     */
    float getDefaultRating();

    /**
     * Sets the default rating
     *
     * @param defaultRating the default rating
     */
    void setDefaultRating(float defaultRating);

    /**
     * Updates the rating of
     *
     * @param killer the killer's Record
     * @param killed the player killed's Record
     * @param tie if the final result is a tie
     */
    void updateRating(Member<?> killer, Member<?> killed, boolean tie);

    /**
     * Updates the rating of
     *
     * @param killer the killer's Record
     * @param killed an array of the players killed's Record
     * @param tie if the final result is a tie
     */
    void updateRating(Member<?> killer, Member<?>[] killed, boolean tie);
}
