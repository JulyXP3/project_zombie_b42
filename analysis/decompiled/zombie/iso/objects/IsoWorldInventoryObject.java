/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.vm.KahluaTable
 *  zombie.GameTime
 *  zombie.Lua.LuaEventManager
 *  zombie.UsedFromLua
 *  zombie.core.Core
 *  zombie.core.PerformanceSettings
 *  zombie.core.Translator
 *  zombie.core.logger.ExceptionLogger
 *  zombie.core.math.PZMath
 *  zombie.core.network.ByteBufferReader
 *  zombie.core.network.ByteBufferWriter
 *  zombie.core.opengl.Shader
 *  zombie.core.properties.IsoObjectChange
 *  zombie.core.raknet.UdpConnection
 *  zombie.core.random.Rand
 *  zombie.core.skinnedmodel.model.ItemModelRenderer$RenderStatus
 *  zombie.core.skinnedmodel.model.WorldItemModelDrawer
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.debug.DebugOptions
 *  zombie.debug.LineDrawer
 *  zombie.entity.Component
 *  zombie.entity.ComponentType
 *  zombie.entity.GameEntity
 *  zombie.entity.GameEntityFactory
 *  zombie.entity.components.fluids.Fluid
 *  zombie.entity.components.fluids.FluidCategory
 *  zombie.entity.components.fluids.FluidContainer
 *  zombie.entity.components.fluids.FluidType
 *  zombie.input.Mouse
 *  zombie.inventory.ItemSoundManager
 *  zombie.inventory.types.InventoryContainer
 *  zombie.iso.IItemProvider
 *  zombie.iso.IsoCamera
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoUtils
 *  zombie.iso.IsoWorld
 *  zombie.iso.PlayerCamera
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.IConnection
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.ServerGUI
 *  zombie.network.packets.INetworkPacket
 *  zombie.ui.ObjectTooltip
 *  zombie.util.io.BitHeader
 *  zombie.util.io.BitHeader$HeaderSize
 *  zombie.util.io.BitHeaderRead
 *  zombie.util.io.BitHeaderWrite
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.packets.INetworkPacket;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@UsedFromLua
public class IsoWorldInventoryObject
extends IsoObject
implements IItemProvider {
    public InventoryItem item;
    public float xoff;
    public float yoff;
    public float zoff;
    public boolean removeProcess;
    public double dropTime = -1.0;
    public boolean ignoreRemoveSandbox;
    private boolean extendedPlacement;

    public IsoWorldInventoryObject(InventoryItem item, IsoGridSquare sq, float xoff, float yoff, float zoff) {
        this.outlineOnMouseover = true;
        if (item != null) {
            item.worldXRotation = 0.0f;
            item.worldYRotation = 0.0f;
            if (item.worldZRotation < 0.0f) {
                item.worldZRotation = Rand.Next((int)0, (int)360);
            }
            item.setContainer(null);
        }
        this.xoff = xoff;
        this.yoff = yoff;
        this.zoff = zoff;
        if (this.xoff == 0.0f) {
            this.xoff = (float)Rand.Next((int)1000) / 1000.0f;
        }
        if (this.yoff == 0.0f) {
            this.yoff = (float)Rand.Next((int)1000) / 1000.0f;
        }
        this.item = item;
        this.sprite = IsoSprite.CreateSprite((IsoSpriteManager)IsoSpriteManager.instance);
        if (item != null) {
            this.updateSprite();
        }
        this.square = sq;
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
        this.dropTime = GameTime.getInstance().getWorldAgeHours();
        if (item != null && item.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.TransferComponent((GameEntity)item, (GameEntity)this, (ComponentType)ComponentType.FluidContainer);
        }
    }

    public IsoWorldInventoryObject(IsoCell cell) {
        super(cell);
        this.offsetY = 0.0f;
        this.offsetX = 0.0f;
    }

    public void swapItem(InventoryItem newItem) {
        if (newItem == null) {
            return;
        }
        if (this.getItem() != null) {
            if (this.hasComponent(ComponentType.FluidContainer)) {
                GameEntityFactory.TransferComponent((GameEntity)this, (GameEntity)this.item, (ComponentType)ComponentType.FluidContainer);
            }
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this.getItem());
            ItemSoundManager.removeItem((InventoryItem)this.getItem());
            this.getItem().setWorldItem(null);
            newItem.setID(this.getItem().getID());
            newItem.worldScale = this.getItem().worldScale;
            newItem.worldZRotation = this.getItem().worldZRotation;
            newItem.worldYRotation = this.getItem().worldYRotation;
            newItem.worldXRotation = this.getItem().worldXRotation;
        }
        this.item = newItem;
        if (newItem.getWorldItem() != null) {
            throw new IllegalArgumentException("newItem.getWorldItem() != null");
        }
        this.getItem().setWorldItem(this);
        this.setKeyId(this.getItem().getKeyId());
        this.setName(this.getItem().getName());
        if (this.item != null && this.item.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.TransferComponent((GameEntity)this.item, (GameEntity)this, (ComponentType)ComponentType.FluidContainer);
        }
        if (this.getItem().shouldUpdateInWorld()) {
            IsoWorld.instance.currentCell.addToProcessWorldItems(this);
        }
        IsoWorld.instance.currentCell.addToProcessItems(newItem);
        this.updateSprite();
        LuaEventManager.triggerEvent((String)"OnContainerUpdate");
        if (GameServer.server) {
            this.sendObjectChange(IsoObjectChange.SWAP_ITEM);
        }
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.SWAP_ITEM) {
            if (this.getItem() == null) {
                return;
            }
            try {
                this.getItem().saveWithSize(bb.bb, false);
            }
            catch (Exception ex) {
                ExceptionLogger.logException((Throwable)ex);
            }
        } else {
            super.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.SWAP_ITEM) {
            try {
                InventoryItem newItem = InventoryItem.loadItem(bb.bb, 249);
                if (newItem != null) {
                    this.swapItem(newItem);
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException((Throwable)ex);
            }
        } else {
            super.loadChange(change, bb);
        }
    }

    private boolean isWaterSource() {
        if (this.item == null) {
            return false;
        }
        if (this.item.isBroken()) {
            // empty if block
        }
        if (this.hasComponent(ComponentType.FluidContainer)) {
            return this.getFluidContainer().getRainCatcher() > 0.0f;
        }
        return false;
    }

    public boolean isPureWater(boolean includeTainted) {
        FluidContainer fc = this.getFluidContainer();
        if (fc != null && fc.getAmount() > 0.0f) {
            float totalWater = fc.getSpecificFluidAmount(Fluid.Water);
            if (includeTainted) {
                totalWater += fc.getSpecificFluidAmount(Fluid.TaintedWater);
            }
            return totalWater == fc.getAmount();
        }
        return false;
    }

    @Override
    public boolean hasWater() {
        FluidContainer fc = this.getFluidContainer();
        return fc != null && fc.getAmount() > 0.0f && fc.isAllCategory(FluidCategory.Water);
    }

    @Override
    public float getFluidAmount() {
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getAmount();
        }
        return 0.0f;
    }

    @Override
    public void emptyFluid() {
        if (this.getFluidContainer() != null && (GameServer.server || !GameServer.server && !GameClient.client)) {
            float old = this.getFluidAmount();
            FluidContainer fc = this.getFluidContainer();
            if (fc != null) {
                fc.Empty();
                this.sync();
                LuaEventManager.triggerEvent((String)"OnWaterAmountChange", (Object)this, (Object)Float.valueOf(old));
            }
        }
    }

    @Override
    public float useFluid(float amount) {
        if (this.getFluidContainer() != null) {
            FluidContainer fc;
            float avail = this.getFluidAmount();
            float used = PZMath.clamp((float)amount, (float)0.0f, (float)avail);
            if ((GameServer.server || !GameServer.server && !GameClient.client) && used > 0.0f && (fc = this.getFluidContainer()) != null) {
                fc.removeFluid(used);
                this.sync();
                LuaEventManager.triggerEvent((String)"OnWaterAmountChange", (Object)this, (Object)Float.valueOf(avail));
            }
            return used;
        }
        return 0.0f;
    }

    @Override
    public void addFluid(FluidType fluidType, float amount) {
        if (this.getFluidContainer() != null && (GameServer.server || !GameServer.server && !GameClient.client)) {
            FluidContainer fc;
            float old = this.getFluidAmount();
            float freeCapacity = this.getFluidCapacity() - this.getFluidAmount();
            if ((amount = PZMath.clamp((float)amount, (float)0.0f, (float)freeCapacity)) > 0.0f && (fc = this.getFluidContainer()) != null) {
                fc.addFluid(fluidType, amount);
                this.sync();
                LuaEventManager.triggerEvent((String)"OnWaterAmountChange", (Object)this, (Object)Float.valueOf(old));
            }
        }
    }

    @Override
    public boolean canTransferFluidFrom(FluidContainer other) {
        FluidContainer fc = this.getFluidContainer();
        if (fc != null) {
            return FluidContainer.CanTransfer((FluidContainer)other, (FluidContainer)fc);
        }
        return false;
    }

    @Override
    public boolean canTransferFluidTo(FluidContainer other) {
        FluidContainer fc = this.getFluidContainer();
        if (fc != null) {
            return FluidContainer.CanTransfer((FluidContainer)fc, (FluidContainer)other);
        }
        return false;
    }

    @Override
    public float transferFluidTo(FluidContainer target, float amount) {
        if (this.getFluidContainer() != null) {
            FluidContainer fc;
            if (target == null) {
                return 0.0f;
            }
            float sourceAvail = this.getFluidAmount();
            float targetCapacity = target.getFreeCapacity();
            float transferMax = Math.min(targetCapacity, sourceAvail);
            float used = PZMath.clamp((float)amount, (float)0.0f, (float)transferMax);
            if ((GameServer.server || !GameServer.server && !GameClient.client) && used > 0.0f && (fc = this.getFluidContainer()) != null) {
                fc.transferTo(target, used);
                this.sync();
                LuaEventManager.triggerEvent((String)"OnWaterAmountChange", (Object)this, (Object)Float.valueOf(sourceAvail));
                return used;
            }
            return used;
        }
        return 0.0f;
    }

    @Override
    public float transferFluidFrom(FluidContainer source, float amount) {
        if (this.getFluidContainer() != null) {
            if (source == null) {
                return 0.0f;
            }
            float sourceAvail = source.getAmount();
            float targetCapacity = this.getFluidCapacity() - this.getFluidAmount();
            float transferMax = Math.min(targetCapacity, sourceAvail);
            float used = PZMath.clamp((float)amount, (float)0.0f, (float)transferMax);
            if (GameServer.server || !GameServer.server && !GameClient.client) {
                FluidContainer fc;
                float old = this.getFluidAmount();
                if (used > 0.0f && (fc = this.getFluidContainer()) != null) {
                    fc.transferFrom(source, used);
                    this.sync();
                    LuaEventManager.triggerEvent((String)"OnWaterAmountChange", (Object)this, (Object)Float.valueOf(old));
                }
            }
            return used;
        }
        return 0.0f;
    }

    @Override
    public float getFluidCapacity() {
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getCapacity();
        }
        return 0.0f;
    }

    @Override
    public boolean isFluidInputLocked() {
        FluidContainer fc = this.getFluidContainer();
        if (fc != null) {
            return fc.isInputLocked();
        }
        return true;
    }

    @Override
    public boolean isTaintedWater() {
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().isTainted();
        }
        return false;
    }

    @Override
    public String getFluidUiName() {
        if (this.getFluidCapacity() > 0.0f) {
            return this.getFluidContainer().getUiName();
        }
        return Translator.getText((String)"Fluid_HoldingOneType", (Object[])new Object[]{this.getFluidContainer().getTranslatedContainerName(), Translator.getText((String)"Fluid_Empty", (Object[])new Object[0])});
    }

    public String getCustomMenuOption() {
        if (this.getItem() != null) {
            return this.getItem().getCustomMenuOption();
        }
        return null;
    }

    @Override
    public void update() {
        IsoCell cell = IsoWorld.instance.getCell();
        if (!this.removeProcess && this.item != null && this.item.shouldUpdateInWorld()) {
            cell.addToProcessItems(this.item);
        }
    }

    public void updateSprite() {
        Texture tex;
        this.sprite.setTintMod(new ColorInfo(this.item.col.r, this.item.col.g, this.item.col.b, this.item.col.a));
        if (GameServer.server && !ServerGUI.isCreated()) {
            return;
        }
        String str = this.item.getTex().getName();
        if (this.item.isUseWorldItem()) {
            str = this.item.getWorldTexture();
        }
        try {
            tex = Texture.getSharedTexture((String)str);
            if (tex == null) {
                str = this.item.getTex().getName();
            }
        }
        catch (Exception ex) {
            str = "media/inventory/world/WItem_Sack.png";
        }
        tex = this.sprite.LoadSingleTexture(str);
        if (this.item.getScriptItem() == null) {
            this.sprite.def.scaleAspect((float)tex.getWidthOrig(), (float)tex.getHeightOrig(), (float)(16 * Core.tileScale), (float)(16 * Core.tileScale));
        } else {
            float scale = this.item.getScriptItem().scaleWorldIcon * ((float)Core.tileScale / 2.0f);
            this.sprite.def.setScale(scale, scale);
        }
    }

    public boolean finishupdate() {
        return this.removeProcess || this.item == null || !this.item.shouldUpdateInWorld();
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        Texture tex;
        String str;
        this.xoff = input.getFloat();
        this.yoff = input.getFloat();
        this.zoff = input.getFloat();
        float offsetX = input.getFloat();
        float offsetY = input.getFloat();
        this.sprite = IsoSprite.CreateSprite((IsoSpriteManager)IsoSpriteManager.instance);
        this.item = InventoryItem.loadItem(input, worldVersion);
        if (this.item == null) {
            input.getDouble();
            BitHeaderRead bits = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)input);
            bits.release();
            return;
        }
        this.item.setWorldItem(this);
        this.sprite.getTintMod().r = this.item.getR();
        this.sprite.getTintMod().g = this.item.getG();
        this.sprite.getTintMod().b = this.item.getB();
        this.dropTime = input.getDouble();
        BitHeaderRead bits = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)input);
        this.ignoreRemoveSandbox = bits.hasFlags(1);
        if (bits.hasFlags(2)) {
            this.loadEntity(input, worldVersion);
            FluidContainer fluidComponent = this.getFluidContainer();
            if (fluidComponent != null) {
                fluidComponent.setNonSavedFieldsFromItemScript(this.item);
            }
        }
        this.setExtendedPlacement(bits.hasFlags(4));
        bits.release();
        if (GameServer.server && !ServerGUI.isCreated()) {
            return;
        }
        String string = str = this.item.getTex() == null ? "media/inventory/world/WItem_Sack.png" : this.item.getTex().getName();
        if (this.item.isUseWorldItem()) {
            str = this.item.getWorldTexture();
        }
        try {
            tex = Texture.getSharedTexture((String)str);
            if (tex == null) {
                str = this.item.getTex().getName();
            }
        }
        catch (Exception ex) {
            str = "media/inventory/world/WItem_Sack.png";
        }
        tex = this.sprite.LoadSingleTexture(str);
        if (tex == null) {
            return;
        }
        if (this.item.getScriptItem() == null) {
            this.sprite.def.scaleAspect((float)tex.getWidthOrig(), (float)tex.getHeightOrig(), (float)(16 * Core.tileScale), (float)(16 * Core.tileScale));
        } else {
            float scale = this.item.getScriptItem().scaleWorldIcon * ((float)Core.tileScale / 2.0f);
            this.sprite.def.setScale(scale, scale);
        }
        if (this.item != null && this.item.hasComponent(ComponentType.FluidContainer) && this.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.RemoveComponent((GameEntity)this.item, (Component)this.item.getComponent(ComponentType.FluidContainer));
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.put(this.Serialize() ? (byte)1 : 0);
        if (!this.Serialize()) {
            return;
        }
        output.put(IsoObject.factoryGetClassID(this.getObjectName()));
        output.putFloat(this.xoff);
        output.putFloat(this.yoff);
        output.putFloat(this.zoff);
        output.putFloat(this.offsetX);
        output.putFloat(this.offsetY);
        this.item.saveWithSize(output, false);
        output.putDouble(this.dropTime);
        BitHeaderWrite bits = BitHeader.allocWrite((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)output);
        if (this.ignoreRemoveSandbox) {
            bits.addFlags(1);
        }
        if (this.requiresEntitySave()) {
            bits.addFlags(2);
            this.saveEntity(output);
        }
        if (this.isExtendedPlacement()) {
            bits.addFlags(4);
        }
        bits.write();
        bits.release();
    }

    @Override
    public void softReset() {
        this.square.removeWorldObject(this);
    }

    @Override
    public String getObjectName() {
        return "WorldInventoryItem";
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        this.item.DoTooltip(tooltipUI);
    }

    private void debugDrawLocation(float x, float y, float z) {
        if (PerformanceSettings.fboRenderChunk) {
            return;
        }
        if (Core.debug && DebugOptions.instance.model.render.axis.getValue()) {
            LineDrawer.DrawIsoLine((float)((x += this.xoff) - 0.25f), (float)(y += this.yoff), (float)(z += this.zoff), (float)(x + 0.25f), (float)y, (float)z, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.5f, (int)1);
            LineDrawer.DrawIsoLine((float)x, (float)(y - 0.25f), (float)z, (float)x, (float)(y + 0.25f), (float)z, (float)1.0f, (float)1.0f, (float)1.0f, (float)0.5f, (int)1);
        }
    }

    private void debugHitTest() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        float zoom = Core.getInstance().getZoom(playerIndex);
        float mx = Mouse.getXA();
        float my = Mouse.getYA();
        mx -= (float)IsoCamera.getScreenLeft((int)playerIndex);
        my -= (float)IsoCamera.getScreenTop((int)playerIndex);
        float sx = this.getScreenPosX(playerIndex) * zoom;
        float sy = this.getScreenPosY(playerIndex) * zoom;
        float dist = IsoUtils.DistanceTo2D((float)sx, (float)sy, (float)(mx *= zoom), (float)(my *= zoom));
        int radius = 48;
        if (dist < 48.0f) {
            LineDrawer.drawCircle((float)sx, (float)sy, (float)48.0f, (int)16, (float)1.0f, (float)1.0f, (float)1.0f);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (Core.debug) {
            // empty if block
        }
        if (!this.getItem().getScriptItem().isWorldRender().booleanValue()) {
            return;
        }
        ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain((InventoryItem)this.getItem(), (IsoGridSquare)this.getSquare(), (IsoGridSquare)this.getRenderSquare(), (float)(this.getX() + this.xoff), (float)(this.getY() + this.yoff), (float)(this.getZ() + this.zoff), (float)0.0f);
        if (status == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready) {
            this.debugDrawLocation(x, y, z);
            return;
        }
        if (this.sprite.hasNoTextures()) {
            return;
        }
        Texture tex = this.sprite.getTextureForCurrentFrame(this.getForwardIsoDirection(), (IsoObject)this);
        if (tex == null) {
            return;
        }
        float dx = (float)tex.getWidthOrig() * this.sprite.def.getScaleX() / 2.0f;
        float dy = (float)tex.getHeightOrig() * this.sprite.def.getScaleY() * 3.0f / 4.0f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        float oldAlpha = this.getAlpha(playerIndex);
        float oldTargetAlpha = this.getTargetAlpha(playerIndex);
        float alpha = PZMath.min((float)IsoWorldInventoryObject.getSurfaceAlpha(this.square, this.zoff), (float)oldAlpha);
        this.setAlphaAndTarget(playerIndex, alpha);
        float r = col.r;
        float g = col.g;
        float b = col.b;
        float a = col.a;
        if (this.isHighlighted(playerIndex)) {
            ColorInfo highlightColor = Core.getInstance().getWorldItemHighlightColor();
            col.r = r * (1.0f - a) + highlightColor.r * a;
            col.g = g * (1.0f - a) + highlightColor.g * a;
            col.b = b * (1.0f - a) + highlightColor.b * a;
        }
        this.sprite.render((IsoObject)this, x + this.xoff, y + this.yoff, z + this.zoff, this.getForwardIsoDirection(), this.offsetX + dx, this.offsetY + dy, col, true);
        col.set(r, g, b, a);
        this.setAlpha(playerIndex, oldAlpha);
        this.setTargetAlpha(playerIndex, oldTargetAlpha);
        this.debugDrawLocation(x, y, z);
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
        if (this.sprite == null) {
            return;
        }
        if (this.sprite.hasNoTextures()) {
            return;
        }
        Texture tex = this.sprite.getTextureForCurrentFrame(this.getForwardIsoDirection(), (IsoObject)this);
        if (tex == null) {
            return;
        }
        float dx = tex.getWidthOrig() / 2;
        float dy = tex.getHeightOrig();
        this.sprite.renderObjectPicker(this.sprite.def, (IsoObject)this, this.getForwardIsoDirection());
    }

    public InventoryItem getItem() {
        return this.item;
    }

    @Override
    public void addToWorld() {
        InventoryContainer inventoryContainer;
        ItemContainer container;
        InventoryItem inventoryItem;
        if (this.item != null && this.item.shouldUpdateInWorld() && !IsoWorld.instance.currentCell.getProcessWorldItems().contains(this)) {
            IsoWorld.instance.currentCell.getProcessWorldItems().add(this);
        }
        if ((inventoryItem = this.item) instanceof InventoryContainer && (container = (inventoryContainer = (InventoryContainer)inventoryItem).getInventory()) != null) {
            container.addItemsToProcessItems();
        }
        super.addToWorld();
    }

    @Override
    public void removeFromWorld() {
        InventoryContainer inventoryContainer;
        ItemContainer container;
        InventoryItem inventoryItem;
        this.removeProcess = true;
        IsoWorld.instance.getCell().getProcessWorldItems().remove(this);
        if (this.item != null) {
            IsoWorld.instance.currentCell.addToProcessItemsRemove(this.item);
            ItemSoundManager.removeItem((InventoryItem)this.item);
            this.item.atlasTexture = null;
        }
        if ((inventoryItem = this.item) instanceof InventoryContainer && (container = (inventoryContainer = (InventoryContainer)inventoryItem).getInventory()) != null) {
            container.removeItemsFromProcessItems();
        }
        super.removeFromWorld();
    }

    @Override
    public void removeFromSquare() {
        if (this.hasComponent(ComponentType.FluidContainer)) {
            GameEntityFactory.TransferComponent((GameEntity)this, (GameEntity)this.item, (ComponentType)ComponentType.FluidContainer);
        }
        if (this.square != null) {
            this.square.getWorldObjects().remove(this);
        }
        super.removeFromSquare();
    }

    public float getScreenPosX(int playerIndex) {
        float screenX = IsoUtils.XToScreen((float)(this.getX() + this.xoff), (float)(this.getY() + this.yoff), (float)(this.getZ() + this.zoff), (int)0);
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        return (screenX - camera.getOffX()) / Core.getInstance().getZoom(playerIndex);
    }

    public float getScreenPosY(int playerIndex) {
        Texture tex = this.sprite == null ? null : this.sprite.getTextureForCurrentFrame(this.getForwardIsoDirection(), (IsoObject)this);
        float dy = tex == null ? 0.0f : (float)tex.getHeightOrig() * this.sprite.def.getScaleY() * 1.0f / 4.0f;
        float screenY = IsoUtils.YToScreen((float)(this.getX() + this.xoff), (float)(this.getY() + this.yoff), (float)(this.getZ() + this.zoff), (int)0);
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        return (screenY - camera.getOffY() - dy) / Core.getInstance().getZoom(playerIndex);
    }

    public void setIgnoreRemoveSandbox(boolean b) {
        this.ignoreRemoveSandbox = b;
    }

    public boolean isIgnoreRemoveSandbox() {
        return this.ignoreRemoveSandbox;
    }

    public void setExtendedPlacement(boolean b) {
        this.extendedPlacement = b;
    }

    public boolean isExtendedPlacement() {
        return this.extendedPlacement;
    }

    public float getWorldPosX() {
        return this.getX() + this.xoff;
    }

    public float getWorldPosY() {
        return this.getY() + this.yoff;
    }

    public float getWorldPosZ() {
        return this.getZ() + this.zoff;
    }

    public static float getSurfaceAlpha(IsoGridSquare square, float zoff) {
        return IsoWorldInventoryObject.getSurfaceAlpha(square, zoff, false);
    }

    public static float getSurfaceAlpha(IsoGridSquare square, float zoff, boolean bTargetAlpha) {
        if (square == null) {
            return 1.0f;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        float alpha = 1.0f;
        if (zoff > 0.01f) {
            boolean bHasSurface = false;
            for (int i = 0; i < square.getObjects().size(); ++i) {
                IsoObject obj = (IsoObject)square.getObjects().get(i);
                if (!(obj.getSurfaceOffsetNoTable() > 0.0f) || obj.getProperties().isSurfaceOffset()) continue;
                if (!bHasSurface) {
                    bHasSurface = true;
                    alpha = 0.0f;
                }
                alpha = bTargetAlpha ? PZMath.max((float)alpha, (float)obj.getRenderInfo((int)playerIndex).targetAlpha) : PZMath.max((float)alpha, (float)obj.getAlpha(playerIndex));
            }
        }
        return alpha;
    }

    public void setOffset(float x, float y, float z) {
        if (x == this.xoff && y == this.yoff && z == this.zoff) {
            return;
        }
        this.xoff = x;
        this.yoff = y;
        this.zoff = z;
        this.sync(1);
        this.invalidateRenderChunkLevel(256L);
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(this.square.getObjects().indexOf((Object)this));
        b.putBoolean(true);
        b.putBoolean(true);
        b.putFloat(this.xoff);
        b.putFloat(this.yoff);
        b.putFloat(this.zoff);
        this.syncFluidContainerSend(b);
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBufferReader bb) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
            return;
        }
        if (this.getObjectIndex() == -1) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " not found on square " + this.square.getX() + "," + this.square.getY() + "," + this.square.getZ());
            return;
        }
        if (GameClient.client && !bRemote) {
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.SyncIsoObject.doPacket(b);
            this.syncIsoObjectSend(b);
            PacketTypes.PacketType.SyncIsoObject.send((IConnection)GameClient.connection);
        } else if (GameServer.server && !bRemote) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send((IConnection)connection);
            }
        } else if (bRemote) {
            this.xoff = bb.getFloat();
            this.yoff = bb.getFloat();
            this.zoff = bb.getFloat();
            this.syncFluidContainerReceive(bb);
            if (GameServer.server) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    if (source == null || connection.getConnectedGUID() == source.getConnectedGUID()) continue;
                    ByteBufferWriter b = connection.startPacket();
                    PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                    this.syncIsoObjectSend(b);
                    PacketTypes.PacketType.SyncIsoObject.send((IConnection)connection);
                }
            }
            this.invalidateRenderChunkLevel(256L);
            this.square.RecalcProperties();
        }
        this.flagForHotSave();
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        }
        int chunksPerWidth = 8;
        if (PZMath.coordmodulo((int)this.square.x, (int)8) == 0 && PZMath.coordmodulo((int)this.square.y, (int)8) == 7) {
            return this.square.getAdjacentSquare(IsoDirections.S);
        }
        if (PZMath.coordmodulo((int)this.square.x, (int)8) == 7 && PZMath.coordmodulo((int)this.square.y, (int)8) == 0) {
            return this.square.getAdjacentSquare(IsoDirections.E);
        }
        return this.getSquare();
    }

    @Override
    public void setHighlighted(int playerIndex, boolean bHighlight, boolean bRenderOnce) {
        super.setHighlighted(playerIndex, bHighlight, bRenderOnce);
        ColorInfo highlightColor = Core.getInstance().getWorldItemHighlightColor();
        this.setHighlightColor(playerIndex, highlightColor.r, highlightColor.g, highlightColor.b, 1.0f);
        this.setOutlineHighlight(playerIndex, bHighlight);
        this.setOutlineHighlightCol(playerIndex, highlightColor);
    }

    @Override
    public boolean couldBePoweredByGenerator() {
        return false;
    }

    public float getOffX() {
        return this.xoff;
    }

    public float getOffY() {
        return this.yoff;
    }

    public float getOffZ() {
        return this.zoff;
    }

    public void setOffX(float newoff) {
        this.xoff = newoff;
    }

    public void setOffY(float newoff) {
        this.yoff = newoff;
    }

    public void setOffZ(float newoff) {
        this.zoff = newoff;
    }

    public void syncExtendedPlacement() {
        if (this.getWorldObjectIndex() == -1) {
            return;
        }
        if (GameClient.client) {
            INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.SyncExtendedPlacement, (Object[])new Object[]{this});
        } else if (GameServer.server) {
            INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.SyncExtendedPlacement, (float)this.getX(), (float)this.getY(), (Object[])new Object[]{this});
        }
    }
}
