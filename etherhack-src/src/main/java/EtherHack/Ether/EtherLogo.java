/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.GameWindow
 *  zombie.gameStates.GameState
 *  zombie.gameStates.TISLogoState
 */
package EtherHack.Ether;

import EtherHack.states.EtherLogoState;
import EtherHack.utils.Logger;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.gameStates.GameState;
import zombie.gameStates.TISLogoState;

public class EtherLogo {
    private static EtherLogo instance;

    private EtherLogo() {
    }

    public void init() {
        Logger.printLog("Initializing EtherLogo...");
        try {
            ArrayList statesList = GameWindow.states.states;
            Logger.printLog("States list size: " + String.valueOf(statesList != null ? Integer.valueOf(statesList.size()) : "null"));
            if (statesList == null || statesList.isEmpty()) {
                Logger.printLog("States list is null or empty, skipping logo injection");
                return;
            }
            GameState firstState = (GameState)statesList.get(0);
            Logger.printLog("First state: " + firstState.getClass().getName());
            if (firstState instanceof TISLogoState) {
                GameWindow.states.states.add(0, new EtherLogoState());
                GameWindow.states.loopToState = 1;
                Logger.printLog("EtherLogo initialized successfully!");
            } else {
                Logger.printLog("First state is not TISLogoState, skipping logo injection");
            }
        }
        catch (Exception e) {
            Logger.printLog("Error when initializing the EtherLogo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static EtherLogo getInstance() {
        if (instance == null) {
            instance = new EtherLogo();
        }
        return instance;
    }
}
