/** ledb2c - ArrayList object without exception control-flow */

import java.util.ArrayList;
import java.util.List;

class ledb2c {
  public static int ledb(List<Integer> range, ArrayList<Integer> restricted) {
    int ind = 0;
    while (ind < range.size()) {
      Integer nextkey = range.get(ind);
 
      if (!restricted.contains(nextkey.intValue())) {
        ind++;
      } else {
        Integer getkey = range.get(ind);
             
        while ((restricted.contains(getkey.intValue())) && (ind < range.size())) {
          ind++;
          if (ind < range.size()) {
            getkey = range.get(ind);
          }
        }
      }
    }
    return 0;
  }
}
