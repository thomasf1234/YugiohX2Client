import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static sun.net.NetProperties.get;

/**
 * Created by tfisher on 31/05/2017.
 */
public class Client {
    public static String NEW_LINE = String.format("%n");
    public static String GET = "GET";
    public static String HEAD = "HEAD";
    public static String POST = "POST";
    public static String PUT = "PUT";
    public static String DELETE = "DELETE";
    protected static String USER_AGENT = "Java";
    //5 second timeout
    protected static int TIMEOUT = 5000;

    protected String serverIp;
    protected int serverPort;
    protected String loginUuid;


    public Client(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.loginUuid = null;
    }

    public String healthcheck() throws IOException {
        String json = get("/healthcheck");
        return json;
    }

    public String dbNames() throws IOException {
        String json = get("/cards/db_names");
        return json;
    }

    public String card(String dbName) throws IOException {
        String url = String.format("/cards/get?db_name=%s", dbName);
        String json = post(url);
        return json;
    }

    public String userGet() throws IOException {
        String json = get("/users/get");
        return json;
    }

    public String userCards() throws IOException {
        String json = get("/users/cards");
        return json;
    }

    public String userDeposit(String username, int dpAmount) throws IOException {
        String jsonBody = String.format("{\"username\":\"%s\",\"amount\":%d}", username, dpAmount);
        String json = post("/users/deposit", jsonBody);
        return json;
    }

    public String shopPurchase(String boosterPackDbName) throws IOException {
        String jsonBody = String.format("{\"booster_pack_db_name\":\"%s\"}", boosterPackDbName);
        String json = post("/shops/purchase", jsonBody);
        return json;
    }

    public String shopPasswordMachine(String serialNumber) throws IOException {
        String jsonBody = String.format("{\"serial_number\":\"%s\"}", serialNumber);
        String json = post("/shops/password_machine", jsonBody);
        return json;
    }

    public String login(String username, String password) throws IOException, ScriptException {
        String jsonBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        String responseJsonBody = post("/login", jsonBody);

        Map<String, ? extends Object> responseBody = parseJSON(responseJsonBody);
        String uuid = (String) responseBody.get("uuid");
        this.loginUuid = uuid;

        return uuid;
    }

    public String logout() throws IOException {
        String json = post("/logout");
        return json;
    }

    //this is terrible :(
    private Map<String, ? extends Object> parseJSON(String json) throws IOException, ScriptException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        String javaScriptLine = String.format("JSON.parse('%s')", json);
        Object obj = engine.eval(javaScriptLine);

        return (Map<String, ? extends Object>) obj;
    }

    protected String get(String endPoint) throws IOException {
        HttpURLConnection connection = null;
        String returnValue = null;
        try {
            URL url = getEndpoint(endPoint);
            int contentLength = 0;

            connection = newConnection(url, GET, contentLength);
            connection.connect();

            returnValue = getResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return returnValue;
    }

    protected String post(String endPoint) throws IOException {
        return post(endPoint, "{}");
    }

    protected String post(String endPoint, String jsonBody) throws IOException {
        HttpURLConnection connection = null;
        String returnValue = null;
        OutputStream os = null;
        try {
            URL url = getEndpoint(endPoint);
            byte[] postData  = jsonBody.getBytes(StandardCharsets.UTF_8);
            int    contentLength = postData.length;

            connection = newConnection(url, POST, contentLength);
            connection.setRequestProperty("Content-Type", "application/json");
            // For POST only - START
            connection.setDoOutput(true);
            os = connection.getOutputStream();
            os.write(postData);
            os.flush();
            os.close();
            // For POST only - END

            returnValue = getResponse(connection);
        } finally {
            if (os != null) {
                os.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return returnValue;
    }

    protected URL getEndpoint(String endPoint) throws MalformedURLException {
        URL url = new URL(String.format("http://%s:%d%s", serverIp, serverPort, endPoint));
        return url;
    }

    protected HttpURLConnection newConnection(URL url, String httpMethod, int contentLength) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(httpMethod);
        connection.setRequestProperty("Content-length", Integer.toString(contentLength));
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (this.loginUuid != null) {
            connection.setRequestProperty("Uuid", this.loginUuid);
        }

        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        return connection;
    }

    protected String getResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append(NEW_LINE);
        }
        br.close();
        String rawString = sb.toString();
        String responseBody = rawString.substring(0, rawString.length() - 1);

        //logging
        String logMessage = String.format("responseCode: %d, responseBody: %s", status, responseBody);
        System.out.println(logMessage);

        switch (status) {
            case HttpURLConnection.HTTP_OK:
                return responseBody;
            default:
                throw new HTTPException(status, responseBody);
        }
    }
}
