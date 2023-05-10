package Core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static Core.Tokenizer.TokenType;
import static Core.Tokenizer.TokenType.*;
import static Core.SymbolTable.Scope;
import static Core.VMWriter.Segment;
import static Core.VMWriter.Segment.*;
import static Core.VMWriter.Arithmetic.*;
import static Core.VMWriter.OP_TO_COMMAND;
import static Core.VMWriter.UNARY_OP_TO_COMMAND;

/* The Core.CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private final Map<Scope, Segment> scopeToSegment = new HashMap<>() {{
        put(Scope.STATIC, Segment.STATIC);
        put(Scope.FIELD, Segment.THIS);
        put(Scope.VAR, Segment.LOCAL);
        put(Scope.ARG, Segment.ARGUMENT);
    }};
    private static final Set<String> PRIMITIVES = Set.of("int","char","boolean");
    private static final Set<String> STATEMENTS = Set.of("let","if","while","do","return");
    private static final Set<String> OP = Set.of("+","-","*","/","&","|","<",">","=");
    private static final Set<String> UNARY_OP = Set.of("-", "~");
    private static final Set<String> KEYWORD_CONST = Set.of("true","false","null","this");

    private final Tokenizer tk;
    private final PrintWriter writer;
    private final VMWriter vmWriter;
    private final SymbolTable classSymTable = new SymbolTable();
    private SymbolTable subSymTable = new SymbolTable(classSymTable);
    private String className;
    private int labelCounter = 0;

    /** Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        vmWriter = new VMWriter(prefix);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + "_ce.xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compile() throws IOException {
        compileClass();
        tk.close();
        writer.close();
        vmWriter.close();
    }

    /* Method for compiling class, ex., class Main {...}
    "class" className "{" classVarDec* subroutineDec* ")" */
    // set className
    private void compileClass() throws IOException {
        check("class");

        /* className identifier */
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!type.equals(identifier)) { throwRuntimeException("className", identifier, token, type); }
        className = token; // all subroutines have an implicit instance of this type
        tk.advance();

        check("{");
        compileClassVarDec(); // classVarDec*
        compileSubroutineDec(); // subroutineDec*
        check("}");
    }

    /* Method for compiling static or field (non-static) variables of object.
    ('static' | 'field') type varName (',' varName)* ';'
    ex., field int x, y, z
    Consists only of symbol table entry adding */
    // class sym table gets filled here
    private void compileClassVarDec() throws IOException {
        /* ("static" | "field") */
        /* Immediately return if neither */
        Scope scopeOf;
        String token = tk.getCurrToken();
        if (token.equals("static")) {
            scopeOf = Scope.STATIC;
        } else if (token.equals("field")){
            scopeOf = Scope.FIELD;
        } else {
            return;
        }
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

        // Add to class symbol table - either primitive type or object
        classSymTable.define(token, dataTypeOf, scopeOf);
        tk.advance();

        /* Zero or more (',' varName) */
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

            /* varName identifier must follow ',' */
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

            // Add to class symbol table - either primitive type or object
            classSymTable.define(token, dataTypeOf, scopeOf);
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
        /* ('constructor' | 'function' | 'method') */
        subSymTable = new SymbolTable(classSymTable);
        tk.advance();

        /* type: void, primitive type, or identifier */
        token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("'void' | type", keyword + ", " + identifier, token, type);
        }
        tk.advance();

        /* subroutineName */
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("subroutineName", identifier, token, type); }

        // Add 'this' to subroutine symbol table
        subSymTable.define("this", className, Scope.ARG);
        String subroutineName = className + '.' + token;
        tk.advance();

        check("(");
        compileParameterList(); // vars are added to subroutine table
        check(")");

        // VM code generation to make new object the current object
        // todo: handle method and class function calls
        int nVars = subSymTable.varCount(Scope.VAR);
        int nArgs = subSymTable.varCount(Scope.ARG);
        vmWriter.writeFunction(subroutineName, nVars);
        vmWriter.writePush(CONSTANT, nArgs);
        vmWriter.writeCall("Memory.alloc", 1);
        vmWriter.writePop(POINTER, 0);

        compileSubroutineBody(); // contains at least "{}"

        // 0 or more subroutineDec in class -> recursive call
        compileSubroutineDec();
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec, i.e., subroutine args
    ((type varName) (',' type varName)*)?
    ex., int x, int y, boolean flag */
    private void compileParameterList() throws IOException {
        /* Immediately return if not type -> 0 parameterList */
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            return;
        }
        /* type: primitive or className */
        String dataTypeOf = token;
        tk.advance();

        /* varName */
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

        // Add first arg to subroutine symbol table
        subSymTable.define(token, dataTypeOf, Scope.ARG);
        tk.advance();

        /* 0 or more (',' type varName) */
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

            /* type: primitive or className */
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!PRIMITIVES.contains(token) && type != identifier) {
                throwRuntimeException("type", keyword + ", " + identifier, token, type);
            }
            dataTypeOf = token;
            tk.advance();

            /* varName */
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

            // Add next arg(s) to subroutine symbol table
            subSymTable.define(token, dataTypeOf, Scope.ARG);
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
        subSymTable.define(token, dataTypeOf, Scope.VAR);
        tk.advance();

        // Zero or more (',' varName)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");

            // varName identifier must follow ','
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }

            /* Add var to subroutine symbol table */
            subSymTable.define(token, dataTypeOf, Scope.VAR);
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
        vmWriter.write("<let statement>");
        check("let");

        /* varName identifier */
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (type != identifier) { throwRuntimeException("varName", identifier, token, type); }
        String varName = token;
        tk.advance();

        /* 0 or 1 ('[' expression ']') */
        token = tk.getCurrToken();
        if (!token.equals("=")) {
            check("[");
            compileExpression();
            check("]");
        }
        check("=");
        compileExpression();
        check(";");

        // VM code for LHS of let statement
        Segment segment = scopeToSegment.get(subSymTable.scopeOf(varName));
        vmWriter.writePop(segment, subSymTable.indexOf(varName));
    }

    /* 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws IOException {
        vmWriter.write("<if statement>");
        String elseLabel = "L" + labelCounter;
        String endLabel = "L" + (labelCounter + 1);
        labelCounter += 2;

        // 'if' '(' expression ')' '{' statements '}'
        check("if");
        check("(");

        compileExpression(); // resultant should be at top of stack
        vmWriter.writeArithmetic(not); // negate the resultant
        vmWriter.writeIf(elseLabel); // skip to else if true

        check(")");
        check("{");
        compileStatements(); // statements (0 or more statement)
        check("}");
        vmWriter.writeGoto(endLabel);
        vmWriter.writeLabel(elseLabel);

        // ('else' '{' statements '}')?
        if (tk.getCurrToken().equals("else")) {
            check("else");
            check("{");
            compileStatements(); // statements (0 or more statement)
            check("}");
        }
        vmWriter.writePush(CONSTANT, 0); // todo: evaluate need for these two filler instructions
        vmWriter.writePop(TEMP, 0);
        vmWriter.writeLabel(endLabel);
    }

    /* 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws IOException {
        vmWriter.write("<while statement>");
        String whileLabel = "L" + labelCounter;
        String endLabel = "L" + (labelCounter + 1);
        labelCounter += 2;

        vmWriter.writeLabel(whileLabel);
        check("while");
        check("(");
        compileExpression();
        check(")");
        vmWriter.writeArithmetic(not); // negate resultant, which is currently on stack
        vmWriter.writeIf(endLabel); // jump to end if true

        check("{");
        compileStatements(); // statements (0 or more statement)
        check("}");

        vmWriter.writeGoto(whileLabel);
        vmWriter.writeLabel(endLabel);
    }

    /* 'do' subroutineCall ';' */
    private void compileDo() throws IOException {
        vmWriter.write("<do statement>");

        check("do");

        // subroutineCall
        String token = tk.getCurrToken();
        tk.advance();
        String nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken);

        check(";");
        vmWriter.writePush(TEMP, 0);
    }

    /* 'return' expression? ';' */
    private void compileReturn() throws IOException {
        vmWriter.write("  <return statement>");

        check("return");

        // expression?
        String token = tk.getCurrToken();
        if (!token.equals(";")) {
            compileExpression();
        }
        check(";");
        vmWriter.writeReturn();
    }

    /* term (op term)* */
    private void compileExpression() throws IOException {
        compileTerm();

        // 0 or more (op term)
        String token = tk.getCurrToken();
        while (OP.contains(token)) {
            String op = token; // store the current op
            tk.advance();

            compileTerm();
            token = tk.getCurrToken(); // Prepare next token

            if (op.equals("*")) { // write the VM equivalent of the current op
                vmWriter.writeCall("Math.multiply", 2);
            } else if (op.equals("/")) {
                vmWriter.writeCall("Math.divide", 2);
            } else {
                vmWriter.writeArithmetic(OP_TO_COMMAND.get(op));
            }
        }
    }

    /* integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
    '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm() throws IOException {
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();

        if (type == integerConstant) {
            // VM: push the int
            vmWriter.writePush(CONSTANT, Integer.parseInt(token));
            tk.advance();
        }
        else if (type == stringConstant) {
            // VM: call String constructor, then initialize the new object with the String chars
            // by generating a sequence of calls to the String method appendChar, one for each char
            vmWriter.writePush(CONSTANT, token.length());
            vmWriter.writeCall("String.new", 1);
            for (int i = 0; i < token.length(); i++) {
                vmWriter.writePush(CONSTANT, token.charAt(i));
                vmWriter.writeCall("String.appendChar", 1);
            }
            tk.advance();
        }
        else if (KEYWORD_CONST.contains(token)) { // true, false, null, this
            switch (token) {
                case "false", "null" -> vmWriter.writePush(CONSTANT, 0);
                case "this" -> vmWriter.writePush(POINTER, 0);
                case "true" ->  {
                    vmWriter.writePush(CONSTANT, 1);
                    vmWriter.writeArithmetic(neg);
                }
            }
            tk.advance();
        }
        else if (token.equals("(")) { // '(' expression ')'
            check("(");
            compileExpression();
            check(")");
        }
        else if (UNARY_OP.contains(token)) { // (unaryOp term)
            String op = token;
            tk.advance();

            // VM: push exp; unary op
            compileTerm();
            vmWriter.writeArithmetic(UNARY_OP_TO_COMMAND.get(op));
        }
        else if (type == identifier) { // varName | varName '[' expression ']' | subroutineCall
            // Need to peek ahead to determine next steps
            tk.advance();

            // if next token is '(' or '.', must be subroutineCall
            // else, must be varName | varName '[' expression ']'
            String nextToken = tk.getCurrToken();
            if (nextToken.equals("(") || nextToken.equals(".")) { // subroutine call
                compileSubroutineCall(token, nextToken);
            }
            else if (nextToken.equals("[")) { // second token is '[' -> varName '[' expression ']'
                // todo: maybe remove duplicate
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(token));
                vmWriter.writePush(segment, subSymTable.indexOf(token));

                check("[");
                compileExpression();
                check("]");
            }
            else { // varName
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(token));
                vmWriter.writePush(segment, subSymTable.indexOf(token));
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
        String subroutineName;

        // subroutineName '(' expressionList ')'
        // ex: g(2, y, -5, -z)
        if (nextToken.equals("(")) {
            subroutineName = className + "." + token;
            check("(");
            int nArgs = compileExpressionList(); // args should be pushed on stack
            check(")");

            vmWriter.writeCall(subroutineName, nArgs);
        }
        // (className | varName) '.' subroutineName '(' expressionList ')'
        else if (nextToken.equals(".")) {

            if (subSymTable.contains(token)) { // is varName
                printSymTableData(token, "used");
            } else { // is className
                writer.println(token + " class used");
            }
            check(".");

            // subroutineName
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!type.equals(identifier)) { throwRuntimeException("subroutineName", identifier, token, type); }
            tk.advance();

            check("(");
            int nArgs = compileExpressionList();
            check(")");

            // todo

        }
    }

    /* ( expression (',' expression)* )? */
    private int compileExpressionList() throws IOException {
        // Immediately return if closing bracket (no expressions)
        String token = tk.getCurrToken();
        if (token.equals(")")) {
            return 0;
        }
        compileExpression();
        int numExpressions = 1;

        // 0 or more (',' expression)
        token = tk.getCurrToken();
        while (token.equals(",")) {
            check(",");
            compileExpression();
            numExpressions += 1;

            // Prepare next token
            token = tk.getCurrToken();
        }
        return numExpressions;
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