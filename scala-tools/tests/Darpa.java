public class Darpa {
    public static int dup(int n, int m, int random1, int random2) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (random1 <= i && random2 <= j) {
                    return i;
                }
            }
        }
        return n;
    }

}
