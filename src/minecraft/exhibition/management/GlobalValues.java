package exhibition.management;

import exhibition.module.data.settings.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlobalValues {

    public static List<Setting> globalValues = new ArrayList<>();

    public static Setting<Boolean> saveVisuals = new Setting<>("Save Visuals", false, "Saves visuals to your configs."),
            loadVisuals = new Setting<>("Load Visuals", false, "Load visuals from configs.");

    public static Setting<Boolean> scaleFix = new Setting<>("Scale Fix", true, "Forces the ClickGUI to be the same size regardless of scale.");
    public static Setting<Boolean> allowDebug = new Setting<>("Debug", false, "Enables some debug features in the client.");
    public static Setting<Boolean> showCape = new Setting<>("Show Cape", true, "Renders a custom cape on your player.");
    public static Setting<Boolean> keepPriority = new Setting<>("Keep Priority", true, "Prioritized players are not reset each match.");
    public static Setting<Boolean> showFlags = new Setting<>("Show Flags", false, "Shows predicted NCP flags in chat.");

    public static Setting<Boolean> centerNotifs = new Setting<>("Center Notifs", false, "Centers Notifications under your crosshair.");
    public static Setting<Boolean> showCursor = new Setting<Boolean>("Show Cursor", true, "Shows your real cursor in ClickGUI.") {

        @Override
        public void setValue(Boolean value) {
            super.setValue(value);

            Minecraft mc = Minecraft.getMinecraft();

            if (mc.thePlayer != null) {
                if (mc.currentScreen != null) {
                    Mouse.setGrabbed(!Mouse.isGrabbed());
                }
            }

        }
    };
    public static Setting<Number> targetHUDWidth = new Setting<>("Target HUD Width", 0.5078125,"", 0.01, 0,1);
    public static Setting<Number> targetHUDHeight = new Setting<>("Target HUD Height", 0.569444444,"", 0.01, 0,1);

    static {
        globalValues.addAll(Arrays.asList(saveVisuals, loadVisuals, centerNotifs, showCursor, scaleFix,
                allowDebug, showCape, keepPriority, showFlags, targetHUDWidth, targetHUDHeight));
    }

}
