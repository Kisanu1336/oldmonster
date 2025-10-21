package ru.onelove.ui.display.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import ru.onelove.onelove;
import ru.onelove.events.EventDisplay;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.impl.render.HUD;
import ru.onelove.ui.display.ElementRenderer;
import ru.onelove.ui.display.ElementUpdater;
import ru.onelove.utils.math.StopWatch;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.DisplayUtils;
import ru.onelove.utils.render.font.Fonts;
import net.minecraft.util.math.vector.Vector4f;
import ru.hogoshi.Animation;
import ru.onelove.utils.client.ClientUtil;
import ru.onelove.ui.styles.Style;

import java.util.List;

public class ArrayListRenderer implements ElementRenderer, ElementUpdater {

    private int lastIndex;
    List<Function> list;
    StopWatch stopWatch = new StopWatch();

    @Override
    public void update(EventUpdate e) {
        if (stopWatch.isReached(1000)) {
            list = onelove.getInstance().getFunctionRegistry().getSorted(Fonts.sfui, 9 - 1.5f)
                    .stream()
                    .toList(); // Убраны фильтры, теперь показываются все модули
            stopWatch.reset();
        }
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        MatrixStack ms = eventDisplay.getMatrixStack();
        Style style = onelove.getInstance().getStyleManager().getCurrentStyle();

        float rounding = 6;
        float padding = 5f; // Увеличено расстояние
        float elementSpacing = 2f; // Расстояние между элементами
        float posX = 4;
        float posY = 4 + 28;
        int index = 0;

        if (list == null) return;


        for (Function f : list) {
            float fontSize = 7f; 
            Animation anim = f.getAnimation();
            float value = (float) anim.getValue();
            String text = f.getName();
            float textWidth = Fonts.sfui.getWidth(text, fontSize);

            if (value != 0) {
                float localFontSize = fontSize * value;
                float localTextWidth = textWidth * value;

                posY += (localFontSize + padding * 2 + elementSpacing) * value;
                index++;
            }
        }


        index = 0;
        posY = 4 + 28;
        for (Function f : list) {
            float fontSize = 7f;
            Animation anim = f.getAnimation();
            anim.update();

            float value = (float) anim.getValue();
            String text = f.getName();
            float textWidth = Fonts.sfui.getWidth(text, fontSize);

            if (value != 0) {
                float localFontSize = fontSize * value;
                float localTextWidth = textWidth * value;

                boolean isFirst = index == 0;
                boolean isLast = index == lastIndex;

                float localRounding = rounding;

                for (Function f2 : list.subList(list.indexOf(f) + 1, list.size())) {
                    if (f2.getAnimation().getValue() != 0) {
                        localRounding = isLast ? rounding : Math.min(textWidth - Fonts.sfui.getWidth(f2.getName(), fontSize), rounding);
                        break;
                    }
                }


                drawStyledModuleRect(posX, posY, localTextWidth + padding * 2 + 10, localFontSize + padding * 2,
                        localRounding, ColorUtils.setAlpha(ColorUtils.rgb(21, 21, 21), (int) (255 * value)));


                float iconSpacing = 3.0f;
                float iconPosX = posX + padding;
                float textPosX = iconPosX + 10 + iconSpacing;


                Fonts.icons2.drawText(ms, getCategoryIcon(f.getCategory()), iconPosX, posY + padding + 1f,
                        ColorUtils.setAlpha(style.getSecondColor().getRGB(), (int) (255 * value)), localFontSize);


                Fonts.sfui.drawText(ms, text, textPosX, posY + padding,
                        ColorUtils.setAlpha(ColorUtils.rgb(255, 255, 255), (int) (255 * value)), localFontSize);

                posY += (localFontSize + padding * 2 + elementSpacing) * value;
                index++;
            }
        }

        lastIndex = index - 1;
    }

    private void drawStyledModuleRect(float x, float y, float width, float height, float radius, int color) {

        DisplayUtils.drawRoundedRect(x - 0.5f, y - 0.5f, width + 1, height + 1, radius + 0.5f,
                ColorUtils.setAlpha(ColorUtils.rgb(10, 15, 13), 90));


        DisplayUtils.drawRoundedRect(x, y, width, height, radius, color);


        DisplayUtils.drawShadow(x + 2, y + 2, width, height, 5, ColorUtils.rgba(10, 15, 13, 15));
    }

    private String getCategoryIcon(Category category) {

        switch (category) {
            case Combat: return "⚔"; // Мечи
            case Movement: return "➤"; // Стрелка
            case Player: return "⛁"; // Человек
            case Render: return "☘"; // Звезда
            case Misc: return "⚐"; // Флажок
            default: return "◉"; // Круг
        }
    }
}