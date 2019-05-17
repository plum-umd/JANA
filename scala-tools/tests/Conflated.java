class Conflated {
  static int simple(int N) {
    int i = 0;
    while (i < N) {
      i++;
    }
    return 0;
  }
  
  static int conflated1(int n, int m) {
    // int c = 0;
    int i = 0;
    // int j = 0;

    while (i < n) {
      int j = 0;
      while (j < m) {
        j++;
        i++;
        // c++;
      }
    }
    
    // return c;
    return 0;
  }

  static int conflated2(int n, int m) {
    int c = 0;
    int i = 0;
    int j = 0;

    while (i < n) {
      j = 0;
      while (j < m) {
        j++;
        i++;
        c++;
      }
      i++;
    }
    
    return c;
  }

  static int conflated3(int n, int m) {
    int c = 0;
    int i = 0;
    int j = 0;

    while (i < n) {
      j = i;
      while (j < m) {
        j++;
        i++;
        c++;
      }
      i++;
    }
    
    return c;
  }

  static int conflated4(int n) {
    int i = 0;
    while (i < n) {
      int j = 0;
      while (j < i) {
        j++;
      }
      i++;
    }
    return 0;
  }

  static int conflated5(int N) {
    int new_N = N + 1;
    int i = 0;
    int j = 0;
    while (i < new_N) {
      i = i + 1;
      j = i + 1;
    }
    while (j >= 0) j--;

    return 0;
  }

  static int conflated6(int N) {
    int new_N = N + 1;
    if (new_N > 10) {
      int i = 0;
      int j = 0;
      while (i < new_N) {
        i = i + 1;
        j = i + 1;
      }
      while (j >= 0) j--;
    }

    return 0;
  }

  static int conflated7(int N) {
    int new_N = N + 1;
    int i = 0;
    int j = 0;
    int inc = 2;
    if (N > 10) inc = 3;
    while (i < new_N) {
      i = i + inc;
      j = i + 1;
    }
    while (j >= 0) j--;

    return 0;
  }
}

