package Core;

import java.io.*;
import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private Tokenizer tk;
    private PrintWriter writer;

    /* Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + ".xml")));
    }

    public void compile() throws IOException {
        compileClass(1);
        tk.close();
        writer.close();
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compileClass(int indentLevel) throws IOException {
        /* "class" className "{" classVarDec* subroutineDec* ")" */
        String token;
        Tokenizer.TokenType type;
        String indent = "   ".repeat(indentLevel);

        writer.println("<class>");

        token = tk.getCurrToken(); type = tk.getCurrType(); // "class" keyword
        if (!token.equals("class")) { throwRuntimeException("'class'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance(); // className identifier

        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        token = tk.getCurrToken(); type = tk.getCurrType(); // "{" symbol
        if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        token = tk.getCurrToken(); type = tk.getCurrType();
        if (token.equals("static") || token.equals("field")) { // 0 or more classVarDec
            compileClassVarDec(indentLevel + 1);
            tk.advance();
        }
        if (token.equals("constructor") || token.equals("function") || token.equals("method")) { // 0 or more subroutineDec
            compileSubroutineDec(indentLevel + 1);
            tk.advance();
        }

        token = tk.getCurrToken(); type = tk.getCurrType(); // "}" symbol
        if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");

        writer.println("</class>");
    }

    private void throwRuntimeException(String expected, TokenType expectedType, String actual, TokenType actualType) {
        String expectedStr = expected + " " + expectedType;
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expectedStr  + " but found " + actualStr);
    }

    private void compileClassVarDec(int indentLevel) {
        writer.println("<classVarDec>");
        writer.println("</classVarDec>");
    }

    private void compileSubroutineDec(int indentLevel) {
        writer.println("<subroutineDec>");
        writer.println("</subroutineDec>");
    }

    private void compileParameterList() {}

    private void compileSubroutineBody() {}
}
