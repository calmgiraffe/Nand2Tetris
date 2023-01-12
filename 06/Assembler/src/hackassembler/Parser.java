package hackassembler;

import java.io.*;
import java.util.*;

/*
Class to parse the input into instructions and instructions into field.
 */
public class Parser {
    // Static variables
    private static final Set<Character> C_CHARS = new HashSet<>(){
        {
            addAll(List.of('0', '1', '-', '+', 'A', 'D', 'M', '!', '&', '|', 'J',
                    'G', 'T', 'E', 'Q', 'L', 'T', 'N', 'M', 'P', '=', ';'));
        }
    };
    private static final Set<Character> SYMBOL_CHARS = new HashSet<>(){
        {
            addAll(List.of('_', '.', '$', ':'));
        }
    };
    private static final Set<String> PREDEFINED = new HashSet<>(){
        {
            addAll(List.of(new String[]{"SP", "LCL", "ARG", "THIS", "THAT", "SCREEN", "KBD"}));
        }
    };

    // Instance variables
    private BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private final String source;
    private final Map<String, Integer> symbolTable = new HashMap<>();

    private String currInstruct;
    private char instructType; // 'C' = C instruction, 'A' = A instruction, 'L' = label
    private String symbol; // null if instructType = C
    private String CString;
    private String byteInstruct;

    // Constructor
    public Parser(String source) throws IOException {
        // Make the output file name from the source file prefix & initialize BufferedReader
        this.source = source;
        this.bufferedReader = new BufferedReader(new FileReader(source));

        // Initialize PrintReader
        String output = source.substring(0, source.length() - 4) + ".hack";
        this.printWriter = new PrintWriter(new BufferedWriter(new FileWriter(output)));

        // Add R0 through R15 in Map
        for (int i = 0; i <= 15; i += 1) {
            symbolTable.put("R" + i, i);
            PREDEFINED.add("R" + i);
        }
        // Add other special symbol in Map
        symbolTable.put("SP", 0);
        symbolTable.put("LCL", 1);
        symbolTable.put("ARG", 2);
        symbolTable.put("THIS", 3);
        symbolTable.put("THAT", 4);
        symbolTable.put("SCREEN", 16384);
        symbolTable.put("KBD", 24576);
    }

    /*
     * First pass of the assembler, where the symbol table is generated.
     * Initially advance the parser to the first valid line.
     * If no more valid lines to parse, currInstruction = null
     * Todo: this can be made private
     */
    private void makeSymbolTable() throws IOException {
        int lineNum = 1;

        advance();
        while (currInstruct != null) {
            if (instructType == 'L') {
                if (PREDEFINED.contains(symbol)) {
                    throw new IllegalArgumentException("Symbol cannot be a predefined word");
                }
                symbolTable.put(symbol, lineNum - 1);
            } else {
                lineNum += 1;
            }
            advance();
        }
    }

    /*
     * Does two passes: one to make symbol table, another to translate to binary.
     */
    public void assemble() throws IOException {
        makeSymbolTable();

        // Reinitialize BufferedReader for second pass of input file
        FileReader reader = new FileReader(source);
        this.bufferedReader = new BufferedReader(reader);
        advance();

        int variable = 16;
        String dest, comp, jmp;
        while (currInstruct != null) {
            if (instructType == 'A') {

                if (symbolTable.containsKey(symbol)) { // Predefined symbols & label symbols
                    byteInstruct = Code.generateAInstruct(symbolTable.get(symbol));

                } else if (symbol.matches("[0-9]+")) { // Integers being assigned to A reg
                    byteInstruct = Code.generateAInstruct(Integer.parseInt(symbol));

                } else { // New variable, starts from 16 and increases by one for each new
                    byteInstruct = Code.generateAInstruct(variable);
                    symbolTable.put(symbol, variable);
                    variable += 1;
                }
                printWriter.println(byteInstruct);
            } else if (instructType == 'C') {
                dest = dest(CString);
                comp = comp(CString);
                jmp = jump(CString);
                byteInstruct = Code.generateCInstruct(dest, comp, jmp);
                printWriter.println(byteInstruct);
            }
            // System.out.println(byteInstruction);
            advance();
        }
        bufferedReader.close();
        printWriter.close();
    }

