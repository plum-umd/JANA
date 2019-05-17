public class Bar extends Foo {
    private int z;
    public void set(int y) {
	z = y;
    }
    public int get() {
	return z;
    }
    public static void main(String args[]) {
	Bar b = new Bar();
	int x = new Integer(args[0]);
	b.set(x);
	System.out.println(b.get());
    }
}
