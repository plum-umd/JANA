import java.util.HashMap;
import java.util.Random;

/**
 * Created by ins on 6/24/15.
 */
public class Timing {
    private static HashMap<String, String> map = new HashMap<String, String>();

    /* Maybe someone with more experience creating expensive no-ops could make this better */
    public static String md5(String s) {
        int lim = 100000000 * s.length();

        int k = 0;

        for (int i = 0; i < lim; i++) {
            if (k % 3 == 0) {
                k--;
            } else {
                k++;
            }
        }

        return Integer.toString(k);
    }

    public static boolean login(String u, String p) {
        boolean outcome = false;

        if (map.containsKey(u)) {
            if (map.get(u).equals(md5(p))) {
                outcome = true;
            }
        }

        return outcome;

    }

    /*
        If the attacker attempts to login using username of "foobar@gmail.com" the login process will take longer
        than if the attacker selected an email not in the hashmap. This reveals to the attacker that "foobar@gmail.com"
        is a registered username/email.
     */

    public static void main(String[] args) {
        String user = args[0];
        String pass = args[1];

        map.put("foobar@gmail.com", md5("qwerty123"));

        System.out.println(login(user, pass));
    }
}
