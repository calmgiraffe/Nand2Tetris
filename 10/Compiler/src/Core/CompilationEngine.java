package Core;

import java.io.*;
import java.util.Set;

import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private static String INDENT = "    ";
    private static final Set<String> PRIMITIVE_TYPES = Set.of("int","char","boolean");
    private Tokenizer tk;
    private PrintWriter writer;

    /* Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + ".xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compile() throws IOException {
        compileClass(1);
        tk.close();
        writer.close();
    }

    /* Method for compiling class
    example: class Main {...} */
    public void compileClass(int indentLevel) throws IOException {
        /* "class" className "{" classVarDec* subroutineDec* ")" */
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<class>");

        // "class" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("class")) { throwRuntimeException("'class'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // className identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // "{" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileClassVarDec(indentLevel + 1); // classVarDec*
        compileSubroutineDec(indentLevel + 1); // subroutineDec*

        // "}" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</class>");
    }

    /* Method for compiling static or non-static variables of object
    example: field int x, y, z */
    private void compileClassVarDec(int indentLevel) throws IOException {
        /* ('static' | 'field') type varName (',' varName)* ';' */
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not 'static' or 'field'
        token = tk.getCurrToken();
        if (!(token.equals("static") || token.equals("field"))) {
            return;
        }
        writer.println(headerIndent + "<classVarDec>"); // Header
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // Primitive type or className identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVE_TYPES.contains(token)) {
            if (type != identifier) {
                throwRuntimeException("type", keyword + ", " + identifier, token, type);
            }
            writer.println(indent + "<identifier> " + token + " </identifier>");
        } else {
            writer.println(indent + "<keyword> " + token + " </keyword>");
        }
        tk.advance();

        // At least one varName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // Zero or more (',' varName)
        token = tk.getCurrToken(); type = tk.getCurrType();
        while (token.equals(",")) {
            // token has to equal ',' -> print it
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            writer.println(indent + "<identifier> " + token + " </identifier>");

            // Prepare next token
            tk.advance();
            token = tk.getCurrToken(); type = tk.getCurrType();
        }
        // Terminating semicolon
        if (!token.equals(";")) { throwRuntimeException(";", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</classVarDec>");

        compileClassVarDec(indentLevel);
    }


    private void compileSubroutineDec(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not one of the three
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!(token.equals("constructor") || token.equals("function") || token.equals("method"))) { // 0 or more subroutineDec
            return;
        }
        writer.println("<subroutineDec>");



        writer.println("</subroutineDec>");
    }

    private void compileParameterList() {}

    private void compileSubroutineBody() {}

    private void throwRuntimeException(String expected, String expectedType, String actual, TokenType actualType) {
        String expectedStr = expected + " " + expectedType;
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expectedStr  + " but found " + actualStr);
    }

    private void throwRuntimeException(String expected, TokenType expectedType, String actual, TokenType actualType) {
        String expectedStr = expected + " " + expectedType;
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expectedStr  + " but found " + actualStr);
    }
}
