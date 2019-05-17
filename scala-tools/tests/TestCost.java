public class TestCost {
  public static int foo(int a, int b) {
    int c = a-2;
    if(a > b || a == c) {
      return a+b;
    } else if (a < c){
      return b-a;
    } else {
      return 0;
    }
  }
}
