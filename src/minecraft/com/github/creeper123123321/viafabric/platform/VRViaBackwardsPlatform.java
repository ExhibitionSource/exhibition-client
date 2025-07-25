package com.github.creeper123123321.viafabric.platform;

import com.github.creeper123123321.viafabric.ViaFabric;
import de.gerrygames.viarewind.fabric.util.LoggerWrapper;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.ViaBackwardsConfig;
import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

public class VRViaBackwardsPlatform implements ViaBackwardsPlatform {

    private Logger logger = (Logger) new LoggerWrapper(LogManager.getLogger("ViaBackwards"));

    private File configDir;

    public VRViaBackwardsPlatform() {
        ViaBackwards.init(this, new ViaBackwardsConfig() {
            @Override
            public boolean addCustomEnchantsToLore() {
                return true;
            }

            @Override
            public boolean addTeamColorTo1_13Prefix() {
                return true;
            }

            @Override
            public boolean isFix1_13FacePlayer() {
                return true;
            }

            @Override
            public boolean alwaysShowOriginalMobName() {
                return true;
            }
        });

        Path file = ViaFabric.directoryPath.resolve("ViaBackwards");
        this.configDir = file.toFile();
        init(file.toFile());
    }

    @Override
    public boolean isOutdated() {
        return false;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void disable() {
    }

    @Override
    public File getDataFolder() {
        return configDir;
    }
}