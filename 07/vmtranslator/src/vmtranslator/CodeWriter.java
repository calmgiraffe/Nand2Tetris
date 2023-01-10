package vmtranslator;

/*
Translates a parsed VM command into Hack assembly code.
For example,
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeWriter {

    final PrintWriter printWriter;

    public CodeWriter(String source) throws IOException {
        // Make the output file name from the source file name prefix
        String output = source.substring(0, source.length() - 3) + ".asm";

        // Initialize PrintReader, can write lines to an output file with println() method
        FileWriter fileWriter = new FileWriter(output);
        this.printWriter = new PrintWriter(new BufferedWriter(fileWriter));
        //printWriter.println("Sample text");

    }

    /*
    Writes to the output file the asm code that implements the given arithmetic-logical command
    */
    public void writeArithmetic(String command) {
        return;
    }

    /*
    Writes to the output file the asm code that implements the given push or pop command
    */
    public void writePushPop(String command) {

    }

    /*
    Closes the outfile file/stream
    */
    public void close() {

    }
}
