/*
 * Red-team POC: learn every crafting recipe and sync the list to the server
 * (multiplayer).
 *
 * Problem: the existing learnAllRecipes() only mutates the local client's
 * knownRecipes list. In MP the server keeps its own copy; the client list is
 * used for UI (which recipes the crafting menu shows), but server-side
 * validation checks the server copy — so learned recipes silently fail
 * validation, and the list is never persisted server-side.
 *
 * Chain (verified against the B42 decompile):
 *   SyncPlayerFieldsPacket.parseParam(PF_Recipes=1) (:89-96) appends every
 *   recipe string from the packet to the player's server-side knownRecipes —
 *   no validation, no capability gate beyond LoginOnServer (granted to the
 *   default "user" role, Roles.java). The packet's processServer is the
 *   default no-op: the parse itself is the write. This is the same packet
 *   vanilla's own ISReadABook/ISResearchRecipe server flow uses
 *   (sendSyncPlayerFields 0x1) — from the server's perspective the two are
 *   indistinguishable.
 *
 * Recipes are also persisted in the character save (IsoGameCharacter.save
 * :5053), so the learned list survives relogging once the server has it.
 *
 * Only for the user's own server / self-built test environment.
 */
package EtherHack.Ether;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Recipe;
import EtherHack.utils.Logger;

public class RecipeAPI {

    @LuaMethod(name = "learnAllRecipesSynced", global = true)
    public static boolean learnAllRecipesSynced() {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                Logger.printLog("[RecipeAPI] no local player");
                return false;
            }
            // 本地学习 (驱动制作菜单 UI; learnRecipe 自带去重)
            int learned = 0;
            // B42 新配方系统: 合成界面 (骨质腿甲等) 走 CraftRecipe.known 判定
            // (isRecipeKnown(CraftRecipe) 只查 knownRecipes.contains(getName())),
            // 此处传入 checkMetaRecipe=false: 我们本来就要学全部, 跳过 meta 解锁的
            // O(N^2) 全表扫描, 把整体代价压到毫秒级。
            for (CraftRecipe recipe : ScriptManager.instance.getAllCraftRecipes()) {
                if (!recipe.needToBeLearn()) continue; // 无需学习的配方不占列表
                if (player.learnRecipe(recipe.getName(), false)) {
                    ++learned;
                }
            }
            // B41 遗留 Recipe 系统兜底 (兼容 mod 的老式配方)
            int learnedLegacy = 0;
            for (Recipe recipe : ScriptManager.instance.getAllRecipes()) {
                if (recipe.getOriginalname() == null) continue;
                if (player.learnRecipe(recipe.getOriginalname(), false)) {
                    ++learnedLegacy;
                }
            }
            // MP: 上行 SyncPlayerFields(PF_Recipes=1) 把整份清单写进服务端
            // (writeParam case 1 序列化全部 knownRecipes; 服务端 parse 逐条采纳)。
            // SP: 服务端即本地, 上述本地学习已覆盖存档, 不发包。
            if (GameClient.client && GameClient.connection != null) {
                INetworkPacket.send(PacketTypes.PacketType.SyncPlayerFields, player, (byte)1);
                Logger.printLog("[RecipeAPI] synced " + learned + " craft + " + learnedLegacy
                        + " legacy recipes to server");
            } else {
                Logger.printLog("[RecipeAPI] learned " + learned + " craft + " + learnedLegacy
                        + " legacy recipes (singleplayer)");
            }
            return true;
        } catch (Throwable t) {
            Logger.printLog("[RecipeAPI] learnAllRecipesSynced error: " + t.getMessage());
            return false;
        }
    }
}
