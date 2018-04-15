//disjunctions, loops, byte arrays / heap

public class PWCheck {
  //                                    T
  //   0 <= pc <= guess.length
  //   pc = guess.length
  public static boolean verifyS(byte[] guess, byte[] pw) {
    //            T
    if(guess.length != pw.length) {
      return false;
    }

    int i;

    //            T
    for(i = 0; i < guess.length; i++) {
      //          T
      if(guess[i] != pw[i]) {
        return false;
      }
    }
    // K <= pc <= guess.length + J

    return true;

  }

  //                                    T
  //   0 <= pc <= guess.length
  //   pc = guess.length
  public static boolean verifyS(byte[] guess) {
    byte[]  pw = new byte[4];

//  S
    pw[0] = 'p';
    pw[1] = 'a';
    pw[2] = 's';
    pw[3] = 's';

    //            T
    if(guess.length != pw.length) {
      return false;
    }

    int i;

    //            T
    for(i = 0; i < guess.length; i++) {
      //          T
      if(guess[i] != pw[i]) {
        return false;
      }
    }
    // K <= pc <= guess.length + J

    return true;

  }

  public static boolean verifyS(byte[] guess, int b) {
    byte[]  pw = new byte[4];

//  S
    pw[0] = 'p';
    pw[1] = 'a';
    pw[2] = 's';
    pw[3] = 's';

    //            T
    if(guess.length != pw.length) {
      return false;
    }

    int i;

    //            T
    for(i = b; i < guess.length; i++) {
      //          T
      if(guess[i] != pw[i]) {
        return false;
      }
    }
    // K <= pc <= guess.length + J

    return true;

  }

  public static boolean verifyS2(byte[] guess, int b) {
    byte[]  pw = new byte[4];

//  S
    pw[0] = 'p';
    pw[1] = 'a';
    pw[2] = 's';
    pw[3] = 's';

    // //            T
    // if(guess.length != pw.length) {
    //   return false;
    // }

    int i;
    int l = guess.length;

    //            T
    for(i = b; i < l; i++) {
      //          T
      i++;
      if(guess[i] != pw[i]) {
        return false;
      }
      i++;
    }
    // K <= pc <= guess.length + J

    return true;

  }

    public static boolean test() {
        //        byte[] guess = new byte[3];
        //        guess[1] = 'p';
        //        return verifyS(guess); //anticipated: ret = false + heap results (checked)
        byte[] guess = new byte[4];
        guess[1] = 'p';
        return verifyS(guess); //anticipated: ret = false + heap results; actual: 0
        //        byte[] guess = new byte[4];
        //        guess[0] = 'p';
        //        guess[1] = 'a';
        //        guess[2] = 's';
        //        guess[3] = 's';
        //        return verifyS(guess); //anticipated: ret = true + heap results; actual: 0<=ret<=1
    }

    /*
     < Application, Lsoucis/taint/test/PWCheck, verifyS([B)Z >
     CFG:
     BB0[-1..-2]
     -> BB1
     BB1[0..1]
     -> BB2
     -> BB19
     BB2[2..6]
     -> BB3
     -> BB19
     BB3[7..10]
     -> BB4
     -> BB19
     BB4[11..14]
     -> BB5
     -> BB19
     BB5[15..18]
     -> BB6
     -> BB19
     BB6[19..20]
     -> BB7
     -> BB19
     BB7[21..22]
     -> BB8
     -> BB19
     BB8[23..23]
     -> BB10
     -> BB9
     BB9[24..25]
     -> BB19
     BB10[26..28]
     -> BB16
     BB11[29..31]
     -> BB12
     -> BB19
     BB12[32..34]
     -> BB13
     -> BB19
     BB13[35..35]
     -> BB15
     -> BB14
     BB14[36..37]
     -> BB19
     BB15[38..41]
     -> BB16
     BB16[42..44]
     -> BB17
     -> BB19
     BB17[45..45]
     -> BB11
     -> BB18
     BB18[46..47]
     -> BB19
     BB19[-1..-2]
     Instructions:
     BB0
     BB1
     1   v4 = new <Primordial,[B>@1v3:#4          (line 10)
     BB2
     6   arraystore v4[v5:#0] = v6:#112           (line 13) {4=[pw]}
     BB3
     10   arraystore v4[v7:#1] = v8:#97           (line 14) {4=[pw]}
     BB4
     14   arraystore v4[v9:#2] = v10:#115         (line 15) {4=[pw]}
     BB5
     18   arraystore v4[v11:#3] = v10:#115        (line 16) {4=[pw]}
     BB6
     20   v12 = arraylength v1                    (line 19) {1=[guess]}
     BB7
     22   v13 = arraylength v4                    (line 19) {4=[pw]}
     BB8
     23   conditional branch(eq) v12,v13          (line 19)
     BB9
     25   return v5:#0                            (line 20)
     BB10
     28   goto (from iindex= 28 to iindex = 42)   (line 26)
     BB11
     31   v15 = arrayload v1[v18]                 (line 28) {1=[guess], 18=[i]}
     BB12
     34   v16 = arrayload v4[v18]                 (line 28) {4=[pw], 18=[i]}
     BB13
     35   conditional branch(eq) v15,v16          (line 28)
     BB14
     37   return v5:#0                            (line 29)
     BB15
     40   v17 = binaryop(add) v18 , v7:#1         (line 26) {18=[i]}
     BB16
     v18 = phi  v5:#0,v17
     44   v14 = arraylength v1                    (line 26) {1=[guess]}
     BB17
     45   conditional branch(lt) v18,v14          (line 26) {18=[i]}
     BB18
     47   return v7:#1                            (line 34)
     BB19
     */

}