    /*
     * Skips through lines of input file until a valid instruction is found, trims leading and
     * trailing whitespace, and makes it the current instruction. Skips over non-instructions
     * like comments, i.e., //. If there are no more valid lines to parse, currInstruct is null.
     */
    private void advance() throws IOException {
        while (true) {
            currInstruct = bufferedReader.readLine();
            if (currInstruct == null) { // break if EOF
                break;
            }
            currInstruct = currInstruct.trim();
            // If currInstruction is not empty && does not start with //, is A or C instruct
            if (!currInstruct.isEmpty() && !currInstruct.startsWith("//")) {
                switch (currInstruct.charAt(0)) {
                    case '@' -> instructType = 'A';
                    case '(' -> instructType = 'L';
                    default -> instructType = 'C';
                }
                updateSymbol();
                updateCString();
                break;
            }
        }
    }

    /*
     * Updates this.symbol to the symbolic portion of currInstruct.
     * For example, if currInstruct = "@123", symbol is set to "123".
     * If currInstruct = "(END)", symbol is set to "END".
     * If this.instructType = 'C', symbol is set to null.
     */
    private void updateSymbol() {
        StringBuilder str = new StringBuilder();
        int len, i;
        char currChar;
        boolean isSymbol = false;

        if (instructType == 'C') {
            symbol = null;
            return;
        }
        len = currInstruct.length();
        for (i = 0; i < len; i += 1) {
            currChar = currInstruct.charAt(i);
            if (Character.isLetterOrDigit(currChar) || SYMBOL_CHARS.contains(currChar)) {
                str.append(currChar);
            } else if (currChar == '/' || currChar == ' ') { // start of comment or space
                break;
            } else if (currChar == '(' || currChar == ')' || currChar == '@') {
                continue;
            } else {
                throw new IllegalArgumentException("Invalid character for symbol");
            }
        }
        symbol = str.toString();

        // checking if this.symbol is a constant like "123" or a symbol
        len = symbol.length();
        for (i = 0; i < len; i += 1) {
            currChar = symbol.charAt(i);
            if ((SYMBOL_CHARS.contains(currChar))) {
                isSymbol = true;
            }
        }
        if (isSymbol && Character.isDigit(symbol.charAt(0))) {
            throw new IllegalArgumentException("Symbol cannot begin with digit");
        }
    }

    /*
     * Sets CString to the substring that makes up valid instruction components.
     * Only appends CChars to intermediate string. Thus, there will be no whitespace in str.
     * If instructType != 'C', CString set to null.
     */
    private void updateCString() {
        StringBuilder str = new StringBuilder();
        int len;

        if (instructType != 'C') {
            CString = null;
            return;
        }
        len = currInstruct.length();
        for (int i = 0; i < len; i += 1) {
            char currChar = currInstruct.charAt(i);
            if (C_CHARS.contains(currChar)) {
                str.append(currChar);
            } else if (currChar == '/') { // start of comment or space
                break;
            }
        }
        CString = str.toString();
    }

    /*
     * Returns the symbolic dest part of the current C_INSTRUCTION.
     * If no symbolic dest part, returns null.
     * Recall: dest = comp; jmp
     */
    private static String dest(String CInstruct) {
        String dest = null;

        // If substring has =, then part before = is dest
        if (CInstruct.contains("=")) {
            dest = CInstruct.substring(0, CInstruct.indexOf('='));
        }
        return dest;
    }

    /*
     * Returns the symbolic comp part of the current C_INSTRUCTION.
     * If no symbolic comp part, returns null.
     * Recall: dest = comp; jmp
     */
    private static String comp(String CInstruct) {
        String comp = null;

        // Determine comp based off whether ; and = exist
        if (CInstruct.contains("=") && CInstruct.contains(";")) { // M = 1; JMP
            comp = CInstruct.substring(CInstruct.indexOf('='), CInstruct.indexOf(';'));

        } else if (!CInstruct.contains("=") && CInstruct.contains(";")) { // ex: D; JMP
            comp = CInstruct.substring(0, CInstruct.indexOf(';'));

        } else if (CInstruct.contains("=") && !CInstruct.contains(";")) {
            comp = CInstruct.substring(CInstruct.indexOf('=') + 1);
        }
        return comp;
    }

    /*
     * Returns the symbolic jmp part of the current C_INSTRUCTION;
     * if no symbolic jmp part, returns null.
     * Recall: dest = comp; jmp
     */
    private static String jump(String instruction) {
        String jmp = null;

        // All instruction with jmp have ;
        if (instruction.contains(";")) {
            jmp = instruction.substring(instruction.indexOf(';') + 1);
        }
        return jmp;
    }
}