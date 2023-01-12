package hackassembler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        /*
        If one arg, print error msg
        Else, run the assembler where the arg is the .asm file to be processed
        */
        if (args.length != 1) {
            System.out.println("Must have one arg.");
        } else {
            // example: Pong.asm is 195 KB
            Parser parser = new Parser(args[0]);
            parser.assemble();
        }
    }
}
