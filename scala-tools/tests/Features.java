//package edu.umd.soucis;

public class Features {
    /* 
     * Logic.
     */
    public static int disjunct(int a, int b) {
        if (a == b) {
            return 0;
        } else {
            return 1;
        }
        /* ideally:
             a ≥ b + 1, ret = 1
           ∨ b ≥ a + 1, ret = 1
           ∨ a = b,     ret = 0
         */
    }
    
    /*
     * Basic types in method parameters.
     */
    public static long types_int(byte a_byte, short a_short, int a_int, long a_long) {
        return a_byte + a_short + a_int + a_long;
    }

    public static double types_float(float a_float, double a_double) {
        return a_float + a_double;
    }

    public static int types_other(char a_char, boolean a_boolean) {
        return a_char + (a_boolean ? 0 : 1);
    }

    /*
     * Loops
     */
    public static void simplest_loop() {
        // Note this results in an error after interpretation due to
        // unreachable exit.
        int c = 0;
        while (true) {
            c++;
        }
    }

    /*
     * Various arithmetic features.
     */
    public static int div_const(int a) {
        return a / 37;
    }
    public static int div_const_neg(int a) {
        return a / -37;
    }
    public static int div(int a, int b) {
        return a / b;
    }
    public static int mult_const(int a) {
        return a * 37;
    }
    public static int mult_const_neg(int a) {
        return a * -37;
    }
    public static int mult(int a, int b) {
        return a * b;
    }
    public static int mod_const(int a) {
        return a % 37;
    }
    public static int mod_const_neg(int a) {
        return a % -37;
    }
    public static int mod(int a, int b) {
        return a % b;
    }

    /*
     * Floating point arithmetic.
     */
    public static float div_float_const(float a) {
        return a / 0.25f;
    }
    public static float mult_float_const(float a) {
        return a * 0.25f;
    }
}
