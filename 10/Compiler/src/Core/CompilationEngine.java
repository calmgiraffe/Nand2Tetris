package Core;

import java.io.*;
import static Core.Tokenizer.TokenType.identifier;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private Tokenizer tk;
    private PrintWriter writer;

    /* Build the list of output files */
    CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + ".xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compileClass(int indentLevel) throws IOException {
        /* Should have:
        "class" keyword
        className identifier
        '{' symbol
        0 or more classVarDec
        0 or more subroutineDec
        '}' symbol
        */
        String token;
        Tokenizer.TokenType type;
        String indent = " ".repeat(indentLevel);

        writer.println("<class>");

        tk.advance();
        token = tk.getCurrToken();
        if (token.equals("class")) {
            writer.println(indent + "<keyword> " + token + " </keyword>");
        } else {
            throw new RuntimeException("Expected 'class' keyword but found '" + token + "'");
        }

        tk.advance();
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type.equals(identifier)) {
            writer.println(indent + "<keyword> " + token + " </keyword>");
        } else {
            throw new RuntimeException("Expected className identifier but found '" + token + "'");
        }
        writer.println("</class>");
    }

    private void compileClassVarDec() {}

    private void compileSubroutine() {}

    private void compileParameterList() {}

    private void compileSubroutineBody() {}
}
