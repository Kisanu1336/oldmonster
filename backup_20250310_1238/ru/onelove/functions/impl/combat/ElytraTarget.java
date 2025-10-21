//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package ru.onelove.functions.impl.combat;

import com.google.common.eventbus.Subscribe;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector2f;
import ru.onelove.events.EventUpdate;
import ru.onelove.functions.api.Category;
import ru.onelove.functions.api.Function;
import ru.onelove.functions.api.FunctionRegister;
import ru.onelove.functions.settings.Setting;
import ru.onelove.functions.settings.impl.BooleanSetting;
import ru.onelove.functions.settings.impl.SliderSetting;
import ru.onelove.utils.player.InventoryUtil;

@FunctionRegister(
        name = "ElytraTarget",
        type = Category.Combat
)
public class ElytraTarget extends Function {
    private final Set<PlayerEntity> targetedPlayers = new HashSet();
    private boolean isTargeting = false;
    private long lastFireworkTime = 0L;
    private long fireworkCooldown = 750L;
    private long lastChatMessageTime = 0L;
    private final long chatMessageInterval = 5000L;
    public Vector2f rotateVector = new Vector2f(0.0F, 0.0F);
    private final BooleanSetting save = new BooleanSetting("Save Mode", true);
    private final BooleanSetting autofirework = new BooleanSetting("Auto Firework", true);
    private final BooleanSetting deadtoggle = new BooleanSetting("Disable On Death", true);
    private final SliderSetting distance = new SliderSetting("Target Distance", 50.0F, 5.0F, 50.0F, 1.0F);
    private final SliderSetting hptoggle = (new SliderSetting("Min HP", 6.0F, 0.0F, 20.0F, 1.0F)).setVisible(() -> (Boolean)this.save.get());

    public ElytraTarget() {
        this.addSettings(new Setting[]{this.save, this.autofirework, this.deadtoggle, this.distance, this.hptoggle});
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player != null && mc.world != null) {
            if (mc.player.isElytraFlying()) {
                if (!this.isTargeting) {
                    this.targetPlayer();
                } else {
                    this.updateRotationToPlayer();
                    this.useFirework();
                    this.checkChatMessage();
                }
            } else if (this.isTargeting) {
                this.stopTargeting();
            }

        }
    }

    private void targetPlayer() {
        ClientWorld clientWorld = mc.world;

        for(Entity entity : clientWorld.getEntitiesWithinAABBExcludingEntity(mc.player, new AxisAlignedBB(mc.player.getPosX() - (double)10.0F, mc.player.getPosY() - (double)5.0F, mc.player.getPosZ() - (double)10.0F, mc.player.getPosX() + (double)10.0F, mc.player.getPosY() + (double)5.0F, mc.player.getPosZ() + (double)10.0F))) {
            if (entity instanceof PlayerEntity target && entity.isAlive()) {
                if (!this.targetedPlayers.contains(target)) {
                    this.targetedPlayers.clear();
                    this.targetedPlayers.add(target);
                    this.isTargeting = true;
                    this.setRotationToPlayer(target);
                    return;
                }
            }
        }

    }

    private void setRotationToPlayer(PlayerEntity player) {
        if (player != null) {
            double dx = player.getPosX() - mc.player.getPosX();
            double dy = player.getPosY() - mc.player.getPosY();
            double dz = player.getPosZ() - mc.player.getPosZ();
            double yaw = Math.toDegrees(Math.atan2(dz, dx)) - (double)90.0F;
            double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
            this.rotateVector = new Vector2f((float)yaw, (float)pitch);
            mc.player.rotationYaw = (float)yaw;
            mc.player.rotationPitch = (float)pitch;
        }

    }

    private void updateRotationToPlayer() {
        if (!this.targetedPlayers.isEmpty()) {
            PlayerEntity target = (PlayerEntity)this.targetedPlayers.iterator().next();
            this.setRotationToPlayer(target);
        }

    }

    private void useFirework() {
        long currentTime = System.currentTimeMillis();
        if ((Boolean)this.autofirework.get() && currentTime - this.lastFireworkTime >= this.fireworkCooldown) {
            int hbSlot = InventoryUtil.getSlotInInventoryOrHotbar(Items.FIREWORK_ROCKET, true);
            int invSlot = InventoryUtil.getSlotInInventoryOrHotbar(Items.FIREWORK_ROCKET, false);
            if (hbSlot == -1 && invSlot == -1) {
                return;
            }

            int currentSlot = mc.player.inventory.currentItem;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            mc.player.connection.sendPacket(new CHeldItemChangePacket(currentSlot));
            this.lastFireworkTime = currentTime;
            PlayerEntity target = (PlayerEntity)this.targetedPlayers.iterator().next();
            double distanceToTarget = (double)mc.player.getDistance(target);
            double distValue = (double)(Float)this.distance.get();
            this.fireworkCooldown = distanceToTarget > distValue ? 300L : 200L;
        }

    }

    private void stopTargeting() {
        this.targetedPlayers.clear();
        this.isTargeting = false;
    }

    private void checkChatMessage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastChatMessageTime >= 5000L) {
            if (!this.targetedPlayers.isEmpty()) {
                PlayerEntity target = (PlayerEntity)this.targetedPlayers.iterator().next();
                if (target.getHealth() <= 0.01F) {
                    this.targetedPlayers.clear();
                    this.onDisable();
                }
            }

            this.lastChatMessageTime = currentTime;
        }

        if ((Boolean)this.save.get()) {
            float playerHp = mc.player.getHealth();
            float hpValue = (Float)this.hptoggle.get();
            if (playerHp < hpValue) {
                ClientPlayerEntity var10000 = mc.player;
                var10000.rotationYaw += 180.0F;
                this.stopTargeting();
                this.onDisable();
            }
        }

    }

    public PlayerEntity[] getTargetedPlayers() {
        return (PlayerEntity[])this.targetedPlayers.toArray(new PlayerEntity[0]);
    }

    public void onDisable() {
        this.stopTargeting();
        super.onDisable();
    }

    public boolean onEnable() {
        super.onEnable();
        this.stopTargeting();
        return false;
    }
}
