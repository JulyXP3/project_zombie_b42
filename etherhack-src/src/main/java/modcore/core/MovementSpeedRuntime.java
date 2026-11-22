package modcore.core;

import zombie.ai.State;
import zombie.ai.states.IdleState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.player.PlayerMovementState;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugOptions;
import zombie.iso.Vector2;

/** Runtime target called from the game class. No ASM types or persistent velocity writes. */
public final class MovementSpeedRuntime {
    private MovementSpeedRuntime() { }

    public static Vector2 apply(Vector2 movement, IsoPlayer player) {
        if (movement == null || player == null) return movement;
        try {
            if (player != IsoPlayer.getInstance() || !player.isLocalPlayer() || !player.isAlive()) return movement;
            CoreMain main = CoreMain.getInstance();
            CoreAPI api = main == null ? null : main.CoreAPI;
            if (api == null || MovementSpeedPolicy.normalizePreset(api.movementSpeedMultiplier) == 1) return movement;
            State state = player.getCurrentState();
            // PlayerMovementState is the state a local player actually occupies while walking/running/sprinting
            // (IsoPlayer registers "movement"/"run"/"sprint" to it); IdleState/WalkTowardState cover standing and
            // path-driven NPC-style movement. Missing PlayerMovementState silently disabled every multiplier.
            boolean knownGround = state == IdleState.instance() || state == WalkTowardState.instance()
                    || state == PlayerMovementState.instance();
            boolean manualMove = player.isPlayerMoving() && player.pressedMovement(false);
            boolean special = player.getVehicle() != null || player.isClimbing() || player.isAttacking()
                    || player.isPerformingAnAction() || player.isOnFloor() || player.isRagdoll()
                    || player.isGrappling() || player.isBeingGrappled() || player.isSittingOnFurniture();
            boolean aimOrPath = player.isAiming() || player.getPath2() != null || player.isAutoWalk();
            boolean extra = TeleportClimbRuntime.suppressActive(player.getNetworkCharacterAI())
                    || (player.isGhostMode() && DebugOptions.instance.cheat.player.invisibleSprint.getValue());
            float factor = MovementSpeedPolicy.effectiveMultiplier(api.movementSpeedMultiplier,
                    true, knownGround, manualMove, special, aimOrPath, extra);
            if (factor != 1f && MovementSpeedPolicy.canScale(movement.x, movement.y, factor)) {
                movement.set(MovementSpeedPolicy.scaledComponent(movement.x, factor),
                        MovementSpeedPolicy.scaledComponent(movement.y, factor));
            }
        } catch (Throwable ignored) {
            // Incomplete world/player context keeps the original result, without per-frame log spam.
        }
        return movement;
    }
}
