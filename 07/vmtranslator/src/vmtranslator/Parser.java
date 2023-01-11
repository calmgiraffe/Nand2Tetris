
package vmtranslator;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
The Parser iterates through the input .vm file, analyzes each VM command,
and determines what methods to call from the CodeWriter class next.
*/
public class Parser {
    // Static variables
    public static final HashSet<String> ARITHMETIC = new HashSet<>();

    // Instance variables
    BufferedReader bufferedReader;
    private String currInstruct;
    private String commandType;
    private String arg1;
    private String arg2;
    HashMap<String, String> symbolTable = new HashMap<>();

    // Constructor
    public Parser(String source) throws IOException {
        // Initialize BufferedReader, which stores a line from the source into a 8K buffer
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);

        // Add arithmetic commands to ARITHMETIC
        ARITHMETIC.addAll(List.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not"));

        CodeWriter codeWriter = new CodeWriter(source);
    }


    private boolean hasMoreLines() {
        return currInstruct != null;
    }

    /*
    Advances the parser one line, setting currInstruction to the next valid instruction
    This method skips over comments, i.e., lines that start with //
    */
    private void advance() throws IOException {
        while (true) {
            currInstruct = bufferedReader.readLine();
            if (currInstruct == null) { // indicating EOF
                break;
            }
            currInstruct = currInstruct.trim();
            /*
            If currInstruction is not empty && does not start with //,
            and if the input file is error free, then currInstruction must be a VM command
            Split to list of words, the separator being whitespace.
            Then, change values of commandType, arg1, arg2.
            */
            if (!currInstruct.isEmpty() && !currInstruct.startsWith("//")) {
                String[] words = currInstruct.split(" ", 3);

                if (ARITHMETIC.contains(words[0])) {
                    commandType = "arithmetic";
                    arg1 = words[0];
                    arg2 = null;
                } else {
                    commandType = words[0];
                    arg1 = words[1];
                    arg2 = words[2];
                }
                break;
            }
        }
    }

    /*
    Iterates through the input, analyzes each line of instruction, and call appropriate translation
    method.
    Translates the source vm file into an asm file
     */
    public void translate() throws IOException {
        this.advance();
        while (this.hasMoreLines()) {
            // Parse the current instruction, determine whether it is arithmetic, push, or pop

            if (commandType.equals("arithmetic")) {
                writeArithmetic();
            } else if (commandType.equals("push") || commandType.equals("pop")) {
                writePushPop();
            }
            this.advance();
        }
        // Todo: close Writer and Reader at end
    }

    /*
    Writes to the output file the asm code that implements the given arithmetic-logical command
    */
    private void writeArithmetic(String arg1) {
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
    private void writePushPop(String arg1, String arg2) {
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
}