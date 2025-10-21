package ru.onelove.functions.impl.render;

import com.google.common.eventbus.Subscribe;
import ru.onelove.events.WorldEvent;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.client.world.ClientWorld;
import net.optifine.render.RenderUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@FunctionRegister(name = "Xray", type = Category.Render)
public class Xray extends Function {

    private final Map<Block, Integer> ores = new HashMap<>();

    public Xray() {

        ores.put(Blocks.COAL_ORE, new Color(0, 0, 0).getRGB());
        ores.put(Blocks.IRON_ORE, new Color(200, 200, 200).getRGB());
        ores.put(Blocks.GOLD_ORE, new Color(255, 215, 0).getRGB());
        ores.put(Blocks.DIAMOND_ORE, new Color(0, 191, 255).getRGB());
        ores.put(Blocks.EMERALD_ORE, new Color(0, 255, 0).getRGB());
        ores.put(Blocks.REDSTONE_ORE, new Color(255, 0, 0).getRGB());
        ores.put(Blocks.LAPIS_ORE, new Color(0, 0, 255).getRGB());
    }

    @Subscribe
    private void onRender(WorldEvent e) {
        ClientWorld world = mc.world;

        for (int x = -300; x <= 300; x++) {
            for (int z = -300; z <= 300; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (ores.containsKey(block)) {
                        RenderUtils.drawBlockBox(pos, ores.get(block));
                    }
                }
            }
        }

        for (Entity entity : world.getAllEntities()) {
            if (entity instanceof ChestMinecartEntity) {
                RenderUtils.drawBlockBox(entity.getPosition(), -1);
            }
        }
    }
}