package vmtranslator;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;

/*
The Parser iterates through the input .vm file, analyzes each VM command,
and determines what assembly code to write to the output file.
*/
public class Parser {
    private static final HashMap<String, String> REG_MAP = new HashMap<>(){
        {
            put("local", "LCL");
            put("argument", "ARG");
            put("this", "THIS");
            put("that", "THAT");
        }
    };
    private enum Command {
        PUSH_POP,
        BRANCHING,
        FUNCTION,
        ARITHMETIC
    }
    private static final int TMP_OFFSET = 5;
    private static final boolean ENABLE_COMMENTS = true;

    private BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private Command commandType;
    private String currFile;
    private String currInstruct, currFunction, arg1, arg2, arg3;
    private final String parentDirectory;
    private int jumpNum = 0, callNum = 0;
    private final ArrayDeque<String> files = new ArrayDeque<>();

    public Parser(String source) throws IOException {
        String outputFileName;

        if (source.endsWith(".vm")) { // one .vm file -> one .asm file
            outputFileName = source.substring(0, source.length() - 3);
            files.add(source);

            // Initialize PrintWriter, writes lines to output file with println()
            parentDirectory = "./";
            printWriter = new PrintWriter(new BufferedWriter(
                    new FileWriter(outputFileName + ".asm")));

        } else { // -> multiple .vm files in a specified folder -> one .asm file
            outputFileName = source;
            File directoryPath = new File("./" + source);
            FilenameFilter VMFileFilter = (dir, name) -> {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".vm");
            };
            files.addAll(List.of(directoryPath.list(VMFileFilter)));

            parentDirectory = "./" + outputFileName + '/';
            printWriter = new PrintWriter(new BufferedWriter(
                    new FileWriter(parentDirectory + outputFileName + ".asm")));
        }
    }

    /*
    Advances the parser one line, setting currInstruction to the next valid instruction.
    This method skips over comments, i.e., lines that start with //
    If there are no more valid lines to parse, this.currInstruct = null.
    */
    private void advance() throws IOException {
        String[] words;

        while ((currInstruct = bufferedReader.readLine()) != null) {
            currInstruct = currInstruct.trim();
            /*
            If currInstruction is not empty && does not start with //, then currInstruction must be VM command.
            Split to list of words about whitespace, then set values of commandType, arg1, arg2.
            Note: assumes that the input VM file has no errors, i.e., only comments, valid commands, or blank lines
            */
            if (!currInstruct.isEmpty() && !currInstruct.startsWith("//")) {
                words = currInstruct.split("\\s+");

                if (List.of("push", "pop").contains(words[0])) {
                    commandType = Command.PUSH_POP;
                    arg1 = words[0];    // push or pop
                    arg2 = words[1];    // one of the stack segment names
                    arg3 = words[2];    // positive int
                } else if (List.of("label", "goto", "if-goto").contains(words[0])) {
                    commandType = Command.BRANCHING;
                    arg1 = words[0];    // label, goto, if-goto
                    arg2 = words[1];    // label name
                    arg3 = null;
                } else if (List.of("function", "call").contains(words[0])) {
                    commandType = Command.FUNCTION;
                    arg1 = words[0];    // function or call
                    arg2 = words[1];    // function f
                    arg3 = words[2];    // nArgs
                } else if (words[0].equals("return")) {
                    commandType = Command.FUNCTION;
                    arg1 = words[0];    // return
                    arg2 = null;
                    arg3 = null;
                } else {
                    commandType = Command.ARITHMETIC;
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
        printWriter.println("// Bootstrap code: SP = 256; call Sys.init");
        printWriter.println("@256");
        printWriter.println("D=A");
        printWriter.println("@SP");
        printWriter.println("M=D");
        writeFunction("call", "Sys.init", "0");
        printWriter.println("@Sys.init");
        printWriter.println("0;JMP");

        // Initialize new bufferReader for each .vm file, initially advance to first valid instruction,
        // translate corresponding args, goto next instruct, repeat until no more valid lines or EOF
        for (String file : files) {
            bufferedReader = new BufferedReader(new FileReader(parentDirectory + file));
            currFile = file.substring(0, file.length() - 3);

            advance();
            while (currInstruct != null) { // null if EOF
                if (ENABLE_COMMENTS) {
                    printWriter.println("// " + currInstruct);
                }
                switch (commandType) {
                    case PUSH_POP -> writePushPop(arg1, arg2, arg3);
                    case BRANCHING -> writeBranching(arg1, arg2);
                    case FUNCTION -> writeFunction(arg1, arg2, arg3);
                    case ARITHMETIC -> writeArithmetic(arg1);
                    default -> {
                        // Handle unexpected command type
                    }
                }
                advance();
            }
            bufferedReader.close();
        }
        printWriter.close();
    }

    /*
    Writes to the output file the asm code that implements the current arithmetic-logical command.
    Cases: [add, sub, and, or], [not, neg], [eq, gt, lt]
    */
    private void writeArithmetic(String command) {
        String op, jumpName;
        if (List.of("add", "sub", "and", "or").contains(command)) { // add, sub, and, or
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");
            printWriter.println("@SP");
            printWriter.println("A=M-1");
            op = switch (command) {
                case "add" -> "+";
                case "sub" -> "-";
                case "and" -> "&";
                case "or" -> "|";
                case default -> throw new IllegalArgumentException("Unexpected arithmetic op");
            };
            printWriter.println("M=M" + op + "D");

        } else if (List.of("not", "neg").contains(command)) { // not, neg
            printWriter.println("@SP");
            printWriter.println("A=M-1");
            op = switch (command) {
                case "not" -> "!";
                case "neg" -> "-";
                case default -> throw new IllegalArgumentException("Unexpected negation op");
            };
            printWriter.println("M=" + op + "M");

        } else if (List.of("eq", "gt", "lt").contains(command)) { // eq, gt, lt
            jumpName = "EQ_jump" + jumpNum;
            printWriter.println("@SP");
            printWriter.println("AM=M-1");
            printWriter.println("D=M");
            printWriter.println("A=A-1");
            printWriter.println("D=M-D");
            printWriter.println("M=-1");
            printWriter.println("@" + jumpName);
            op = switch (command) {
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
    private void writePushPop(String arg1, String arg2, String arg3) {
        String op;
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
                    op = currFile + "." + arg3;
                    printWriter.println("@" + op);
                    printWriter.println("D=M");
                }
                case "argument", "local", "this", "that" -> {
                    printWriter.println("@" + REG_MAP.get(arg2));
                    printWriter.println("D=M");
                    printWriter.println("@" + arg3);
                    printWriter.println("A=D+A");
                    printWriter.println("D=M");
                }
                case "LCL", "ARG", "THIS", "THAT" -> {  // push LCL, push ARG, push THIS, push THAT
                    printWriter.println("@" + arg2);
                    printWriter.println("D=M");
                }
                default -> { // some returnAddress
                    printWriter.println("@" + arg2);
                    printWriter.println("D=A");
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
            } else {    // Else case: pointer, temp, static
                printWriter.println("@SP");
                printWriter.println("AM=M-1");
                printWriter.println("D=M");
            }
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
                    op = currFile + "." + arg3;
                    printWriter.println("@" + op);
                    printWriter.println("M=D");
                }
            }
        }
    }

    /*
    Writes to the output file the asm code that implements the current branching command.
    arg1 command = [label, goto, if-goto]
    arg2 label = label name
    */
    private void writeBranching(String command, String label) {
        String symbol = String.format("%s$%s", currFunction, label);
        switch (command) {
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
    command = [call, function, return]
    */
    private void writeFunction(String command, String label, String nArgs) {
        String returnAddress = String.format("%s$ret.%d", currFunction, callNum);
        switch (command) {
            case "call" -> {
                // push returnAddress, LCL, ARG, THIS, THAT
                writePushPop("push", returnAddress, null);
                writePushPop("push", "LCL", null);
                writePushPop("push", "ARG", null);
                writePushPop("push", "THIS", null);
                writePushPop("push", "THAT", null);
                // ARG = SP - 5 - nArgs
                printWriter.println("@5");
                printWriter.println("D=A");
                printWriter.println("@" + nArgs);
                printWriter.println("D=D+A");
                printWriter.println("@SP");
                printWriter.println("D=M-D");
                printWriter.println("@ARG");
                printWriter.println("M=D");
                // LCL = SP
                printWriter.println("@SP");
                printWriter.println("D=M");
                printWriter.println("@LCL");
                printWriter.println("M=D");
                // goto f
                printWriter.println("@" + label);
                printWriter.println("0;JMP");
                // (returnAddress)
                printWriter.println("(" + returnAddress + ")");
                // Increment this.callNum
                this.callNum += 1;
            }
            case "function" -> {
                // generate a symbol filePrefix.functionName that labels the entry point of the function's code
                // At new function block, update currFunction for next potential function VM command
                this.currFunction = label;

                printWriter.println("(" + label + ")");
                // repeat nVar times for nVar local variables
                int nVars = Integer.parseInt(nArgs);
                for (int i = 0; i < nVars; i++) {
                    writePushPop("push", "constant", "0");
                }
            }
            case "return" -> {
                // frame = LCL
                printWriter.println("@LCL");
                printWriter.println("D=M");
                printWriter.println("@frame");
                printWriter.println("M=D");
                // retAddress = *(frame - 5)
                dereference("retAddress", "frame", 5);
                // *ARG = pop()
                printWriter.println("@SP");
                printWriter.println("AM=M-1");
                printWriter.println("D=M");
                printWriter.println("@ARG");
                printWriter.println("A=M");
                printWriter.println("M=D");
                // SP = ARG + 1
                printWriter.println("@ARG");
                printWriter.println("D=M+1");
                printWriter.println("@SP");
                printWriter.println("M=D");
                // THAT = *(frame - 1);
                // THIS = *(frame - 2);
                // ARG = *(frame - 3);
                // LCL = *(frame - 4)
                dereference("THAT", "frame", 1);
                dereference("THIS", "frame", 2);
                dereference("ARG", "frame", 3);
                dereference("LCL", "frame", 4);
                // goto retAddress
                printWriter.println("@retAddress");
                printWriter.println("A=M");
                printWriter.println("0;JMP");
            }
        }
    }

    /*
    Helper function for writeFunction(), prints the corresponding asm code for output = *(input + offset)
    For example, if output = retAddress, input = frame, offset = -5, the pseudo-assembly is retAddress = *(frame - 5)
    */
    private void dereference(String output, String input, int offset) {
        printWriter.println(String.format("// pseudo-assembly: %s = *(%s - %s)", output, input, offset));
        printWriter.println("@" + input);
        printWriter.println("D=M");
        printWriter.println("@" + offset);
        printWriter.println("A=D-A");
        printWriter.println("D=M");
        printWriter.println("@" + output);
        printWriter.println("M=D");
    }
}