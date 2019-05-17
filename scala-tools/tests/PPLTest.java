public class PPLTest {
    public static int testAnnotSyntax(int a, int b, int c) {
        return c;
    }

    public static int testChat() {
        int i = -10;
        while (i < -5) {
            i = i + 1;
        }
        return i;
    }

    public static int testForLoop(int a, int b) {
        for(int i = 0; i<100; i++) {
            if(a != b) {
                return 10;
            }
        }
        return 100;
    }

    public int search(int a, int b, int c, int d) {
        for(int i = 0; i < a; i++) {
            for(int j = 0; j < b; j++) {
                if(c == d) {
                    return i;
                }
            }
        }

        return a;
    }

    public static int test1(int a, int b) {
        int c = 0;
        // pre: c == 0
        while (c <= a) { // a <= -1
        // trans: c' = c + b
            c = c + b;
        }
        // post: c > a
        return c;
    }

    public static int test1mult(int a, int b) {
        int c = 2;
        // pre: c == 0
        while (c <= a) { // a <= -1
        // trans: c' = c + b
            c = c * b;
        }
        // post: c > a
        return c;
    }
    /*
    BB0
    BB1
    BB2
    v6 = phi  v5,v4:#0
    4   conditional branch(gt, to iindex=10) v6,v1(line 4) {6=[c], 1=[a]}
    BB3
    7   v5 = binaryop(add) v6 , v2               (line 5) {6=[c], 2=[b]}
    9   goto (from iindex= 9 to iindex = 2)      (line 5)
    BB4
    11   return v6                               (line 7) {6=[c]}
    BB5

    ideally:
       ret == a/b
    ideal with AI:
       ret > a
    computeBound ideally:
       (a-0)/b
    */


    public static int test1anno(int a, int b) {
        int c = 0;
	// ANNO ASSUME: b > 0
        // pre: c == 0
        while (c <= a) { // a <= -1
        // trans: c' = c + b
            c = c + b;
        }
        // post: c > a
        return c;
    }

    public static int test1annoB(int a, int b) {
        int c = 0;
	// ANNO ASSUME: a < 10
        while (c <= a) {
            c = c + 1;
        }
        return c;
    }

    public static int test1minus(int a, int b) {
        int c = 100;
        // pre: c == 0
        while (c > a) { // a <= -1
        // trans: c' = c + b
            c = c - b;
        }
        // post: c > a
        return c;
    }

    public static int test1renamed(int c, int b) {
        int a = 0;
        while (a <= c) {
            a = a + b;
        }
        return a;
    }

    public static int test2(int v0, int v1, int v2) {
        if (v0 >= v1) {
            return 0;
        } else {
            return 1;
        }
    }

    public static int test3(int v0, int v1) {
        int v2;
        if (v0 >= v1) {
            v2 = 0;
        } else {
            v2 = 1;
        }
        return v2;
    }

    public static int test4(int v0, int v1) {
        int v2;
        if (v0 >= v1) {
            v2 = 0;
        } else {
            v2 = 1;
        }
        return v2;
    }

    public static int test5(int i) {
        int x;
        if (i == 0) {
            x = 200;
        } else {
            x = 300;
        }
        return x;
    }

    public static int test6(int i, int j) {
        int x = 0;
        int z = 0;

        while (x < i) {
            if (i == 0) {
                x = 200;
                while (x > j) {
                    x--;
                }
            } else {
                x = 300;
                if (x < z) {
                    while (x > i) {
                        i++;
                    }
                }
            }
        }
        return x;
    }

    public static int twovars1(int a, int b) {
        int c = 20;
        int d = 0;

        // pre: c = 0 ^ d = 0
        while (c <= a) {
            // trans: c' = c + b ^ d' = 2 * b
            c = c + 3 * b;
            d = d + 2 * b;
        }
        // post: c > a ^ d?

        return c + d;
    }

    public static int twovars2(int i, int j) {
        while(i<0) {
            i++;
            j++;
        }
        return j;
    }
    /* ideally:
       if i >= 0: ret == j(in)
       if i <  0: ret == j(in) - i(in)
     */

     public static int nobounds(int i) {
         i = 0;
         while(i>=0) {
             i++;
         }
         return i;
     }

     public static int exce(int i, int j) {
         i = 0;
         j = 4/i;
         return i;
     }

    public static int twoloops1(int a, int b) {
        int c = 0;

        while (c <= a) {
            c = c + b;
        }

        while (c <= 2*a) {
            c = c + 2*b;
        }

        return c;
    }

    public static int twoloops1sep(int a, int b) {
        int c = 0;

        while (c <= a) {
            c = c + b;
        }

        c = c + 1;

        while (c <= 2*a) {
            c = c + 2*b;
        }

        return c;
    }

    public static int test_image(int x, int y) {
        int v1 = x;
        while (v1 < y) {
            v1 += 1;
        }
        return v1;
    }

    public static int bar1(int a, int b) {
        if(a < b)
            a++;
        else
            a--;

        return a;
    }

    public static int bar2(int a, int b) {
        if(a < b)
            a++;

        return a;
    }

    public static int bar3(int a, int b) {
        while(a < b)
            a++;

        return a;
    }

    public int goo0(int i) {
        return i + 2;
    }

    public static int goo1(int i) {
        return i + 2;
    }

    public static int goo2(int i, int j) {
        return i + j;
    }

    public static int updown(int i, int j) {
        while(j>0) {
            i++;
            j--;
        }
        return i;
    }
    public int goo3() {
        int i = -10;
        while(i>0) {
            i++;
        }
        return 0;
    }

  public static int simplenested(int max1, int max2) {
    int a = 0;
    while (a < max1) {
      int b = 0;
      while (b < max2) {
        b++;
      }
      a++;
    }

    return a;
  }

    public static int nested(int v0) {
        int v1 = 0;
        int v2 = 0;
        int v3 = 5;
        while (v1 <= 10) {
            v1 = v1 + 1;
            if (v1 % 2 == 0) {
                v2 = 0;
                while(v2 <= 5) {
                    v2 = v2 + 1;
                }
            } else {
                v2 = 0;
                while(v2 <= 6) {
                    v2 = v2 + 2;
                }
            }

            v3 = 6;
        }

        return v2;
    }

    public int testJoin6(int i) {
        while(i <= 0) {
            i++;
        }
        i = i + 1;
        return i;

        /* ideally:
           ret >= 2
        */
    }

    public static int testJoin7(int i) {
        while(i <= 0) {
            i++;
        }
        return i;
        /*
        CFG:
BB0[-1..-2]
    -> BB1
BB1[0..2]
    -> BB3
    -> BB2
BB2[3..7]
    -> BB1
BB3[8..9]
    -> BB4
BB4[-1..-2]
Instructions:
BB0
BB1
           v6 = phi  v5,v1
2   conditional branch(gt, to iindex=8) v6,v3:#0(line 204) {6=[i]}
BB2
5   v5 = binaryop(add) v6 , v4:#1            (line 205) {6=[i]}
7   goto (from iindex= 7 to iindex = 0)      (line 205)
BB3
9   return v6                                (line 207) {6=[i]}
BB4

          ideally:
            ret >= 1
        */
    }


    public static int nonconstantPre(int a, int b, int nonconstant) {
        int c = nonconstant;
        // pre: c == nonconstant
        while (c <= a) {
        // trans: c' = c + b
            c = c + b;
        }
        // post: c > a + nonconstant
        return c;
    }

    public static int testGhost(int a, int b, int d) {
        int c = d;
        // pre: c == 0
        while (c <= a) { // a <= -1
        // trans: c' = c + b
            c = c + b;
        }
        // post: c > a
        return c;
    }

  public static void testvoid(int a, int b) {
    int c = 0;
    while (c < a) {
      c++;
    }
  }
}
