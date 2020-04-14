package org.battleplugins.tracker.sign;

import org.battleplugins.api.world.Location;

/**
 * Holds information about tracker signs.
 *
 * @author Redned
 */
public interface LeaderboardSign {

    /**
     * The location of the sign
     *
     * @return the location of the sign
     */
    Location getLocation();

    /**
     * The stat type displayed on the sign
     *
     * @return the stat type displayed on the sign
     */
    String getStatType();

    /**
     * The tracker name to retrieve information from
     * when displaying information on the sign
     *
     * @return the tracker name to retrieve information from
     */
    String trackerName;
}
