package match.main;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import match.util.StringUtil;

public class Main {

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path, new String[0]));
        return new String(encoded, StandardCharsets.US_ASCII);
    }


    public static void main(String[] argv) {
        if (argv.length != 2 && argv.length != 3) {
            System.out.println("Usage: java -jar Sunday.jar /path/to/text.txt /path/to/pattern.txt [--debug]");

            return;
        }

        boolean debug = argv.length == 3 && argv[2].equals("--debug");

        try {
            String text = readFile(argv[0]);
            String pattern = readFile(argv[1]);

            if (debug) {
                System.out.println("Text\n-------");
                System.out.println(text);

                System.out.println("\nPattern\n-------");
                System.out.println(pattern);

                System.out.println();
            }

                System.out.println("Result\n-------");

            int idx = StringUtil.substring(text, pattern);

            if (idx == -1) {
                System.out.println("No match.");
            } else {
                System.out.println("Match @ " + idx + ".");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
