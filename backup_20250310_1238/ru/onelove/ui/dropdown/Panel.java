package ru.onelove.ui.dropdown;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import ru.onelove.onelove;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.ui.dropdown.components.ModuleComponent;
import ru.onelove.ui.dropdown.impl.Component;
import ru.onelove.ui.dropdown.impl.IBuilder;
import ru.onelove.utils.math.MathUtil;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.DisplayUtils;
import ru.onelove.utils.render.Scissor;
import ru.onelove.utils.render.Stencil;
import ru.onelove.utils.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Panel implements IBuilder {

    private final Category category;
    protected float x;
    protected float y;
    protected final float width = 120;
    protected final float height = 600 / 2f;
    private float scroll, animatedScrool;
    private boolean draggingScrollbar = false;
    private float lastMouseY;

    private List<ModuleComponent> modules = new ArrayList<>();
    private DisplayUtils RectUtils;

    public Panel(Category category) {
        this.category = category;

        for (Function function : onelove.getInstance().getFunctionRegistry().getFunctions()) {
            if (function.getCategory() == category) {
                ModuleComponent component = new ModuleComponent(function);
                component.setPanel(this);
                modules.add(component);
            }
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        animatedScrool = MathUtil.fast(animatedScrool, scroll, 10);
        float header = 55 / 2.3f;
        float headerFont = 9;

        // Основной чёрный фон панели (полностью чёрный без эффектов)
        DisplayUtils.drawRoundedRect(x, y, width, height, 7, ColorUtils.rgba(0, 0, 0, 255));

        // Чёрный фон заголовка (полностью чёрный без эффектов)
        DisplayUtils.drawRoundedRect(x, y, width, header, 7, ColorUtils.rgba(0, 0, 0, 255));

        Fonts.montserrat.drawCenteredText(stack, category.name(), x + width / 2f,
                y + header / 2f - Fonts.montserrat.getHeight(headerFont) / 2f - 1,
                ColorUtils.rgba(255, 255, 255, 255), headerFont, 0.2f);

        drawComponents(stack, mouseX, mouseY);

        drawOutline();
    }

    protected void drawOutline() {
        Stencil.initStencilToWrite();
        // Чёрный контур (полностью чёрный без эффектов)
        DisplayUtils.drawRoundedRect(x + 0.5f, y + 0.5f, width - 1, height - 1, new Vector4f(7, 7, 7, 7),
                ColorUtils.rgba(0, 0, 0, 255));
        Stencil.readStencilBuffer(0);
        Stencil.uninitStencilBuffer();
    }

    float max = 0;

    private void drawComponents(MatrixStack stack, float mouseX, float mouseY) {
        float animationValue = (float) DropDown.getAnimation().getValue() * DropDown.scale;

        float halfAnimationValueRest = (1 - animationValue) / 2f;
        float height = getHeight();
        float testX = getX() + (getWidth() * halfAnimationValueRest);
        float testY = getY() + 55 / 2f + (height * halfAnimationValueRest);
        float testW = getWidth() * animationValue;
        float testH = height * animationValue;

        testX = testX * animationValue + ((Minecraft.getInstance().getMainWindow().getScaledWidth() - testW) *
                halfAnimationValueRest);

        Scissor.push();
        Scissor.setFromComponentCoordinates(testX, testY, testW, testH-34);
        float offset = 0;
        float header = 55 / 2f;

        if (max > height - header - 10) {
            scroll = MathHelper.clamp(scroll, -max + height - header - 10, 0);
            animatedScrool = MathHelper.clamp(animatedScrool, -max + height - header - 10, 0);
        }
        else {
            scroll = 0;
            animatedScrool = 0;
        }
        for (ModuleComponent component : modules) {
            if(onelove.getInstance().getDropDown().searchCheck(component.getFunction().getName())){
                continue;
            }
            component.setX(getX() + 5);
            component.setY(getY() + header + offset + 6 + animatedScrool);
            component.setWidth(getWidth() - 10);
            component.setHeight(20);
            component.animation.update();
            if (component.animation.getValue() > 0) {
                float componentOffset = 0;
                for (Component component2 : component.getComponents()) {
                    if (component2.isVisible())
                        componentOffset += component2.getHeight();
                }

                componentOffset *= component.animation.getValue();
                component.setHeight(component.getHeight() + componentOffset);
            }
            component.render(stack, mouseX, mouseY);
            offset += component.getHeight() + 3.5f;
            Scissor.setFromComponentCoordinates(testX, testY, testW, testH-34);
        }
        animatedScrool = MathUtil.fast(animatedScrool, scroll, 10);
        float scrollbarHeight = MathHelper.clamp((height - header - 10) * (height - header - 10) / max, 10, height - header - 10);
        float scrollbarY = getY() + header + (-getScroll() / (max - height + header + 4)) * (height - header - 4 - scrollbarHeight);
        scrollbarHeight = MathHelper.clamp(scrollbarHeight, 20, height - header - 10);
        scrollbarY = MathHelper.clamp(scrollbarY, getY() + header, getY() + height - scrollbarHeight - 4);

        if (max > height - header - 10) {
            setScroll(MathHelper.clamp(getScroll(), -max + height - header - 10, 0));
            setAnimatedScrool(MathHelper.clamp(animatedScrool, -max + height - header - 10, 0));

            if (scroll >= 0) {
                setScroll(0);
                setAnimatedScrool(0);
            }

            // Чёрный скроллбар (полностью чёрный без эффектов)
            RectUtils.drawRoundedRect(getX() + getWidth() - 2.5f, scrollbarY, 3.5f, scrollbarHeight, 1.5f, ColorUtils.rgba(0, 0, 0, 255));
        } else {
            setScroll(0);
            setAnimatedScrool(0);
        }

        max = offset;

        Scissor.unset();
        Scissor.pop();
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        for (ModuleComponent component : modules) {
            component.mouseClick(mouseX, mouseY, button);
        }

        if (button == 0) {
            float header = 55 / 2f;
            float scrollbarHeight = MathHelper.clamp((height - header - 10) * (height - header - 10) / max, 10, height - header - 10);
            float scrollbarY = getY() + header + (-getScroll() / (max - height + header + 4)) * (height - header - 4 - scrollbarHeight);
            scrollbarHeight = MathHelper.clamp(scrollbarHeight, 20, height - header - 10);
            scrollbarY = MathHelper.clamp(scrollbarY, getY() + header, getY() + height - scrollbarHeight - 4);

            if (mouseX >= getX() + getWidth() - 2.5f && mouseX <= getX() + getWidth() + 1.0f && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                draggingScrollbar = true;
                lastMouseY = mouseY;
            }
        }
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (ModuleComponent component : modules) {
            component.keyPressed(key, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (ModuleComponent component : modules) {
            component.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int button) {
        for (ModuleComponent component : modules) {
            component.mouseRelease(mouseX, mouseY, button);
        }
        if (button == 0) {
            draggingScrollbar = false;
        }
    }
}