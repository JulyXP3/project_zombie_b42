package modcore.core;

/** Owns one item and one in-flight action for the current client connection. */
final class VehicleRepairState {
    enum Status {
        IDLE("idle"), PENDING("pending"), DONE("done"), REJECTED("rejected"), MISSING("missing");

        final String code;

        Status(String code) { this.code = code; }
    }

    private Object owner;
    private Object connection;
    private Object part;
    private int itemId;
    private String fullType;
    private Object action;
    private byte actionId;
    private Status status = Status.IDLE;
    private boolean quarantined;

    void connect(Object owner, Object connection) {
        if (this.owner == owner && this.connection == connection) return;
        this.owner = owner;
        this.connection = connection;
        part = null;
        fullType = null;
        action = null;
        actionId = 0;
        status = Status.IDLE;
        quarantined = false;
    }

    boolean claim(Object part, int itemId, String fullType) {
        if (owner == null || connection == null || this.part != null || quarantined
                || part == null || fullType == null || fullType.isEmpty()) return false;
        this.part = part;
        this.itemId = itemId;
        this.fullType = fullType;
        return true;
    }

    boolean owns(Object part, int itemId, String fullType) {
        return this.part == part && blocks(itemId, fullType);
    }

    boolean blocks(int itemId, String fullType) {
        return part != null && this.itemId == itemId && this.fullType.equals(fullType);
    }

    boolean startAction(byte id, Object action) {
        if (part == null || status != Status.IDLE || quarantined || id == 0 || action == null) return false;
        actionId = id;
        this.action = action;
        status = Status.PENDING;
        return true;
    }

    Status observeAction(Object action, boolean done, boolean rejected) {
        if (status == Status.IDLE || status == Status.DONE || status == Status.REJECTED) return status;
        if (action == null || action != this.action) return status = Status.MISSING;
        status = done ? Status.DONE : rejected ? Status.REJECTED : Status.PENDING;
        return status;
    }

    boolean completeAction() {
        if (quarantined || (status != Status.DONE && status != Status.REJECTED)) return false;
        action = null;
        actionId = 0;
        status = Status.IDLE;
        return true;
    }

    boolean release() {
        if (quarantined || status != Status.IDLE) return false;
        part = null;
        fullType = null;
        return true;
    }

    void quarantine() { quarantined = true; }
    boolean isQuarantined() { return quarantined; }
    boolean isClaimed() { return part != null; }
    Status status() { return status; }
    byte actionId() { return actionId; }
    Object action() { return action; }
}
