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
    private static final HashSet<String> ARITHMETIC = new HashSet<>();
    private static final HashMap<String, String> REG_MAP = new HashMap<>();
    private static final int TMP_OFFSET = 5;

    // Instance variables
    private final BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private final String filePrefix;
    private String currInstruct;
    private String commandType;
    private String arg1;
    private String arg2;
    private int jumpNum;

    // Constructor
    public Parser(String source) throws IOException {
        // Initialize BufferedReader, which stores a line from the source into a 8K buffer
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);

        // Initialize PrintWriter, which write a line to an output file with println() method
        // Make the output file name from the filename prefix
        this.filePrefix = source.substring(0, source.length() - 3);
        FileWriter fileWriter = new FileWriter(filePrefix + ".asm");
        this.printWriter = new PrintWriter(new BufferedWriter(fileWriter));

        ARITHMETIC.addAll(List.of("add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not"));
        REG_MAP.put("local", "LCL");
        REG_MAP.put("argument", "ARG");
        REG_MAP.put("this", "THIS");
        REG_MAP.put("that", "THAT");

        this.jumpNum = 0;
    }

    /*
    Advances the parser one line, setting currInstruction to the next valid instruction
    This method skips over comments, i.e., lines that start with //
    If there are no more valid lines to parse, currInstruct is null
    */
    private void advance() throws IOException {
        String[] words;

        while (true) {
            currInstruct = bufferedReader.readLine();
            if (currInstruct == null) { // indicating EOF
                break;
            }
            currInstruct = currInstruct.trim();
            /*
            If currInstruction is not empty && does not start with //,
            and if the input file is error free, then currInstruction must be a VM command.
            Split to list of words, the separator being a space.
            Finally, set values of commandType, arg1, arg2.
            */
            if (!currInstruct.isEmpty() && !currInstruct.startsWith("//")) {
                words = currInstruct.split(" ", 3);

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
    General method to translate the source vm file into an asm file. Iterates through the input,
    analyzes each line of instruction, and call appropriate translation method.
    */
    public void translate() throws IOException {
        // Initially advance to first valid line
        this.advance();
        while (currInstruct != null) { // null if EOF
            if (commandType.equals("arithmetic")) {
                writeArithmetic();
            } else if (commandType.equals("push") || commandType.equals("pop")) {
                writePushPop();
            }
            // Todo: add more commandType cases later
            this.advance();
        }
        // put infinite loop at end of asm file
        printWriter.println("// infinite loop");
        printWriter.println("(END)");
        printWriter.println("@END");
        printWriter.println("0;JMP");

        bufferedReader.close();
        printWriter.close();
    }

    /*
    Writes to the output file the asm code that implements the given arithmetic-logical command.
    Cases: [add, sub, and, or], [not, neg], [eq, gt, lt]
    arg2 = null in the case of arithmetic-logical commands.
    */
    private void writeArithmetic() {
        String op;
        String jumpName;

        if (ARITHMETIC.contains(arg1)) {
            printWriter.println("// " + currInstruct);

            if (List.of("add", "sub", "and", "or").contains(arg1)) {
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

            } else if (List.of("not", "neg").contains(arg1)) {
                printWriter.println("@SP");
                printWriter.println("A=M-1");
                op = switch (arg1) {
                    case "not" -> "!";
                    case "neg" -> "-";
                    case default -> throw new IllegalArgumentException("Unexpected negation op");
                };
                printWriter.println("M=" + op + "M");

            } else { // eq, gt, lt
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
    }

    /*
    Writes to the output file the asm code that implements the given push or pop command.
    arg1 cases: [pop, push]
    arg2 cases: [LCL, ARG, THIS, THAT], [POINTER, TEMP], [CONSTANT], [STATIC]
    */
    private void writePushPop() {
        String op;

        if (!commandType.equals("pop") && !commandType.equals("push")) {
            return;
        }
        printWriter.println("// " + currInstruct);

        if (commandType.equals("push")) {
            switch (arg1) {
                case "constant" -> {
                    printWriter.println("@" + arg2); // arg2 = i
                    printWriter.println("D=A");
                }
                case "pointer" -> {
                    op = switch (arg2) {
                        case "0" -> "THIS";
                        case "1" -> "THAT";
                        case default -> throw new IllegalArgumentException("Unexpected pointer value");
                    };
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                case "temp" -> {
                    op = "R" + (Integer.parseInt(arg2) + TMP_OFFSET);
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                case "static" -> {
                    op = filePrefix + "." + arg2;
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                default -> {  // argument, local, this, that
                    printWriter.println("@" + REG_MAP.get(arg1));
                    printWriter.println("D=M");
                    printWriter.println("@" + arg2);
                    printWriter.println("A=D+A");
                    printWriter.println("D=M");
                }
            }
            // All push commands end with these 4 lines
            printWriter.println("@SP");
            printWriter.println("M=M+1");
            printWriter.println("A=M-1");
            printWriter.println("M=D");

        } else { // commandType.equals("pop")
            if (List.of("argument", "local", "this", "that").contains(arg1)) {
                printWriter.println("@" + REG_MAP.get(arg1));
                printWriter.println("D=M");
                printWriter.println("@" + arg2);
                printWriter.println("D=D+A");
                printWriter.println("@R13");
                printWriter.println("M=D");
                printWriter.println("@SP");     // These 3 lines are in common with other cases.
                printWriter.println("AM=M-1");  // but don't have good way to separate them
                printWriter.println("D=M");     //
                printWriter.println("@R13");
                printWriter.println("A=M");
                printWriter.println("M=D");
                return;
            }
            // Else case: pointer, temp, static
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");

            switch (arg1) {
                case "pointer" -> {
                    op = switch (arg2) {
                        case "0" -> "THIS";
                        case "1" -> "THAT";
                        case default -> throw new IllegalArgumentException("Unexpected pointer value");
                    };
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
                case "temp" -> {
                    op = "R" + (Integer.parseInt(arg2) + TMP_OFFSET);
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
                case "static" -> {
                    op = filePrefix + "." + arg2;
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
            }
        }
    }
}