package ru.onelove.functions.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import ru.onelove.events.EventDisplay;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.impl.combat.KillAura;
import ru.onelove.utils.math.Vector4i;
import ru.onelove.utils.projections.ProjectionUtil;
import ru.onelove.utils.render.ColorUtils;
import ru.onelove.utils.render.DisplayUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

@FunctionRegister(name = "TargetESP", type = Category.Render)
public class TargetESP extends Function {

    private final KillAura killAura;

    public TargetESP(KillAura killAura) {
        this.killAura = killAura;
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.PRE) {
            return;
        }
        if (killAura.isState() && killAura.getTarget() != null) {
            double sin = Math.sin(System.currentTimeMillis() / 1000.0);
            float size = 150.0F;

            Vector3d interpolated = killAura.getTarget().getPositon(e.getPartialTicks());
            Vector2f pos = ProjectionUtil.project(interpolated.x, interpolated.y + killAura.getTarget().getHeight() / 2f, interpolated.z);
            GlStateManager.pushMatrix();
            GlStateManager.translatef(pos.x, pos.y, 0);
            GlStateManager.rotatef((float) sin * 360, 0, 0, 1);
            GlStateManager.translatef(-pos.x, -pos.y, 0);
            DisplayUtils.drawImage(new ResourceLocation("onelove/images/target.png"), pos.x - size / 2f, pos.y - size / 2f, size, size, new Vector4i(
                    ColorUtils.setAlpha(HUD.getColor(0, 1), 220),
                    ColorUtils.setAlpha(HUD.getColor(90, 1), 220),
                    ColorUtils.setAlpha(HUD.getColor(180, 1), 220),
                    ColorUtils.setAlpha(HUD.getColor(270, 1), 220)
            ));
            GlStateManager.popMatrix();
        }
    }

}
