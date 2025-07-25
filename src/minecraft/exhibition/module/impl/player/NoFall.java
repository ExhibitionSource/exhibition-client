package exhibition.module.impl.player;

import exhibition.Client;
import exhibition.event.Event;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.EventMotionUpdate;
import exhibition.event.impl.EventMove;
import exhibition.event.impl.EventPacket;
import exhibition.module.Module;
import exhibition.module.data.ModuleData;
import exhibition.module.data.settings.Setting;
import exhibition.module.impl.combat.Bypass;
import exhibition.util.*;
import exhibition.util.security.BypassValues;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;

public class NoFall extends Module {

    private Setting<Boolean> fastFall = new Setting("FASTFALL", false, "Fast falls for last 5 blocks.");
    private Setting<Boolean> vanilla = new Setting("VANILLA", false, "Vanilla NoFall.");

    private float dist;

    private Timer timer = new Timer();

    private Timer lagbackTimer = new Timer();

    public NoFall(ModuleData data) {
        super(data);
        addSetting(fastFall);
        addSetting(vanilla);
    }

    @Override
    public Priority getPriority() {
        return Priority.LAST;
    }

    boolean sentLastTick = false;

    @RegisterEvent(events = {EventPacket.class, EventMotionUpdate.class, EventMove.class})
    public void onEvent(Event event) {
        if (mc.thePlayer == null || mc.theWorld == null || PlayerUtil.isInLiquid() || PlayerUtil.isOnLiquid() || !mc.thePlayer.capabilities.allowEdit || mc.thePlayer.capabilities.allowFlying || mc.thePlayer.isSpectator())
            return;

        if (event instanceof EventPacket) {
            EventPacket ep = event.cast();
            if (ep.getPacket() instanceof S08PacketPlayerPosLook) {
                this.lagbackTimer.reset();
            }

            if (HypixelUtil.isVerifiedHypixel() && mc.thePlayer.motionY < 0 && mc.thePlayer.fallDistance > 2.124) {
                if (ep.getPacket() instanceof C03PacketPlayer) {
                    C03PacketPlayer c03PacketPlayer = (C03PacketPlayer) ep.getPacket();
                    if (c03PacketPlayer.isMoving() && c03PacketPlayer.getRotating()) {
                        ep.setPacket(new C03PacketPlayer.C04PacketPlayerPosition(c03PacketPlayer.x, c03PacketPlayer.y, c03PacketPlayer.z, c03PacketPlayer.onGround));
                    }
                } else if (ep.getPacket() instanceof C02PacketUseEntity) {
                    C02PacketUseEntity c02PacketUseEntity = (C02PacketUseEntity) ep.getPacket();
                    if (c02PacketUseEntity.getAction().equals(C02PacketUseEntity.Action.ATTACK)) {
                        ep.setCancelled(true);
                    }
                }
            }
        }

        if (event instanceof EventMotionUpdate) {
            EventMotionUpdate em = (EventMotionUpdate) event;
            if (!lagbackTimer.delay(1000) || HypixelUtil.isVerifiedHypixel() && HypixelUtil.isInGame("PIT")) {
                dist = 0;
                return;
            }

            double distanceToGround = -1;
            for (int i = (int) (mc.thePlayer.posY - 1); i >= 0; i--) {
                BlockPos pos = new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ);
                if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                    distanceToGround = mc.thePlayer.getDistance(mc.thePlayer.posX, pos.getY(), mc.thePlayer.posZ);
                    break;
                }
            }

            if (dist > mc.thePlayer.fallDistance || mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) {
                dist = 0;
            }

            if (mc.thePlayer.motionY < 0 && mc.thePlayer.fallDistance > 2.124) {
                double fallY = mc.thePlayer.motionY;
                double fallen = mc.thePlayer.fallDistance - dist;
                double predictedFallen = fallen + -((fallY - 0.08D) * 0.9800000190734863D);
                if (predictedFallen >= 3.0 && mc.thePlayer.posY > 50 && mc.thePlayer.posY < 255) {
//                    Bypass bypass = Client.getModuleManager().get(Bypass.class);
//                    boolean allowVanilla = bypass.allowBypassing() && (bypass.option.getSelected().equals("Watchdog Off") || (bypass.bruh == 0 || bypass.bruh > 10));

                    boolean preModification = !HypixelUtil.isVerifiedHypixel() || Bypass.shouldSabotage();

                    if (isBlockUnder() && fallY > -4) {
                        if (em.isPre()) {
                            if (preModification) {
//                                if (bypass.bruh > 10) {
//                                    bypass.bruh -= 1;
//                                }
                                em.setGround(true);
                                dist = mc.thePlayer.fallDistance;

                                if (fastFall.getValue() && distanceToGround != -1 && distanceToGround <= 15 && timer.delay(2500)) {
                                    //mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY - , mc.thePlayer.posZ);
                                    mc.thePlayer.motionY = -Math.min(distanceToGround, 9);
                                    timer.reset();
                                }
                            }
                        } else if (em.isPost() && HypixelUtil.isVerifiedHypixel()) {
                            if (!sentLastTick) {
                                dist = mc.thePlayer.fallDistance;
                                BypassValues.sendNoFallPacket(em);
                                sentLastTick = true;
                                return;
                            } else {
                                sentLastTick = false;
                            }
                        }
                    }

                    if(em.isPost() && sentLastTick) {
                        sentLastTick = false;
                    }
                }
            } else {
                sentLastTick = false;
            }
        }
    }

    private boolean isBlockUnder() {
        for (int i = (int) (mc.thePlayer.posY); i >= 0; i--) {
            double[][] offsets = new double[][]{new double[]{0, 0}, new double[]{-0.35, -0.35}, new double[]{-0.35, 0.35}, new double[]{0.35, 0.35}, new double[]{0.35, -0.35}};
            for (double[] offset : offsets) {
                double offsetX = offset[0];
                double offsetZ = offset[1];

                double posX = offsetX + mc.thePlayer.posX;
                double posY = i;
                double posZ = offsetZ + mc.thePlayer.posZ;
                BlockPos pos = new BlockPos(posX, posY, posZ);
                if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                    return true;
                }
            }
        }
        return false;
    }

}
