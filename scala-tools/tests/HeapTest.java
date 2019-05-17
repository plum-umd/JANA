public class HeapTest {
    
    public static void testPut1(X x) {
        
        //X.f = 100 V X.f = X.f(in)
        
        x.f = 100;
    }
    
    public static int testGet1(X x) {
        
        //ret = X.f(in)
        
        return x.f;
    }
    
    public static int testPutGet1(X x) {
        
        //100<=X.f<=300 V X.f=X.f(in)
        //ret = 300
        
        x.f = 100;
        x.f = x.f + 200;;
        return x.f;
    }
    
    public static int testPutGet2(X x) {
        
        //X.f(in)<=X.f<=X.f(in)+100
        //ret = X.f(in)+100
        
        x.f = x.f + 100;
        return x.f;
    }
    
    public static int testPutGet3(X x) {
        
        //X.f(in)<=X.f<=X.f(in)+100 V X.f=200
        //ret = 200
        
        x.f = x.f + 100;
        x.f = 200;
        return x.f;
    }
    
    public static int testTwoObjs1(X x1, X x2) {
        
        //called as the root:
        //(x1->X1, x2->X2)
        //X1.f = X1.f(in) V X1.f=200
        //X2.f = X2.f(in) V X2.f=300
        
        //called from testTwoObjs1Caller1
        //(x1->X, x2->X)
        //200<=X.f<=300 V X.f=X.f(in)
        //200<=ret<=300
        
        //called from testTwoObj1caller2
        //(x1->X1, x2->X2)
        //X1.f = X1.f(in) V X1.f=200
        //X2.f = X2.f(in) V X2.f=300
        //ret = 200
        
        //called from testTwoObj1caller3
        //context-insensitive
        //(x1->X1, x2->{X1,X2})
        //X1.f = X1.f(in) V 200<=X1.f<=300
        //X2.f = X2.f(in) V X2.f=300
        //200<=ret<=300
        
        //called from testTwoObj1caller3
        //1-cfa context-sensitive
        //context 1:
        //(x1->X1, x2->X1)
        //X1.f = X1.f(in) V 200<=X1.f<=300
        //200<=ret<=300
        //context 2:
        //(x1->X1, x2->X2)
        //X1.f = X1.f(in) V X1.f=200
        //X2.f = X2.f(in) V X2.f=300
        //ret = 200

        
        x1.f = 200;
        x2.f = 300;
        return x1.f;
    }
    
    public int testTwoObjs1Caller1() {
        
        //0<=X.f<=300
        //200<ret<=300
        
        X x = new X();
        return testTwoObjs1(x, x);
    }
    
    public int testTwoObjs1Caller2() {
        
        //0<=X1.f<=200
        //0<=X2.f<=300
        //ret = 200
        
        X x1 = new X();
        X x2 = new X();
        return testTwoObjs1(x1, x2);
    }
    
    public int testTwoObjs1Caller3() {
        
        //context-insensitive:
        //0<=X1.f<=300
        //0<=X2.f<=300
        //400<=ret<=600
        
        //1-cfa context-sensitive:
        //0<=X1.f<=300
        //0<=X2.f<=300
        //400<=ret<=500
        
        X x1 = new X();
        X x2 = new X();
        int a = testTwoObjs1(x1, x1);
        int b = testTwoObjs1(x1, x2);
        return a+b;
    }
    
}

class X {
    
    public int f;
    public int g;
    public X next;
    public X() {
        this.f = 100;
    }
    public X(int f) {
        this.f = f;
    }
}

