/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.GameTime
 *  zombie.core.Core
 *  zombie.core.SpriteRenderer
 *  zombie.core.textures.Texture
 *  zombie.gameStates.GameState
 *  zombie.gameStates.GameStateMachine$StateAction
 *  zombie.input.GameKeyboard
 *  zombie.input.Mouse
 *  zombie.ui.UIManager
 */
package EtherHack.states;

import java.util.function.Consumer;
import zombie.GameTime;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.ui.UIManager;

public class EtherLogoState
extends GameState {
    private float alpha = 0.0f;
    private float logoDisplayTime = 40.0f;
    private int stage = 0;
    private float targetAlpha = 0.0f;
    private boolean noRender = false;
    private final LogoElement etherLogo = new LogoElement("EtherHack/media/EtherLogo.png");

    public void enter() {
        UIManager.suspend = true;
        this.alpha = 0.0f;
        this.targetAlpha = 1.0f;
    }

    public void exit() {
        UIManager.suspend = false;
    }

    public void render() {
        Core var1 = Core.getInstance();
        if (this.noRender) {
            var1.StartFrameUI();
            SpriteRenderer.instance.renderi((Texture)null, 0, 0, var1.getOffscreenWidth(0), var1.getOffscreenHeight(0), 0.0f, 0.0f, 0.0f, 1.0f, (Consumer)null);
            var1.EndFrame();
        } else {
            var1.StartFrameUI();
            var1.EndFrame();
            boolean var2 = UIManager.useUiFbo;
            UIManager.useUiFbo = false;
            var1.StartFrameUI();
            SpriteRenderer.instance.renderi((Texture)null, 0, 0, var1.getOffscreenWidth(0), var1.getOffscreenHeight(0), 0.0f, 0.0f, 0.0f, 1.0f, (Consumer)null);
            this.etherLogo.centerOnScreen();
            this.etherLogo.render(this.alpha);
            var1.EndFrameUI();
            UIManager.useUiFbo = var2;
        }
    }

    public GameStateMachine.StateAction update() {
        if (Mouse.isLeftDown() || GameKeyboard.isKeyDown((int)28) || GameKeyboard.isKeyDown((int)57) || GameKeyboard.isKeyDown((int)1)) {
            this.stage = 2;
        }
        GameTime var1 = GameTime.getInstance();
        switch (this.stage) {
            case 0: {
                this.targetAlpha = 1.0f;
                if (this.alpha != 1.0f) break;
                this.stage = 1;
                break;
            }
            case 1: {
                this.logoDisplayTime -= var1.getMultiplier() / 1.6f;
                if (!(this.logoDisplayTime <= 0.0f)) break;
                this.stage = 2;
                break;
            }
            case 2: {
                this.targetAlpha = 0.0f;
                if (this.alpha != 0.0f) break;
                this.noRender = true;
                return GameStateMachine.StateAction.Continue;
            }
        }
        this.updateAlpha(var1);
        return GameStateMachine.StateAction.Remain;
    }

    private void updateAlpha(GameTime var1) {
        float var3 = 0.02f * var1.getMultiplier();
        if (this.alpha < this.targetAlpha) {
            this.alpha += var3;
            if (this.alpha > this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        } else if (this.alpha > this.targetAlpha) {
            this.alpha -= var3;
            if (this.stage == 2) {
                this.alpha -= var3;
            }
            if (this.alpha < this.targetAlpha) {
                this.alpha = this.targetAlpha;
            }
        }
    }

    private static final class LogoElement {
        private final Texture texture;
        private int x;
        private int y;
        private int width;
        private int height;

        LogoElement(String var1) {
            this.texture = Texture.getSharedTexture((String)var1);
            if (this.texture != null) {
                this.width = this.texture.getWidth();
                this.height = this.texture.getHeight();
            }
        }

        void centerOnScreen() {
            Core var1 = Core.getInstance();
            this.x = (var1.getScreenWidth() - this.width) / 2;
            this.y = (var1.getScreenHeight() - this.height) / 2;
        }

        void render(float var1) {
            if (this.texture != null && this.texture.isReady()) {
                SpriteRenderer.instance.renderi(this.texture, this.x, this.y, this.width, this.height, 1.0f, 1.0f, 1.0f, var1, (Consumer)null);
            }
        }
    }
}
