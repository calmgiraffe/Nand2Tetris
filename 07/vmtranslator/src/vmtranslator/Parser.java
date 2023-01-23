package vmtranslator;

import java.io.*;
import java.util.HashMap;
import java.util.List;

/*
The Parser iterates through the input .vm file, analyzes each VM command,
and determines what assembly code to write to the output file.
*/
public class Parser {
    // Static variables
    private static final HashMap<String, String> REG_MAP = new HashMap<>(){
        {
            put("local", "LCL");
            put("argument", "ARG");
            put("this", "THIS");
            put("that", "THAT");
        }
    };
    private static final int TMP_OFFSET = 5;
    private static final char PUSH_POP = 'P';
    private static final char BRANCHING = 'B';
    private static final char FUNCTION = 'F';
    private static final char ARITHMETIC = 'A';

    // Instance variables
    private final BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private final String filePrefix;
    private char commandType;
    private String currInstruct, arg1, arg2, arg3;
    private String currFunction = "Main";
    private int jumpNum, callNum;

    // Constructor
    public Parser(String source) throws IOException {
        this.jumpNum = 0;
        this.callNum = 0;

        // Initialize BufferedReader, which stores a line from the source into a 8K buffer
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);

        // Initialize PrintWriter, which write a line to an output file with println() method
        // Make the output file name from the filename prefix
        this.filePrefix = source.substring(0, source.length() - 3);
        FileWriter fileWriter = new FileWriter(filePrefix + ".asm");
        this.printWriter = new PrintWriter(new BufferedWriter(fileWriter));
    }

    /*
    Advances the parser one line, setting currInstruction to the next valid instruction.
    This method skips over comments, i.e., lines that start with //
    If there are no more valid lines to parse, this.currInstruct = null.
    */
    private void advance() throws IOException {
        String[] words;

        while (true) {
            currInstruct = bufferedReader.readLine();
            if (currInstruct == null) {
                break;
            }
            currInstruct = currInstruct.trim();
            /*
            If currInstruction is not empty && does not start with //, then currInstruction must be VM command.
            Split to list of words about whitespace, then set values of commandType, arg1, arg2.

            Note: assumes that the input VM file has no errors, i.e., only comments, valid commands, or blank lines */
            if (!currInstruct.isEmpty() && !currInstruct.startsWith("//")) {
                words = currInstruct.split("\\s+");

                if (List.of("push", "pop").contains(words[0])) {
                    commandType = PUSH_POP;
                    arg1 = words[0];    // push or pop
                    arg2 = words[1];    // one of the stack segment names
                    arg3 = words[2];    // positive int
                } else if (List.of("label", "goto", "if-goto").contains(words[0])) {
                    commandType = BRANCHING;
                    arg1 = words[0];    // label, goto, if-goto
                    arg2 = words[1];    // label name
                    arg3 = null;
                } else if (List.of("function", "call", "return").contains(words[0])) {
                    commandType = FUNCTION;
                    arg1 = words[0];        // function, call, or return
                    if (arg1.equals("return")) {
                        arg2 = null;
                        arg3 = null;
                    } else {
                        arg2 = words[1];    // function f
                        arg3 = words[2];    // nArgs
                    }
                    arg1 = words[0];

                } else {
                    commandType = ARITHMETIC;
                    arg1 = words[0];    // add, sub, neg, eq, gt, lt, and, or, not
                    arg2 = null;
                    arg3 = null;
                }
                break;
            }
        }
    }

    /*
    Main method to translate the source vm file into an asm file. Iterates through the input,
    analyzes each line of instruction, and call appropriate translation method.
    */
    public void translate() throws IOException {
        this.advance(); // Initially advance to first valid line
        while (currInstruct != null) { // null if EOF

            if (commandType == PUSH_POP) {
                writePushPop();
            } else if (commandType == BRANCHING) {
                writeBranching();
            } else if (commandType == FUNCTION) {
                writeFunction();
            } else if (commandType == ARITHMETIC) {
                writeArithmetic();
            }
            this.advance();
        }
        // Put infinite loop at end of asm file
        printWriter.println("// infinite loop");
        printWriter.println("(END)");
        printWriter.println("@END");
        printWriter.print("0;JMP");

        bufferedReader.close();
        printWriter.close();
    }

    /*
    Writes to the output file the asm code that implements the current arithmetic-logical command.
    Cases: [add, sub, and, or], [not, neg], [eq, gt, lt]
    arg2, arg3 = null in the case of arithmetic-logical commands.
    */
    private void writeArithmetic() {
        String op;
        String jumpName;

        // Writing a comment to the asm file; can disable
        printWriter.println("// " + currInstruct);

        if (List.of("add", "sub", "and", "or").contains(arg1)) { // add, sub, and, or
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");
            printWriter.println("@SP");
            printWriter.println("A=M-1");
            op = switch (arg1) {
                case "add" -> "+";
                case "sub" -> "-";
                case "and" -> "&";
                case "or" -> "|";
                case default -> throw new IllegalArgumentException("Unexpected arithmetic op");
            };
            printWriter.println("M=M" + op + "D");

        } else if (List.of("not", "neg").contains(arg1)) { // not, neg
            printWriter.println("@SP");
            printWriter.println("A=M-1");
            op = switch (arg1) {
                case "not" -> "!";
                case "neg" -> "-";
                case default -> throw new IllegalArgumentException("Unexpected negation op");
            };
            printWriter.println("M=" + op + "M");

        } else if (List.of("eq", "gt", "lt").contains(arg1)) { // eq, gt, lt
            jumpName = "EQ_jump" + jumpNum;
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");
            printWriter.println("A=A-1");
            printWriter.println("D=M-D");
            printWriter.println("M=-1");
            printWriter.println("@" + jumpName);
            op = switch (arg1) {
                case "eq" -> "JEQ";
                case "gt" -> "JGT";
                case "lt" -> "JLT";
                case default -> throw new IllegalArgumentException("Unexpected equality op");
            };
            printWriter.println("D;" + op);
            printWriter.println("@SP");
            printWriter.println("A=M-1");
            printWriter.println("M=0");
            printWriter.println("(" + jumpName + ")");
            jumpNum += 1;
        }
    }

    /*
    Writes to the output file the asm code that implements the current push or pop command.
    arg1 = [pop, push]
    arg2 = [local, argument, this, that], [pointer, temp], [constant], [static]
    arg3 = some positive int
    */
    private void writePushPop() {
        String op;

        // Writing a comment to the asm file; can disable
        printWriter.println("// " + currInstruct);

        if (arg1.equals("push")) {
            switch (arg2) {
                case "constant" -> {
                    printWriter.println("@" + arg3); // arg3 = i
                    printWriter.println("D=A");
                }
                case "pointer" -> {
                    op = switch (arg3) {
                        case "0" -> "THIS";
                        case "1" -> "THAT";
                        case default -> throw new IllegalArgumentException("Unexpected pointer value");
                    };
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                case "temp" -> {
                    op = "R" + (Integer.parseInt(arg3) + TMP_OFFSET);
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                case "static" -> {
                    op = filePrefix + "." + arg3;
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                default -> {  // argument, local, this, that
                    printWriter.println("@" + REG_MAP.get(arg2));
                    printWriter.println("D=M");
                    printWriter.println("@" + arg3);
                    printWriter.println("A=D+A");
                    printWriter.println("D=M");
                }
            }
            // All push commands end with these 4 lines
            printWriter.println("@SP");
            printWriter.println("M=M+1");
            printWriter.println("A=M-1");
            printWriter.println("M=D");

        } else if (arg1.equals("pop")) {
            if (List.of("argument", "local", "this", "that").contains(arg2)) {
                printWriter.println("@" + REG_MAP.get(arg2));
                printWriter.println("D=M");
                printWriter.println("@" + arg3);
                printWriter.println("D=D+A");
                printWriter.println("@R13");
                printWriter.println("M=D");
                printWriter.println("@SP");     // These 3 commented lines are in common with other cases,
                printWriter.println("AM=M-1");  // but I don't have good way to separate them.
                printWriter.println("D=M");     // :-(
                printWriter.println("@R13");
                printWriter.println("A=M");
                printWriter.println("M=D");
                return;
            }
            // Else case: pointer, temp, static
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");

            switch (arg2) {
                case "pointer" -> {
                    op = switch (arg3) {
                        case "0" -> "THIS";
                        case "1" -> "THAT";
                        case default -> throw new IllegalArgumentException("Unexpected pointer value");
                    };
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
                case "temp" -> {
                    op = "R" + (Integer.parseInt(arg3) + TMP_OFFSET);
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
                case "static" -> {
                    op = filePrefix + "." + arg3;
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
            }
        }
    }

    /*
    Writes to the output file the asm code that implements the current branching command.
    arg1 = [label, goto, if-goto]
    arg2 = label name
    arg3 = null
    */
    private void writeBranching() {
        printWriter.println("// " + currInstruct);

        String symbol = filePrefix + "." + currFunction + "$" + arg2;
        switch (arg1) {
            case "label" -> printWriter.println("(" + symbol + ")");
            case "goto" -> {  // unconditional jump
                printWriter.println("@" + symbol);
                printWriter.println("0;JMP");
            }
            case "if-goto" -> {  // jump if stack's topmost value is not 0
                printWriter.println("@SP");
                printWriter.println("AM=M-1");
                printWriter.println("D=M");
                printWriter.println("@" + symbol);
                printWriter.println("D;JNE");
            }
        }
    }

    /*
    Writes to the output file the asm code that implements the current function command.
    arg1 = [call, function, return]
    arg2 = function name
    arg3 = nArgs
    */
    private void writeFunction() {
        printWriter.println("// " + currInstruct);

        String symbol = filePrefix + "." + currFunction + "$" + arg2;
        switch (arg1) {
            case "call" -> {

            }
            case "function" -> {  // unconditional jump

            }
            case "return" -> {  // jump if stack's topmost value is not 0

            }
        }
    }
}