package exhibition.util.security;

import exhibition.Client;
import exhibition.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoginUtil {

    public static final File LOGIN = FileUtils.getConfigFile("Data");

    public static int lastResponseCode = -1;

    public static void saveLogin(String encryptedUsername, String encryptedPassword) {
        List<String> fileContent = new ArrayList<>();
        fileContent.add(encryptedUsername);
        fileContent.add(encryptedPassword);
        fileContent.add(String.valueOf(LoginUtil.lastResponseCode));
        fileContent.add(Client.isBeta() ? getLastVersion() : Client.version);
        FileUtils.write(LOGIN, fileContent, true);
    }

    public static List<String> getLoginInformation() {
        return FileUtils.read(LOGIN);
    }

    public static int getLastResponseCode() {
        List<String> info = getLoginInformation();
        return info.isEmpty() || info.size() < 3 ? -1 : Integer.parseInt(info.get(2));
    }

    public static String getUsername() {
        if(getLoginInformation().isEmpty())
            return "New User";
        return getLoginInformation().get(0);
    }

    public static String getLastVersion() {
        List<String> data = getLoginInformation();
        if(data.size() < 4) {
            return "";
        }
        return data.get(3);
    }

}
