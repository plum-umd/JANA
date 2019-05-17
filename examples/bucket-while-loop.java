package bucket-while-loop;

public class bucket-while-loop {
    public boolean nondet;

    void bucketExample1(int n) {
        assert(0 < n);
        int i = 0;
        int m = 0;
        while (i < n && nondet) {
            if (nondet) {
                m++;
            }
            j = 0;
            while (j < m) {
                j++;
            }
            i++;
        }
    }

    void bucketExampleNoInc(int n) {
        assert(0 < n);
        int i = 0;
        int m = 0;
        while (i < n && nondet) {
            j = 0;
            while (j < m) {
                j++;
            }
            i++;
        }
    }

    void bucketExampleAlwaysInc(int n) {
        assert(0 < n);
        int i = 0;
        int m = 0;
        while (i < n && nondet) {
            m++;
            j = 0;
            while (j < m) {
                j++;
            }
            i++;
        }
    }
}
