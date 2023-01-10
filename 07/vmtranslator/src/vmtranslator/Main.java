package vmtranslator;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Must have one arg.");
        } else {
            String inputFile = args[0];
            Parser parser = new Parser(inputFile);
            CodeWriter codeWriter = new CodeWriter(inputFile);

            parser.translate();
        }
    }
}
