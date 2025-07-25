/*
 * Copyright (c) MineSense.pub 2018.
 * Developed by Arithmo
 */

package exhibition.module.impl.movement;

import exhibition.Client;
import exhibition.event.Event;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.EventMotionUpdate;
import exhibition.event.impl.EventMove;
import exhibition.event.impl.EventRender3D;
import exhibition.management.ColorManager;
import exhibition.management.friend.FriendManager;
import exhibition.module.Module;
import exhibition.module.data.ModuleData;
import exhibition.module.data.Options;
import exhibition.module.data.settings.Setting;
import exhibition.module.impl.combat.AntiBot;
import exhibition.module.impl.combat.Killaura;
import exhibition.module.impl.player.Scaffold;
import exhibition.module.impl.render.TargetESP;
import exhibition.util.*;
import exhibition.util.render.Colors;
import net.minecraft.block.BlockAir;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TargetStrafe extends Module {

    private Vec3 targetPos;
    private EntityLivingBase target;
    private Setting<Boolean> behind = new Setting<>("BEHIND", false, "Attempts to move behind the target if possible.");
    private Setting<Boolean> teams = new Setting<>("TEAMS", false, "Ignores enemies on the same team.");
    private Setting<Boolean> flip = new Setting<>("ANTI-STUCK", true, "Attempts to stop you from getting stuck.");
    private Setting<Boolean> antiVoid = new Setting<>("ANTI-VOID", false, "Attempts to stop you from jumping into the void. (Requires Anti-Stuck)");

    private Options targetMode = new Options("Target Mode", "Nearby", "Nearby", "Priority", "Aura Only");
    private Options pathMode = new Options("Path Mode", "Normal", "Normal", "Adaptive");

    private Setting<Number> range = new Setting<>("RANGE", 5, "Range to select targets.", 0.1, 1, 10);
    private Setting<Number> radius = new Setting<>("RADIUS", 2, "Radius of circle strafe.", 0.1, 0.1, 5);
    private Setting<Number> steps = new Setting<>("STEPS", 60, "Like speed Steps but for target strafe.", 1, 15, 180);
    private Setting<Number> offset = new Setting<>("OFFSET", 0, "Angle offset for Behind", 5, -180, 180);

    private Setting<Number> jitterMagnitude = new Setting<>("JITTER-DIST", 0, "The amount that TargetStrafe should Jitter.", 0.1, 0, 2);
    private Setting<Number> jitterFrequency = new Setting<>("JITTER-FREQ", 0.5, "The frequency that Jitter goes in/out.", 0.1, 0, 2);

    private boolean reverse = false;

    private Timer delay = new Timer();

    public TargetStrafe(ModuleData data) {
        super(data);
        addSetting(flip);
        addSetting(behind);
        addSetting(teams);
        addSetting(offset);
        addSetting(steps);
        addSetting(range);
        addSetting(radius);
        addSettings(jitterMagnitude, jitterFrequency);
        addSetting("PATHMODE", new Setting<>("PATHMODE", pathMode, "The path-finding mode for TargetStrafe."));
        addSetting("TARGETMODE", new Setting<>("TARGETMODE", targetMode, "The targeting mode for TargetStrafe. Aura Only is suggested against legits."));
    }

    @Override
    public void onEnable() {
        reverse = false;
    }

    @Override
    public Priority getPriority() {
        return Priority.FIRST;
    }

    public boolean isSmart() {
        return pathMode.getSelected().equalsIgnoreCase("Adaptive");
    }

    @RegisterEvent(events = {EventMotionUpdate.class, EventRender3D.class, EventMove.class})
    public void onEvent(Event event) {
        double rad = this.radius.getValue().doubleValue();

//        if (jitterMagnitude.getValue().doubleValue() > 0) {
//            double add = jitterMagnitude.getValue().doubleValue();
//
//            double frequency = jitterFrequency.getValue().doubleValue();
//
//            double percent = ((1 + frequency) * (mc.thePlayer.ticksExisted)) / 5D;
//
//            double addRad = Math.sin(percent) * add;
//
//            rad += addRad;
//        }

        double range = this.range.getValue().doubleValue();

        boolean isStrafing = (Client.getModuleManager().isEnabled(Speed.class) || Client.getModuleManager().get(LongJump.class).allowTargetStrafe() || Client.getModuleManager().get(Fly.class).allowTargetStrafe());

        if (event instanceof EventRender3D) {
            EventRender3D er = event.cast();
            float pTicks = er.renderPartialTicks;

            boolean debug = false;

            if (!debug) {
                if (target != null) {
                    double x = (target.prevPosX + (target.posX - target.prevPosX) * pTicks) - RenderManager.renderPosX;
                    double y = (target.prevPosY + (target.posY - target.prevPosY) * pTicks) - RenderManager.renderPosY;
                    double z = (target.prevPosZ + (target.posZ - target.prevPosZ) * pTicks) - RenderManager.renderPosZ;

                    float factor = 30;
                    GlStateManager.pushMatrix();
                    for (float i = 0; i < (360F / factor); i += 1) {
                        double cos = Math.cos(((i * factor)) * (Math.PI * 2 / 360));
                        double sin = Math.sin(((i * factor)) * (Math.PI * 2 / 360));
                        double rotX = (rad * cos);
                        double rotY = (rad * sin);

                        double cos2 = Math.cos(((i + 1) * factor) * (Math.PI * 2 / 360));
                        double sin2 = Math.sin(((i + 1) * factor) * (Math.PI * 2 / 360));
                        double rotX2 = (rad * cos2);
                        double rotY2 = (rad * sin2);

                        GL11.glLineWidth(8);
                        RenderingUtil.draw3DLine(x + rotX, y, z + rotY, x + rotX2, y, z + rotY2, Colors.getColor(0, 150));


                        GL11.glLineWidth(5);
                        RenderingUtil.draw3DLine(x + rotX, y, z + rotY, x + rotX2, y, z + rotY2, isStrafing ? ColorManager.strafeColor.getColorHex() : Colors.getColor(255, 255));
                    }

                    GlStateManager.popMatrix();
                }
            } else {
//                if (target != null) {
//                    double x = (target.prevPosX + (target.posX - target.prevPosX) * pTicks) - RenderManager.renderPosX;
//                    double y = (target.prevPosY + (target.posY - target.prevPosY) * pTicks) - RenderManager.renderPosY;
//                    double z = (target.prevPosZ + (target.posZ - target.prevPosZ) * pTicks) - RenderManager.renderPosZ;
//
//                    float factor = 10;
//                    double var41 = 0.4;
//                    GlStateManager.pushMatrix();
//                    double lastRad = -1;
//
//                    List<Vec3> posList = new ArrayList<>();
//
//                    Vec3 closest = null;
//
//                    double lowestDist = Double.MAX_VALUE;
//
//                    double diff = MathUtils.getIncremental(MathHelper.wrapAngleTo180_float(RotationUtils.getRotations(target)[0] - 90F), 10);
//
//                    for (int i = 0; i < (360F / factor); i++) {
//
//                        int i1 = i * (reverse ? -1 : 1);
//
//                        double cos = Math.cos(((i1 * factor) + diff) * (Math.PI * 2 / 360));
//                        double sin = Math.sin(((i1 * factor) + diff) * (Math.PI * 2 / 360));
//                        double rotX = (rad * cos);
//                        double rotY = (rad * sin);
//
//                        double diffX = (target.posX + rotX) - mc.thePlayer.posX;
//                        double diffZ = (target.posZ + rotY) - mc.thePlayer.posZ;
//                        double diffY = (target.posY - mc.thePlayer.posY);
//
//                        boolean isVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));
//
//                        for (int _y = (int) target.posY; _y >= 0; _y--) {
//                            if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotX, _y, target.posZ + rotY)).getBlock() instanceof BlockAir)) {
//                                isVoid = false;
//                                break;
//                            }
//                        }
//
//                        boolean isFineTick = !isSmart() || (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffX, 0, diffZ).expand(var41, 0, var41)).isEmpty() && !isVoid) && lastRad == -1;
//
//                        GlStateManager.pushMatrix();
//                        RenderingUtil.pre3D();
//                        GlStateManager.translate(x + rotX, y, z + rotY);
//                        AxisAlignedBB var12 = new AxisAlignedBB(-0.025, -0.025, -0.025, 0.025, 0.025, 0.025);
//                        RenderingUtil.glColor(isFineTick ? -1 : Colors.getColor(255, 150, 0));
//                        RenderingUtil.drawBoundingBox(var12);
//                        RenderingUtil.post3D();
//                        GlStateManager.popMatrix();
//
//                        double increment = 0.1;
//
//                        Vec3 vec = new Vec3(rotX, 0, rotY);
//
//                        List<Vec3> validPoints = new ArrayList<>();
//
//                        if (!isFineTick || lastRad != -1) {
//                            for (int bruh = 1; bruh < 30; bruh++) {
//                                double posRad = rad + (increment * bruh);
//
//                                double adjustedRad = posRad;
//
//                                double rotPosX = (adjustedRad * cos);
//                                double rotPosY = (adjustedRad * sin);
//
//                                double diffPosX = (target.posX + rotPosX) - mc.thePlayer.posX;
//                                double diffPosZ = (target.posZ + rotPosY) - mc.thePlayer.posZ;
//
//                                boolean willNotCollide = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffPosX, 0, diffPosZ).expand(var41, 0, var41)).isEmpty();
//
//                                boolean isCurrentlyVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));
//
//                                for (int _y = (int) target.posY; _y >= 0; _y--) {
//                                    if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotPosX, _y, target.posZ + rotPosY)).getBlock() instanceof BlockAir)) {
//                                        isCurrentlyVoid = false;
//                                        break;
//                                    }
//                                }
//
//                                boolean isFinePos = willNotCollide && !isCurrentlyVoid;
//
//                                if (!isFinePos) {
//                                    GlStateManager.pushMatrix();
//                                    RenderingUtil.pre3D();
//                                    GlStateManager.translate(x + rotPosX, y, z + rotPosY);
//                                    RenderingUtil.glColor(isFinePos ? Colors.getColor(0, 255, 0, 255) : Colors.getColor(255, 0, 0, 30));
//                                    RenderingUtil.drawBoundingBox(var12);
//                                    RenderingUtil.post3D();
//                                    GlStateManager.popMatrix();
//                                }
//
//                                if (isFinePos) {
//                                    isFineTick = true;
//                                    vec = new Vec3(rotPosX, 0, rotPosY);
//                                    validPoints.add(vec);
//                                }
//                            }
//
//                            int lowest = (int) (rad / increment);
//
//                            for (int bruh = 1; bruh < lowest; bruh++) {
//                                double posRad = rad - (increment * bruh);
//
//                                double adjustedRad = posRad;
//
//                                double rotPosX = (adjustedRad * cos);
//                                double rotPosY = (adjustedRad * sin);
//
//                                double diffPosX = (target.posX + rotPosX) - mc.thePlayer.posX;
//                                double diffPosZ = (target.posZ + rotPosY) - mc.thePlayer.posZ;
//
//                                boolean willNotCollide = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffPosX, 0, diffPosZ).expand(var41, 0, var41)).isEmpty();
//
//                                boolean isCurrentlyVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));
//
//                                for (int _y = (int) target.posY; _y >= 0; _y--) {
//                                    if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotPosX, _y, target.posZ + rotPosY)).getBlock() instanceof BlockAir)) {
//                                        isCurrentlyVoid = false;
//                                        break;
//                                    }
//                                }
//
//                                boolean isFinePos = willNotCollide && !isCurrentlyVoid;
//
//                                if (!isFinePos) {
//                                    GlStateManager.pushMatrix();
//                                    RenderingUtil.pre3D();
//                                    GlStateManager.translate(x + rotPosX, y, z + rotPosY);
//                                    RenderingUtil.glColor(isFinePos ? Colors.getColor(0, 255, 0, 255) : Colors.getColor(255, 0, 0, 30));
//                                    RenderingUtil.drawBoundingBox(var12);
//                                    RenderingUtil.post3D();
//                                    GlStateManager.popMatrix();
//                                }
//
//                                if (isFinePos) {
//                                    vec = new Vec3(rotPosX, 0, rotPosY);
//                                    validPoints.add(vec);
//                                }
//                            }
//
//                            Vec3 closestValid = null;
//
//                            double currentRad = Double.MAX_VALUE;
//
//                            double lastPosRad = lastRad;
//
//                            for (Vec3 validPoint : validPoints) {
//                                double radius = Math.sqrt(validPoint.getX() * validPoint.getX() + validPoint.getZ() * validPoint.getZ());
//
//                                double radDiff = Math.abs(lastPosRad - radius);
//                                if (lastRad == rad || radDiff < currentRad) {
//                                    currentRad = radDiff;
//                                    lastRad = radius;
//                                    closestValid = validPoint;
//                                }
//
//                            }
//
//                            if (closestValid != null) {
//                                GlStateManager.pushMatrix();
//                                RenderingUtil.pre3D();
//                                GlStateManager.translate(x + closestValid.getX(), y, z + closestValid.getZ());
//                                RenderingUtil.glColor(Colors.getColor(150, 150, 255, 255));
//                                RenderingUtil.drawBoundingBox(var12);
//                                RenderingUtil.post3D();
//                                GlStateManager.popMatrix();
//                                vec = closestValid;
//                            }
//                        } else {
//                            vec = new Vec3(rotX, 0, rotY);
//                            posList.add(vec);
//                        }
//
////                        double realX = x + vec.getX() + RenderManager.renderPosX;
////                        double realZ = z + vec.getZ() + RenderManager.renderPosZ;
////
////                        float rotationOffset = RotationUtils.getYawChangeGiven(realX, realZ, mc.thePlayer.rotationYaw);
////
////                        double mx = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw + rotationOffset + 90));
////                        double mz = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw + rotationOffset + 90));
////
////                        double predictedX = mc.thePlayer.posX - RenderManager.renderPosX;
////                        double predictedZ = mc.thePlayer.posZ - RenderManager.renderPosZ;
////
////                        double posX = vec.getX();
////                        double posZ = vec.getZ();
////
////                        double d0 = 0 - posX;
////                        double d2 = 0 - posZ;
////
////                        double hypot = Math.hypot(d0, d2);
//
//                        double distance = mc.thePlayer.getDistance(x + vec.getX() + RenderManager.renderPosX, mc.thePlayer.posY, z + vec.getZ() + RenderManager.renderPosZ);
//                        if (distance < lowestDist) {
//                            lowestDist = distance;
//                            closest = vec;
//                        }
//
////                        GL11.glLineWidth(2);
////                        RenderingUtil.draw3DLine(0,0,0, hypot * mx, y, hypot * mz, Colors.getColor(0, 150));
//
//
//                        //GL11.glLineWidth(5);
//                        //RenderingUtil.draw3DLine(x + rotX, y, z + rotY, x + rotX2, y, z + rotY2, Client.getModuleManager().isEnabled(Speed.class) ? Colors.getColor(120, 255, 120, 255) : Colors.getColor(255, 255));
//                    }
//
//                    if (closest != null) {
//                        GlStateManager.pushMatrix();
//                        RenderingUtil.pre3D();
//                        GlStateManager.translate(x + closest.getX(), y, z + closest.getZ());
//                        RenderingUtil.glColor(Colors.getColor(255, 255, 0, 255));
//                        AxisAlignedBB var12 = new AxisAlignedBB(-0.05, -0.025, -0.05, 0.05, 0.025, 0.05);
//                        RenderingUtil.drawBoundingBox(var12);
//                        RenderingUtil.post3D();
//                        GlStateManager.popMatrix();
//                    }
//
//                    Vec3 bestPos = null;
//                    for (Vec3 vec3 : posList) {
//
//                        float currentYaw = getEntityYawToPos(target, vec3.getX(), vec3.getZ());
//
//                        float bestYaw = bestPos == null ? 0 : getEntityYawToPos(target, bestPos.getX(), bestPos.getZ());
//
//                        double bestDistance = Math.abs(MathHelper.wrapAngleTo180_float(bestYaw));
//
//                        double currentDistance = Math.abs(MathHelper.wrapAngleTo180_float(currentYaw));
//
//                        if (currentDistance >= bestDistance && currentDistance > 150) {
//                            bestPos = vec3;
//                        }
//                    }
//
//                    if (bestPos != null) {
//                        GlStateManager.pushMatrix();
//                        RenderingUtil.pre3D();
//                        GlStateManager.translate(x + bestPos.getX(), y, z + bestPos.getZ());
//                        RenderingUtil.glColor(Colors.getColor(0, 0, 255, 255));
//                        AxisAlignedBB var12 = new AxisAlignedBB(-0.025, -0.025, -0.025, 0.025, 0.025, 0.025);
//                        RenderingUtil.drawBoundingBox(var12);
//                        RenderingUtil.post3D();
//                        GlStateManager.popMatrix();
//                    }
//
//                    GlStateManager.popMatrix();
//                }
            }
        }

        if (event instanceof EventMove) {
            if (flip.getValue() && delay.delay(450)) {

                boolean checkVoid = antiVoid.getValue();
                boolean isVoid = false;

                if (checkVoid) {
                    isVoid = true;

                    double[][] offsets = new double[][]{{0.4, 0.4}, {-0.4, 0.4}, {0.4, -0.4}, {-0.4, -0.4}};

                    if (PlayerUtil.isMoving()) {
                        for (double[] offset : offsets) {
                            int posY = (int) mc.thePlayer.posY;
                            for (int _y = posY; _y >= 0; _y--) {
                                if (!(mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX + offset[0], _y, mc.thePlayer.posZ + offset[1])).getBlock() instanceof BlockAir)) {
                                    isVoid = false;
                                    break;
                                }
                            }
                        }
                    }
                }


                boolean shouldFlip = mc.thePlayer.isCollidedHorizontally || isVoid;
                if (shouldFlip) {
                    reverse = !reverse;
                    delay.reset();
                }
            }
        }
        if (event instanceof EventMotionUpdate) {
            EventMotionUpdate em = event.cast();
            if (em.isPre()) {
                setSuffix(pathMode.getSelected());
                target = null;
                if (targetMode.getSelected().equals("Aura Only")) {
                    target = Killaura.getTarget();
                } else {
                    for (Entity entity : mc.theWorld.getPlayerEntities().stream().filter(Entity::isPlayerMP).sorted(Comparator.comparingDouble(o -> -o.getDistanceToEntity(mc.thePlayer))).collect(Collectors.toList())) {
                        if (!AntiBot.isBot(entity) && !FriendManager.isFriend(entity.getName())) {
                            EntityPlayer ent = (EntityPlayer) entity;
                            boolean nearest = targetMode.getSelected().equalsIgnoreCase("Nearby");
                            if ((nearest || (targetMode.getSelected().equals("Priority") && TargetESP.isPriority(ent)) || (targetMode.getSelected().equals("Aura Only") && Killaura.getTarget() == ent)) && mc.thePlayer.getDistanceToEntity(ent) <= range && (!(boolean) teams.getValue() || !TeamUtils.isTeam(mc.thePlayer, ent))) {
                                target = ent;
                            }
                        }
                    }
                }
            }
        }
    }

    public float getEntityYawToPos(EntityLivingBase entity, double posX, double posZ) {
        double yawToEntity;
        if ((posZ < 0.0D) && (posX < 0.0D)) {
            yawToEntity = 90.0D + Math.toDegrees(Math.atan(posZ / posX));
        } else if ((posZ < 0.0D) && (posX > 0.0D)) {
            yawToEntity = -90.0D + Math.toDegrees(Math.atan(posZ / posX));
        } else {
            yawToEntity = Math.toDegrees(-Math.atan(posX / posZ));
        }

        float lastDelta = MathHelper.clamp_float((entity.rotationYaw - entity.prevRotationYaw) * 6F, -90, 90);

        return MathHelper.wrapAngleTo180_float(-((entity.rotationYaw + lastDelta + offset.getValue().floatValue()) - (float) yawToEntity));
    }

    public float getTargetYaw(float speedYaw, double motionY) {
        float newYaw = speedYaw;
        if (target != null && isEnabled() && !Client.getModuleManager().isEnabled(Scaffold.class)) {
            double rad = radius.getValue().doubleValue();

            if (jitterMagnitude.getValue().doubleValue() > 0) {
                double add = jitterMagnitude.getValue().doubleValue();

                double frequency = jitterFrequency.getValue().doubleValue();

                double percent = ((1 + frequency) * (mc.thePlayer.ticksExisted)) / 5D;

                double addRad = Math.sin(percent) * add;

                rad += addRad;
            }

            double x = target.posX;
            double y = target.posY;
            double z = target.posZ;

            int factor = 10;
            int closest = -1;

            double var41 = 0.4;

            double lowestDist = Double.MAX_VALUE;

            double lastRad = rad;

            List<Vec3> posList = new ArrayList<>();

            double diff = MathUtils.getIncremental(MathHelper.wrapAngleTo180_float(RotationUtils.getRotations(target)[0] - 90F), 10);

            for (int i = 0; i < (360 / factor); i++) {
                int i1 = i * (reverse ? -1 : 1);

                if (i > (behind.getValue() ? 10 : 2))
                    break;

                double cos = Math.cos(((i1 * factor) + diff) * (Math.PI * 2 / 360));
                double sin = Math.sin(((i1 * factor) + diff) * (Math.PI * 2 / 360));
                double rotX = (rad * cos);
                double rotY = (rad * sin);

                double diffX = (target.posX + rotX) - mc.thePlayer.posX;
                double diffZ = (target.posZ + rotY) - mc.thePlayer.posZ;
                double diffY = (target.posY - mc.thePlayer.posY);

                boolean isVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));

                for (int _y = (int) target.posY; _y >= 0; _y--) {
                    if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotX, _y, target.posZ + rotY)).getBlock() instanceof BlockAir)) {
                        isVoid = false;
                        break;
                    }
                }

                double correctedY = motionY;

                AxisAlignedBB playerBoundingBox = mc.thePlayer.getEntityBoundingBox();
                AxisAlignedBB tempBoundingBox = new AxisAlignedBB(playerBoundingBox.minX, playerBoundingBox.minY, playerBoundingBox.minZ, playerBoundingBox.maxX, playerBoundingBox.maxY, playerBoundingBox.maxZ).offset(diffX, 0, diffZ);

                final List<AxisAlignedBB> var15 = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, tempBoundingBox.addCoord(0, correctedY, 0));
                for (final AxisAlignedBB var18 : var15) {
                    correctedY = var18.calculateYOffset(tempBoundingBox, correctedY);
                }

                if (correctedY != motionY) {
                    correctedY += 0.1;
                }

                boolean isFineTick = !isSmart() || (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffX, correctedY, diffZ).expand(var41, 0, var41)).isEmpty() && !isVoid) && lastRad == -1;

                double increment = 0.1;

                if (!isFineTick || lastRad != rad) {
                    List<Vec3> validPoints = new ArrayList<>();

                    for (int bruh = 1; bruh < 30; bruh++) {

                        double posRad = rad + (increment * bruh);

                        double adjustedRad = posRad;

                        double rotPosX = (adjustedRad * cos);
                        double rotPosY = (adjustedRad * sin);

                        double diffPosX = (target.posX + rotPosX) - mc.thePlayer.posX;
                        double diffPosZ = (target.posZ + rotPosY) - mc.thePlayer.posZ;

                        double correctedYTemp = motionY;

                        AxisAlignedBB tempBoundingBoxTemp = new AxisAlignedBB(playerBoundingBox.minX, playerBoundingBox.minY, playerBoundingBox.minZ, playerBoundingBox.maxX, playerBoundingBox.maxY, playerBoundingBox.maxZ).offset(diffPosX, 0, diffPosZ);

                        final List<AxisAlignedBB> var15Temp = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, tempBoundingBoxTemp.addCoord(0, correctedYTemp, 0));
                        for (final AxisAlignedBB var18 : var15Temp) {
                            correctedYTemp = var18.calculateYOffset(tempBoundingBoxTemp, correctedYTemp);
                        }

                        if (correctedYTemp != motionY) {
                            correctedYTemp += 0.1;
                        }

                        boolean willNotCollide = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffPosX, correctedYTemp, diffPosZ).expand(var41, 0, var41)).isEmpty();

                        boolean isCurrentlyVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));

                        for (int _y = (int) target.posY; _y >= 0; _y--) {
                            if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotPosX, _y, target.posZ + rotPosY)).getBlock() instanceof BlockAir)) {
                                isCurrentlyVoid = false;
                                break;
                            }
                        }

                        boolean isFinePos = willNotCollide && !isCurrentlyVoid;

                        if (isFinePos) {
                            rotX = rotPosX;
                            rotY = rotPosY;
                            validPoints.add(new Vec3(rotX, 0, rotY));
                        }
                    }

                    int lowest = (int) (rad / increment);

                    for (int bruh = 1; bruh < lowest; bruh++) {
                        double posRad = rad - (increment * bruh);

                        double adjustedRad = posRad;

                        double rotPosX = (adjustedRad * cos);
                        double rotPosY = (adjustedRad * sin);

                        double diffPosX = (target.posX + rotPosX) - mc.thePlayer.posX;
                        double diffPosZ = (target.posZ + rotPosY) - mc.thePlayer.posZ;

                        double correctedYTemp = motionY;

                        AxisAlignedBB tempBoundingBoxTemp = new AxisAlignedBB(playerBoundingBox.minX, playerBoundingBox.minY, playerBoundingBox.minZ, playerBoundingBox.maxX, playerBoundingBox.maxY, playerBoundingBox.maxZ).offset(diffPosX, 0, diffPosZ);

                        final List<AxisAlignedBB> var15Temp = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, tempBoundingBoxTemp.addCoord(0, correctedYTemp, 0));
                        for (final AxisAlignedBB var18 : var15Temp) {
                            correctedYTemp = var18.calculateYOffset(tempBoundingBoxTemp, correctedYTemp);
                        }

                        if (correctedYTemp != motionY) {
                            correctedYTemp += 0.1;
                        }

                        boolean willNotCollide = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.boundingBox.offset(diffPosX, correctedYTemp, diffPosZ).expand(var41, 0, var41)).isEmpty();

                        boolean isCurrentlyVoid = !(Client.getModuleManager().isEnabled(LongJump.class) || Client.getModuleManager().isEnabled(Fly.class));

                        for (int _y = (int) target.posY; _y >= 0; _y--) {
                            if (!(mc.theWorld.getBlockState(new BlockPos(target.posX + rotPosX, _y, target.posZ + rotPosY)).getBlock() instanceof BlockAir)) {
                                isCurrentlyVoid = false;
                                break;
                            }
                        }

                        boolean isFinePos = willNotCollide && !isCurrentlyVoid;

                        if (isFinePos) {
                            rotX = rotPosX;
                            rotY = rotPosY;
                            validPoints.add(new Vec3(rotX, 0, rotY));
                        }
                    }

                    Vec3 closestValid = null;

                    double currentRad = Double.MAX_VALUE;

                    double lastPosRad = lastRad;

                    for (Vec3 validPoint : validPoints) {
                        double radius = Math.sqrt(validPoint.getX() * validPoint.getX() + validPoint.getZ() * validPoint.getZ());

                        double radDiff = Math.abs(lastPosRad - radius);
                        if (lastRad == -1 || radDiff < currentRad) {
                            currentRad = radDiff;
                            lastRad = radius;
                            closestValid = validPoint;
                        }
                    }

                    if (closestValid != null) {
                        rotX = closestValid.getX();
                        rotY = closestValid.getZ();
                    }
                }

                posList.add(new Vec3(rotX, 0, rotY));

