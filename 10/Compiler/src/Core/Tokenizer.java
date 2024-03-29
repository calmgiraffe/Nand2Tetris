package Core;

import java.io.*;
import java.util.ArrayDeque;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tokenizer {
    public enum TokenType {
        keyword, symbol, identifier, integerConstant, stringConstant
    }
    private record Pair(String token, TokenType type) {
    }
    private static final int EOF = -1;
    private static final int NEWLINE = 0x0a;
    private static final int CARRIAGE_RETURN = 0x0d;
    public static final Set<String> KEYWORDS = Set.of(
            "class","method","function","constructor","int","boolean","char","void", "var","static",
            "field","let","do","if","else","while","return","true","false","null","this");
    public static final Map<String, String> XML_EXCEP = new HashMap<>() {{
        put("<", "&lt;");
        put(">", "&gt;");
        put("&", "&amp;");
        put("\"", "&quot;");
    }};
    private static final Set<Character> SYMBOLS = Set.of(
            '{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~');

    private boolean dataIsRemaining = true;
    private BufferedReader bufferedReader;
    private String filePrefix;
    private final ArrayDeque<Pair> queue = new ArrayDeque<>();

    public Tokenizer(String source) throws IOException {
        if (source.endsWith(".jack")) {
            filePrefix = source.substring(0, source.length() - 5);
            bufferedReader = new BufferedReader(new FileReader(source));
            advance(); // initially point to first token, if it exists
        }
    }

    /** Output an XML file with the tokens and their type. Used for testing correctness of tokenizer. */
    public void printToXML() throws IOException {
        String outputXMLFile = filePrefix + "T_test.xml";
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputXMLFile)));

        writer.println("<tokens>");
        while (hasMoreTokens()) {
            writer.print("<" + getCurrType() + "> ");
            if (XML_EXCEP.containsKey(getCurrToken())) {
                writer.print(XML_EXCEP.get(getCurrToken()));
            }
            else {
                writer.print(getCurrToken());
            }
            writer.println(" </" + getCurrType() + ">");
            advance();
        }
        writer.println("</tokens>");
        writer.close();
    }

    /** Frees all resources used by Core.Tokenizer */
    public void close() throws IOException {
        bufferedReader.close();
    }

    /** Returns true if there are more tokens in the input */
    public boolean hasMoreTokens() {
        // queue is not empty OR dataIsRemaining = true
        return !queue.isEmpty() || dataIsRemaining;
    }

    /** Gets the next token from the input, and makes it the current token.
    * Should ignore whitespace and comments
    * If encountered a symbol (single char), should consider this as one token
    * If encountered //, should ignore everything until after newline
    * If encountered /*, should ignore everything between start and terminating * and /
    * Note: Handling /* will also handle the case of /**
    */
    public void advance() throws IOException {
        queue.poll(); // initially remove the head of the queue
        if (!queue.isEmpty()) { // at least one token in the queue, remove the first one
            return;
        }
        /* If the queue is empty, need to read file and get more tokens */
        int curr, next;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean strConstant = false;
        StringBuilder buffer = new StringBuilder();

        while (dataIsRemaining) { // loop breaks if EOF or string constant found
            curr = bufferedReader.read();
            // char tmp = (char) curr;

            /* Terminating condition */
            if (curr == EOF) {
                dataIsRemaining = false;
            }
            else if (lineComment) {
                if (curr == CARRIAGE_RETURN || curr == NEWLINE) {
                    if(curr == CARRIAGE_RETURN){
                        bufferedReader.read();
                    }
                    lineComment = false;
                }
            }
            else if (blockComment) {
                if (curr == '*') {
                    next = (char) bufferedReader.read();
                    if (next == '/') {
                        blockComment = false;
                    }
                }
            }
            else if (strConstant) {
                if (curr == '"') {
                    queue.addLast(new Pair(buffer.toString(), TokenType.stringConstant));
                    break;
                }
                buffer.append((char) curr);
            }
            else { // none of the above
                if (Character.isWhitespace(curr)) {
                    addToQueue(buffer);
                }
                else if (curr == '/') {
                    /* '/' has to be a symbol or start of comment, so add current buffer to queue */
                    addToQueue(buffer);
                    next = bufferedReader.read();
                    if (next == EOF) {
                        dataIsRemaining = false;
                    } else if (next == '/') { // start of line comment
                        lineComment = true;
                    } else if (next == '*') { // start of block comment
                        blockComment = true;
                    } else {                  // '/' is thus a symbol
                        queue.addLast(new Pair(Character.toString(curr), TokenType.symbol));
                        if (!Character.isWhitespace(next)) {
                            buffer.append((char) next);
                        }
                    }
                }
                else if (SYMBOLS.contains((char) curr)) { // symbols other than '/'
                    addToQueue(buffer);
                    queue.addLast(new Pair(Character.toString(curr), TokenType.symbol));
                }
                else if (curr == '"') {
                    addToQueue(buffer);
                    strConstant = true;
                }
                else { // curr is not whitespace, a symbol, quotation mark, or '/'
                    buffer.append((char) curr);
                }
            }
        }
    }

    /* Abstraction to handle the current buffer (even if buffer is empty)
    * Doesn't do anything if buffer is empty, but if it's not, determines whether the string in the
    * buffer is an INT_CONST, KEYWORD, or IDENTIFIER */
    private void addToQueue(StringBuilder buffer) {
        if (!buffer.isEmpty()) {
            String token = buffer.toString();
            TokenType type;
            if (token.matches("\\d+")) {
                type = TokenType.integerConstant;
            } else if (KEYWORDS.contains(token)) {
                type = TokenType.keyword;
            } else {
                type = TokenType.identifier;
            }
            queue.addLast(new Pair(token, type));
            buffer.delete(0, buffer.length());
        }
    }

    /** Returns the current token as a string */
    public String getCurrToken() {
        return queue.peek().token();
    }

    /** Returns the type of the current token as a constant of TokenType */
    public TokenType getCurrType() {
        return queue.peek().type();
    }
}