package exhibition;

import com.github.creeper123123321.viafabric.ViaFabric;
import com.github.creeper123123321.viafabric.handler.CommonTransformer;
import com.github.creeper123123321.viafabric.handler.clientside.VRDecodeHandler;
import exhibition.event.Event;
import exhibition.event.EventListener;
import exhibition.event.EventSystem;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.EventMotionUpdate;
import exhibition.event.impl.EventPacket;
import exhibition.event.impl.EventTick;
import exhibition.gui.altmanager.FileManager;
import exhibition.gui.click.ClickGui;
import exhibition.gui.console.SourceConsoleGUI;
import exhibition.gui.generators.handlers.altening.AlteningGenHandler;
import exhibition.gui.generators.handlers.altening.stupidaltserviceshit.AltService;
import exhibition.gui.generators.handlers.altening.stupidaltserviceshit.SSLVerification;
import exhibition.gui.screen.GuiChangelog;
import exhibition.gui.screen.impl.mainmenu.ClientMainMenu;
import exhibition.gui.screen.impl.mainmenu.GuiLoginMenu;
import exhibition.management.ColorManager;
import exhibition.management.SubFolder;
import exhibition.management.command.CommandManager;
import exhibition.management.config.ConfigManager;
import exhibition.management.font.DynamicTTFFont;
import exhibition.management.font.TTFFontRenderer;
import exhibition.management.friend.FriendManager;
import exhibition.management.macros.MacroManager;
import exhibition.management.notifications.dev.DevNotifications;
import exhibition.management.waypoints.WaypointManager;
import exhibition.module.Module;
import exhibition.module.ModuleManager;
import exhibition.module.impl.combat.Bypass;
import exhibition.util.HypixelUtil;
import exhibition.util.MathUtils;
import exhibition.util.PlayerUtil;
import exhibition.util.Timer;
import exhibition.util.misc.ChatUtil;
import exhibition.util.security.*;
import io.netty.channel.ChannelHandler;
import net.arikia.dev.drpc.DiscordRPC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.*;
import net.minecraft.util.CryptManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import org.apache.commons.io.FileUtils;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import static exhibition.util.security.AuthenticationUtil.getHwid;
import static exhibition.util.security.Snitch.snitch;

// Credits to LPK for initial base
public class Client extends Castable implements EventListener {

    public static Client instance;

    public static boolean isNewUser;

    // Client data
    public static String version = "051924";
    public static String parsedVersion;
    public static String clientName = "ArthimoWare";
    public static ColorManager cm = new ColorManager();
    public static MacroManager macroManager;
    public static ConfigManager configManager;
    public static WaypointManager waypointManager;

    // Alt Gen Handlers
    public static AlteningGenHandler alteningGenHandler;
    public static AltService altService = new AltService();
    public static SSLVerification sslVerification = new SSLVerification();

    // Managers
    private ModuleManager<Module> moduleManager;

    private static FileManager fileManager;
    private static ClickGui clickGui;
    private static SourceConsoleGUI sourceConsoleGUI;

    public static CommandManager commandManager;

    public static FontRenderer virtueFont;
    public static FontRenderer blockyFont;

    // Other data
    private File dataDirectory;
    private GuiScreen mainMenu = new ClientMainMenu();
    private boolean isHidden;

    public static long ticksInGame = -1;
    public static long joinTime = -1;

    public static ResourceLocation chainmailTexture = new ResourceLocation("textures/skeetchainmail.png");
    public static ResourceLocation capeLocation = new ResourceLocation("textures/cape.png");
    public static ResourceLocation overlayLocation = new ResourceLocation("textures/overlay.png");

    // Auth
    private static AuthenticatedUser authUser;

    // Fonts
    public static TTFFontRenderer f, fs, fsnotbold, fss, fssBold, badCache, verdana16, verdana10, fsmallbold, nametagsFont, test1, test2, test3;

    public static DynamicTTFFont fsmallboldscaled, nametagsFontscaled, header, subHeader, fssDynamic;

