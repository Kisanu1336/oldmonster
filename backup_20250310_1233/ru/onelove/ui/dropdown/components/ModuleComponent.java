package ru.onelove.ui.dropdown.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.impl.render.HUD;
import ru.onelove.functions.settings.Setting;
import ru.onelove.functions.settings.impl.*;
import ru.onelove.ui.dropdown.components.settings.*;
import ru.onelove.ui.dropdown.impl.Component;
import ru.onelove.utils.client.KeyStorage;
import ru.onelove.utils.math.MathUtil;
import ru.onelove.utils.math.Vector4i;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.Cursors;
import ru.onelove.utils.render.DisplayUtils;
import ru.onelove.utils.render.Stencil;
import ru.onelove.utils.render.font.Fonts;
import ru.onelove.utils.text.GradientUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.awt.*;

@Getter
public class ModuleComponent extends Component {
    private final Vector4f ROUNDING_VECTOR = new Vector4f(5, 5, 5, 5);
    private final Vector4i BORDER_COLOR = new Vector4i(ColorUtils.rgb(18, 18, 18), ColorUtils.rgb(18, 18, 18), ColorUtils.rgb(18, 18, 18), ColorUtils.rgb(18, 18, 18));

    private final Function function;
    public Animation animation = new Animation();
    public boolean open;
    private boolean bind;

    private final ObjectArrayList<Component> components = new ObjectArrayList<>();

    public ModuleComponent(Function function) {
        this.function = function;
        for (Setting<?> setting : function.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                components.add(new BooleanComponent(bool));
            }
            if (setting instanceof SliderSetting slider) {
                components.add(new SliderComponent(slider));
            }
            if (setting instanceof BindSetting bind) {
                components.add(new BindComponent(bind));
            }
            if (setting instanceof ModeSetting mode) {
                components.add(new ModeComponent(mode));
            }
            if (setting instanceof ModeListSetting mode) {
                components.add(new MultiBoxComponent(mode));
            }
            if (setting instanceof StringSetting string) {
                components.add(new StringComponent(string));
            }
        }
        animation = animation.animate(open ? 1 : 0, 0.3);
    }

    public void drawComponents(MatrixStack stack, float mouseX, float mouseY) {
        if (animation.getValue() > 0) {
            if (animation.getValue() > 0.1 && components.stream().filter(Component::isVisible).count() >= 1) {
                DisplayUtils.drawRectVerticalW(getX() + 5, getY() + 20, getWidth() - 10, 0.5f, ColorUtils.rgb(255, 255, 255), ColorUtils.rgb(255, 255, 255));
            }
            Stencil.initStencilToWrite();
            DisplayUtils.drawRoundedRect(getX() + 0.5f, getY() + 0.5f, getWidth() - 1, getHeight() - 1, ROUNDING_VECTOR, ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)));
            Stencil.readStencilBuffer(1);
            float y = getY() + 20;
            for (Component component : components) {
                if (component.isVisible()) {
                    component.setX(getX());
                    component.setY(y);
                    component.setWidth(getWidth());
                    component.render(stack, mouseX, mouseY);
                    y += component.getHeight();
                }
            }
            Stencil.uninitStencilBuffer();
        }
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        for (Component component : components) {
            component.mouseRelease(mouseX, mouseY, mouse);
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    private boolean hovered = false;

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        int color = ColorUtils.interpolate(-1, ColorUtils.rgb(255, 255, 255), (float) function.getAnimation().getValue());

        function.getAnimation().update();
        super.render(stack, mouseX, mouseY);

        drawOutlinedRect(mouseX, mouseY, color);

        drawText(stack, color);
        drawComponents(stack, mouseX, mouseY);
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        if (isHovered(mouseX, mouseY, 20)) {
            if (button == 0) function.toggle();
            if (button == 1) {
                open = !open;
                animation = animation.animate(open ? 1 : 0, 0.2, Easings.CIRC_OUT);
            }
            if (button == 2) {
                bind = !bind;
            }
        }
        if (isHovered(mouseX, mouseY)) {
            if (open) {
                for (Component component : components) {
                    if (component.isVisible()) component.mouseClick(mouseX, mouseY, button);
                }
            }
        }
        super.mouseClick(mouseX, mouseY, button);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (Component component : components) {
            if (component.isVisible()) component.charTyped(codePoint, modifiers);
        }
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (Component component : components) {
            if (component.isVisible()) component.keyPressed(key, scanCode, modifiers);
        }
        if (bind) {
            if (key == GLFW.GLFW_KEY_DELETE) {
                function.setBind(0);
            } else function.setBind(key);
            bind = false;
        }
        super.keyPressed(key, scanCode, modifiers);
    }

    private void drawOutlinedRect(float mouseX, float mouseY, int color) {
        Stencil.initStencilToWrite();
        DisplayUtils.drawRoundedRect(getX() + 0.5f, getY() + 0.5f, getWidth() - 1, getHeight() - 1, ROUNDING_VECTOR, ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)));

        Stencil.readStencilBuffer(0);
        DisplayUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), ROUNDING_VECTOR, BORDER_COLOR);
        Stencil.uninitStencilBuffer();
        DisplayUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), ROUNDING_VECTOR, new Vector4i(ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)), ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)), ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)), ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33))));
        DisplayUtils.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), ROUNDING_VECTOR, ColorUtils.rgba(18, 18, 18, (int) (255 * 0.33)));

        if (MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), 20)) {
            if (!hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
                hovered = true;
            }
        } else {
            if (hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
                hovered = false;
            }
        }
    }

    private void drawText(MatrixStack stack, int color) {
        String name = function.getName();
        String statusText = function.isState() ? "[ON]" : "[OFF]";
        int statusColor = function.isState() ? ColorUtils.rgb(0, 255, 0) : ColorUtils.rgb(255, 0, 0);
        int whiteColor = ColorUtils.rgb(255, 255, 255);

        // Draw function name in white
        Fonts.sfui.drawText(stack, name, getX() + 6, getY() + 6.5f, whiteColor, 8);

        // Draw status text (colored)
        float statusX = getX() + 6 + Fonts.sfui.getWidth(name, 8) + 5;
        Fonts.sfui.drawText(stack, statusText, statusX, getY() + 6.5f, statusColor, 8);

        DisplayUtils.drawShadow(getX() + 6, getY() + 6.5f,
                Fonts.sfui.getWidth(function.getName(), 8) + 3,
                Fonts.sfui.getHeight(8),
                10,
                ColorUtils.setAlpha(color, (int) (0 * function.getAnimation().getValue())));

        // Draw bind if in bind mode
        if (bind) {
            Fonts.montserrat.drawText(stack,
                    function.getBind() == 0 ? "..." : KeyStorage.getReverseKey(function.getBind()),
                    getX() + getWidth() - 6 - Fonts.montserrat.getWidth(
                            function.getBind() == 0 ? "..." : KeyStorage.getReverseKey(function.getBind()),
                            6, 0.1f),
                    getY() + Fonts.icons.getHeight(6) + 1,
                    whiteColor,
                    6, 0.1f);
        }
    }
}