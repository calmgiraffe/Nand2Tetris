package hackassembler;

import java.io.*;
import java.util.*;

/*
Class to parse the input into instructions and instructions into field.
 */
public class Parser {
    /* Static variables */
    public static final String CINSTRUCT = "C_INSTRUCTION";
    public static final String AINSTRUCT = "A_INSTRUCTION";
    public static final String LABEL = "LABEL";
    public static final Set<Character> validCChars = new HashSet<>();

    /* Instance variables */
    BufferedReader bufferedReader;
    final PrintWriter printWriter;
    final String source;
    String currInstruction;
    String byteInstruction;
    final Map<String, Integer> symbolTable = new HashMap<>();
    final Set<String> predefined = new HashSet<>();

    /* Constructor */
    public Parser(String source) throws IOException {
        // Get the name of the output file
        this.source = source;
        String output = source.substring(0, source.length() - 4) + ".hack";

        // Initialize BufferedReader
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);
        advance();

        // Initialize PrintReader
        FileWriter fileWriter = new FileWriter(output);
        this.printWriter = new PrintWriter(new BufferedWriter(fileWriter));

        // Add R0 through R15 in Map
        for (int i = 0; i <= 15; i += 1) {
            symbolTable.put("R" + i, i);
            predefined.add("R" + i);
        }
        // Add other special symbol in Map
        symbolTable.put("SP", 0);
        symbolTable.put("LCL", 1);
        symbolTable.put("ARG", 2);
        symbolTable.put("THIS", 3);
        symbolTable.put("THAT", 4);
        symbolTable.put("SCREEN", 16384);
        symbolTable.put("KBD", 24576);

        // Add predefined symbols to Set
        predefined.addAll(List.of(new String[]{"SP", "LCL", "ARG", "THIS", "THAT", "SCREEN", "KBD"}));

