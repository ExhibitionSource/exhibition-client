package exhibition.util.security;

import com.google.common.collect.Maps;
import exhibition.Client;
import net.minecraft.client.Minecraft;
import net.minecraft.util.CryptManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by DoubleParallax on 12/18/2016.
 */
public class Connection {

    private String url, json;
    private String response = null;
    private Map<String, String> parameters = Maps.newHashMap(), headers = Maps.newHashMap();

    public Connection(String url) {
        this(url, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
    }

    public Connection(String url, String userAgent) {
        setUrl(url);
        setUserAgent(userAgent);
    }

    public static Connection createConnection(String url) {
        return new Connection(url, " E. " + " U: ");
    }

    public static Connection normalConnection(String url) {
        return new Connection(url, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Connection setJson(String json) {
        this.json = json;
        return this;
    }

    public Connection setUserAgent(String userAgent) {
        headers.put("User-Agent", userAgent);
        return this;
    }

    public Connection setAccept(String accept) {
        headers.put("Accept", accept);
        return this;
    }

    public Connection setContentType(String contentType) {
        headers.put("Content-Type", contentType);
        return this;
    }

    public Connection setParameters(String... parameters) {
        for (int i = 0; i < parameters.length; i += 2) {
            this.parameters.put(parameters[i], parameters[i + 1]);
        }
        return this;
    }

    public Connection setHeaders(String... headers) {
        for (int i = 0; i < headers.length; i += 2) {
            this.headers.put(headers[i], headers[i + 1]);
        }
        return this;
    }

    public Connection setResponse(Object responseStrInstance) {
        this.response = (String)responseStrInstance;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public String getJson() {
        return json;
    }

    private Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getResponse() {
        return this.response;
    }

    public String getPayload() {
        if (getJson() != null) {
            return getJson();
        }
        StringBuilder payload = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = getParameters().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> parameter = itr.next();
            payload.append(String.format("%s=%s", parameter.getKey(), parameter.getValue())).append(itr.hasNext() ? "&" : "");
        }
        return payload.toString();
    }

}

