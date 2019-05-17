import java.util.Hashtable;

/**
 * Created by ins on 6/24/15.
 */
public class Complexity {
    private static class Dummy {
        int i;

        public Dummy(int i) {
            this.i = i;
        }

        @Override
        public int hashCode() {
            if (i == 77) {
                return 0;
            } else {
                return super.hashCode();
            }
        }
    }

    /*
        If attacker selects k = 77, the hashtable will have O(n) inserts for the Dummy object.
     */

    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);

        Hashtable<Dummy, Integer> ht = new Hashtable<Dummy, Integer>();

        System.out.println("n = " + n);
        System.out.println("k = " + k);

        // (Universal) Harness
        for (int i = 0; i < n; i++) {
            // call the HashTable method
            ht.put(new Dummy(k), i);
        }
    }
}
