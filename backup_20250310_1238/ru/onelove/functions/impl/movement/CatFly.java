package ru.onelove.functions.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.math.StopWatch;
import ru.onelove.utils.player.InventoryUtil;
import ru.onelove.events.EventUpdate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * CatFly — универсальная версия, безопасно обходит разницы в сигнатурах настроек и StopWatch.
 */
@FunctionRegister(name = "CatFly", type = Category.Movement)
public class CatFly extends Function {

    private final StopWatch fireworkTimer = new StopWatch();
    private final StopWatch elytraTimer = new StopWatch();

    // Настройки
    private final SliderSetting fireworkDelay = new SliderSetting(
            "Задержка фейерверка", 400f, 50f, 1500f, 1f
    );
    private final BooleanSetting grimBypass = new BooleanSetting("Обход только Grim", false);
    private final BooleanSetting noFireworkIfEating = new BooleanSetting("Не запускать при еде", false);

    public CatFly() {
        addSettings(fireworkDelay, grimBypass, noFireworkIfEating);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        // Если нет фейерверков в инвентаре — выходим
        if (InventoryUtil.findItem(Items.FIREWORK_ROCKET) == -1) return;

        // Получаем значение настройки "grimBypass" безопасно
        boolean onlyGrim = resolveBoolean(grimBypass, false);
        int timeSwap = onlyGrim ? 0 : 200; // int для StopWatch

        boolean startFirework = true;
        if (resolveBoolean(noFireworkIfEating, false)
                && mc.player.getActiveHand() == Hand.MAIN_HAND
                && mc.player.getHeldItemMainhand().getUseAction() == UseAction.EAT) {
            startFirework = false;
        }

        // Получаем delay из SliderSetting безопасно (в ms, int)
        int delay = resolveSliderInt(fireworkDelay, 400);

        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA
                    && mc.world.getBlockState(new BlockPos(
                    mc.player.getPosX(),
                    mc.player.getPosY() - 0.01,
                    mc.player.getPosZ()
            )).getBlock() == Blocks.AIR
                    && !mc.player.isOnGround()
                    && !mc.player.isInWater()
                    && !mc.player.isInLava()
                    && !mc.player.isElytraFlying()) {

                // Используем windowId = 0 (как в оригинальных реализациях)
                if (stopWatchHasTimeElapsed(elytraTimer, timeSwap)) {
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    mc.player.startFallFlying();
                    mc.player.connection.sendPacket(
                            new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING)
                    );
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                    elytraTimer.reset();
                }

                // Запуск фейерверка (таймер) — используем обёртку для вызова StopWatch.hasTimeElapsed
                if (stopWatchHasTimeElapsed(fireworkTimer, delay) && mc.player.isElytraFlying()) {
                    if (startFirework) {
                        InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, false);
                    }
                    fireworkTimer.reset();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    /* ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (reflection) ==================== */

    /**
     * Попытка безопасно получить boolean-значение из BooleanSetting.
     */
    private boolean resolveBoolean(BooleanSetting setting, boolean def) {
        if (setting == null) return def;
        List<String> candidates = Arrays.asList("get", "getValue", "isEnabled", "is", "getState", "getBoolean", "getVal");
        for (String name : candidates) {
            try {
                Method m = setting.getClass().getMethod(name);
                Object r = m.invoke(setting);
                if (r instanceof Boolean) return (Boolean) r;
                if (r instanceof Number) return ((Number) r).intValue() != 0;
                if (r instanceof String) return Boolean.parseBoolean((String) r);
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        // Попробуем публичные поля
        for (String fieldName : Arrays.asList("value", "enabled", "state", "val")) {
            try {
                Field f = setting.getClass().getField(fieldName);
                Object v = f.get(setting);
                if (v instanceof Boolean) return (Boolean) v;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return def;
    }

    /**
     * Попытка безопасно получить int-значение из SliderSetting.
     */
    private int resolveSliderInt(SliderSetting slider, int def) {
        if (slider == null) return def;
        List<String> candidates = Arrays.asList("get", "getValue", "getFloat", "getDouble", "getAsFloat", "getAsDouble", "getNumber", "getInt", "intValue");
        for (String name : candidates) {
            try {
                Method m = slider.getClass().getMethod(name);
                Object r = m.invoke(slider);
                if (r instanceof Number) return ((Number) r).intValue();
                if (r instanceof String) {
                    try {
                        return (int) Double.parseDouble((String) r);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        // Попробуем публичные поля
        for (String fieldName : Arrays.asList("value", "val", "current", "number")) {
            try {
                Field f = slider.getClass().getField(fieldName);
                Object v = f.get(slider);
                if (v instanceof Number) return ((Number) v).intValue();
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return def;
    }

    /**
     * Безопасный вызов StopWatch.hasTimeElapsed с поддержкой int/long сигнатур.
     * При ошибке возвращает false.
     */
    private boolean stopWatchHasTimeElapsed(StopWatch sw, long ms) {
        if (sw == null) return false;
        // Попробуем long
        try {
            Method m = sw.getClass().getMethod("hasTimeElapsed", long.class);
            Object r = m.invoke(sw, ms);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }

        // Попробуем int
        try {
            Method m = sw.getClass().getMethod("hasTimeElapsed", int.class);
            Object r = m.invoke(sw, (int) ms);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }

        // Попробуем метод без параметров (редко встречается)
        try {
            Method m = sw.getClass().getMethod("hasTimeElapsed");
            Object r = m.invoke(sw);
            if (r instanceof Boolean) return (Boolean) r;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }

        return false;
    }
}
