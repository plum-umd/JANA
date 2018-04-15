class InterBounds {
  public static int test1(int a, int b) {
    int c = 0;
    while (c <= a) { // a <= -1
      c = c + nestedLoops1(a, b);
      // c = c + 1
    }
    return c;
  }
  
  public static int nestedLoops1(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
      for (int j = 0; j < b; j++) {
        c++;
      }
    }
    return c;
  }

  public static int sumFactorial(int x) {
    int sum = 0;
    
    for (int i = x; i >= 0; i--) {
      sum += factorial(i);
    }

    return sum;
  }

  public static int factorial(int x) {
    if (x < 0) return -1;

    int product = 1;
    for (int i = 1; i <= x; i++) {
      product *= 2;
    }

    return product;
  }
}
