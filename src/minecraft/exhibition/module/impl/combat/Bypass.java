package exhibition.module.impl.combat;

import exhibition.Client;
import exhibition.event.Event;
import exhibition.event.EventSystem;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.*;
import exhibition.management.notifications.dev.DevNotifications;
import exhibition.management.notifications.usernotification.Notifications;
import exhibition.module.Module;
import exhibition.module.data.ModuleData;
import exhibition.module.data.Options;
import exhibition.module.data.settings.Setting;
import exhibition.module.impl.movement.Fly;
import exhibition.module.impl.movement.LongJump;
import exhibition.util.*;
import exhibition.util.Timer;
import exhibition.util.render.Colors;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S30PacketWindowItems;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Bypass extends Module {

    private final ConcurrentLinkedQueue<BruhPacket> packetList = new ConcurrentLinkedQueue<>();

    private final Queue<Packet> chokePackets = new ConcurrentLinkedQueue<>();

    public Setting<Number> DELAY = new Setting<>("DELAY", 300, "Spoof offset. This should be 500 - (your ping).", 5, 0, 1000);
    public Setting<Boolean> AUTOBYPASS = new Setting<>("AUTOBYPASS", false, "Automatically detects optimal delay value. (Hypixel Compass)");
    public Setting<Boolean> AUTODONG = new Setting<>("AUTO DONG", false, "If Watchdog Off is bugged, Dong mode will be used.");
    public Options option = new Options("Mode", "Watchdog Off", "Watchdog Off", "Dong", "Old");

    private long startMS = -1;
    private int state = 0;

    public int bruh;
    public int randomDelay;
    public int lastUid;
    public int lastSentUid = -1;

    private final Timer c13Timer = new Timer();

    public Bypass(ModuleData data) {
        super(data);
        addSetting(DELAY);
        addSetting(AUTOBYPASS);
        addSetting(AUTODONG);
        addSetting(new Setting<>("MODE", option, "Which bypass method should be used. (DETECTED)"));
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGHEST;
    }

    public void reset() {
        this.startMS = -1;
        this.state = 0;
    }

    public boolean allowBypassing() {
        if (mc.thePlayer == null)
            return false;
        return ((isEnabled() && !option.getSelected().equals("Old")) || shouldSabotage()) && (mc.thePlayer.isAllowEdit() || HypixelUtil.isInGame("HOUSING") || HypixelUtil.isInGame("ZOMBIE"));
    }

    public void worldChange() {
        this.bruh = 0;
        this.lastUid = 0;
        this.lastSentUid = -1;
        if (!option.getSelected().equals("Watchdog Off"))
            this.resetPackets();
        //DevNotifications.getManager().post("\247d\247lRESET");

    }

    @Override
    public void onEnable() {
        if(mc.thePlayer == null || mc.theWorld == null || mc.getCurrentServerData() == null || mc.getIntegratedServer() != null || HypixelUtil.isVerifiedHypixel()) {
            toggle();
            EventSystem.unregister(this);
        }
    }

    @Override
    public void onDisable() {

    }

    private final Random random = new Random();

    private String lastMode = null;

    @RegisterEvent(events = {EventPacket.class, EventTick.class, EventScreenDisplay.class, EventRenderGui.class})
    public void onEvent(Event event) {
        if (mc.getIntegratedServer() != null)
            return;

        setHidden(false);

        if (event instanceof EventTick) {
            List<BruhPacket> packetsToRemove = new ArrayList<>();
            for (BruhPacket bruhPacket : packetList) {
                if (bruhPacket.timeToSend <= System.currentTimeMillis()) {
                    NetUtil.sendPacketNoEvents(bruhPacket.packet);
                    packetsToRemove.add(bruhPacket);
                }
            }
            packetList.removeAll(packetsToRemove);

            // If bruh is not set or they're in a lobby and it's set, let them change it
            if (lastMode != null && bruh > 0 && allowBypassing()) {
                if (!option.getSelected().equals(lastMode)) {
                    if (lastMode.equals("Watchdog Off") && lastSentUid >= 1) {
                        this.bruh = 0;
                        this.lastUid = 0;
                        this.lastSentUid = -1;
                        this.sendPackets();
                        return;
                    }

                    option.setSelected(lastMode);
                    Notifications.getManager().post("Bypass Warning", "Please change the method before a match.", 3_000, Notifications.Type.WARNING);
                }
            }
            lastMode = option.getSelected();

            setSuffix(option.getSelected());
            if (AUTOBYPASS.getValue()) {
                if (mc.currentScreen == null && mc.thePlayer.ticksExisted > 25 && startMS == -1 && state == 0) {
                    if (mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() != null) {
                        Item item = mc.thePlayer.inventory.getCurrentItem().getItem();
                        if (Item.getIdFromItem(item) == 345) {
                            ItemStack itemstack1 = mc.thePlayer.inventory.getCurrentItem();
                            startMS = System.currentTimeMillis();
                            state = 1;
                            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, itemstack1);
                        }
                    }
                }
            }
            return;
        }

        Fly fly = Client.getModuleManager().get(Fly.class).cast();
        LongJump longJump = Client.getModuleManager().get(LongJump.class);
        boolean fallFly = fly.isEnabled() && !((boolean) fly.getSetting("BLORP").getValue()) && (boolean) fly.getSetting("CHOKE").getValue();

        boolean isFlying = Client.getModuleManager().isEnabled(LongJump.class) || fallFly;

        if (event instanceof EventPacket) {

            if (option.getSelected().equals("Old")) {
                if (mc.thePlayer != null) {
                    if (!isFlying || c13Timer.delay(15_000)) {
                        c13Timer.reset();
                        sendPackets();
                    }
                } else {
                    resetPackets();
                }
            }

            if (event.isCancelled())
                return;
            EventPacket ep = (EventPacket) event;

            Packet p = ep.getPacket();

            if (p instanceof S30PacketWindowItems && (boolean) AUTOBYPASS.getValue()) {
                if (state == 2) {
                    state = 3;
                    event.setCancelled(true);
                }
            }

            if (p instanceof S2DPacketOpenWindow && (boolean) AUTOBYPASS.getValue()) {
                S2DPacketOpenWindow packetOpenWindow = (S2DPacketOpenWindow) p;
                if (state == 1 && packetOpenWindow.getGuiId().contains("chest")) {
                    long roundTrip = (System.currentTimeMillis() - startMS);
                    int newBypass = 500 - (int) (roundTrip / 2);
                    int bypass = Math.max((int) MathUtils.getIncremental(newBypass, 5), 0);
                    Notifications.getManager().post("Auto Bypass Delay", "Updated bypass MS to \247a" + bypass + "ms\247f.", 3000, Notifications.Type.OKAY);
                    DELAY.setValue(bypass);
                    state = 2;
                    event.setCancelled(true);
                    NetUtil.sendPacketNoEvents(new C0DPacketCloseWindow(packetOpenWindow.getWindowId()));
                }
            }

            boolean debug = false;

            if (HypixelUtil.isVerifiedHypixel())
                if (option.getSelected().equals("Dong")) {
                    if (mc.thePlayer == null) {
                        resetPackets();
                    } else {
                        boolean canSend = mc.thePlayer.ticksExisted % 7 != 0;
                        if (canSend && isFlying && c13Timer.delay(2500) && bruh > 20) {
                            c13Timer.reset();
                            int sent = 0;
                            while (chokePackets.peek() != null && sent < 1) {
                                Packet chokedPacket = chokePackets.poll();
                                if (chokedPacket != null) {
                                    sent++;
                                    packetList.add(new BruhPacket(chokedPacket, 0));
                                }
                            }
                            if (debug)
                                DevNotifications.getManager().post("\247e\247lRELIEF " + bruh);
                        }
                    }

                    if (p instanceof C0FPacketConfirmTransaction) {
                        C0FPacketConfirmTransaction packet = (C0FPacketConfirmTransaction) p;
                        if (packet.getUid() < 0) {
                            if (HypixelUtil.isVerifiedHypixel() && allowBypassing()) {
                                this.bruh++;

                                if (bruh > 5) {
                                    event.setCancelled(!shouldSabotage());
                                    if (Math.abs(packet.getUid() - lastUid) > 5 && packet.getUid() != -1) {
                                        chokePackets.add(packet);
                                        sendPackets();
                                        if (debug)
                                            DevNotifications.getManager().post("\247bRESET BRUH TO " + packet.getUid() + " " + lastUid);
                                        bruh = 0;
                                        lastUid = packet.getUid();
                                        return;
                                    }

                                    boolean canSend = !isFlying ? mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically : (mc.thePlayer.ticksExisted % 7 != 0);

                                    if (isFlying && (bruh - 10) >= (10)) {
                                        bruh = 25;
                                    }

                                    if ((bruh - 10) >= (45 + randomDelay) && (!isFlying || canSend)) {
                                        short lastbruh = (short) lastSentUid;
                                        int sent = 0;
                                        int size = chokePackets.size();
                                        chokePackets.add(packet);
                                        while (chokePackets.peek() != null) {
                                            Packet chokedPacket = chokePackets.poll();
                                            if (chokedPacket != null) {
                                                sent++;
                                                packetList.add(new BruhPacket(chokedPacket, 0));
                                            }
                                        }

                                        if (sent > 0 && debug) {
                                            DevNotifications.getManager().post("\247b\247lSent " + sent + " out of " + size);
                                        }

                                        lastSentUid = packet.getUid();
                                        if (debug)
                                            DevNotifications.getManager().post("\247eSent from \247c" + (lastbruh) + "\247e to \247a" + (lastSentUid + 1) + " " + mc.thePlayer.ticksExisted);
                                        randomDelay = 30 + (int) (random.nextInt(100));
                                        bruh = 10;
                                    } else {
                                        chokePackets.add(packet);
                                    }

                                    if (!isFlying)
                                        c13Timer.reset();

                                } else {
                                    event.setCancelled(!shouldSabotage());
                                    C0FPacketConfirmTransaction confirmTransaction = new C0FPacketConfirmTransaction(packet.getWindowId(), packet.getUid(), packet.getAccepted());
                                    this.packetList.add(new BruhPacket(confirmTransaction, Math.max(250, DELAY.getValue().longValue())));

                                    int transactions = 0;

                                    for (Packet chokePacket : chokePackets) {
                                        if (chokePacket instanceof C0FPacketConfirmTransaction)
                                            transactions++;
                                    }

                                    if (debug)
                                        DevNotifications.getManager().post("\247eNormal " + confirmTransaction.getUid() + " " + transactions);

                                    c13Timer.reset();

                                    lastSentUid = packet.getUid();
                                }
                                lastUid = packet.getUid();
                            } else {
                                event.setCancelled(!shouldSabotage());
                                this.packetList.add(new BruhPacket(packet, Math.max(250, DELAY.getValue().longValue())));
                            }
                        }
                    }
                } else if (option.getSelected().equals("Watchdog Off")) {
                    if (c13Timer.delay(1000) && !Client.instance.isLagging() && bruh > 0) {
                        if (debug)
                            DevNotifications.getManager().post("\247bBypass is ready.");
                        int sent = 0;
                        int max = chokePackets.size();
                        while (chokePackets.peek() != null && sent < max - 1) {
                            Packet chokedPacket = chokePackets.poll();
                            if (chokedPacket != null) {
                                sent++;
                                NetUtil.sendPacketNoEvents(chokedPacket);
                            }
                        }
                        if (debug && sent > 0) {
                            DevNotifications.getManager().post("\247b\247lSent " + sent);
                        }
                        bruh = -1;
                    }

                    if (c13Timer.delay(1000) && !Client.instance.isLagging() && bruh == -1) {
                        if (debug)
                            DevNotifications.getManager().post("\247aReady to resend bursts.");
                        bruh = -2;
                        lastSentUid = 1;
                    }

                    if (p instanceof C0FPacketConfirmTransaction) {
                        C0FPacketConfirmTransaction packet = (C0FPacketConfirmTransaction) p;
                        if (packet.getUid() < 0 && HypixelUtil.isVerifiedHypixel()) {
                            if (bruh == -2 && lastSentUid == -1) {
                                if (debug)
                                    DevNotifications.getManager().post("\247aSent last bruh A \247e" + mc.thePlayer.ticksExisted);
                                sendPackets();
                                bruh = -3;
                            }

                            if (bruh >= 0) {
                                if (bruh == 0 && lastSentUid == -1) {
                                    if (debug)
                                        DevNotifications.getManager().post("\247aSent last bruh B \247e" + mc.thePlayer.ticksExisted);
                                    sendPackets();
                                }
                                event.setCancelled(!shouldSabotage());
                                chokePackets.add(packet);
                                if (debug)
                                    DevNotifications.getManager().post("\247eChoking \247c" + packet.getUid() + " \247e" + mc.thePlayer.ticksExisted);
                                if (bruh == 0 || !c13Timer.delay(5)) { // You get sent them in an instant
                                    bruh++;
                                }
                            } else {
                                event.setCancelled(!shouldSabotage());
                                chokePackets.add(packet);
                                if (c13Timer.delay(1000) && lastSentUid != 2) {
                                    if (debug)
                                        DevNotifications.getManager().post("\247e\247lBurst detected \247c" + packet.getUid() + " \2476" + mc.thePlayer.ticksExisted);
                                    c13Timer.reset();
                                    if (lastSentUid == 1) {
                                        if (debug)
                                            DevNotifications.getManager().post("\247aWatchdog handshake success " + mc.thePlayer.ticksExisted);
                                        sendPackets();
                                        resetPackets();
                                        if (AUTODONG.getValue()) {
                                            Notifications.getManager().post("Bypass Error", "Automatically swapped to Dong mode.", 5000, Notifications.Type.WARNING);
                                            Notifications.getManager().post("Dong Warning", "Flying too much may result in a ban.", 5000, Notifications.Type.NOTIFY);
                                            this.bruh = 5;
                                            this.lastUid = 0;
                                            this.lastSentUid = -1;
                                            String newMode = "Dong";
                                            this.option.setSelected(newMode);
                                            this.lastMode = newMode;
                                        } else {
                                            Notifications.getManager().post("Bypass Error", "Possible WD ban. Please rejoin or finish your match quickly.", 10000, Notifications.Type.WARNING);
                                        }

                                    }
                                    lastSentUid++;
                                }
                                if (debug)
                                    DevNotifications.getManager().post("\2476Canceled \247c" + packet.getUid() + " \2476" + mc.thePlayer.ticksExisted);

                                lastUid = packet.getUid();
                            }
                            c13Timer.reset();
                        }
                    }
                } else {
                    if (p instanceof C0FPacketConfirmTransaction) {
                        C0FPacketConfirmTransaction packet = (C0FPacketConfirmTransaction) p;
                        if (packet.getUid() < 0 && HypixelUtil.isVerifiedHypixel()) {
                            if (Client.getModuleManager().isEnabled(Fly.class)) {
                                event.setCancelled(true);
                                chokePackets.add(packet);
                            } else {
                                if (chokePackets.size() > 0)
                                    sendPackets();
                            }
                        }
                    }
                }

            if (p instanceof C00PacketKeepAlive) {
                if (DELAY.getValue().longValue() != 0) {
                    packetList.add(new BruhPacket(p, DELAY.getValue().longValue()));
                    event.setCancelled(true);
                }
            }
        }

        if (event instanceof EventRenderGui) {
            ScaledResolution res = new ScaledResolution(mc);
            if (state < 2 && AUTOBYPASS.getValue()) {
                GlStateManager.pushMatrix();
                float centerX = (float) res.getScaledWidth_double() / 2F, centerY = (float) res.getScaledHeight_double() / 2F;
                boolean holding = mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() != null && Item.getIdFromItem(mc.thePlayer.inventory.getCurrentItem().getItem()) == 345;
                String str = state == 0 ? holding ? "\247a\247lWaiting to open Game Menu" : "\247c\247lPlease hold the Compass in your hand" : "\247lListening for Game Menu to open";
                float strWidth = Client.verdana16.getWidth(str);
                float width = strWidth + 10;

                RenderingUtil.rectangleBordered(centerX - width / 2, centerY - 70, centerX + width / 2, centerY - 50, 1, Colors.getColor(0, 150), Colors.getColor(0, 100));
                Client.verdana16.drawString(str, centerX - strWidth / 2F, centerY - 60 - Client.verdana16.getHeight(str) / 2F, -1);
                GlStateManager.popMatrix();
            }

            // Because people are retards.
            if (lastSentUid > 1 && option.getSelected().equals("Watchdog Off")) {
                GlStateManager.pushMatrix();
                float centerX = (float) res.getScaledWidth_double() / 2F, centerY = (float) res.getScaledHeight_double() / 2F;
                String str = "\247c\247lWARNING \247c| Bypass Error";
                float strWidth = Client.verdana16.getWidth(str);
                float width = strWidth + 10;
                RenderingUtil.rectangleBordered(centerX - width / 2, centerY - 70, centerX + width / 2, centerY - 50, 1, Colors.getColor(0, 150), Colors.getColor(0, 100));
                Client.verdana16.drawString(str, centerX - strWidth / 2F, centerY - 60 - Client.verdana16.getHeight(str) / 2F, -1);
                GlStateManager.popMatrix();
            }
        }
        if (event instanceof EventScreenDisplay) {
            EventScreenDisplay esd = event.cast();
            if (esd.getGuiScreen() instanceof GuiConnecting || esd.getGuiScreen() instanceof GuiDownloadTerrain || esd.getGuiScreen() instanceof GuiMultiplayer || esd.getGuiScreen() instanceof GuiDisconnected) {
                packetList.clear();
            }
        }
    }

    public void sendPackets() {
        int sent = 0;
        while (chokePackets.peek() != null) {
            Packet packet = chokePackets.poll();
            if (packet != null) {
                sent++;
                NetUtil.sendPacketNoEvents(packet);
            }
        }

        boolean debug = false;
        if (sent > 0 && debug) {
            DevNotifications.getManager().post("\247b\247lSent " + sent);
        }
        this.resetPackets();
    }

    public static boolean shouldSabotage() {
        return false;
    }

    public void resetPackets() {
        this.chokePackets.clear();
    }

    public static class BruhPacket {

        Packet packet;
        long timeToSend;

        BruhPacket(Packet packet, long delay) {
            this.packet = packet;
            this.timeToSend = System.currentTimeMillis() + delay;
        }

    }

}
