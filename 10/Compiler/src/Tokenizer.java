import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;

import java.util.Set;

public class Tokenizer {
    public enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }
    private static class Pair {
        String token;
        TokenType type;

        Pair(String token, TokenType type) {
            this.token = token;
            this.type = type;
        }
        String getToken() {
            return token;
        }
        TokenType getType() {
            return type;
        }
    }
    private static final int EOF = -1;
    private static final int CARRIAGE_RETURN = 0x0d;
    private static final Set<String> KEYWORDS = Set.of(
            "class","method","function","constructor","int","boolean","char","void", "var","static",
            "field","let","do","if","else","while","return","true","false","null","this");
    private static final Set<Character> SYMBOLS = Set.of(
            '{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~');

    private boolean hasMoreTokens = true;
    private String parentDirectory;
    private Pair currPair;
    private BufferedReader bufferedReader;
    private ArrayDeque<Pair> queue = new ArrayDeque<>();
    private ArrayDeque<String> files = new ArrayDeque<>();

    public Tokenizer(String source) throws FileNotFoundException {
        if (source.endsWith(".jack")) {
            bufferedReader = new BufferedReader(new FileReader("./" + source));
        }
    }

    /* Returns true if there are more tokens in the input */
    public boolean hasMoreTokens() {
        return !queue.isEmpty() || hasMoreTokens;
    }

    /* Gets the next token from the input, and makes it the current token.
    * Should ignore whitespace and comments
    * If encountered a symbol (single char), should consider this as one token
    * If encountered //, should ignore everything until after newline
    * If encountered /*, should ignore everything between start and terminating * and /
    * Note: Handling /* will also handle the case of /**
    */
    public void advance() throws IOException, InterruptedException {
        /* At least one token in the queue, remove the first one */
        if (!queue.isEmpty()) {
            currPair = queue.removeFirst();
            return;
        }
        int curr, next;
        boolean lineComment = false;
        boolean blockComment = false;
        boolean strConstant = false;
        StringBuilder buffer = new StringBuilder();

        /* No more tokens. Need to add more to queue */
        while (hasMoreTokens) {
            curr = bufferedReader.read();
            char tmp = (char) curr;

            /* Terminating condition */
            if (curr == EOF) {
                hasMoreTokens = false;
            }
            else if (lineComment) {
                if (curr == CARRIAGE_RETURN) {
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
                    queue.addLast(new Pair(buffer.toString(), TokenType.STRING_CONST));
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
                        hasMoreTokens = false;
                    } else if (next == '/') { // start of line comment
                        lineComment = true;
                    } else if (next == '*') { // start of block comment
                        blockComment = true;
                    } else {                  // '/' is thus a symbol
                        queue.addLast(new Pair(Character.toString(curr), TokenType.SYMBOL));
                        if (!Character.isWhitespace(next)) {
                            buffer.append((char) next);
                        }
                    }
                }
                else if (SYMBOLS.contains((char) curr)) { // symbols other than '/'
                    addToQueue(buffer);
                    queue.addLast(new Pair(Character.toString(curr), TokenType.SYMBOL));
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
        currPair = queue.removeFirst();
    }

    public void addToQueue(StringBuilder buffer) {
        if (!buffer.isEmpty()) {
            String token = buffer.toString();
            TokenType type;
            if (token.matches("\\d+")) {
                type = TokenType.INT_CONST;
            } else if (KEYWORDS.contains(token)) {
                type = TokenType.KEYWORD;
            } else {
                type = TokenType.IDENTIFIER;
            }
            queue.addLast(new Pair(token, type));
            buffer.delete(0, buffer.length());
        }
    }

    /* Returns the current token as a string */
    public String getCurrToken() {
        return currPair.getToken();
    }

    /* Returns the type of the current token as a constant of TokenType */
    public TokenType getTokenType() {
        return currPair.getType();
    }
}