    public static DynamicTTFFont.DynamicTTForMC hudFont;

    public static TTFFontRenderer[] fonts;

    public static DynamicTTFFont[] dynamicFonts;

    public static DynamicTTFFont.DynamicTTForMC[] dynamicMcFonts;

    public boolean isHypixel;

    public ProgressScreen progressScreenTask;

    public String hypixelApiKey = null;

    public static boolean isDiscordReady = false;

    public static boolean shouldThreadRun = true;

    public Client(ProgressScreen args) {
        init(args);
    }

    // ([Ljava/lang/Object;)[LJava/lang/Object;
    public void init(ProgressScreen args) {
        Client.instance = this;
        this.progressScreenTask = args;

        this.progressScreenTask.render();

        this.progressScreenTask.incrementStage(); // Stage 1 pass arguments check

//        String version = "";
//        try {
//            Connection connection = Connection.createConnection("https://api2.minesense.pub/version");
//            connection.setJson(Base64.getEncoder().encodeToString(SystemUtil.getHardwareIdentifiers().getBytes()));
//            SSLConnector.post(connection);
//            version = connection.getResponse().trim();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        parsedVersion = Client.version;

        this.progressScreenTask.incrementStage(); // Stage 2 version was fetched correctly

        this.progressScreenTask.incrementStage(); // Stage 3 login cache was checked

        this.progressScreenTask.incrementStage(); // Stage 4 passed basic hwid check

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shouldThreadRun = false;
                DiscordRPC.discordShutdown();
            }
        });

        DiscordUtil.initDiscord();

        long currentTime = System.currentTimeMillis();

        Thread discordThread = new Thread() {
            @Override
            public void run() {
                while (shouldThreadRun) {
                    DiscordRPC.discordRunCallbacks();
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {

                    }
                }
            }
        };

        discordThread.start();

        // Wait at MAX 5 seconds, this should take < 1 second normally.
        while (!isDiscordReady && (System.currentTimeMillis() - currentTime) < 5_000) ;

        this.progressScreenTask.incrementStage(); // Stage 5 discord RPC is setup

        instance.setupFonts();
        dataDirectory = new File(Client.clientName);
        this.progressScreenTask.incrementStage(); // Stage 6
        commandManager = new CommandManager();
        moduleManager = new ModuleManager<>(Module.class);
        this.progressScreenTask.incrementStage(); // Stage 7
        try {
            new ViaFabric().onInitialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.progressScreenTask.incrementStage(); // Stage 8

        // Loads extra information like consents/etc
        this.load();
    }

    public boolean is1_16_4() {
        try {
            ChannelHandler viaDecoder = (Minecraft.getMinecraft().getNetHandler().getNetworkManager()).channel.pipeline().get(CommonTransformer.HANDLER_DECODER_NAME);
            if (viaDecoder instanceof VRDecodeHandler) {
                ProtocolInfo protocol = ((VRDecodeHandler) viaDecoder).getInfo().getProtocolInfo();
                if (protocol != null) {
                    return protocol.getServerProtocolVersion() == 754;
                }
            }
        } catch (Exception e) {

        }

        return false;
    }

    public boolean is1_9orGreater() {
        try {
            ChannelHandler viaDecoder = (Minecraft.getMinecraft().getNetHandler().getNetworkManager()).channel.pipeline().get(CommonTransformer.HANDLER_DECODER_NAME);
            if (viaDecoder instanceof VRDecodeHandler) {
                ProtocolInfo protocol = ((VRDecodeHandler) viaDecoder).getInfo().getProtocolInfo();
                if (protocol != null) {
                    return protocol.getServerProtocolVersion() >= 107;
                }
            }
        } catch (Exception e) {

        }

        return false;
    }

    public static AuthenticatedUser getAuthUser() {
        return authUser;
    }

    public static void setAuthUser(Object authUser) {
        Client.authUser = ((Castable) authUser).cast();
    }

    public static ClickGui getClickGui() {
        return clickGui;
    }

    public static SourceConsoleGUI getSourceConsoleGUI() {
        return sourceConsoleGUI;
    }

    public static FileManager getFileManager() {
        return fileManager;
    }

    public void setup() {
        commandManager.setup();
        isNewUser = !getDataDir().exists();
        moduleManager.setup();
        /*accountManager.setup();*/
        ModuleManager.loadSettings();
        (Client.fileManager = new FileManager()).loadFiles();
        sourceConsoleGUI = new SourceConsoleGUI();
        waypointManager = new WaypointManager();
        configManager = new ConfigManager();
        macroManager = new MacroManager();
        alteningGenHandler = new AlteningGenHandler();
        clickGui = new ClickGui();
        FriendManager.start();

        EventSystem.register(this);
    }

    /*
        f = new TTFFontRenderer(new Font("Impact", Font.PLAIN, 24), true);
        fs = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 11), true);
        fsnotbold = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 9), false);
        test2 = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 10), true);
        fss = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 10), false);
        test3 = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 10), true);
        fsmallbold = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 10), true);
        header = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 20), true);
        subHeader = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 16), true);
        verdana16 = new TTFFontRenderer(new Font("Lucida Console", Font.PLAIN, 9), false);
        test1 = new TTFFontRenderer(new Font("Verdana", Font.PLAIN, 9), true);
        verdana10 = new TTFFontRenderer(new Font("Lucida Console", Font.PLAIN, 10), false);
        nametagsFont = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 18), true);
     */

    public void setupFonts() {

        f = new TTFFontRenderer(new Font("Impact", Font.PLAIN, 24), true);
        fs = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 11), true);
        fsnotbold = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 9), false);
        test2 = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 10), true);
        fss = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 10), false);
        fssBold = new TTFFontRenderer(new Font("Arial Bold", Font.BOLD, 11), false);

        fssDynamic = new DynamicTTFFont(new Font("Tahoma", Font.PLAIN, 10), false);
        test3 = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 10), true);
        fsmallbold = new TTFFontRenderer(new Font("Tahoma Bold", Font.PLAIN, 10), true);
        header = new DynamicTTFFont(new Font("Tahoma", Font.PLAIN, 20), true);
        subHeader = new DynamicTTFFont(new Font("Tahoma", Font.PLAIN, 16), true);
        verdana16 = new TTFFontRenderer(new Font("Lucida Console", Font.PLAIN, 9), false);
        test1 = new TTFFontRenderer(new Font("Verdana", Font.PLAIN, 9), true);
        verdana10 = new TTFFontRenderer(new Font("Lucida Console", Font.PLAIN, 10), false);
        nametagsFont = new TTFFontRenderer(new Font("Tahoma", Font.PLAIN, 18), true);

        fsmallboldscaled = new DynamicTTFFont(new Font("Tahoma Bold", Font.PLAIN, 10), true);
        nametagsFontscaled = new DynamicTTFFont(new Font("Tahoma", Font.PLAIN, 18), true);
        hudFont = new DynamicTTFFont.DynamicTTForMC(new DynamicTTFFont(new Font("Calibri", Font.PLAIN, 20), true));

        try {
            InputStream istream = getClass().getResourceAsStream("/assets/minecraft/font.ttf");
            Font myFont = Font.createFont(Font.PLAIN, istream);
            myFont = myFont.deriveFont(Font.PLAIN, 36);
            badCache = new TTFFontRenderer(myFont, true);
        } catch (Exception e) {
            System.out.println("Error loading font?");
            badCache = new TTFFontRenderer(new Font("Impact", Font.PLAIN, 36), true);
        }

        fonts = new TTFFontRenderer[10];

        fonts[0] = new TTFFontRenderer(new Font("Calibri Bold", Font.PLAIN, 12), true);
        fonts[1] = new TTFFontRenderer(new Font("Calibri Bold", Font.PLAIN, 18), true);
        fonts[2] = new TTFFontRenderer(new Font("Calibri Bold", Font.PLAIN, 11).deriveFont(10.5F), false);
        fonts[3] = new TTFFontRenderer(new Font("Helvetica", Font.PLAIN, 13).deriveFont(13.5F), true);
        fonts[4] = new TTFFontRenderer(new Font("Calibri", Font.PLAIN, 20), true);

    }

    private boolean confirmed;

    private void load() {
        String file = "";
        try {
            file = FileUtils.readFileToString(getFile());
        } catch (IOException e) {
            return;
        }
        for (String line : file.split("\n")) {
            if (line.contains("loginConfirm")) {
                String[] split = line.split(":");
                if (split.length > 1) {
                    confirmed = Boolean.parseBoolean(split[1]);
                }
            }
        }
    }

    public void save() {
        try {
            FileUtils.write(getFile(), "loginConfirm:" + confirmed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getFile() {
        File file = new File(getFolder().getAbsolutePath() + File.separator + "Consents.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public File getFolder() {
        File folder = new File(Client.getDataDir().getAbsolutePath() + File.separator + SubFolder.Other.getFolderName());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    public static GuiScreen getScreen() {
        try {
            String currentVersion = version.substring(0, 2) + "/" + version.substring(2, 4) + "/20" + version.substring(4, 6);

            String version = LoginUtil.getLastVersion();
            if (version.equals(""))
                return new GuiChangelog();

            String lastVersion = version.substring(0, 2) + "/" + version.substring(2, 4) + "/20" + version.substring(4, 6);
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

            if (format.parse(currentVersion).after(format.parse(lastVersion)) && Client.version.equals(parsedVersion)) {
                return new GuiChangelog();
            }
        } catch (Exception ignored) {
        }
        return new GuiLoginMenu(true);
    }

    private static boolean isBeta = false;

    private static boolean first = false;

    public static boolean isBeta() {
        if (!first) {
            try {
                String currentVersion = version.substring(0, 2) + "/" + version.substring(2, 4) + "/20" + version.substring(4, 6);
                String e = parsedVersion.substring(0, 2) + "/" + parsedVersion.substring(2, 4) + "/20" + parsedVersion.substring(4, 6);
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                isBeta = format.parse(currentVersion).after(format.parse(e));
                first = true;
            } catch (Exception ignored) {
            }
        }
        return isBeta;
    }

    public static ModuleManager<Module> getModuleManager() {
        return instance.moduleManager;
    }

    public static File getDataDir() {
        return instance.dataDirectory;
    }

    public static boolean isHidden() {
        return instance.isHidden;
    }

    public static void setHidden(boolean hidden) {
        instance.mainMenu = new ClientMainMenu();
    }

    public static void resetClickGui() {
        clickGui = new ClickGui();
    }

    private final Timer packetTimer = new Timer();

    public boolean isLagging() {
        return packetTimer.roundDelay(350);
    }

    public double spawnY = 86;

    public final ArrayBlockingQueue<Long> differenceQueue = new ArrayBlockingQueue<>(5);

    private long lastTime = -1;

    public static String getTPS() {
        Client instance = Client.instance;

        if (instance.differenceQueue.size() <= 1) {
            return "N/A";
        }

        double totalTPS = 0;

        for (Long diff : instance.differenceQueue) {
            double seconds = diff / 1000D;
            double calculatedTPS = MathHelper.clamp_double(20D / seconds, 0, 20);
            totalTPS += calculatedTPS;
        }

        return String.format("%.1f", totalTPS / instance.differenceQueue.size());
    }

    public String getName() {
        return "main";
    }

    private final Timer timer = new Timer();

    @RegisterEvent(events = {EventPacket.class, EventTick.class, EventMotionUpdate.class})
    public void onEvent(Event event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event instanceof EventPacket) {
            EventPacket eventPacket = event.cast();
            Packet packet = eventPacket.getPacket();
            if (eventPacket.isIncoming() && !(packet instanceof S00PacketKeepAlive) && !(packet instanceof S32PacketConfirmTransaction)) {
                packetTimer.reset();
            }
            if (packet instanceof S08PacketPlayerPosLook && (mc.thePlayer == null || mc.thePlayer.ticksExisted < 40)) {
                S08PacketPlayerPosLook spawnPosition = (S08PacketPlayerPosLook) packet;

                double x = spawnPosition.getX();
                double y = spawnPosition.getY();
                double z = spawnPosition.getZ();

                double distance = Math.sqrt(x * x + z * z);

                double yOffset = MathUtils.roundToPlace((y - (int) y), 10);

                if (distance < 50 && (yOffset == 0.6 || yOffset == 0)) {
                    spawnY = y - 4;
                }
            }

            if (mc.thePlayer != null && mc.theWorld != null && HypixelUtil.isVerifiedHypixel()) {
                if (packet instanceof C03PacketPlayer && hypixelApiKey == null && mc.thePlayer.ticksExisted > 10) {
                    DevNotifications.getManager().post("Grabbing key");
                    ChatUtil.sendChat_NoFilter("/api new");
                    hypixelApiKey = "";
                    return;
                }
                if (packet instanceof S03PacketTimeUpdate) {
                    try {
                        if (lastTime == -1) {
                            lastTime = System.currentTimeMillis();
                            return;
                        }

                        while (differenceQueue.remainingCapacity() == 0) {
                            differenceQueue.remove();
                        }

                        long current = System.currentTimeMillis();
                        differenceQueue.offer(current - lastTime);
                        lastTime = current;
                    } catch (Exception ignored) {

                    }
                }
                if (packet instanceof S02PacketChat) {
                    S02PacketChat packetChat = (S02PacketChat) packet;
                    String unformatted = StringUtils.stripControlCodes(packetChat.getChatComponent().getUnformattedText());
                    if (unformatted.contains("Your new API key is ")) {
                        hypixelApiKey = unformatted.split("Your new API key is ")[1].trim();
                        DevNotifications.getManager().post("Key " + hypixelApiKey);
                    }
                }
            }
        }
        if (event instanceof EventMotionUpdate) {
            EventMotionUpdate em = event.cast();
            if (em.isPre()) {
                if (PlayerUtil.isMoving() && Bypass.shouldSabotage()) {
                    em.setGround(true);
                }
            }
        }
        if (event instanceof EventTick) {
            if (joinTime == -1) {
                joinTime = System.currentTimeMillis();
                if (mc.getCurrentServerData() != null) {
                    DiscordUtil.setDiscordPresence("In Game", "IP: " + mc.getCurrentServerData().serverIP);
                }
            }
            boolean inPit = HypixelUtil.isVerifiedHypixel() && HypixelUtil.isInGame("PIT");

            boolean inSpawn = mc.thePlayer.posY > Client.instance.spawnY && mc.thePlayer.posX < 30 && mc.thePlayer.posX > -30 && mc.thePlayer.posZ < 30 && mc.thePlayer.posZ > -30;

            if (PlayerUtil.isMoving() && (!inPit || !inSpawn)) {
                timer.reset();
            }

            if (mc.thePlayer.isAllowEdit() && !HypixelUtil.isGameStarting() && HypixelUtil.scoreboardContains("www.hypixel.net") && !timer.delay(15_000)) {
                ticksInGame++;
            }

            if (inPit) {
                for (Entity entity : mc.theWorld.getLoadedEntityList()) {
                    if (entity instanceof EntityPlayer && !(entity instanceof EntityPlayerSP)) {
                        EntityPlayer player = (EntityPlayer) entity;
                        if (player.isRiding()) {
                            if (player.ridingEntity instanceof EntityArmorStand) {
                                EntityArmorStand armorStand = (EntityArmorStand) player.ridingEntity;
                                if ((player.posX == player.lastTickPosX && player.posY == player.lastTickPosY && player.posZ == player.lastTickPosZ) ||
                                        (armorStand.posX == armorStand.lastTickPosX && armorStand.posY == armorStand.lastTickPosY && armorStand.posZ == armorStand.lastTickPosZ)) {
                                    player.flags++;
                                    if (player.flags > 10) {
                                        player.ridingEntity = null;
                                    }
                                } else {
                                    player.flags--;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
