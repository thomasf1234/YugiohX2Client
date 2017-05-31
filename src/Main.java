import jdk.nashorn.api.scripting.ScriptUtils;

import javax.script.ScriptException;
import java.io.IOException;

/**
 * Created by tfisher on 31/05/2017.
 */
public class Main {
    public static void main(String[] args) throws IOException, ScriptException {
        Client client = new Client("localhost", 2000);

        try {
            client.login("Seto", "blueeyes99");
            client.userGet();
            client.userCards();
            client.userDeposit("Seto", 1000);
            client.userGet();
            client.shopPurchase("Metal_Raiders");
            client.userGet();
            client.userCards();

        } finally {
            if (client != null) {
                client.logout();
            }
        }
    }
}
