package crypt.main;

import java.math.BigInteger;

import crypt.util.CryptConfig;

public class Main {
    private static void printUsage() {
        System.out.println("Usage: java -jar monday.jar <seed> [show | encrypt <plain> | decrypt <cipher>]");

        System.out.println("Note: <seed> must be a base 10 integer, which fits into a Java long.");
    }

    public static void main(String[] argv) {
        if (argv.length != 2 && argv.length != 3) {
            printUsage();

            return;
        }

        CryptConfig c = new CryptConfig(Long.parseLong(argv[0], 10));

        if (argv[1].equals("show")) {
            System.out.println("Public Key\n----------");
            System.out.println("exponent = " + c.getExponent());
            System.out.println("modulus  = " + c.getModulus());
        } else if (argv[1].equals("encrypt")) {
            System.out.println(c.encrypt(new BigInteger(argv[2].getBytes())));
        } else if (argv[1].equals("decrypt")) {
            System.out.println(new String(c.decrypt(new BigInteger(argv[2])).toByteArray()));
        } else {
            printUsage();
        }
    }
}
