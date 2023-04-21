package Core;

import java.io.*;
import java.util.Set;

import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private static final String INDENT = "    ";
    private static final Set<String> PRIMITIVE_TYPES = Set.of("int","char","boolean");
    private static final Set<String> STATEMENT_TYPES = Set.of("let","if","while","do","return");
    private final Tokenizer tk;
    private final PrintWriter writer;

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

    /* Method for compiling class, ex., class Main {...} */
    public void compileClass(int indentLevel) throws IOException {
        // "class" className "{" classVarDec* subroutineDec* ")"
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

    /* Method for compiling static or non-static variables of object.
    ex., field int x, y, z */
    private void compileClassVarDec(int indentLevel) throws IOException {
        // ('static' | 'field') type varName (',' varName)* ';'
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

        // Primitive type or className (type)
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
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken(); type = tk.getCurrType();
        }
        // Terminating semicolon
        if (!token.equals(";")) { throwRuntimeException("';'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</classVarDec>");

        // 0 or more classVarDec in class -> recursive call
        compileClassVarDec(indentLevel);
    }

    /* Method for compiling object routines like constructor, static/non-static methods
    ex., method void draw(int x, int y) */
    private void compileSubroutineDec(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not one of the three
        token = tk.getCurrToken();
        if (!(token.equals("constructor") || token.equals("function") || token.equals("method"))) {
            return;
        }
        writer.println(headerIndent + "<subroutineDec>");
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // void, primitive type, or identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVE_TYPES.contains(token)) {
            if (type != identifier) {
                throwRuntimeException("'void' | type", keyword + ", " + identifier, token, type);
            }
            writer.println(indent + "<identifier> " + token + " </identifier>");
        } else {
            writer.println(indent + "<keyword> " + token + " </keyword>");
        }
        tk.advance();

        // subroutineName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("subroutineName", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // "(" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("(")) { throwRuntimeException("'('", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // 0 or 1 parameterList
        compileParameterList(indentLevel + 1);

        // ")" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals(")")) { throwRuntimeException("')'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // subroutineBody (contains at least "{}")
        compileSubroutineBody(indentLevel + 1);

        // Footer
        writer.println(headerIndent + "</subroutineDec>");

        // 0 or more subroutineDec in class -> recursive call
        compileSubroutineDec(indentLevel);
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec
    ex., int x, int y, boolean flag */
    private void compileParameterList(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not type -> 0 parameterList
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVE_TYPES.contains(token) && type != identifier) {
            return;
        }
        // Header & type
        writer.println(headerIndent + "<parameterList>");
        if (type == identifier) {
            writer.println(indent + "<identifier> " + token + " </identifier>");
        } else {
            writer.println(indent + "<keyword> " + token + " </keyword>");
        }
        tk.advance();

        // varName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // 0 or more (',' type varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            // token has to equal ',' -> print it
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();

            // primitive or className (type)
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

            // varName identifier must follow type
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            writer.println(indent + "<identifier> " + token + " </identifier>");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }
        // Footer
        writer.println(headerIndent + "</parameterList>");
    }

    /* Method for compiling subroutine body, i.e., { varDec* statement* } */
    private void compileSubroutineBody(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<subroutineBody>");

        // "{" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileVarDec(indentLevel + 1); // 0 or more varDec
        compileStatements(indentLevel + 1); // statements (0 or more statement)

        // "}" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</subroutineBody>");
    }

    /* Method for compiling varDec, ex., {
    var char key;
    var boolean exit;
    ... } */
    private void compileVarDec(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not var
        token = tk.getCurrToken();
        if (!token.equals("var")) {
            return;
        }
        // Header & var
        writer.println(headerIndent + "<varDec>");
        writer.println(indent + "<var> " + token + " </var>");
        tk.advance();

        // Primitive type or className (type)
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

        // varName identifier
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
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken(); type = tk.getCurrType();
        }
        // Terminating semicolon
        if (!token.equals(";")) { throwRuntimeException("';'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</varDec>");

        // 0 or more varDec in subroutineDec -> recursive call
        compileVarDec(indentLevel);
    }

    /* Compile a let, if, while, do, or return statement */
    private void compileStatements(int indentLevel) throws IOException {
        String token = tk.getCurrToken();
        String headerIndent = INDENT.repeat(indentLevel - 1);

        // Header
        writer.println(headerIndent + "<statements>");

        // If token is one of the valid statement types
        // Implicit assumption that tokenizer advances to next valid token after each call
        while (STATEMENT_TYPES.contains(token)) {
            switch (token) {
                case "let" -> compileLet(indentLevel + 1);
                case "if" -> compileIf(indentLevel + 1);
                case "while" -> compileWhile(indentLevel + 1);
                case "do" -> compileDo(indentLevel + 1);
                case "return" -> compileReturn(indentLevel + 1);
            }
        }
        // Footer
        writer.println(headerIndent + "</statements>");
    }

    private void compileLet(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<letStatement>");

        // "let" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("let")) { throwRuntimeException("'let'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // varName identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // 0 or 1 ('[' expression ']')
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("=")) {
            if (!token.equals("[")) {
                throwRuntimeException("'[' or '='", symbol, token, type);
            }
            // '[' symbol
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();

            compileExpression(indentLevel + 1); // expression

            // ']' symbol
            token = tk.getCurrToken();
            if (!token.equals("]")) { throwRuntimeException("']'", symbol, token, type); }
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();

            // '=' symbol
            token = tk.getCurrToken();
            if (!token.equals("=")) { throwRuntimeException("'='", symbol, token, type); }
        }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileExpression(indentLevel + 1); // expression

        // terminating ';' symbol
        token = tk.getCurrToken();
        if (!token.equals(";")) { throwRuntimeException("';'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</letStatement>");
    }

    private void compileIf(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<ifStatement>");

        // "if" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("if")) { throwRuntimeException("'if'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // '(' symbol
        token = tk.getCurrToken();
        if (!token.equals("(")) { throwRuntimeException("'('", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileExpression(indentLevel + 1); // expression

        // ')' symbol
        token = tk.getCurrToken();
        if (!token.equals(")")) { throwRuntimeException("')'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // "{" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileStatements(indentLevel + 1); // statements (0 or more statement)

        // "}" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // ('else' '{' statements '}')?
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (token.equals("else")) {
            writer.println(indent + "<keyword> " + token + " </keyword>");
            tk.advance();

            // "{" symbol
            token = tk.getCurrToken();
            if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();

            compileStatements(indentLevel + 1); // statements (0 or more statement)

            // "}" symbol
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
            writer.println(indent + "<symbol> " + token + " </symbol>");
            tk.advance();
        }
        // Footer
        writer.println(headerIndent + "</ifStatement>");
    }

    private void compileWhile(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<whileStatement>");

        // "while" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("while")) { throwRuntimeException("'while'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // '(' symbol
        token = tk.getCurrToken();
        if (!token.equals("(")) { throwRuntimeException("'('", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileExpression(indentLevel + 1); // expression

        // ')' symbol
        token = tk.getCurrToken();
        if (!token.equals(")")) { throwRuntimeException("')'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // "{" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("{")) { throwRuntimeException("'{'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        compileStatements(indentLevel + 1); // statements (0 or more statement)

        // "}" symbol
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("}")) { throwRuntimeException("'}'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</whileStatement>");
    }

    private void compileDo(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<doStatement>");

        // "do" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("do")) { throwRuntimeException("'do'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // subroutineCall //

        // terminating ';' symbol
        token = tk.getCurrToken();
        if (!token.equals(";")) { throwRuntimeException("';'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</doStatement>");
    }

    private void compileReturn(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<returnStatement>");

        // "return" keyword
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("return")) { throwRuntimeException("'return'", keyword, token, type); }
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // expression

        // terminating ';' symbol
        token = tk.getCurrToken();
        if (!token.equals(";")) { throwRuntimeException("';'", symbol, token, type); }
        writer.println(indent + "<symbol> " + token + " </symbol>");
        tk.advance();

        // Footer
        writer.println(headerIndent + "</returnStatement>");
    }

    private void compileExpression(int indentLevel) {}

    private void throwRuntimeException(String expected, String expectedType, String actual, TokenType actualType) throws IOException {
        tk.close();
        writer.close();
        String expectedStr = expected + " " + expectedType;
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expectedStr  + " but found " + actualStr);
    }

    private void throwRuntimeException(String expected, TokenType expectedType, String actual, TokenType actualType) throws IOException {
        tk.close();
        writer.close();
        String expectedStr = expected + " " + expectedType;
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expectedStr  + " but found " + actualStr);
    }
}