        // Add valid instruction chars to Set
        validCChars.addAll(List.of('0', '1', '-', '+', 'A', 'D', 'M', '!', '&', '|'));
        validCChars.addAll(List.of('J', 'G', 'T', 'E', 'Q', 'L', 'T', 'N', 'M', 'P'));
    }

    /**
     * First pass of the assembler, where the symbol table is generated.
     */
    public void makeSymbolTable() throws IOException {
        int lineNum = 1;
        while (this.hasMoreLines()) {
            String type = type(currInstruction);

            if (type.equals(LABEL)) {
                String symbol = symbol(currInstruction);
                // Throw error if label tag is a predefined special string
                assert !predefined.contains(symbol);
                symbolTable.put(symbol, lineNum - 1);
            } else {
                lineNum += 1;
            }
            advance();
        }
    }

    /**
     * Second and final pass of the assembler, where instructions are parsed.
     */
    public void assemble() throws IOException {
        // Reinitialize BufferedReader
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);
        advance();

        int variable = 16;
        while (this.hasMoreLines()) {
            if (type(currInstruction).equals(AINSTRUCT)) {

                String symbol = symbol(currInstruction);
                if (symbolTable.containsKey(symbol)) {
                    // Predefined symbols & label symbols
                    byteInstruction = Code.generateAInstruct(symbolTable.get(symbol));

                } else if (symbol.matches("[0-9]+")) {
                    // Integers being assigned to A reg
                    byteInstruction = Code.generateAInstruct(Integer.parseInt(symbol));

                } else {
                    // Variable name
                    byteInstruction = Code.generateAInstruct(variable);
                    symbolTable.put(symbol, variable);
                    variable += 1;
                }
                write(byteInstruction);
            } else if (type(currInstruction).equals(CINSTRUCT)) {
                String dest = dest(currInstruction);
                String comp = comp(currInstruction);
                String jmp = jump(currInstruction);
                // System.out.println(dest + " " + comp + " " + jmp);
                byteInstruction = Code.generateCInstruct(dest, comp, jmp);
                write(byteInstruction);
            }
            // System.out.println(byteInstruction);
            advance();
        }
        bufferedReader.close();
        printWriter.close();
    }

    /**
     * Write a line to the target .hack file.
     */
    public void write(String instruction) {
        printWriter.println(instruction);
    }

    /**
     * Returns true if there are more lines to parse, else return false.
     * When parser gets to EOF, currInstruction should be null
     */
    boolean hasMoreLines() {
        return currInstruction != null;
    }

    /**
     * Skips through lines of input file until a valid instruction is found, trims whitespace,
     * and makes it the current instruction. Skips over non-instructions like comments.
     */
    void advance() throws IOException {
        while (true) {
            currInstruction = bufferedReader.readLine();
            // Immediately break if EOF
            if (currInstruction == null) {
                break;
            }
            currInstruction = currInstruction.trim();
            // If currInstruction is not empty && does not start with //, i.e., is A or C instruct
            if (!currInstruction.isEmpty() && !currInstruction.startsWith("//")) {
                break;
            }
        }
    }

    /**
     * Returns the type of the current instruction.
     * @return AINSTRUCT, CINSTRUCT, LABEL
     */
    public static String type(String instruction) {
        assert !instruction.startsWith("//");

        if (instruction.startsWith("@")) {
            return AINSTRUCT;
        } else if (instruction.startsWith("(")) {
            return LABEL;
        } else {
            return CINSTRUCT;
        }
    }

    /**
     * Assuming the instruction is (xxx) or @xxx, returns the portion of the String that is xxx.
     */
    public static String symbol(String instruction) {
        assert !type(instruction).equals(CINSTRUCT);

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < instruction.length(); i += 1) {
            char currChar = instruction.charAt(i);

            if (Character.isLetterOrDigit(currChar)) {
                // If alphanumeric, append to StrBuilder
                strBuilder.append(currChar);
            } else if (currChar == '/') {
                // Else if start of comment, break
                break;
            }
        }
        return strBuilder.toString();
    }

    /**
     * Assuming a substring of a C instruction,
     * returns the portion of the substring that makes up valid instruction components
     */
    private static String cSubstring(String instruction) {
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < instruction.length(); i += 1) {
            char currChar = instruction.charAt(i);

            if (validCChars.contains(currChar)) {
                // If in approved char set, append to StrBuilder
                strBuilder.append(currChar);
            }
        }
        return strBuilder.toString();
    }

    /**
     * Returns the symbolic dest part of the current C_INSTRUCTION
     * Recall: dest = comp; jmp
     * @return substring that represents comp
     */
    public static String dest(String instruction) {
        assert type(instruction).equals(CINSTRUCT);

        String dest = null;
        // Trim comments at end, if they exist
        if (instruction.contains("//")) {
            instruction = instruction.substring(0, instruction.indexOf('/'));
        }
        // If substring has =, then part before = is dest
        if (instruction.contains("=")) {
            dest = cSubstring(instruction.substring(0, instruction.indexOf('=')));
        }
        return dest;
    }

    /**
     * Returns the symbolic comp part of the current C_INSTRUCTION;
     * Recall: dest = comp; jmp
     * @return substring that represents dest
     */
    public static String comp(String in) {
        assert type(in).equals(CINSTRUCT);

        String comp = null;
        // Trim comments at end, if they exist
        if (in.contains("//")) {
            in = in.substring(0, in.indexOf('/'));
        }
        // Determine comp based off whether ; and = exist
        if (in.contains("=") && in.contains(";")) {
            comp = cSubstring(in.substring(in.indexOf('='), in.indexOf(';')));
        } else if (!in.contains("=") && in.contains(";")) {
            comp = cSubstring(in.substring(0, in.indexOf(';')));
        } else if (in.contains("=") && !in.contains(";")) {
            comp = cSubstring(in.substring(in.indexOf('=')));
        }
        return comp;
    }

    /**
     * Returns the symbolic jmp part of the current C_INSTRUCTION;
     * Recall: dest = comp; jmp
     * @return substring that represents jmp
     */
    public static String jump(String instruction) {
        assert type(instruction).equals(CINSTRUCT);

        String jmp = null;
        // Trim comments at end, if they exist
        if (instruction.contains("//")) {
            instruction = instruction.substring(0, instruction.indexOf('/'));
        }
        // All instruction with jmp have ;
        if (instruction.contains(";")) {
            jmp = cSubstring(instruction.substring(instruction.indexOf(';')));
        }
        return jmp;
    }
}