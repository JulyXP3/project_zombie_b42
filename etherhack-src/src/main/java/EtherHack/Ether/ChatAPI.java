/*
 * Red-team POC: send a chat message under another player's name (multiplayer).
 *
 * Chain (verified against the B42 decompile, see
 * analysis/冒名聊天与僵尸皮肤-方案.md):
 *   1. Client calls the public ChatManager.sendMessageToChat(author, type, msg)
 *      with an arbitrary author string — no reflection needed (unlike the B41
 *      Kairos plugin this idea came from).
 *   2. ChatMessageFromPlayer packet carries { chatId, author, text }; the
 *      server's ChatServer.processMessageFromPlayerPacket() never compares
 *      author against the sending connection's username — zero auth.
 *   3. ChatBase.sendMessageToChatMembers() broadcasts to every member except
 *      the player matching the forged author (and drops the message entirely
 *      if that player is offline).
 *
 * Channel whitelist: general (server-wide) + say (30-tile range). Radio is
 * rejected server-side; admin/faction/shout add nothing; whisper throws.
 *
 * Only for the user's own server / self-built test environment.
 */
package EtherHack.Ether;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatManager;
import zombie.network.GameClient;
import zombie.network.chat.ChatType;
import EtherHack.utils.Logger;

public class ChatAPI {

    @LuaMethod(name = "sendChatAs", global = true)
    public static boolean sendChatAs(String author, String channel, String text) {
        try {
            if (author == null || author.trim().isEmpty()) {
                return false;
            }
            if (text == null || text.trim().isEmpty()) {
                return false;
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                Logger.printLog("[ChatAPI] sendChatAs: no local player");
                return false;
            }
            // 冒名的价值只在多人; 单人发送只会在本地聊天框自言自语 (无害但无意义)
            if (!GameClient.client || GameClient.connection == null) {
                Logger.printLog("[ChatAPI] sendChatAs: multiplayer client only");
                return false;
            }
            ChatType type;
            if ("general".equals(channel)) {
                type = ChatType.general;
            } else if ("say".equals(channel)) {
                type = ChatType.say;
            } else {
                Logger.printLog("[ChatAPI] sendChatAs: channel not allowed: " + channel);
                return false;
            }
            ChatManager.getInstance().sendMessageToChat(author.trim(), type, text.trim());
            Logger.printLog("[ChatAPI] sent as '" + author + "' via " + channel);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[ChatAPI] sendChatAs error: " + t.getMessage());
            return false;
        }
    }
}
