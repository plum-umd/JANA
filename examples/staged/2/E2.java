public class E2 {
  //disjunctions, no loops

  public int checkPwI(int a, int b, int c, int d, int e) {
    int ct = 0;
    //      S
    if(a == 1) {
      ct++;
      //      S
      if(b == 2) {
        ct++;
        //      S
        if(c == 3) {
          ct++;
          //      S
          if(d == 4) {
            ct++;
            //      S
            if(e == 5) {
              ct++;
              //assert(ct == 5)
              return ct;
            }
          }
        }
      }
    }

    //assert(ct >= 0 && ct < 5)
    return ct;
  }
    
    public int test() {
        return checkPwI(1, 2, 3, 4, 5); //anticipated: ret = 5 (checked)
        //        return checkPwI(0, 1, 1, 1, 1); //anticipated: ret = 0 (checked)
        //        return checkPwI(1, 2, 1, 4, 5); //anticipated: ret = 2 (checked)
    }
    
    /*
     < Application, Lsoucis/taint/test/E2, checkPwI(IIIII)I >
     CFG:
     BB0[-1..-2]
     -> BB1
     BB1[0..4]
     -> BB7
     -> BB2
     BB2[5..11]
     -> BB7
     -> BB3
     BB3[12..18]
     -> BB7
     -> BB4
     BB4[19..25]
     -> BB7
     -> BB5
     BB5[26..32]
     -> BB7
     -> BB6
     BB6[33..38]
     -> BB8
     BB7[39..40]
     -> BB8
     BB8[-1..-2]
     Instructions:
     BB0
     BB1
     4   conditional branch(ne) v2,v9:#1          (line 9) {2=[a]}
     BB2
     7   v10 = binaryop(add) v8:#0 , v9:#1        (line 10) {8=[ct]}
     11   conditional branch(ne) v3,v11:#2        (line 12) {3=[b]}
     BB3
     14   v12 = binaryop(add) v10 , v9:#1         (line 13) {10=[ct]}
     18   conditional branch(ne) v4,v13:#3        (line 15) {4=[c]}
     BB4
     21   v14 = binaryop(add) v12 , v9:#1         (line 16) {12=[ct]}
     25   conditional branch(ne) v5,v15:#4        (line 18) {5=[d]}
     BB5
     28   v16 = binaryop(add) v14 , v9:#1         (line 19) {14=[ct]}
     32   conditional branch(ne) v6,v17:#5        (line 21) {6=[e]}
     BB6
     35   v19 = binaryop(add) v16 , v9:#1         (line 22) {16=[ct]}
     38   return v19                              (line 24) {19=[ct]}
     BB7
     v18 = phi  v8:#0,v10,v12,v14,v16
     40   return v18                              (line 32) {18=[ct]}
     BB8
     */
}
