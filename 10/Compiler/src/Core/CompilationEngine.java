package Core;

import java.io.*;
import java.util.Set;

import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;
import static Core.Tokenizer.XML_EXCEP;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private static final String INDENT = "  ";
    private static final Set<String> PRIMITIVES = Set.of("int","char","boolean");
    private static final Set<String> STATEMENTS = Set.of("let","if","while","do","return");
    private static final Set<String> OP = Set.of("+","-","*","/","&","|","<",">","=");
    private static final Set<String> UNARY_OP = Set.of("-", "~");
    private static final Set<String> KEYWORD_CONST = Set.of("true","false","null","this");
    private final Tokenizer tk;
    private final PrintWriter writer;

    /* Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + "_ce.xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compile() throws IOException {
        compileClass(1);
        tk.close();
        writer.close();
    }

    /* Method for compiling class, ex., class Main {...}
    "class" className "{" classVarDec* subroutineDec* ")" */
    private void compileClass(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<class>"); // Header

        write("class", indent);

        // className identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        write("{", indent);
        compileClassVarDec(indentLevel + 1); // classVarDec*
        compileSubroutineDec(indentLevel + 1); // subroutineDec*
        write("}", indent);

        writer.println(headerIndent + "</class>"); // Footer
    }

    /* Method for compiling static or non-static variables of object.
    ('static' | 'field') type varName (',' varName)* ';'
    ex., field int x, y, z  */
    private void compileClassVarDec(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Immediately return if not 'static' or 'field'
        token = tk.getCurrToken();
        if (!(token.equals("static") || token.equals("field"))) {
            return;
        }
        writer.println(headerIndent + "<classVarDec>"); // Header
        writer.println(indent + "<keyword> " + token + " </keyword>"); // 'static' or 'field'
        tk.advance();

        // Primitive type or className (type)
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token)) {
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
        token = tk.getCurrToken();
        while (token.equals(",")) {
            write(",", indent);

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            writer.println(indent + "<identifier> " + token + " </identifier>");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }
        write(";", indent);
        writer.println(headerIndent + "</classVarDec>"); // Footer
        compileClassVarDec(indentLevel); // 0 or more classVarDec in class -> recursive call
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
        writer.println(headerIndent + "<subroutineDec>"); // Header
        writer.println(indent + "<keyword> " + token + " </keyword>");
        tk.advance();

        // void, primitive type, or identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token)) {
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

        write("(", indent);
        compileParameterList(indentLevel + 1); // 0 or 1 parameterList
        write(")", indent);
        compileSubroutineBody(indentLevel + 1); // subroutineBody (contains at least "{}")

        writer.println(headerIndent + "</subroutineDec>"); // Footer

        compileSubroutineDec(indentLevel); // 0 or more subroutineDec in class -> recursive call
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec
    ex., int x, int y, boolean flag */
    private void compileParameterList(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<parameterList>");

        // Immediately return if not type -> 0 parameterList
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            writer.println(headerIndent + "</parameterList>");
            return;
        }
        // type
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
            write(",", indent);

            // primitive or className (type)
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!PRIMITIVES.contains(token)) {
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
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<subroutineBody>"); // Header

        write("{", indent);
        compileVarDec(indentLevel + 1); // 0 or more varDec
        compileStatements(indentLevel + 1); // statements (0 or more statement)
        write("}", indent);

        writer.println(headerIndent + "</subroutineBody>"); // Footer
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
        writer.println(headerIndent + "<varDec>"); // header
        write("var", indent); // 'var' keyword

        // Primitive type or className (type)
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token)) {
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
        token = tk.getCurrToken();
        while (token.equals(",")) {
            // token has to equal ',' -> print it
            write(",", indent);

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            writer.println(indent + "<identifier> " + token + " </identifier>");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }
        write(";", indent);
        writer.println(headerIndent + "</varDec>"); // Footer
        compileVarDec(indentLevel); // 0 or more varDec in subroutineDec -> recursive call
    }

    /* Compile a let, if, while, do, or return statement */
    private void compileStatements(int indentLevel) throws IOException {
        String token = tk.getCurrToken();
        String headerIndent = INDENT.repeat(indentLevel - 1);

        writer.println(headerIndent + "<statements>"); // Header

        // If token is one of the valid statement types
        // Implicit assumption that tokenizer advances to next valid token after each call
        while (STATEMENTS.contains(token)) {
            switch (token) {
                case "let" -> compileLet(indentLevel + 1);
                case "if" -> compileIf(indentLevel + 1);
                case "while" -> compileWhile(indentLevel + 1);
                case "do" -> compileDo(indentLevel + 1);
                case "return" -> compileReturn(indentLevel + 1);
            }
            // Prepare next token
            token = tk.getCurrToken();
        }
        writer.println(headerIndent + "</statements>"); // Footer
    }

    /* 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet(int indentLevel) throws IOException {
        String token; TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<letStatement>"); // Header

        write("let", indent);

        // varName identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        writer.println(indent + "<identifier> " + token + " </identifier>");
        tk.advance();

        // 0 or 1 ('[' expression ']')
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("=")) {
            write("[", indent);
            compileExpression(indentLevel + 1); // expression
            write("]", indent);
        }
        write("=", indent);
        compileExpression(indentLevel + 1);
        write(";", indent);

        writer.println(headerIndent + "</letStatement>"); // footer
    }

    /* 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf(int indentLevel) throws IOException {
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<ifStatement>"); // Header

        write("if", indent);
        write("(", indent);
        compileExpression(indentLevel + 1);
        write(")", indent);
        write("{", indent);
        compileStatements(indentLevel + 1); // statements (0 or more statement)
        write("}", indent);

        // ('else' '{' statements '}')?
        if (tk.getCurrToken().equals("else")) {
            write("else", indent);
            write("{", indent);
            compileStatements(indentLevel + 1); // statements (0 or more statement)
            write("}", indent);
        }
        writer.println(headerIndent + "</ifStatement>");  // Footer
    }

    /* 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile(int indentLevel) throws IOException {
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<whileStatement>"); // Header

        write("while", indent);
        write("(", indent);
        compileExpression(indentLevel + 1); // expression
        write(")", indent);
        write("{", indent);
        compileStatements(indentLevel + 1); // statements (0 or more statement)
        write("}", indent);

        writer.println(headerIndent + "</whileStatement>"); // Footer
    }

    /* 'do' subroutineCall ';' */
    private void compileDo(int indentLevel) throws IOException {
        String token; String nextToken;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<doStatement>"); // Header

        write("do", indent);

        // subroutineCall
        token = tk.getCurrToken();
        tk.advance();
        nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken, indentLevel);

        write(";", indent);

        writer.println(headerIndent + "</doStatement>"); // Footer
    }

    /* 'return' expression? ';' */
    private void compileReturn(int indentLevel) throws IOException {
        String token;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<returnStatement>"); // Header

        write("return", indent);

        token = tk.getCurrToken();
        if (!token.equals(";")) {
            compileExpression(indentLevel + 1);
        }
        write(";", indent);

        writer.println(headerIndent + "</returnStatement>"); // Footer
    }

    /* term (op term)* */
    private void compileExpression(int indentLevel) throws IOException {
        String token;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        // Header
        writer.println(headerIndent + "<expression>");

        // term (op term)* -> at least one term
        compileTerm(indentLevel + 1);

        // 0 or more (op term)
        token = tk.getCurrToken();
        while (OP.contains(token)) {
            writer.println(indent + "<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>"); // op symbol
            tk.advance();

            compileTerm(indentLevel + 1);

            // Prepare next token
            token = tk.getCurrToken();
        }
        // Footer
        writer.println(headerIndent + "</expression>");
    }

    /* integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
    '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm(int indentLevel) throws IOException {
        String token; Tokenizer.TokenType type;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<term>"); // Header

        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type == integerConstant) {
            writer.println(indent + "<integerConstant> " + token + " </integerConstant>");
            tk.advance();
        }
        else if (type == stringConstant) {
            writer.println(indent + "<stringConstant> " + token + " </stringConstant>");
            tk.advance();
        }
        else if (KEYWORD_CONST.contains(token)) {
            writer.println(indent + "<keyword> " + token + " </keyword>");
            tk.advance();
        }
        else if (token.equals("(")) { // '(' expression ')'
            write("(", indent);
            compileExpression(indentLevel + 1);
            write(")", indent);
        }
        else if (UNARY_OP.contains(token)) { // (unaryOp term)
            writer.println(indent + "<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>");
            tk.advance();

            compileTerm(indentLevel + 1);
        }
        else if (type == identifier) { // varName identifier
            tk.advance(); // Need to peek ahead to determine next steps

            String nextToken = tk.getCurrToken();
            /* second token is op -> just a term, so return to term (op term)* */
            if (OP.contains(nextToken)) {
                writer.println(indent + "<identifier> " + token + " </identifier>");
            }
            /* second token is '[' -> '[' expression ']' */
            else if (nextToken.equals("[")) {
                writer.println(indent + "<identifier> " + token + " </identifier>");
                write("[", indent);
                compileExpression(indentLevel + 1);
                write("]", indent);
            }
            /* subroutine call */
            else {
                compileSubroutineCall(token, nextToken, indentLevel);
            }
        }
        else {
            throwRuntimeException(
                    "integerConstant, stringConstant, keywordConstant, varName, '(', unaryOp, subroutineCall",
                    "symbol or identifier", token, type);
        }
        writer.println(headerIndent + "</term>"); // Footer
    }

    /* Helper function for compileDo and compileTerm
    subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')' */
    private void compileSubroutineCall(String token, String nextToken, int indentLevel) throws IOException {
        Tokenizer.TokenType type;
        String indent = INDENT.repeat(indentLevel);

        // print subroutineName, className, or varName
        writer.println(indent + "<identifier> " + token + " </identifier>");

        if (nextToken.equals("(")) {
            write("(", indent);
            compileExpressionList(indentLevel + 1);
            write(")", indent);
        }
        else if (nextToken.equals(".")) { // '.'
            write(".", indent);

            // subroutineName
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!type.equals(identifier)) {
                throwRuntimeException("subroutineName", identifier, token, type);
            }
            writer.println(indent + "<identifier> " + token + " </identifier>");
            tk.advance();

            write("(", indent);
            compileExpressionList(indentLevel + 1);
            write(")", indent);
        }
    }

    /* ( expression (',' expression)* )? */
    private void compileExpressionList(int indentLevel) throws IOException {
        String token;
        String headerIndent = INDENT.repeat(indentLevel - 1);
        String indent = INDENT.repeat(indentLevel);

        writer.println(headerIndent + "<expressionList>"); // Header

        token = tk.getCurrToken();
        if (!token.equals(")")) {
            compileExpression(indentLevel + 1);

            // 0 or more (',' expression)
            token = tk.getCurrToken();
            while (token.equals(",")) {
                write(",", indent);
                compileExpression(indentLevel + 1);

                // Prepare next token
                token = tk.getCurrToken();
            }
        }
        writer.println(headerIndent + "</expressionList>"); // Footer
    }

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

    private void write(String expected, String indent) throws IOException {
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();

        TokenType expectedType;
        if (Tokenizer.KEYWORDS.contains(expected)) {
            expectedType = keyword;
        } else {
            expectedType = symbol;
        }
        if (!token.equals(expected)) {
            throwRuntimeException("'" + expected + "'", expectedType, token, type);
        }
        writer.println(indent + "<" + expectedType + "> " + XML_EXCEP.getOrDefault(token, token) + " </" + expectedType + ">");
        tk.advance();
    }
}