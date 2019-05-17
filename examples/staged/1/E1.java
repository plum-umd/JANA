public class E1 {
  // no loops, no equality, no methods, strings, heap, etc...
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
    
    public static int test(){
        return foo(10, 20, 1, 50, 100); //anticipated: ret = 4; actual: 298/99(3.000..) <= ret <= 4
        //        return foo(1, 1, 1, 1, 1); //anticipated: ret  = 1; (checked)
    }
    
    /*
     < Application, Lsoucis/taint/test/E1, foo(IIIII)I >
     CFG:
     BB0[-1..-2]
     -> BB1
     BB1[0..4]
     -> BB3
     -> BB2
     BB2[5..8]
     -> BB3
     BB3[9..11]
     -> BB5
     -> BB4
     BB4[12..15]
     -> BB5
     BB5[16..18]
     -> BB8
     -> BB6
     BB6[19..21]
     -> BB8
     -> BB7
     BB7[22..25]
     -> BB8
     BB8[26..28]
     -> BB11
     -> BB9
     BB9[29..31]
     -> BB11
     -> BB10
     BB10[32..35]
     -> BB11
     BB11[36..38]
     -> BB13
     -> BB12
     BB12[39..42]
     -> BB13
     BB13[43..44]
     -> BB14
     BB14[-1..-2]
     Instructions:
     BB0
     BB1
     4   conditional branch(le) v1,v8:#5          (line 8) {1=[a]}
     BB2
     7   v10 = binaryop(add) v7:#0 , v9:#1        (line 9) {7=[x]}
     BB3
     v11 = phi  v7:#0,v10
     11   conditional branch(le) v2,v12:#10       (line 12) {2=[b]}
     BB4
     14   v13 = binaryop(add) v11 , v9:#1         (line 13) {11=[x]}
     BB5
     v14 = phi  v11,v13
     18   conditional branch(ge) v3,v12:#10       (line 16) {3=[c]}
     BB6
     21   conditional branch(le) v3,v15:#20       (line 16) {3=[c]}
     BB7
     24   v16 = binaryop(add) v14 , v9:#1         (line 17) {14=[x]}
     BB8
     v17 = phi  v14,v14,v16
     28   conditional branch(ge) v4,v18:#100      (line 20) {4=[d]}
     BB9
     31   conditional branch(le) v4,v7:#0         (line 20) {4=[d]}
     BB10
     34   v19 = binaryop(add) v17 , v9:#1         (line 21) {17=[x]}
     BB11
     v20 = phi  v17,v17,v19
     38   conditional branch(le) v5,v7:#0         (line 24) {5=[e]}
     BB12
     41   v21 = binaryop(add) v20 , v9:#1         (line 25) {20=[x]}
     BB13
     v22 = phi  v20,v21
     44   return v22                              (line 29) {22=[x]}
     BB14
     */
}
