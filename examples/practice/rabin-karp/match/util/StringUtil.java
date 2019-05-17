package match.util;

public class StringUtil {

    private static int r = 256;
    private static int q = 6291469;

    private static int hash(String s, int m) {
        int h = 0;

        for (int i = 0; i < m; i++) {
            h = (r * h + s.charAt(i)) % q;
        }

        return h;
    }

    private static boolean naive(String text, String pattern, int idx) {
        int n = text.length();
        int m = pattern.length();

        for (int i = 0; i < m; i++) {
            if (text.charAt(idx + i) != pattern.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    public static int substring(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();

        if (n < m) {
            return -1;
        }

        int ht = hash(text, m);
        int hp = hash(pattern, m);

        if (ht == hp && naive(text, pattern, 0)) {
            return 0;
        }

        int rm = 1;
        
        for (int i = 1; i < m; i++) {
            rm = (r * rm) % q;
        }

        for (int i = m; i < n; i++) {
            ht = (ht + q - rm * text.charAt(i - m) % q) % q;
            ht = (ht * r + text.charAt(i)) % q;

            int curr = i - m + 1;

            if (ht == hp && naive(text, pattern, curr)) {
                return curr;
            }
        }

        System.out.println();

        return -1;
    }

}
