package ru.onelove.ui.dropdown.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.DisplayUtils;
import ru.onelove.utils.render.font.Fonts;
import lombok.Setter;
import lombok.Getter;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.onelove.utils.math.MathUtil;

@Setter
@Getter
public class SearchField {
    private int x, y, width, height;
    private String text;
    private boolean isFocused;
    private boolean typing;
    private final String placeholder;

    public SearchField(int x, int y, int width, int height, String placeholder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        this.text = "";
        this.isFocused = false;
        this.typing = false;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Чёрный фон поля поиска
        DisplayUtils.drawRoundedRect(x, y, width, height, new Vector4f(5, 5, 5, 5), ColorUtils.rgba(0, 0, 0, 255));

        // Текст и курсор
        String textToDraw = text.isEmpty() && !typing ? placeholder : text;
        String cursor = typing && System.currentTimeMillis() % 1000 > 500 ? "_" : "";

        // Белый текст
        Fonts.montserrat.drawText(
                matrixStack,
                textToDraw + cursor,
                x + 5,
                y + (height - 8) / 2 + 1,
                ColorUtils.rgb(255, 255, 255),
                6
        );
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (isFocused) {
            text += codePoint;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isFocused && keyCode == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
            return true;
        }
        if(keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE){
            typing = false;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!MathUtil.isHovered((float) mouseX, (float) mouseY, x, y, width, height)){
            isFocused = false;
        }
        isFocused = MathUtil.isHovered((float) mouseX, (float) mouseY, x, y, width, height);
        typing = isFocused;
        return isFocused;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public void setFocused(boolean focused) {
        isFocused = focused;
    }
}