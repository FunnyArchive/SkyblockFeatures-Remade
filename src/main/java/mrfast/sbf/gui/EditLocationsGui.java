package mrfast.sbf.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import mrfast.sbf.SkyblockFeatures;
import mrfast.sbf.gui.components.MoveableFeature;
import mrfast.sbf.gui.components.UIElement;
import mrfast.sbf.utils.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class EditLocationsGui extends GuiScreen {

    private float xOffset;
    private float yOffset;
    int screenWidth = Utils.GetMC().displayWidth/2;
    int screenHeight = Utils.GetMC().displayHeight/2;
    private UIElement dragging;
    private final Map<UIElement, MoveableFeature> MoveableFeatures = new HashMap<>();

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        for (Map.Entry<Integer, UIElement> e : SkyblockFeatures.GUIMANAGER.getElements().entrySet()) {
            MoveableFeature lb = new MoveableFeature(e.getValue());
            this.buttonList.add(lb);
            this.MoveableFeatures.put(e.getValue(), lb);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        onMouseMove();
        this.drawGradientRect(0, 0, this.width, this.height, new Color(0, 0, 0, 50).getRGB(), new Color(0, 0, 0, 200).getRGB());
        for (GuiButton button : this.buttonList) {
            if (button instanceof MoveableFeature) {
                if (((MoveableFeature) button).element.getToggled()) {
                    MoveableFeature moveableFeature = (MoveableFeature) button;
                    GlStateManager.pushMatrix();

                    GlStateManager.translate(moveableFeature.x * screenWidth, moveableFeature.y * screenHeight, 0);

                    button.drawButton(this.mc, mouseX, mouseY);
                    GlStateManager.popMatrix();

                }
            } else {
                button.drawButton(this.mc, mouseX, mouseY);
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button instanceof MoveableFeature) {
            MoveableFeature lb = (MoveableFeature) button;
            dragging = lb.getElement();

            ScaledResolution sr = new ScaledResolution(mc);
            float minecraftScale = sr.getScaleFactor();
            float floatMouseX = Mouse.getX() / minecraftScale;
            float floatMouseY = (mc.displayHeight - Mouse.getY()) / minecraftScale;

            xOffset = floatMouseX - dragging.getX() * screenWidth;
            yOffset = floatMouseY - dragging.getY() * screenHeight;
        }
    }

    protected void onMouseMove() {
        ScaledResolution sr = new ScaledResolution(mc);
        float minecraftScale = sr.getScaleFactor();
        float floatMouseX = Mouse.getX() / minecraftScale;
        float floatMouseY = (mc.displayHeight - Mouse.getY()) / minecraftScale;
        
        if (dragging != null) {
            MoveableFeature lb = MoveableFeatures.get(dragging);
            if (lb == null) {
                return;
            }
            float x = floatMouseX-xOffset;
            float y = floatMouseY-yOffset;
            float x2 = (floatMouseX - xOffset+dragging.getWidth());
            float y2 = (floatMouseY - yOffset+dragging.getHeight());
            if(x<2) x = 2;
            if(y<2) y = 2;
            if(x2+2>sr.getScaledWidth()) x = sr.getScaledWidth()-dragging.getWidth()-2;
            if(y2+2>sr.getScaledHeight()) y = sr.getScaledHeight()-dragging.getHeight()-2;

            dragging.setPos(x/screenWidth, y/screenHeight);
        }
    }

    /**
     * Reset the dragged feature when the mouse is released.
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        dragging = null;
    }

    /**
     * Saves the positions when the gui is closed
     */
    @Override
    public void onGuiClosed() {
        GuiManager.saveConfig();
    }
}
