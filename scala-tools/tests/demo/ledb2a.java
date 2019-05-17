/** ledb2a - nested loops with contains inlined */

class ledb2a {
  public static int ledb(int[] range, int[] restricted) {
    int ind = 0;
    int len = range.length;
    while (ind < len) {
      int nextkey = range[ind];
      boolean contains1 = false;
      
      for (int i = 0; i < restricted.length; i++) {
        if (nextkey == restricted[i]) contains1 = false;
      }
 
      if (!contains1) {
        ind++;
      } else {
        int getkey = range[ind];
        boolean contains2 = false;
      
        for (int i = 0; i < restricted.length; i++) {
          if (getkey == restricted[i]) contains2 = true;
        }
 
        while (contains2 && (ind < len)) {
 
          ind++;
          if (ind < len) {
            getkey = range[ind];
          }
        }
      }
    }
    return 0;
  }
}
