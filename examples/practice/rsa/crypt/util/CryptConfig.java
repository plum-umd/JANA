package crypt.util;

import java.math.BigInteger;
import java.util.Random;

public class CryptConfig {
    private BigInteger n;
    private BigInteger e;
    private BigInteger d;

    public void genKeys(long seed) {
        Random r = new Random(seed);
        BigInteger p = new BigInteger(512, 100, r);
        BigInteger q = new BigInteger(512, 100, r);
        this.n = p.multiply(q);
        BigInteger phi_n = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        BigInteger const_2 = new BigInteger("2");
        for (this.e = new BigInteger("3"); phi_n.gcd(this.e).intValue() > 1; this.e = this.e.add(const_2));
        this.d = this.e.modInverse(phi_n);
    }

    public CryptConfig(long seed) {
        this.genKeys(seed);
    }

    public BigInteger getExponent() {
        return this.e;
    }

    public BigInteger getModulus() {
        return this.n;
    }

    private static BigInteger modExp(BigInteger b, BigInteger e, BigInteger n) {
        BigInteger ret = BigInteger.ONE;

        while (!e.equals(BigInteger.ZERO)) {
            if (e.and(BigInteger.ONE).equals(BigInteger.ONE)) {
                ret = ret.multiply(b).mod(n);
            }

            e = e.shiftRight(1);
            b = b.multiply(b).mod(n);
        }

        return ret.mod(n);
    }

    public BigInteger encrypt(BigInteger plain) {
        return modExp(plain, e, n);
    }

    public BigInteger decrypt(BigInteger cipher) {
        return modExp(cipher, d, n);
    }
}
