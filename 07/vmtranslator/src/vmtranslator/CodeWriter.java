package vmtranslator;

/*
Translates a parsed VM command into Hack assembly code.
For example,
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class CodeWriter {

    private static final int THIS = 3;
    private static final int THAT = 4;

    private final String fileName;
    private int staticVar = 16;
    private final HashMap<Integer, Integer> staticVarMap;
    private final PrintWriter printWriter;


    public CodeWriter(String source) throws IOException {
        // Make the output file name from the source file name prefix
        this.fileName = source.substring(0, source.length() - 3);
        this.staticVarMap = new HashMap<>();
        String output = fileName + ".asm";

        // Initialize PrintReader, can write lines to an output file with println() method
        FileWriter fileWriter = new FileWriter(output);
        this.printWriter = new PrintWriter(new BufferedWriter(fileWriter));
        //printWriter.println("Sample text");
    }

    /*
    Writes to the output file the asm code that implements the given arithmetic-logical command
    */
    public void writeArithmetic(String arg1) {
        /*
        Cases:
        and, sub, and, or
        not, neg
        eq, gt, lt

        no 2nd or 3rd args
         */
    }

    /*
    Writes to the output file the asm code that implements the given push or pop command
    */
    public void writePushPop(String arg1, String arg2) {
        /*
        Cases:
        pop
        push

        Sub cases:
        LCL, ARG, THIS, THAT
        POINTER, TEMP
        CONSTANT
        STATIC
        */
    }

    /*
    Closes the outfile file/stream
    */
    public void close() {

    }
}
