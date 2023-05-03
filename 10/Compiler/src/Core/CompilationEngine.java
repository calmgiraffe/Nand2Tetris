package Core;

import java.io.*;
import java.util.Set;

import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;
import static Core.Tokenizer.XML_EXCEP;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private static final Set<String> PRIMITIVES = Set.of("int","char","boolean");
    private static final Set<String> STATEMENTS = Set.of("let","if","while","do","return");
    private static final Set<String> OP = Set.of("+","-","*","/","&","|","<",">","=");
    private static final Set<String> UNARY_OP = Set.of("-", "~");
    private static final Set<String> KEYWORD_CONST = Set.of("true","false","null","this");

    private final Tokenizer tk;
    private final PrintWriter writer;
    private final SymbolTable classSymTable = new SymbolTable();
    private SymbolTable subroutineSymTable;
    private String className;

    /* Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + "_ce.xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compile() throws IOException {
        compileClass();
        tk.close();
        writer.close();
    }

    /* Method for compiling class, ex., class Main {...}
    "class" className "{" classVarDec* subroutineDec* ")" */
    private void compileClass() throws IOException {
        String token; Tokenizer.TokenType type;

        // Header, 'class'
        writer.println("<class>");
        write("class");

        // className identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        className = token; // all subroutines have an implicit instance of this type
        writer.println(className + " class declared");
        tk.advance();

        write("{");
        compileClassVarDec(); // classVarDec*
        compileSubroutineDec(); // subroutineDec*
        write("}");
        writer.println("</class>"); // Footer
    }

    /* Method for compiling static or field (non-static) variables of object.
    ('static' | 'field') type varName (',' varName)* ';'
    ex., field int x, y, z  */
    private void compileClassVarDec() throws IOException {
        String scopeOf, dataTypeOf;
        String token; Tokenizer.TokenType type;

        // Immediately return if not 'static' or 'field'
        token = tk.getCurrToken();
        if (!(token.equals("static") || token.equals("field"))) {
            return;
        }
        // Header, ("static" | "field")
        writer.println("<classVarDec>");
        scopeOf = token;
        tk.advance();

        // type: primitive or className
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("type", keyword + ", " + identifier, token, type);
        }
        dataTypeOf = token; // all class var decs are of this type
        tk.advance();

        // At least one varName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        /* Add to class symbol table - either primitive type or object */
        classSymTable.define(token, dataTypeOf, scopeOf);
        writer.println(token + " " + scopeOf + " " + classSymTable.indexOf(token) + " declared");
        tk.advance();

        // Zero or more (',' varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            write(",");

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            /* Add to class symbol table - either primitive type or object */
            classSymTable.define(token, dataTypeOf, scopeOf);
            writer.println(token + " " + scopeOf + " " + classSymTable.indexOf(token) + " declared");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }

        // Terminating ';', footer
        write(";");
        writer.println("</classVarDec>");

        compileClassVarDec(); // 0 or more classVarDec in class -> recursive call
    }

    /* Method for compiling object routines like constructor, static/non-static methods
    ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
    ex., method void draw(int x, int y) */
    private void compileSubroutineDec() throws IOException {
        String token; Tokenizer.TokenType type;

        // Immediately return if not one of 'constructor', 'function', 'method'
        token = tk.getCurrToken();
        if (!(token.equals("constructor") || token.equals("function") || token.equals("method"))) {
            return;
        }

        // Header, ('constructor' | 'function' | 'method')
        subroutineSymTable = new SymbolTable(classSymTable);
        writer.println("<subroutineDec>");
        tk.advance();

        // subroutine type: void, primitive type, or identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("'void' | type", keyword + ", " + identifier, token, type);
        }
        tk.advance();

        // subroutineName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("subroutineName", identifier, token, type); }
        writer.println(token + " subroutine declared");
        /* SYMBOL TABLE - add 'this' to subroutine table */
        subroutineSymTable.define("this", className, "arg");
        writer.println("this arg " + subroutineSymTable.indexOf("this") + " declared");
        tk.advance();

        write("(");
        compileParameterList(); // 0 or 1 parameterList
        write(")");
        compileSubroutineBody(); // subroutineBody (contains at least "{}")

        writer.println("</subroutineDec>"); // footer

        compileSubroutineDec(); // 0 or more subroutineDec in class -> recursive call
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec, i.e., subroutine args
    ((type varName) (',' type varName)*)?
    ex., int x, int y, boolean flag */
    private void compileParameterList() throws IOException {
        String dataTypeOf;
        String token; Tokenizer.TokenType type;

        // Header
        writer.println("<parameterList>");

        // Immediately return if not type -> 0 parameterList
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            writer.println("</parameterList>");
            return;
        }

        // type: primitive or className
        dataTypeOf = token;
        tk.advance();

        // varName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        /* SYMBOL TABLE - add first arg to subroutine table */
        subroutineSymTable.define(token, dataTypeOf, "arg");
        writer.println(token + " arg " + subroutineSymTable.indexOf(token) + " declared");
        tk.advance();

        // 0 or more (',' type varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            write(",");

            // primitive or className (type)
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!PRIMITIVES.contains(token) && type != identifier) {
                throwRuntimeException("type", keyword + ", " + identifier, token, type);
            }
            dataTypeOf = token;
            tk.advance();

            // varName identifier must follow type
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            /* SYMBOL TABLE - add next args to subroutine table */
            subroutineSymTable.define(token, dataTypeOf, "arg");
            writer.println(token + " arg " + subroutineSymTable.indexOf(token) + " declared");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }

        // Footer
        writer.println("</parameterList>");
    }

    /* Method for compiling subroutine body, i.e., { varDec* statement* } */
    private void compileSubroutineBody() throws IOException {
        writer.println("<subroutineBody>"); // Header

        write("{");
        compileVarDec(); // 0 or more varDec
        compileStatements(); // statements (0 or more statement)
        write("}");

        writer.println("</subroutineBody>"); // Footer
    }

    /* Method for compiling varDec.
    'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws IOException {
        String dataTypeOf;
        String token; Tokenizer.TokenType type;

        // Immediately return if not var
        token = tk.getCurrToken();
        if (!token.equals("var")) {
            return;
        }
        // Header, 'var'
        writer.println("<varDec>");
        write("var");

        // Primitive type or className (type)
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("type", keyword + ", " + identifier, token, type);
        }
        dataTypeOf = token; // all var decs of this type
        tk.advance();

        // varName identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        /* SYMBOL TABLE - add var to subroutine table */
        subroutineSymTable.define(token, dataTypeOf, "var");
        writer.println(token + " var " + subroutineSymTable.indexOf(token) + " declared");
        tk.advance();

        // Zero or more (',' varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            // token has to equal ',' -> print it
            write(",");

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
            /* SYMBOL TABLE - add var to subroutine table */
            subroutineSymTable.define(token, dataTypeOf, "var");
            writer.println(token + " var " + subroutineSymTable.indexOf(token) + " declared");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }

        // Terminating ';', footer
        write(";");
        writer.println("</varDec>");

        compileVarDec(); // 0 or more varDec in subroutineDec -> recursive call
    }

    /* Compile a let, if, while, do, or return statement */
    private void compileStatements() throws IOException {
        String token = tk.getCurrToken();

        writer.println("<statements>"); // Header

        // If token is one of the valid statement types
        // Implicit assumption that tokenizer advances to next valid token after each call
        while (STATEMENTS.contains(token)) {
            switch (token) {
                case "let" -> compileLet();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
            }
            // Prepare next token
            token = tk.getCurrToken();
        }
        writer.println("</statements>"); // Footer
    }

    /* 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws IOException {
        String token; TokenType type;

        writer.println("<letStatement>"); // Header

        write("let");

        // varName identifier
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        // SYMBOL TABLE - retrieve from subroutine
        writer.println(token + " " + subroutineSymTable.scopeOf(token) +
                " " + subroutineSymTable.indexOf(token) + " used");
        tk.advance();

        // 0 or 1 ('[' expression ']')
        token = tk.getCurrToken();
        if (!token.equals("=")) {
            write("[");
            compileExpression(); // expression
            write("]");
        }
        write("=");
        compileExpression();
        write(";");

        writer.println("</letStatement>"); // Footer
    }

    /* 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws IOException {
        writer.println("<ifStatement>"); // Header

        write("if");
        write("(");
        compileExpression();
        write(")");
        write("{");
        compileStatements(); // statements (0 or more statement)
        write("}");

        // ('else' '{' statements '}')?
        if (tk.getCurrToken().equals("else")) {
            write("else");
            write("{");
            compileStatements(); // statements (0 or more statement)
            write("}");
        }
        writer.println("</ifStatement>");  // Footer
    }

    /* 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws IOException {
        writer.println("<whileStatement>"); // Header

        write("while");
        write("(");
        compileExpression(); // expression
        write(")");
        write("{");
        compileStatements(); // statements (0 or more statement)
        write("}");

        writer.println("</whileStatement>"); // Footer
    }

    /* 'do' subroutineCall ';' */
    private void compileDo() throws IOException {
        String token; String nextToken;

        writer.println("<doStatement>"); // Header

        write("do");

        // subroutineCall
        token = tk.getCurrToken();
        tk.advance();
        nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken);

        write(";");

        writer.println("</doStatement>"); // Footer
    }

    /* 'return' expression? ';' */
    private void compileReturn() throws IOException {
        String token;

        writer.println("<returnStatement>"); // Header

        write("return");

        token = tk.getCurrToken();
        if (!token.equals(";")) {
            compileExpression();
        }
        write(";");

        writer.println("</returnStatement>"); // Footer
    }

    /* term (op term)* */
    private void compileExpression() throws IOException {
        String token;

        // Header
        writer.println("<expression>");

        // term (op term)* -> at least one term
        compileTerm();

        // 0 or more (op term)
        token = tk.getCurrToken();
        while (OP.contains(token)) {
            writer.println("<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>"); // op symbol
            tk.advance();

            compileTerm();

            // Prepare next token
            token = tk.getCurrToken();
        }
        // Footer
        writer.println("</expression>");
    }

    /* integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
    '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm() throws IOException {
        String token; Tokenizer.TokenType type;

        writer.println("<term>"); // Header

        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type == integerConstant) {
            writer.println("<integerConstant> " + token + " </integerConstant>");
            tk.advance();
        }
        else if (type == stringConstant) {
            writer.println("<stringConstant> " + token + " </stringConstant>");
            tk.advance();
        }
        else if (KEYWORD_CONST.contains(token)) {
            writer.println("<keyword> " + token + " </keyword>");
            tk.advance();
        }
        else if (token.equals("(")) { // '(' expression ')'
            write("(");
            compileExpression();
            write(")");
        }
        else if (UNARY_OP.contains(token)) { // (unaryOp term)
            writer.println("<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>");
            tk.advance();

            compileTerm();
        }
        else if (type == identifier) { // varName identifier
            tk.advance(); // Need to peek ahead to determine next steps

            String nextToken = tk.getCurrToken();
            /* second token is op -> just a term, so return to term (op term)* */
            if (OP.contains(nextToken)) {
                // SYMBOL TABLE - retrieve from subroutine
                writer.println(token + " " + subroutineSymTable.scopeOf(token) +
                        " " + subroutineSymTable.indexOf(token) + " used");
            }
            /* second token is '[' -> '[' expression ']' */
            else if (nextToken.equals("[")) {
                // SYMBOL TABLE - retrieve from subroutine
                writer.println(token + " " + subroutineSymTable.scopeOf(token) +
                        " " + subroutineSymTable.indexOf(token) + " used");

                write("[");
                compileExpression();
                write("]");
            }
            /* subroutine call */
            else {
                compileSubroutineCall(token, nextToken);
            }
        }
        else {
            throwRuntimeException(
                    "integerConstant, stringConstant, keywordConstant, varName, '(', unaryOp, subroutineCall",
                    "symbol or identifier", token, type);
        }
        writer.println("</term>"); // Footer
    }

    /* Helper function for compileDo and compileTerm
    subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')' */
    private void compileSubroutineCall(String token, String nextToken) throws IOException {
        Tokenizer.TokenType type;

        if (nextToken.equals("(")) {
            writer.println(token + " subroutine used");

            write("(");
            compileExpressionList();
            write(")");
        }
        else if (nextToken.equals(".")) { // '.'
            if (subroutineSymTable.contains(token)) {
                writer.println(token + " " + subroutineSymTable.scopeOf(token) +
                        " " + subroutineSymTable.indexOf(token) + " used");
            } else {
                writer.println(token + " class used");
            }
            write(".");

            // subroutineName
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!type.equals(identifier)) {
                throwRuntimeException("subroutineName", identifier, token, type);
            }
            writer.println(token + " subroutine used");
            tk.advance();

            write("(");
            compileExpressionList();
            write(")");
        }
    }

    /* ( expression (',' expression)* )? */
    private void compileExpressionList() throws IOException {
        String token;

        writer.println("<expressionList>"); // Header

        token = tk.getCurrToken();
        if (!token.equals(")")) {
            compileExpression();

            // 0 or more (',' expression)
            token = tk.getCurrToken();
            while (token.equals(",")) {
                write(",");
                compileExpression();

                // Prepare next token
                token = tk.getCurrToken();
            }
        }
        writer.println("</expressionList>"); // Footer
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

    /* Helper method for writing specific symbols and keywords */
    private void write(String expected) throws IOException {
        String token = tk.getCurrToken();
        TokenType type = tk.getCurrType();

        TokenType expectedType;
        if (Tokenizer.KEYWORDS.contains(expected)) {
            expectedType = keyword;
        } else {
            expectedType = symbol;
        }
        if (!token.equals(expected)) {
            throwRuntimeException("'" + expected + "'", expectedType, token, type);
        }
        //writer.println("<" + expectedType + "> " + XML_EXCEP.getOrDefault(token, token) + " </" + expectedType + ">");
        tk.advance();
    }
}