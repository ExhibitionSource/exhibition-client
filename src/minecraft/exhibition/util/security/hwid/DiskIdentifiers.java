package exhibition.util.security.hwid;

import oshi.hardware.HWDiskStore;

import java.util.ArrayList;
import java.util.List;

public class DiskIdentifiers implements Identifier {

    private final DiskContainer[] diskContainers;

    public DiskIdentifiers(List<HWDiskStore> diskStores) {

        List<HWDiskStore> validDrives = new ArrayList<>();

        for (HWDiskStore disk : diskStores) {
            if (disk.getModel().contains("USB") || disk.getModel().toLowerCase().contains("flash") || disk.getModel().toLowerCase().contains("generic"))
                continue;

            validDrives.add(disk);
        }

        this.diskContainers = new DiskContainer[validDrives.size()];

        for (int i = 0; i < validDrives.size(); i++) {
            HWDiskStore disk = validDrives.get(i);
            this.diskContainers[i] = new DiskContainer(trim(disk.getModel()).split(" \\(")[0], trim(disk.getSerial()).replace(" ",""));
        }

    }

    public DiskContainer[] getDiskContainers() {
        return diskContainers;
    }

    public static class DiskContainer {

        private final String model;
        private final String serial;

        public DiskContainer(String model, String serial) {
            this.model = model;
            this.serial = serial;
        }

        public String getModel() {
            return model;
        }

        public String getSerial() {
            return serial;
        }

    }

}
