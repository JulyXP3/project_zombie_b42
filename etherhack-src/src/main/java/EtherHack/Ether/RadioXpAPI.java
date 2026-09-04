/*
 * Red-team POC: server-persistent skill XP via forged WaveSignalPacket
 * (multiplayer).
 *
 * Chain (verified against the running game jar, see
 * analysis/电台效果注入与尸体物品伪造(已实施完成).md §一/§七):
 *   1. WaveSignalPacket is a client->server packet with the `codes` string
 *      fully client-controlled (no anti-cheat, only LoginOnServer).
 *   2. Server processServer -> ZomboidRadio.SendTransmission ->
 *      DistributeTransmission: any powered-on device (radio or TV, matching
 *      the isTv flag) on the matching channel with volume > 0 triggers
 *      LuaEventManager "OnDeviceText".
 *   3. shared ISRadioInteractions.OnDeviceText applies the codes to every
 *      living player within 5 tiles / same floor / same indoors-outdoors:
 *      skill codes call addXp() -> GameServer.addXp (official server-side
 *      ledger, updateXpChecker keeps the anti-cheat baseline in sync, so
 *      AntiCheatXPUpdate is never triggered).
 *   4. XP is capped at SandboxVars.LevelForMediaXPCutoff (default 3 in every
 *      official preset); 30s per-player-per-code cooldown server-side.
 *
 * Field notes from live testing (see doc §七):
 *   - msg MUST be non-null: ISRadioInteractions.checkPlayer:200 early-returns
 *     when _line == nil (msg is passed through to OnDeviceText as _line).
 *     First PoC attempt sent msg=null -> "sent" but zero XP.
 *   - signalStrength = -1 bypasses the sourceX/Y position gate
 *     (ZomboidRadio.java:626-631) and range distortion; a position offset
 *     like +1 can collide with the anchor device coordinates and fail.
 *   - Codes are built Lua-side from EtherRadioXp.PERK_CODE (perk name ->
 *     3-letter interaction code, full table ISRadioInteractions.lua:109-147);
 *     the UI offers single-skill selection + an all-skills button.
 *
 * Only for the user's own server / self-built test environment.
 */
package EtherHack.Ether;

import EtherHack.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.WaveSignalPacket;

public class RadioXpAPI {

    /*
     * Send one forged WaveSignalPacket carrying the given codes string.
     * Parameters:
     *   channel - must match the anchor device's channel (the Lua wrapper
     *             reads it from the device and passes it in);
     *   isTv    - must match the anchor device type (radio = false, TV = true);
     *   codes   - comma-separated 3-letter codes WITHOUT amounts, e.g. "FIS"
     *             or "SPR,LFT,..." (Lua passes bare codes; Java appends
     *             "+amount" to each - server-side checkPlayer requires every
     *             entry to be >4 chars, a bare "FIS" is silently skipped);
     *   amount  - per-application amount; XP per hit = 50 x amount.
     * The anchor (powered-on radio/TV, volume >= 1, matching channel) must
     * already exist near the player - EtherRadioXp.findAnchor (Lua) checks this.
     */
    @LuaMethod(name = "radioXpBroadcast", global = true)
    public static boolean radioXpBroadcast(int channel, boolean isTv, String codes, int amount) {
        try {
            if (GameClient.connection == null) {
                Logger.printLog("[RadioXp] not connected to a server");
                return false;
            }
            if (codes == null || codes.isEmpty()) {
                Logger.printLog("[RadioXp] empty codes");
                return false;
            }
            if (amount < 1) amount = 1;
            if (amount > 99) amount = 99;

            zombie.characters.IsoPlayer p = zombie.characters.IsoPlayer.getInstance();
            if (p == null) return false;

            // bare codes -> "XXX+amount" each (server requires >4 chars per entry)
            String[] arr = codes.split(",");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                String c = arr[i].trim();
                if (c.length() != 3) {
                    Logger.printLog("[RadioXp] bad code entry: " + c);
                    return false;
                }
                if (i > 0) sb.append(',');
                sb.append(c).append('+').append(amount);
            }
            String codeLine = sb.toString();

            WaveSignalPacket packet = new WaveSignalPacket();
            packet.set((int) Math.floor(p.getX()), (int) Math.floor(p.getY()),
                    channel,               // must match the anchor device's channel
                    "...",                 // msg: MUST be non-null (checkPlayer:200 _line==nil early-return)
                    "",                    // guid: empty string skips isKnownMediaLine dedup
                    codeLine,              // "FIS+99" or "SPR+99,LFT+99,..."
                    1.0f, 1.0f, 1.0f,      // rgb (unused server-side)
                    -1,                    // signalStrength: -1 bypasses position gate + distortion
                    isTv);                 // must match the anchor device type
            ByteBufferWriter bb = GameClient.connection.startPacket();
            PacketTypes.PacketType.WaveSignal.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.WaveSignal.send(GameClient.connection);

            Logger.printLog("[RadioXp] broadcast codes=" + codeLine);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[RadioXp] broadcast error: " + t.getMessage());
            return false;
        }
    }
}
