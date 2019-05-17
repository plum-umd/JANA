import java.net.Socket;

public class red_jit {
    // This example shows that there can be timing leaks introduced by
    // the just-in-time compiler. The demonstrations centers around
    // taking one of two branches of a program based on a secret value
    // (attack_at_dawn) below. Both branches perform the exact same
    // computation and hence would be expected to take the same amount
    // of time. However, if one of those branches is compiled ahead of
    // time due to being invoked many times previously in the program,
    // that branch will be executed faster than the not-yet-compiled
    // branch. This would then lead to the leak of the secret value by
    // an adversary observing the timing of the taken branch. This can
    // be especially bad if the adversary can influence the
    // just-in-time compilation of the program prior to getting the
    // branch to execute.

    // Note: you can observe JIT events by invoking java with:
    // -XX:+PrintCompilation
    
    public static int iters_to_jit = 50;
    // The actual iterations before being Jitted seems to vary from
    // around 20 to 40.
    
    public static int scale = 10000;
    
    public static void main(String[] args) {
        boolean attack_at_dawn = true; // the secret
        
        for (int i = 0; i <= iters_to_jit; i++) {
            System.out.println("branch1:" + i);
            branch1(i);
        }

        if (attack_at_dawn) {
            branch1(42);
        } else {
            branch2(42);
        }
    }

    public static int branch1(int busy) {
        for (int i = 0; i <= scale; i++) {
            busy += i;
        }
        return busy;
    }

    public static int branch2(int busy) {
        for (int i = 0; i <= scale; i++) {
            busy += i;
        }
        return busy;
    }
}
