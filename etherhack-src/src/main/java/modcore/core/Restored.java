package modcore.core;

/** The states one ordinary meal restores. Values are the food's own stored units. */
public enum Restored {
    /** 0..1; good end is 0. */
    THIRST,
    /** 0..1; good end is 1. */
    ENDURANCE,
    /** 0..1; good end is 0. */
    FATIGUE,
    /** 0..1; good end is 0. */
    STRESS,
    /** 0..100; good end is 0. */
    BOREDOM,
    /** 0..100; good end is 0. */
    UNHAPPY
}
