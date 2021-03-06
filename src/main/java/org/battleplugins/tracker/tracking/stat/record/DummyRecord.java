package org.battleplugins.tracker.tracking.stat.record;

import org.battleplugins.tracker.tracking.TrackerInterface;

import java.util.Map;

/**
 * Stores and holds 'dummy' tracker data. None of this is
 * or should be stored into a database.
 *
 * @author Redned
 */
public class DummyRecord extends Record {

    public DummyRecord(TrackerInterface tracker, String id, String name) {
        super(tracker, id, name);
    }

    public DummyRecord(TrackerInterface tracker, String id, String name, Map<String, Float> stats) {
        super(tracker, id, name, stats);
    }
}
