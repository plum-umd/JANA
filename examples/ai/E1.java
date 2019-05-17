public class E1 {
  //sort of CRIME-ish ... do the values in integers 
  //(representing the characters in buffers) change the 
  //numeric constraints on x, which in this example
  //represents the size of a buffer
  public static int foo(int a, int b, int c, int d, int e) {
    int x = 0;

    if(a > 5) {
      x++;
    }

    if(b > 10) {
      x++;
    }

    if(c < 10 && c > 20) {
      x++;
    }

    if(d < 100 && d > 0) {
      x++;
    }

    if(e > 0) {
      x++;
    }

    //assert(x >= 0 && x < 6);
    return x;
  }
}
