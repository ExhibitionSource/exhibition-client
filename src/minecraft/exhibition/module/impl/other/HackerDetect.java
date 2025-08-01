/*
 * Copyright (c) MineSense.pub 2018.
 * Developed by Arithmo
 */

package exhibition.module.impl.other;

import exhibition.event.Event;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.*;
import exhibition.management.PriorityManager;
import exhibition.management.friend.FriendManager;
import exhibition.management.notifications.usernotification.Notifications;
import exhibition.module.Module;
import exhibition.module.data.ModuleData;
import exhibition.module.data.MultiBool;
import exhibition.module.data.settings.Setting;
import exhibition.module.impl.combat.AntiBot;
import exhibition.util.HypixelUtil;
import exhibition.util.MathUtils;
import exhibition.util.RenderingUtil;
import exhibition.util.TeamUtils;
import exhibition.util.misc.ChatUtil;
import exhibition.util.render.Colors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HackerDetect extends Module {

    private Setting<Boolean> killaura = new Setting<>("AURA", false);
    private Setting<Boolean> autoBlock = new Setting<>("AUTOBLOCK", true);
    private Setting<Boolean> speed = new Setting<>("BHOP", true);
    private Setting<Boolean> cleaner = new Setting<>("CLEANER", false);
    private Setting<Boolean> fastfly = new Setting<>("BLINK-FLY", false);
    private Setting<Boolean> scaffold = new Setting<>("FLY/SCAFFOLD", true);
    private Setting<Boolean> phase = new Setting<>("CAGE PHASE", true);
    private Setting<Boolean> debugPhase = new Setting<>("DEBUG PHASE", false);
    private Setting<Boolean> chatBypass = new Setting<>("CHAT BYPASS", true);

    private MultiBool checks = new MultiBool("Checks", chatBypass, killaura, autoBlock, speed, cleaner, fastfly, scaffold, phase, debugPhase);

    private Setting<Boolean> teams = new Setting<>("TEAMS", false, "Doesn't report teammates.");

    private boolean ignore;
    private boolean hypixelLag;
    private double phasePosY = -1;

    private Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");

    public HackerDetect(ModuleData data) {
        super(data);
        settings.put("REPORT", new Setting<>("REPORT", false, "Automatically report players who are suspicious."));
        settings.put("CHECKS", new Setting<>("CHECKS", checks, "Which checks HackerDetect should use."));
        addSetting(teams);
    }

    private double defaultSpeed(EntityPlayer ent) {
        double baseSpeed = 0.27999999999999997D;
        if (ent.isPotionActive(Potion.moveSpeed)) {
            int amplifier = ent.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= (1.0D + 0.2D * (amplifier + 1));
        }
        return baseSpeed;
    }

    public void reset() {
        phasePosY = -1;
        hypixelLag = false;
        ignore = false;
    }

    @RegisterEvent(events = {EventMotionUpdate.class, EventSpawnEntity.class, EventPacket.class, EventTick.class, EventRender3D.class})
    public void onEvent(Event event) {
        if (mc.thePlayer == null || mc.theWorld == null || !mc.thePlayer.isAllowEdit()) {
            return;
        }

        if (event instanceof EventRender3D) {
            EventRender3D er = event.cast();
            if (!debugPhase.getValue())
                return;

            if (phasePosY != -1 && HypixelUtil.isInGame("SKYWARS") && (HypixelUtil.isGameStarting() || HypixelUtil.scoreboardContains("start 0:0"))) {
                GL11.glPushMatrix();
                RenderingUtil.pre3D();
                mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2);

                double x = (mc.thePlayer.prevPosX + (mc.thePlayer.posX - mc.thePlayer.prevPosX) * er.renderPartialTicks) - RenderManager.renderPosX;
                double y = phasePosY - RenderManager.renderPosY;
                double z = (mc.thePlayer.prevPosZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * er.renderPartialTicks) - RenderManager.renderPosZ;
                GlStateManager.translate(x, y, z);

                AxisAlignedBB var11 = mc.thePlayer.getEntityBoundingBox().expand(2, 0, 2);
                AxisAlignedBB var12 = new AxisAlignedBB(var11.minX - mc.thePlayer.posX, var11.minY - 0.01 - mc.thePlayer.posY, var11.minZ - mc.thePlayer.posZ, var11.maxX - mc.thePlayer.posX, var11.minY + 0.01 - mc.thePlayer.posY, var11.maxZ - mc.thePlayer.posZ);

                RenderingUtil.glColor(Colors.getColor(255, 75));
                RenderingUtil.drawBoundingBox(var12);

                GL11.glLineWidth(2);
                RenderingUtil.glColor(Colors.getColor(255, 255));
                RenderingUtil.drawOutlinedBoundingBox(var12);

                RenderingUtil.post3D();
                GL11.glPopMatrix();
            }
        }

        if (event instanceof EventPacket) {
            EventPacket ep = event.cast();
            Packet packet = ep.getPacket();
            if (packet instanceof S08PacketPlayerPosLook) {
                S08PacketPlayerPosLook posLook = (S08PacketPlayerPosLook) packet;
                /*
                BUM CHECK DOESNT WORK AND FLAGS EVERYONE FOR PHASING
                Kept for future reference, weird doesnt detect glass below you sometimes, even though it is glass
                also fences are not taken into account / other misc cages
                 */
//                boolean isOverGlass = false;
//
//                for (int i = 0; i < 4; i++) {
//                    IBlockState bruh = mc.theWorld.getBlockState(new BlockPos(posLook.getX(), (int) posLook.getY() - (i + 1), posLook.getZ()));
//
//                    if(bruh.getBlock().getMaterial() == Material.glass || bruh.getBlock() == Blocks.stained_glass || bruh.getBlock() == Blocks.glass) {
//                        isOverGlass = true;
//                        break;
//                    }
//                }

                /*
                Bum fix to not having other checks, but does fix detecting whole lobby
                 */
                if (mc.thePlayer.ticksExisted <= 10) {
                    reset();
                }
            }

            if (packet instanceof S02PacketChat && chatBypass.getValue()) {
                S02PacketChat s02PacketChat = (S02PacketChat) packet;
//                String[] charList = new String[]{"\u05fc"};
//                for (String character : charList) {
//                    String unformatted = s02PacketChat.getChatComponent().getUnformattedText();
//                    if (unformatted.contains(":") && unformatted.contains(character)) {
//                        List<Entity> validPlayers = mc.theWorld.getLoadedEntityList().stream().filter(o -> o instanceof EntityPlayer && o != mc.thePlayer &&
//                                !AntiBot.isBot(o) && !o.isInvisible() && !FriendManager.isFriend(o.getName())).collect(Collectors.toList());
//                        for (Entity entityPlayer : validPlayers) {
//                            EntityPlayer ent = (EntityPlayer) entityPlayer;
//                            if ((teams.getValue() && TeamUtils.isTeam(mc.thePlayer, ent)))
//                                continue;
//
//                            if (unformatted.contains(ent.getName()) && !PriorityManager.isPriority(ent)) {
//                                Notifications.getManager().post("Hacker Detected", ent.getName() + " may be using Chat Bypass.", 7500, Notifications.Type.WARNING);
//                                if ((boolean) settings.get("REPORT").getValue())
//                                    ChatUtil.sendChat("/wdr " + ent.getName() + " bhop");
//                                PriorityManager.setAsPriority(ent);
//                                break;
//                            }
//                        }
//                    }
//                }

                String unformatted = s02PacketChat.getChatComponent().getUnformattedText();
                if (unformatted.contains(": ")) {
                    List<Entity> validPlayers = new ArrayList<>(mc.theWorld.getLoadedEntityList()).stream().filter(o -> o instanceof EntityPlayer && o != mc.thePlayer &&
                            !AntiBot.isBot(o) && !o.isInvisible() && !FriendManager.isFriend(o.getName())).collect(Collectors.toList());
                    for (Entity entityPlayer : validPlayers) {
                        EntityPlayer ent = (EntityPlayer) entityPlayer;
                        if ((teams.getValue() && TeamUtils.isTeam(mc.thePlayer, ent)))
                            continue;
                        int unicodeCount = 0;

                        try {
                            String message = unformatted.split(": ")[1];

                            boolean valid = pattern.matcher(message).matches();

                            if (!valid) {
                                for (int i = 0; i < message.length(); i++) {
                                    char character = message.charAt(i);
                                    if (!Character.isAlphabetic(character) && mc.fontRendererObj.getStringWidth(character + "") == 0) {
                                        unicodeCount++;
                                        if (unicodeCount > 2 && unformatted.contains(ent.getName()) && !PriorityManager.isPriority(ent)) {
                                            Notifications.getManager().post("Hacker Detected", ent.getName() + " may be using " + (character == '\u05fc' ? "Novoline (CB)." : "Chat Bypass."), 7500, Notifications.Type.WARNING);
                                            PriorityManager.setAsPriority(ent);
                                            if ((boolean) settings.get("REPORT").getValue()) {
                                                ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }

            return;
        }

        if (event instanceof EventSpawnEntity) {
            if (cleaner.getValue()) {
                EventSpawnEntity esp = event.cast();
                Entity entity = esp.getEntity();
                if (entity instanceof EntityItem) {
                    for (Entity ent : mc.theWorld.getLoadedEntityList()) {
                        if (ent == mc.thePlayer)
                            continue;

                        if (ent instanceof EntityPlayer) {
                            if (ent.getDistanceToEntity(entity) < 2 && !FriendManager.isFriend(ent.getName())) {
                                EntityPlayer player = (EntityPlayer) ent;

                                if (PriorityManager.isPriority(player) || (((boolean) settings.get("TEAMS").getValue()) && TeamUtils.isTeam(mc.thePlayer, player)))
                                    continue;

                                double motionX = Math.abs(player.posX - player.lastTickPosX);
                                double motionZ = Math.abs(player.posZ - player.lastTickPosZ);

                                boolean above = entity.posY > player.posY + 1;

                                double velocity = MathUtils.roundToPlace(Math.sqrt(Math.pow(motionX, 2) + Math.pow(motionZ, 2)), 3);

                                boolean isValidItem = (player.getHeldItem() == null || player.getHeldItem().getItem() == null) || !(player.getHeldItem().getItem() instanceof ItemFishingRod);

                                if (above && velocity >= 0.2 && (player.lastDroppedTick == -1 || (player.ticksExisted - player.lastDroppedTick) < 10) && player.lastDroppedTick != player.ticksExisted && isValidItem) {
                                    player.invWalkTicks++;
                                    if (player.invWalkTicks > 10) {
                                        Notifications.getManager().post("Hacker Detected", player.getName() + " may be using InventoryCleaner.", 7500, Notifications.Type.WARNING);
                                        if ((boolean) settings.get("REPORT").getValue())
                                            ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                                        PriorityManager.setAsPriority(player);
                                    }
                                    player.lastDroppedTick = player.ticksExisted;
                                }
                            }
                        }
                    }
                }
                return;
            }
        }

        if (event instanceof EventMotionUpdate) {
            EventMotionUpdate em = event.cast();
            if (!em.isPre())
                return;

            Setting<Boolean>[] checksList = new Setting[]{killaura, autoBlock, speed, cleaner, fastfly, scaffold};

            boolean allow = false;
            for (Setting<Boolean> setting : checksList) {
                if (setting.getValue()) {
                    allow = true;
                    break;
                }
            }

            if (!allow)
                return;

            List<Entity> validPlayers = mc.theWorld.getPlayerEntities().stream().filter(o -> o != mc.thePlayer &&
                    !AntiBot.isBot(o) && !o.isInvisible() && !FriendManager.isFriend(o.getName())).collect(Collectors.toList());
            for (Entity entityPlayer : validPlayers) {
                EntityPlayer ent = (EntityPlayer) entityPlayer;
                if ((teams.getValue() && TeamUtils.isTeam(mc.thePlayer, ent)))
                    continue;

                if (cleaner.getValue()) {
                    if ((ent.ticksExisted - ent.lastDroppedTick) > 40) {
                        ent.lastDroppedTick = -1;
                    }
                }

                //
                if (autoBlock.getValue() && !ent.isRiding()) {
                    double motionX = Math.abs(ent.posX - ent.lastTickPosX);
                    double motionZ = Math.abs(ent.posZ - ent.lastTickPosZ);

                    double speed = MathUtils.roundToPlace(Math.hypot(motionX, motionZ), 2);

                    boolean isOverGround = mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 0.5, ent.posZ)).getBlock().getMaterial() != Material.air ||
                            mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 1, ent.posZ)).getBlock().getMaterial() != Material.air ||
                            mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 1.5, ent.posZ)).getBlock().getMaterial() != Material.air;

                    if (isOverGround && (ent.isBlocking() || (ent.isUsingItem() && ent.getItemInUseDuration() > 5) && ent.isSprinting()) && speed > 0.4 && speed < 7 && !PriorityManager.isPriority(ent)) {
                        //ChatUtil.debug(ent.getName() + " is moving fast " + ent.isSprinting() + " " + Math.hypot(motionX, motionZ));
                        //ChatUtil.debug(ent.isBlocking() + " " + ent.getItemInUseDuration());
                        ent.speedFlags += speed > 0.45 ? (speed / 0.4 * 3) : 1;
                        if (ent.speedFlags > 25) {
                            Notifications.getManager().post("Hacker Detected", ent.getName() + " may be using " + (ent.isBlocking() ? "AutoBlock!" : "NoSlowdown!"), 7500, Notifications.Type.WARNING);
                            if ((boolean) settings.get("REPORT").getValue())
                                ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                            PriorityManager.setAsPriority(ent);
                        }
                    }

//                if(ent.isBlocking() && ent.isSwingInProgress) {
//
//                    int lastSwingDelta = mc.thePlayer.ticksExisted - ent.lastBlockSwingTick;
//
//                    if(lastSwingDelta > 11) {
//                        ent.lastBlockSwingTick = mc.thePlayer.ticksExisted;
//                    }
//
//                    if(lastSwingDelta > 5) {
//
//                    }
//
//                    ent.lastBlockSwingTick = mc.thePlayer.ticksExisted;
//
//                    ChatUtil.debug(mc.thePlayer.ticksExisted + " " + ent.getName() + " is blocking and swinging " + ent.swingProgressInt + " " + ent.prevSwingProgress + " " + ent.swingProgress);
//                    if(ent.swingProgressInt == -1 && ) {
//                    }
//
//                }
                }

                // Blink fly detection
                if (fastfly.getValue()) {
                    if (ent.hurtTime == 9 && ent.getDistance(ent.lastTickPosX, ent.posY, ent.lastTickPosZ) < 1) {
                        ent.lastMovedTick = ent.ticksExisted;
                    }

                    if (ent.lastMovedTick != -1 && (ent.ticksExisted - ent.lastMovedTick) > 5 && !PriorityManager.isPriority(ent) && ent.hurtTime <= 1) {
                        if (ent.getDistance(ent.lastTickPosX, ent.posY, ent.lastTickPosZ) > 5 && ent.getDistance(ent.posX, ent.lastTickPosY, ent.posZ) < 2) {
                            if ((ent.ticksExisted - ent.lastMovedTick) > 20) {
                                Notifications.getManager().post("Hacker Detected", ent.getName() + " has irregular movements (Blink Fly).", 7500, Notifications.Type.WARNING);
                                if ((boolean) settings.get("REPORT").getValue())
                                    ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                                PriorityManager.setAsPriority(ent);

                            }
                        }
                    }
                }

                // Fly Detection
                if (scaffold.getValue()) {
                    double motionX = Math.abs(ent.posX - ent.lastTickPosX);
                    double motionZ = Math.abs(ent.posZ - ent.lastTickPosZ);
                    double motionY = Math.abs(ent.posY - ent.lastTickPosY);

                    if ((Math.sqrt(motionX * motionX + motionZ * motionZ) > 0.23) && motionY <= 0.005 && !ent.isInvisible() && !ent.isRiding() && !ent.isSneaking()) {
                        if (mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 0.45, ent.posZ)).getBlock() == Blocks.air && mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 1.35, ent.posZ)).getBlock() == Blocks.air) {
                            ent.flags += ent.onGround ? 4 : 1;
                            if (Math.sqrt(motionX * motionX + motionZ * motionZ) > defaultSpeed(ent))
                                ent.flags += Math.pow(Math.sqrt(motionX * motionX + motionZ * motionZ), 2);
                            if (ent.flags >= 35 && !PriorityManager.isPriority(ent)) {
                                Notifications.getManager().post("Hacker Detected", ent.getName() + " has irregular movements (Fly/Scaffold).", 7500, Notifications.Type.WARNING);
                                if ((boolean) settings.get("REPORT").getValue())
                                    ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                                PriorityManager.setAsPriority(ent);
                            }
                            //DevNotifications.getManager().post(ent.getName() + " is flying?");
                        }

                        if(ent.getHeldItem() != null && ent.getHeldItem().getItem() instanceof ItemBlock) {
                            // Taken from novoline: Z2.b((a4S)a4S2, (float)(ew.b((int)aRc.d((Mg)this.a.q.bJ)) && !Vl.q((Z5)this.a.F) ? 87.0f : 78.0f));
                            if(Math.abs(ent.rotationPitch - 78.0F) <= 1 || Math.abs(ent.rotationPitch - 87.0f) <= 1) {
                                ent.flags += 5;
                                if (ent.flags >= 30 && !PriorityManager.isPriority(ent)) {
                                    Notifications.getManager().post("Hacker Detected", ent.getName() + " is using Scaffold (Novoline).", 7500, Notifications.Type.WARNING);
                                    if ((boolean) settings.get("REPORT").getValue())
                                        ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " bhop");
                                    PriorityManager.setAsPriority(ent);
                                }
                            }
                        }
                    }


                    if (!PriorityManager.isPriority(ent) && ent.flags != 0 && ent.flags < 10 && ent.lastTickPosY > ent.posY && ent.onGround && mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 0.5, ent.posZ)).getBlock() != Blocks.air) {
                        ent.flags = 0;
                        //DevNotifications.getManager().post(ent.getName() + " reset flags!");
                    }
                }

                // Speed Detection
                if (speed.getValue() && !ent.isRiding()) {
                    double motionX = ent.posX - ent.lastTickPosX;
                    double motionZ = ent.posZ - ent.lastTickPosZ;

                    double motionY = ent.posY - ent.lastTickPosY;

                    double velocity = Math.sqrt(motionX * motionX + motionZ * motionZ);

                    boolean jumped = motionY > 0.1 && motionY < 0.5 && ent.lastMotionY < 0.1;

                    int tickInAir = ent.onGround ? 0 : ent.ticksExisted - ent.jumpedTick;

                    float lastAirYaw = ent.lastAirYaw;

                    int speedFlags = 0;

                    float yawDirection;

                    if ((motionZ < 0.0D) && (motionX < 0.0D)) {
                        yawDirection = 90.0F + (float) Math.toDegrees(Math.atan(motionZ / motionX));
                    } else if ((motionZ < 0.0D) && (motionX > 0.0D)) {
                        yawDirection = -90.0F + (float) Math.toDegrees(Math.atan(motionZ / motionX));
                    } else {
                        yawDirection = (float) Math.toDegrees(-Math.atan(motionX / motionZ));
                    }

                    if (Float.isNaN(yawDirection)) {
                        yawDirection = ent.rotationYaw;
                    }

                    if (jumped) {
                        ent.jumpedTick = ent.ticksExisted;

                        if (Float.isNaN(yawDirection)) {
                            yawDirection = ent.rotationYaw;
                        }

                        ent.jumpedYaw = yawDirection;
                        ent.lastAirYaw = yawDirection;

                        float diff = Math.abs(MathHelper.wrapAngleTo180_float(-(MathHelper.wrapAngleTo180_float(ent.rotationYaw) - (float) MathHelper.wrapAngleTo180_float(yawDirection))));

                        if (MathUtils.roundToPlace(velocity, 1) >= 0.355 && Math.round(diff) > 44.5) {
                            speedFlags += diff > 120 ? 15 : diff > 90 ? 7 : 3;
                            //ChatUtil.debug("Yaw Diff: " + diff + " " + ent.getName() + " " + velocity);
                        }
                    }

//                    if (ent.onGround && Math.abs(motionY) < 0.2) {
//                        float diff = Math.abs(MathHelper.wrapAngleTo180_float(-(MathHelper.wrapAngleTo180_float(ent.rotationYaw) - (float) MathHelper.wrapAngleTo180_float(yawDirection))));
//
//                        if (diff > 120 && ent.isSprinting() && velocity >= 0.2) {
//                            ChatUtil.debug(ent.getName() + " omni sprint " + diff + " " + velocity + " " + ent.onGround);
//                        }
//                    }

                    if (tickInAir > 0 && tickInAir < 10) {
                        float diff = Math.abs(MathHelper.wrapAngleTo180_float(-(MathHelper.wrapAngleTo180_float(lastAirYaw) - (float) MathHelper.wrapAngleTo180_float(yawDirection))));

                        if (diff > 60 && MathUtils.roundToPlace(velocity, 1) >= 0.36 && velocity < 2) {
                            speedFlags += diff > 100 ? 7 : diff > 60 ? 5 : 2;
                            ent.lastFlaggedTick = ent.ticksExisted;
                            ent.lastAirYaw = yawDirection;
                            //ChatUtil.debug("Yaw Diff Air " + tickInAir + ": " + diff + " " + ent.getName() + " " + velocity);
                        }
                    }

                    if (ent.isBlocking()) {
                        speedFlags *= 1.5;
                    }

                    ent.speedFlags += speedFlags;

//                    if((ent.lastMotionY < 0 || ent.lastMotionY == 0) && (motionY > 0.2 && motionY < 0.47) && ent.onGround && mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - motionY - 0.5, ent.posZ)).getBlock() != Blocks.air) {
//                        ChatUtil.debug("Hmm? " + ent.getName() + " " + ent.ticksExisted + " " + motionY + " " + ent.posY);
//                    }
//
//                    if(motionY < -0.55 && ent.lastMotionY < 0 && ent.lastMotionY > motionY && ent.onGround && mc.theWorld.getBlockState(new BlockPos(ent.posX, ent.posY - 1 + motionY, ent.posZ)).getBlock() == Blocks.air) {
//                        ChatUtil.debug("\247cNoFall? " + ent.getName() + " " + ent.ticksExisted + " " + motionY + " " + ent.posY);
//                    }

                    ent.lastMotionY = motionY;

                    if (ent.onGround && (ent.ticksExisted - ent.jumpedTick) > 20 && !PriorityManager.isPriority(ent)) {
                        if (ent.speedFlags > 0)
                            ent.speedFlags -= 5;
                        if (ent.lowhopFlags > 0)
                            ent.lowhopFlags--;
                    }

                    if (ent.speedFlags >= 25 && (ent.ticksExisted - ent.lastFlaggedTick) < 5 && !PriorityManager.isPriority(ent) && isBlockUnder(ent)) {
                        Notifications.getManager().post("Hacker Detected", ent.getName() + " may be using Speed.", 7500, Notifications.Type.WARNING);
                        if ((boolean) settings.get("REPORT").getValue())
                            ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " killaura fly speed scaffold");
                        PriorityManager.setAsPriority(ent);
                    }

                }

                // Irregular Movement Detection
                if (killaura.getValue()) {
                    double motionX = ent.posX - ent.lastTickPosX;
                    double motionZ = ent.posZ - ent.lastTickPosZ;
                    double motionY = ent.posY - ent.lastTickPosY;
                    boolean flagged = false;
                    if (Math.sqrt(motionX * motionX + motionZ * motionZ) > (defaultSpeed(ent) * 1.5) && motionY <= 0) { // Teleport
                        float yawDirection;

                        if ((motionZ < 0.0D) && (motionX < 0.0D)) {
                            yawDirection = 90.0F + (float) Math.toDegrees(Math.atan(motionZ / motionX));
                        } else if ((motionZ < 0.0D) && (motionX > 0.0D)) {
                            yawDirection = -90.0F + (float) Math.toDegrees(Math.atan(motionZ / motionX));
                        } else {
                            yawDirection = (float) Math.toDegrees(-Math.atan(motionX / motionZ));
                        }

                        if (Float.isNaN(yawDirection)) {
                            yawDirection = ent.rotationYaw;
                        }

                        float diff = MathHelper.wrapAngleTo180_float(-(MathHelper.wrapAngleTo180_float(ent.rotationYaw) - (float) MathHelper.wrapAngleTo180_float(yawDirection)));

                        if (Math.abs(diff) > 85) {
                            ent.flags++;
                            flagged = true;
                        }
                    }


                    if (ent.flags >= 30 && !PriorityManager.isPriority(ent) && flagged) {
                        Notifications.getManager().post("Hacker Detected", ent.getName() + " has irregular movements (Aura/Hop).", 7500, Notifications.Type.WARNING);
                        if ((boolean) settings.get("REPORT").getValue())
                            ChatUtil.sendChat_NoFilter("/wdr " + ent.getName() + " killaura fly speed scaffold");
                        PriorityManager.setAsPriority(ent);
                    }
                }

            }
        }
        if (event instanceof EventTick) {
            if (!phase.getValue())
                return;

                    /*
                    Initial y value
                     */
            if (mc.thePlayer.ticksExisted == 30) {
                if (HypixelUtil.scoreboardContains("hypixel")) {
                    //ChatUtil.printChat("Phase pos A " + (int) mc.thePlayer.posY);
                    ignore = false;
                    phasePosY = mc.thePlayer.posY;
                    hypixelLag = false;
                } else {
                    hypixelLag = true;
                }
            }

                    /*
                    Lag check
                     */
            if (hypixelLag) {
                if (HypixelUtil.isGameStarting() && HypixelUtil.isInGame("SKYWARS")) {
                    //ChatUtil.printChat("Phase pos B " + (int) mc.thePlayer.posY);
                    ignore = false;
                    phasePosY = mc.thePlayer.posY;
                    hypixelLag = false;
                } else {
                    hypixelLag = true;
                }
            }

            if (!HypixelUtil.isGameStarting() && HypixelUtil.isInGame("SKYWARS") && phasePosY != -1 && !ignore) {
                //ChatUtil.printChat("Reset phase pos");
                phasePosY = -1;
                hypixelLag = false;
            }

                    /*
                    Team skywars cage check
                     */
            if ((HypixelUtil.scoreboardContains("start 0:0") && HypixelUtil.isInGame("SKYWARS") && HypixelUtil.scoreboardContains("teams left")) && phasePosY == -1) {
                //ChatUtil.printChat("Phase pos C " + (int) mc.thePlayer.posY);
                phasePosY = mc.thePlayer.posY;
                ignore = true;
            }

            List<Entity> validPlayers = mc.theWorld.getPlayerEntities().stream().filter(o -> o != mc.thePlayer && !AntiBot.isBot(o)).collect(Collectors.toList());
            for (Entity entityPlayer : validPlayers) {
                EntityPlayer ent = (EntityPlayer) entityPlayer;
                if (ent.isInvisible() || FriendManager.isFriend(ent.getName()) || (((boolean) settings.get("TEAMS").getValue()) && TeamUtils.isTeam(mc.thePlayer, ent)))
                    continue;
                /*
                Phase check
                 */
                    /*
                    Team skywars cage check
                     */
                if (!PriorityManager.isPriority(ent) && ent.ticksExisted > 40 && HypixelUtil.scoreboardContains("start") && HypixelUtil.isInGame("SKYWARS") && HypixelUtil.scoreboardContains("teams left")) {
                    if (phasePosY - ent.posY > 4.5) {
                        Notifications.getManager().post("Hacker Detected", ent.getName() + " has phased out of their cage!", 7500, Notifications.Type.WARNING);
                        PriorityManager.setAsPriority(ent);
                    }
                }

                    /*
                    Check for phase
                     */
                if (!PriorityManager.isPriority(ent) && ent.ticksExisted > 40 && HypixelUtil.isInGame("SKYWARS") && !HypixelUtil.isGameActive() && HypixelUtil.isGameStarting()) {
                    if (phasePosY - ent.posY > 4.5) {
                        Notifications.getManager().post("Hacker Detected", ent.getName() + " has phased out of their cage!", 7500, Notifications.Type.WARNING);
                        PriorityManager.setAsPriority(ent);
                    }
                }
                /*
                DUMB ANTIFALL BULLSHIT
                 */
//                if (!isBlockUnder(ent) && !ent.onGround && !fallCheck){
//                    fallCheck = true;
//                }
//                if (fallCheck){
//                    if (bruhTick == 0){
//                        lastPosY = ent.serverPosY;
//                        lastHealth = ent.getHealth();
//                        bruhTick++;
//                    }
//                    if (bruhTick < 5)
//                        bruhTick++;
//                    if (bruhTick == 5){
//                        if ((lastPosY - ent.serverPosY) / 32 < -5){
//                            if (ent.getHealth() >= lastHealth){
//                                if (!PriorityManager.isPriority(ent) && ent.ticksExisted > 40) {
//                                    ChatUtil.debug("Test: " + (lastPosY / 32) + " " +(ent.serverPosY /32) + " " + (lastPosY - ent.serverPosY) /32);
//                                    ChatUtil.debug("" + ent.getName() + " " + !ent.hitByEntity(entityPlayer));
//                                    Notifications.getManager().post("Hacker Detected", ent.getName() + " may be using AntiFall.", 7500, Notifications.Type.WARNING);
//                                    PriorityManager.setAsPriority(ent);
//                                }
//                            }
//                        }
//                        bruhTick = 0;
//                        fallCheck = false;
//                    }
//                }
            }
        }
    }

    private boolean isBlockUnder(EntityPlayer player) {
        for (int i = (int) (player.posY); i >= 0; i--) {
            BlockPos pos = new BlockPos(player.posX, i, player.posZ);
            if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTouchingGround(EntityPlayer player) {
        boolean touchingGround = false;
        double[][] offsets = new double[][]{new double[]{0, 0}, new double[]{-0.35, -0.35}, new double[]{-0.35, 0.35}, new double[]{0.35, 0.35}, new double[]{0.35, -0.35}};
        for (double[] offset : offsets) {
            double offsetX = offset[0];
            double offsetZ = offset[1];

            double posX = offsetX + player.posX;
            double posY = -0.5 + player.posY;
            double posZ = offsetZ + player.posZ;

            double lastPosX = offsetX + player.lastTickPosX;
            double lastPosY = -0.5 + player.lastTickPosY;
            double lastPosZ = offsetZ + player.lastTickPosZ;

            if (isPosOnGround(posX, posY, posZ) && isPosOnGround(lastPosX, lastPosY, lastPosZ)) {
                touchingGround = true;
                break;
            }
        }
        return touchingGround;
    }

    public boolean isPosOnGround(double posX, double posY, double posZ) {
        boolean isOnSlab = MathUtils.roundToPlace((posY - (int) posY), 1) == 0.5;

        Block nextBlockUnder = mc.theWorld.getBlockState(new BlockPos(posX, posY - (isOnSlab ? 0 : 0.1), posZ)).getBlock();

        boolean feetBlockAir = isOnSlab ? nextBlockUnder.getMaterial() == Material.air : (nextBlockUnder instanceof BlockSlab && !nextBlockUnder.isFullBlock()) || nextBlockUnder.getMaterial() == Material.air;

        return !feetBlockAir && !mc.theWorld.getBlockState(new BlockPos(posX, posY + 1.5D, posZ)).getBlock().isBlockNormalCube();
    }

}
