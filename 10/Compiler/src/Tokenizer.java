import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;

import java.util.Set;

public class Tokenizer {
    public enum State {
        LINE_COMMENT, BLOCK_COMMENT, STRING_CONST, OTHER
    }
    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }
    public enum Keyword {
        CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID, VAR, STATIC, FIELD, LET,
        DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS
    }
    private static final Set<Character> SYMBOLS = Set.of('{', '}', '(', ')', '[', ']', '.', ',',
            ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~');

    private boolean hasMoreTokens = true;
    private String parentDirectory;
    private String currToken;
    private BufferedReader bufferedReader;
    private ArrayDeque<String> queue = new ArrayDeque<>();
    private ArrayDeque<String> files = new ArrayDeque<>();

    public Tokenizer(String source) throws FileNotFoundException {
        String outputFilename;

        if (source.endsWith(".jack")) {
            outputFilename = source.substring(0, source.length() - 5) + ".vm";
            bufferedReader = new BufferedReader(new FileReader("./" + source));
        }
    }

    /* Returns true if there are more tokens in the input */
    public boolean hasMoreTokens() {
        return hasMoreTokens;
    }

    /* Gets the next token from the input, and makes it the current token. */
    public void advance() throws IOException, InterruptedException {
        // should ignore whitespace and comments
        // If encountered a symbol (single char), should consider this as one token
        // If encountered //, should ignore everything until after newline
        // If encountered /*, should ignore everything between start and terminating */.
        // Note: Handling /* will also handle the case of /**

        /* At least one token in the queue, remove the first one */
        if (!queue.isEmpty()) {
            currToken = queue.removeFirst();
            return;
        }
        char curr, next;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean strConstant = false;
        StringBuilder buffer = new StringBuilder();

        /* No more tokens. Need to add more to queue */
        while (hasMoreTokens) {
            curr = (char) bufferedReader.read();

            /* Terminating condition */
            if (curr == -1) {
                hasMoreTokens = false;
                break;
            }
            if (lineComment) {
                if (curr == 0x0D) {
                    bufferedReader.read();
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
                    strConstant = false;
                    queue.addLast(buffer.toString());
                    break;
                }
                buffer.append(curr);
            }
            else {
                if (Character.isWhitespace(curr)) {
                    if (!buffer.isEmpty()) {
                        queue.addLast(buffer.toString());
                        break;
                    }
                } else if (curr == '/') {
                    // '/' has to be a symbol or start of comment, so add current buffer to queue
                    if (!buffer.isEmpty()) {
                        queue.addLast(buffer.toString());
                        buffer.delete(0, buffer.length());
                    }
                    next = (char) bufferedReader.read();
                    if (next == -1) {
                        hasMoreTokens = false;
                        break;
                    } else if (next == '/') { // start of line comment
                        lineComment = true;
                        continue;
                    } else if (next == '*') { // start of block comment
                        blockComment = true;
                        continue;
                    }
                    queue.addLast(Character.toString(curr));
                    if (!Character.isWhitespace(next)) {
                        buffer.append(next);
                    } else {
                        break;
                    }
                } else if (SYMBOLS.contains(curr)) { // symbols other than '/'
                    if (!buffer.isEmpty()) {
                        queue.addLast(buffer.toString());
                    }
                    queue.addLast(Character.toString(curr));
                    break;
                } else if (curr == '"') {
                    if (!buffer.isEmpty()) {
                        queue.addLast(buffer.toString());
                        break;
                    }
                    strConstant = true;
                } else { // curr is not whitespace, a symbol, quotation mark, or '/'
                    buffer.append(curr);
                }
            }
        }
        currToken = queue.removeFirst();
    }

    public String getCurrToken() {
        return currToken;
    }

    /* Returns the type of the current token as a constant */
    public TokenType tokenType() {
        return null;
    }

    public Keyword keyword() {
        return null;
    }

    public char symbol() {
        return 0;
    }

    public String identifier() {
        // a sequence of letters, digits, and underscore, not starting with a digit
        return null;
    }

    public int intVal() {
        // Return a decimal integer in the range 0..32767
        return 0;
    }

    public String stringVal() {
        // A sequence of characters, not including double quote or newline
        // ex. let sign = "negative"
        return null;
    }
}
