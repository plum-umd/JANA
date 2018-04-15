// none these examples example terminate unless a,b positive

class NestedLoops {
  public static int nestedLoops1(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
      for (int j = 0; j < b; j++) {
        // c++;
      }
    }
    return c;
  }

  public static int nestedLoops1inc(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
      for (int j = 0; j < b; j++) {
        c++;
      }
    }
    return c;
  }

  public static int nestedLoopsWhile(int a, int b) {
    int c = 0;
    int i = 0;
    while (i < a) {
      int j = 0;
      while (j < b) {
        j++;
      }
      i++;
    }
    return c;
  }

  public static int loopAnd(int a, int b) {
    int c = 0;
    for (int i = 0; i < a && i < 10; i++) {
    }
    return c;
  }

  public static int loopOr(int a, int b) {
    int c = 0;
    for (int i = 0; i < a || i < 10; i++) {
    }
    return c;
  }

  public static int loopOrSiblings1(int a, int b) {
    int c = 0;
    for (int i = 0; i < a || i < 10; i++) {
        if (i < 5) {
            int d = 0;
        }
    }
    return c;
  }

  public static int loopOrSiblings2(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
        if (i < 10) {
            int d = 1;
        }
        if (i < 5) {
            int d = 0;
        }
    }
    return c;
  }

  public static int loopOrSiblings3(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
        if (i < 10) {
            if (i < 5) {
                int d = 0;
            }
        }
    }
    return c;
  }

  public static int loopOrSiblings4(int a, int b) {
    int c = 0;
    for (int i = 0; i < a; i++) {
        if (i < 10) {
            int d = 1;
        } else {
            int g = 3;
        }
        if (i < 5) {
            int d = 0;
        }
    }
    return c;
  }

  public static int nestedLoops2(int a, int b) {
    int c = 0;
    int i = 0;
    int j = 0;
    while (i < a) {
      while (j < b) {
        c++;
        j++;
      }
      i++;
    }
    return c;
  }

  public static int nestedLoops3(int a, int b) {
    int c = 0;
    int i = 0;
    int j = 0;
    while (i < a && j < b) {
      if (j >= b) {
        j = 0;
        i++;
      }
      j++;
      c++;
    }
    return c;
  }

  public static int nestedLoopsBackEdges(int a, int b) {
    int c = 0;
    int i = 0;
    int j = 0;
    while (i < a) {
        i++;
        if (i < 3) {
            j++;
        } else {
            c++;
        }
    }
    return c;
  }
}
