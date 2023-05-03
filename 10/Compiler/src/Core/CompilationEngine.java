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
    private SymbolTable subSymTable = new SymbolTable(classSymTable);
    private String className;

    /** Build the list of output files */
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
        check("class");

        /* className identifier */
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        className = token; // all subroutines have an implicit instance of this type
        writer.println(className + " class declared");
        tk.advance();

        check("{");
        compileClassVarDec(); // classVarDec*
        compileSubroutineDec(); // subroutineDec*
        check("}");
    }

    /* Method for compiling static or field (non-static) variables of object.
    ('static' | 'field') type varName (',' varName)* ';'
    ex., field int x, y, z  */
    private void compileClassVarDec() throws IOException {
        /* Immediately return if not 'static' or 'field' */
        String token = tk.getCurrToken();
        if (!(token.equals("static") || token.equals("field"))) {
            return;
        }
        /* ("static" | "field") */
        String scopeOf = token;
        tk.advance();

        /* type: primitive or className */
        token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("type", keyword + ", " + identifier, token, type);
        }
        String dataTypeOf = token; // all class var decs are of this type
        tk.advance();

        /* varName */
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

        /* Add to class symbol table - either primitive type or object */
        classSymTable.define(token, dataTypeOf, scopeOf);
        printSymTableData(token, "declared");
        tk.advance();

        /* Zero or more (',' varName) */
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

            /* varName identifier must follow ',' */
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

            /* Add to class symbol table - either primitive type or object */
            classSymTable.define(token, dataTypeOf, scopeOf);
            printSymTableData(token, "declared");
            tk.advance();

            /* Prepare next token */
            token = tk.getCurrToken();
        }
        check(";");

        // 0 or more classVarDec in class -> recursive call
        compileClassVarDec();
    }

    /* Method for compiling object routines like constructor, static/non-static methods
    ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
    ex., method void draw(int x, int y) */
    private void compileSubroutineDec() throws IOException {
        // Immediately return if not one of 'constructor', 'function', 'method'
        String token = tk.getCurrToken();
        if (!(token.equals("constructor") || token.equals("function") || token.equals("method"))) {
            return;
        }
        // ('constructor' | 'function' | 'method')
        subSymTable = new SymbolTable(classSymTable);
        tk.advance();

        // subroutine type: void, primitive type, or identifier
        token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("'void' | type", keyword + ", " + identifier, token, type);
        }
        tk.advance();

        // subroutineName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("subroutineName", identifier, token, type); }
        writer.println(token + " subroutine declared");

        /* Add 'this' to subroutine symbol table */
        subSymTable.define("this", className, "arg");
        printSymTableData("this", "declared");
        tk.advance();

        check("(");
        compileParameterList(); // 0 or 1 parameterList
        check(")");
        compileSubroutineBody(); // subroutineBody (contains at least "{}")

        // 0 or more subroutineDec in class -> recursive call
        compileSubroutineDec();
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec, i.e., subroutine args
    ((type varName) (',' type varName)*)?
    ex., int x, int y, boolean flag */
    private void compileParameterList() throws IOException {
        // Immediately return if not type -> 0 parameterList
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            return;
        }
        // type: primitive or className
        String dataTypeOf = token;
        tk.advance();

        // varName
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

        /* Add first arg to subroutine symbol table */
        subSymTable.define(token, dataTypeOf, "arg");
        printSymTableData(token, "declared");
        tk.advance();

        // 0 or more (',' type varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

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

            /* Add next args to subroutine symbol table */
            subSymTable.define(token, dataTypeOf, "arg");
            printSymTableData(token, "declared");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }
    }

    /* Method for compiling subroutine body, i.e., { varDec* statement* } */
    private void compileSubroutineBody() throws IOException {
        check("{");
        compileVarDec(); // 0 or more varDec
        compileStatements(); // statements (0 or more statement)
        check("}");
    }

    /* Method for compiling varDec.
    'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws IOException {
        String dataTypeOf;
        String token; TokenType type;

        // Immediately return if not var
        token = tk.getCurrToken();
        if (!token.equals("var")) { return; }
        check("var");

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

        /* Add var to subroutine symbol table */
        subSymTable.define(token, dataTypeOf, "var");
        printSymTableData(token, "declared");
        tk.advance();

        // Zero or more (',' varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

            /* Add var to subroutine symbol table */
            subSymTable.define(token, dataTypeOf, "var");
            printSymTableData(token, "declared");
            tk.advance();

            // Prepare next token
            token = tk.getCurrToken();
        }
        check(";");

        // 0 or more varDec in subroutineDec -> recursive call
        compileVarDec();
    }

    /* Compile a let, if, while, do, or return statement */
    private void compileStatements() throws IOException {
        String token = tk.getCurrToken();

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
    }

    /* 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws IOException {
        check("let");

        // varName identifier
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

        // Retrieve from subroutine symbol table
        printSymTableData(token, "used");
        tk.advance();

        // 0 or 1 ('[' expression ']')
        token = tk.getCurrToken();
        if (!token.equals("=")) {
            check("[");
            compileExpression();
            check("]");
        }
        check("=");
        compileExpression();
        check(";");
    }

    /* 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws IOException {
        // 'if' '(' expression ')' '{' statements '}'
        check("if");
        check("(");
        compileExpression();
        check(")");
        check("{");
        compileStatements(); // statements (0 or more statement)
        check("}");

        // ('else' '{' statements '}')?
        if (tk.getCurrToken().equals("else")) {
            check("else");
            check("{");
            compileStatements(); // statements (0 or more statement)
            check("}");
        }
    }

    /* 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws IOException {
        check("while");
        check("(");
        compileExpression();
        check(")");
        check("{");
        compileStatements(); // statements (0 or more statement)
        check("}");
    }

    /* 'do' subroutineCall ';' */
    private void compileDo() throws IOException {
        check("do");

        // subroutineCall
        String token = tk.getCurrToken();
        tk.advance();
        String nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken);

        check(";");
    }

    /* 'return' expression? ';' */
    private void compileReturn() throws IOException {
        check("return");

        // expression?
        String token = tk.getCurrToken();
        if (!token.equals(";")) {
            compileExpression();
        }

        check(";");
    }

    /* term (op term)* */
    private void compileExpression() throws IOException {
        compileTerm();

        // 0 or more (op term)
        String token = tk.getCurrToken();
        while (OP.contains(token)) {
            writer.println("<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>"); // op symbol
            tk.advance();

            compileTerm();
            token = tk.getCurrToken(); // Prepare next token
        }
    }

    /* integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
    '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm() throws IOException {
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();

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
            check("(");
            compileExpression();
            check(")");
        }
        else if (UNARY_OP.contains(token)) { // (unaryOp term)
            writer.println("<symbol> " + XML_EXCEP.getOrDefault(token, token) + " </symbol>");
            tk.advance();

            compileTerm();
        }
        else if (type == identifier) { // varName or subroutineName
            // Need to peek ahead to determine next steps
            tk.advance();

            String nextToken = tk.getCurrToken();
            /* second token is op -> return to term (op term)* OR token is var */
            if (OP.contains(nextToken)) {
                printSymTableData(token, "used");
            }
            /* second token is '[' -> '[' expression ']' */
            else if (nextToken.equals("[")) {
                printSymTableData(token, "used");

                check("[");
                compileExpression();
                check("]");
            }
            else if (nextToken.equals("(") || nextToken.equals(".")) { // subroutine call
                compileSubroutineCall(token, nextToken);
            }
            else {
                printSymTableData(token, "used");
            }
        }
        else {
            throwRuntimeException(
                    "integerConstant, stringConstant, keywordConstant, varName, '(', unaryOp, subroutineCall",
                    "symbol, constant, or identifier", token, type);
        }
    }

    /* Helper function for compileDo and compileTerm
    subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')' */
    private void compileSubroutineCall(String token, String nextToken) throws IOException {
        TokenType type;

        // subroutineName '(' expressionList ')'
        if (nextToken.equals("(")) {
            writer.println(token + " subroutine used");
            check("(");
            compileExpressionList();
            check(")");
        }
        // (className | varName) '.' subroutineName '(' expressionList ')'
        else if (nextToken.equals(".")) {
            if (subSymTable.contains(token)) {
                printSymTableData(token, "used");
            } else {
                writer.println(token + " class used");
            }
            check(".");

            // subroutineName
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!type.equals(identifier)) { throwRuntimeException("subroutineName", identifier, token, type); }
            writer.println(token + " subroutine used");
            tk.advance();

            check("(");
            compileExpressionList();
            check(")");
        }
    }

    /* ( expression (',' expression)* )? */
    private void compileExpressionList() throws IOException {
        // Immediately return if closing bracket (no expressions)
        String token = tk.getCurrToken();
        if (token.equals(")")) {
            return;
        }
        compileExpression();

        // 0 or more (',' expression)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");
            compileExpression();

            // Prepare next token
            token = tk.getCurrToken();

        }
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
    private void check(String expected) throws IOException {
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

    private void printSymTableData(String name, String declaration) {
        if (!subSymTable.contains(name)) {
            return;
        }
        writer.print(name);
        writer.print(" ");
        writer.print(subSymTable.dataTypeOf(name));
        writer.print(" ");
        writer.print(subSymTable.scopeOf(name));
        writer.print(" ");
        writer.print(subSymTable.indexOf(name));
        writer.print(" ");
        writer.println(declaration);
    }
}