//                float rotationOffset = RotationUtils.getYawChangeGiven(x + rotX, z + rotY, speedYaw);
//
//                double mx = Math.cos(Math.toRadians(speedYaw + rotationOffset));
//                double mz = Math.sin(Math.toRadians(speedYaw + rotationOffset));
//
//                double predictedX = mc.thePlayer.posX + (speed * mx);
//                double predictedZ = mc.thePlayer.posZ + (speed * mz);
//
//                double posX = x + rotX;
//                double posZ = z + rotY;
//
//                double d0 = predictedX - posX;
//                double d2 = predictedZ - posZ;
//
//                double distance = MathHelper.sqrt_double(d0 * d0 + d2 * d2);

                double distance = mc.thePlayer.getDistance(x + rotX, mc.thePlayer.posY, z + rotY) + target.getDistance(x + rotX, target.posY, z + rotY);
                if (distance < lowestDist) {
                    lowestDist = distance;
                    closest = i;
                }
            }

            if (behind.getValue()) {
                Vec3 bestPos = null;
                for (Vec3 vec3 : posList) {

                    float currentYaw = getEntityYawToPos(target, vec3.getX(), vec3.getZ());

                    float bestYaw = bestPos == null ? 0 : getEntityYawToPos(target, bestPos.getX(), bestPos.getZ());

                    double bestDistance = Math.abs(MathHelper.wrapAngleTo180_float(bestYaw));

                    double currentDistance = Math.abs(MathHelper.wrapAngleTo180_float(currentYaw));

                    if (currentDistance >= bestDistance && currentDistance > 150) {
                        bestPos = vec3;
                    }
                }

                if (bestPos != null) {
                    newYaw += RotationUtils.getYawChangeGiven(x + bestPos.getX(), z + bestPos.getZ(), speedYaw);
                    return newYaw;
                }
            }

            int currentStep = (1);

            if (closest == 0) {
                closest = posList.size();
            }

            int closestPos = (36 + closest + currentStep) % posList.size();

            Vec3 vestPos = posList.get(Math.max(closestPos, posList.size() - 1));

            newYaw += RotationUtils.getYawChangeGiven(x + vestPos.getX(), z + vestPos.getZ(), speedYaw);

        }
        return newYaw;
    }

}
