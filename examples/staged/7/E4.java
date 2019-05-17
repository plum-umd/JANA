public class E4 {
  public static int checkit(int x, int y) {
    int a = 0;
    for(int i = 0; i < x; i++) {
      if(i < y) {
        a++;
      }
    }

    return a;
  }
}
