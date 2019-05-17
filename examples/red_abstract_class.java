/* Just a small example to see whether any tool we use can handle the
 * presence of abstract inputs (see function test). */

abstract class AbsTest {
    public int x;
    public int y;
}

class Class1 extends AbsTest {
    public Class1() {
        x = 1;
        y = 0;
    }
}

class Class2 extends AbsTest {
    public Class2() {
        x = 3;
        y = 0;
    }
}

public class red_abstract_class {
    public static int test (AbsTest lol) {
        int div = 0;
        if (lol.x == 1) {
            return 0;
        }
        else if (lol.x == 2) {
            return 1;
        } else {
            return 1/div;
        }
    }

    public static void main(String[] args) {
        Class1 a = new Class1();
        int i = test(a);

        System.out.println(i);
        
        Class2 b = new Class2();
        int j = test(b);
        
        System.out.println(j);
    }
}
