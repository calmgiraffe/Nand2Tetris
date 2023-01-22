package vmtranslator;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Must have at least one argument.");
        } else {
            for (String arg : args) {
                Parser parser = new Parser(arg);
                parser.translate();
            }
        }
    }
}
