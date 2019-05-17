/** ledb3 - nested loops with exception control flow */

class ledb3 {
  public static class RestrictedAccessException extends Exception {
  }

  public static boolean contains(int[] a, int v) {
    for (int i = 0; i < a.length; i++) {
      if (v == a[i]) return true;
    }
    return false;
  }

  public static int ledb(int[] range, int[] restricted) {
    int ind = 0;
    while (ind < range.length) {
      try {
        int nextkey = range[ind];
 
        if (!contains(restricted, nextkey)) {
          ind++;
        } else {
          throw new RestrictedAccessException();
        }
      } catch (RestrictedAccessException e) {
        int getkey = range[ind];
             
        while ((contains(restricted, getkey)) && (ind < range.length)) {
 
          ind++;
          if (ind < range.length) {
            getkey = range[ind];
          }
        }
      }
    }
    
    return 0;
  }
}
