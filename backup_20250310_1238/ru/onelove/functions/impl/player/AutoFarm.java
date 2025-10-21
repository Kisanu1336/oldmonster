package ru.onelove.functions.impl.player;

import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.ModeListSetting;

@FunctionRegister(name = "AutoFarm", type = Category.Player)
public class AutoFarm extends Function {
    private final Set<BlockPos> brokenBlocks = new HashSet<>();
    private final Map<BlockPos, Long> blockBreakingTimes = new HashMap<>();
    private final ModeListSetting elements = new ModeListSetting(
            "Что ломать?",
            new BooleanSetting("Арбуз", true),
            new BooleanSetting("Тыква", true),
            new BooleanSetting("Пшено", true),
            new BooleanSetting("Картофель", true),
            new BooleanSetting("Свекла", true),
            new BooleanSetting("Морковь", true),
            new BooleanSetting("Тростник", true),
            new BooleanSetting("Ягоды", true),
            new BooleanSetting("Трава", false),
            new BooleanSetting("Дерево", false),
            new BooleanSetting("Листва", false),
            new BooleanSetting("Кактусы", false),
            new BooleanSetting("Семена Тыковки", false),
            new BooleanSetting("Семена Арбуза", false),
            new BooleanSetting("Нарост", false),
            new BooleanSetting("Ламинария", false)
    );
    private final BooleanSetting rightClickBerries = new BooleanSetting("ПКМ на ягоды", true);
    private boolean running = false;
    private Thread nukerThread;
    private BlockPos currentBreakingBlock = null;

    public AutoFarm() {
        this.addSettings(this.elements, this.rightClickBerries);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        this.running = true;
        this.nukerThread = new Thread(() -> {
            while (this.running) {
                if (mc == null || mc.player == null || mc.world == null) continue;
                this.nukeBlocks();
                this.clearBrokenBlocks();
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        this.nukerThread.start();
        return true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.running = false;
        if (this.nukerThread != null) {
            this.nukerThread.interrupt();
        }
    }

    private void clearBrokenBlocks() {
        long currentTime = System.currentTimeMillis();
        brokenBlocks.removeIf(pos -> currentTime - blockBreakingTimes.getOrDefault(pos, currentTime) >= 2000);
    }

    private double getDistanceSquared(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void nukeBlocks() {
        if (mc != null && mc.world != null && mc.player != null) {
            BlockPos playerPos = new BlockPos(mc.player.getPosition());
            int rangeValue = 3;
            int heightValue = 3;
            int heightValue1 = -4;
            List<BlockPos> blockPositions = new ArrayList<>();

            for (int x = -rangeValue; x <= rangeValue; x++) {
                for (int y = heightValue1; y <= heightValue; y++) {
                    for (int z = -rangeValue; z <= rangeValue; z++) {
                        BlockPos blockPos = playerPos.add(x, y, z);
                        Block block = mc.world.getBlockState(blockPos).getBlock();

                        if (((Boolean) this.elements.getValueByName("Арбуз").get()) && block == Blocks.MELON) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Тыква").get()) && block == Blocks.PUMPKIN) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Пшено").get()) && block == Blocks.WHEAT) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Картофель").get()) && block == Blocks.POTATOES) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Свекла").get()) && block == Blocks.BEETROOTS) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Морковь").get()) && block == Blocks.CARROTS) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Тростник").get()) && block == Blocks.SUGAR_CANE) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Ягоды").get()) && block == Blocks.SWEET_BERRY_BUSH) {
                            if ((Boolean) this.rightClickBerries.get()) {
                                mc.player.connection.sendPacket(
                                        new CPlayerTryUseItemOnBlockPacket(
                                                Hand.MAIN_HAND,
                                                new BlockRayTraceResult(new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Direction.UP, blockPos, true)
                                        )
                                );
                            } else {
                                blockPositions.add(blockPos);
                            }
                        }
                    }
                }
            }

            Set<BlockPos> var10001 = this.brokenBlocks;
            Objects.requireNonNull(var10001);
            blockPositions.removeIf(var10001::contains);
            blockPositions.sort(Comparator.comparingDouble(pos -> this.getDistanceSquared((BlockPos) pos, playerPos)));
            if (!blockPositions.isEmpty()) {
                BlockPos blockToBreak = (BlockPos) blockPositions.get(0);
                if (this.currentBreakingBlock == null || !this.blockBreakingTimes.containsKey(this.currentBreakingBlock)) {
                    try {
                        if (!this.brokenBlocks.contains(blockToBreak)) {
                            AutoFarm.mc.playerController.onPlayerDamageBlock(blockToBreak, AutoFarm.mc.player.getHorizontalFacing());
                            this.blockBreakingTimes.put(blockToBreak, System.currentTimeMillis());
                        }
                    } catch (Exception sex) {
                        Exception e = sex;
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}