package carkill;

/**
 * Dependency-free constants shared by the attach client and the in-game agent.
 * Kept separate so neither side has to load the other's game/attach classes.
 */
public final class CarKillProtocol {
    /** Set inside the game JVM once the transformer is installed; prevents duplicate attach. */
    public static final String INSTALLED_PROPERTY = "carkill.installed";

    private CarKillProtocol() {
    }
}
