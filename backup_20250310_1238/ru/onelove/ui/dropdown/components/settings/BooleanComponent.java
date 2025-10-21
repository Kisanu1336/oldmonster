package ru.onelove.ui.dropdown.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.ui.dropdown.impl.Component;
import ru.onelove.utils.math.MathUtil;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.Cursors;
import ru.onelove.utils.render.DisplayUtils;
import ru.onelove.utils.render.font.Fonts;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

/**
 * BooleanComponent
 */
public class BooleanComponent extends Component {

    private final BooleanSetting setting;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        setHeight(16);
        animation = animation.animate(setting.get() ? 1 : 0, 0.2, Easings.CIRC_OUT);
    }

    private Animation animation = new Animation();
    private float width, height;
    private boolean hovered = false;

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        // TODO Auto-generated method stub
        super.render(stack, mouseX, mouseY);
        animation.update();
        Fonts.montserrat.drawText(stack, setting.getName(), getX() + 5, getY() + 6.5f / 2f + 1, ColorUtils.rgb(45, 45, 45), 6.5f, 0.05f);

        width = 15;
        height = 7;
        DisplayUtils.drawRoundedRect(getX() + getWidth() - width - 7, getY() + getHeight() / 2f - height / 2f, width,
                height, 3f, ColorUtils.rgb(34, 34, 34));
        int color = ColorUtils.interpolate(ColorUtils.rgb(18, 18, 18), ColorUtils.rgb(18, 18, 18), 1 - (float) animation.getValue());
        DisplayUtils.drawCircle((float) (getX() + getWidth() - width - 7 + 4 + (7 * animation.getValue())),
                getY() + getHeight() / 2f - height / 2f + 3.5f,
                5f, color);

        DisplayUtils.drawShadowCircle((float) (getX() + getWidth() - width - 7 + 4 + (7 * animation.getValue())),
                getY() + getHeight() / 2f - height / 2f + 3.5f,
                7f, ColorUtils.setAlpha(color, (int) (128 * animation.getValue())));
        
        if (isHovered(mouseX, mouseY)) {
            if (MathUtil.isHovered(mouseX, mouseY, getX() + getWidth() - width - 7, getY() + getHeight() / 2f - height / 2f, width,
                    height)) {
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
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        // TODO Auto-generated method stub
        if (MathUtil.isHovered(mouseX, mouseY, getX() + getWidth() - width - 7, getY() + getHeight() / 2f - height / 2f, width,
                height)) {
            setting.set(!setting.get());
            animation = animation.animate(setting.get() ? 1 : 0, 0.2, Easings.CIRC_OUT);
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }

}