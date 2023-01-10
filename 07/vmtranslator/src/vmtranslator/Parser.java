
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
    public static final HashSet<String> C_ARITHMETIC = new HashSet<>();
    public static final String C_PUSH = "push";
    public static final String C_POP = "pop";

    // Instance variables
    BufferedReader bufferedReader;

    private String currInstruction;
    private String[] words;
    HashMap<String, String> symbolTable = new HashMap<>();

    // Constructor
    public Parser(String source) throws IOException {
        // Initialize BufferedReader, which stores a line from the source into a 8K buffer
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);

        // Add strings to C_ARITHMETIC
        C_ARITHMETIC.addAll(List.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not"));
    }

    /*
    Iterates through the input file, analyzes the lines, calls appropriate method from CodeWriter
    to generate the correct asm code
    */
    public boolean translate() throws IOException {
        this.advance();

        String command;
        while (this.hasMoreLines()) {
            // Parse the current instruction, determine whether it is arithmetic, push, or pop
            command = commandType();
            if (C_ARITHMETIC.contains(command)) {

            } else if (command.equals(C_PUSH)) {

            } else if (command.equals(C_POP)) {

            }
            System.out.println(command);
            this.advance();
        }
        return false;
    }


    public boolean hasMoreLines() {
        return currInstruction != null;
    }

    /*
    Advances the parser one line, setting currInstruction to the next valid instruction
    This method skips over comments, i.e., lines that start with //
    */
    public void advance() throws IOException {
        while (true) {
            currInstruction = bufferedReader.readLine();

            if (currInstruction == null) { // indicating EOF
                break;
            }
            currInstruction = currInstruction.trim();
            /* If currInstruction is not empty && does not start with //,
            and if the input file is error free, then currInstruction must be a VM command
            Split to list of words, the separator being whitespace */
            if (!currInstruction.isEmpty() && !currInstruction.startsWith("//")) {
                words = currInstruction.split(" ");
                break;
            }
        }
    }

    /*
    Returns a constant representing the type of the current command.
    For example, if the current command is push local 2, then this method returns PUSH.
    If the current command is add, then this method returns ARITHMETIC.
    */
    public String commandType() {
        return words[0];
    }

    /*
    Returns the first argument of the current VM command.
    For example, if the current command is push local 2, then this method returns local.
    If the current command is arithmetic-logical, returns that command itself.
    */
    public String arg1() {
        return words[1];
    }

    /*
    Returns the second argument of the current VM command.
    For example, if the current command is push local 2, then this method returns 2.
    Returns null if the current command is an arithmetic logical command.
    */
    public String arg2() {
        return words[2];
    }
}