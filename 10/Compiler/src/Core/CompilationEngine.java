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
    // OK
    private void compileClass() throws IOException {
        /* 'class' keyword */
        check("class");

        /* className identifier */
        className = tk.getCurrToken(); // all subroutines have an implicit instance of this type
        checkType(identifier);

        check("{");
        compileClassVarDec(); // classVarDec*
        compileSubroutineDec(); // subroutineDec*
        check("}");
    }

    /* Method for compiling static or field (non-static) variables of object.
    ('static' | 'field') type varName (',' varName)* ';'
    ex., field int x, y, z
    Consists only of symbol table entry adding */
    // OK
    private void compileClassVarDec() throws IOException {
        Scope scopeOf;
        String token = tk.getCurrToken();

        /* ("static" | "field") */
        if (token.equals("static")) {
            scopeOf = Scope.STATIC;
        } else if (token.equals("field")){
            scopeOf = Scope.FIELD;
        } else {
            return; // Immediately return if neither
        }
        tk.advance();

        /* type: primitive or className */
        token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("primitive type or className");
        }
        String dataTypeOf = token; // all class var decs are of this type
        tk.advance();

        /* varName */
        // Add to class symbol table - ('static' | 'field') (primitive | className)
        classSymTable.define(tk.getCurrToken(), dataTypeOf, scopeOf);
        checkType(identifier);

        /* Zero or more (',' varName) */
        while (tk.getCurrToken().equals(",")) {
            check(",");

            /* varName */
            // Add to class symbol table - ('static' | 'field') (primitive | className)
            classSymTable.define(tk.getCurrToken(), dataTypeOf, scopeOf);
            checkType(identifier);
        }
        check(";");

        // 0 or more classVarDec in class -> recursive call
        compileClassVarDec();
    }

    /* Method for compiling object routines like constructor, static/non-static methods
    ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
    ex., method void draw(int x, int y) */
    private void compileSubroutineDec() throws IOException {
        enum SubroutineType {
            METHOD, CONSTRUCTOR, FUNCTION
        }
        /* ('constructor' | 'function' | 'method') */
        SubroutineType subType;
        switch (tk.getCurrToken()) {
            case "method":
                subType = SubroutineType.METHOD;
                break;
            case "constructor":
                subType = SubroutineType.CONSTRUCTOR;
                break;
            case "function":
                subType = SubroutineType.FUNCTION;
                break;
            default: // Immediately return if not one of the three
                return;
        }
        // Link class symbol table with subroutine symbol table
        subSymTable = new SymbolTable(classSymTable);
        tk.advance();

        /* type: void, primitive type, or identifier */
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("'void', primitive type, or className");
        }
        tk.advance();

        /* subroutineName */
        token = tk.getCurrToken(); type = tk.getCurrType();
        if (type != identifier) {
            // Check if the current token type is an identifier
            throwRuntimeException("subroutineName identifier");
        }


        if (subType == SubroutineType.CONSTRUCTOR) {

        }
        else if (subType == SubroutineType.METHOD) {
            // Add 'this' to subroutine symbol table, if subroutine is method
            subSymTable.define("this", className, Scope.ARG);
        }
        String subroutineName = className + '.' + token;
        tk.advance();

        check("(");
        compileParameterList(); // vars are added to subroutine table
        check(")");

        // VM code: function functionName nVars
        int nVars = subSymTable.varCount(Scope.VAR);
        vmWriter.writeFunction(subroutineName, nVars);

        if (subType == SubroutineType.CONSTRUCTOR) {
            // Allocate enough words for object instance variables.
            // The allocated size is equal to the number of field variables.
            int objectSize = classSymTable.varCount(Scope.FIELD);
            vmWriter.writePush(CONSTANT, objectSize);

            // Memory.alloc pushes base address to stack
            vmWriter.writeCall("Memory.alloc", 1);
        }
        else if (subType == SubroutineType.METHOD) {
            // For methods, push the first argument (this) to the stack and pop it to pointer 0.
            vmWriter.writePush(ARGUMENT, 0);
        }
        // Pops the base address to pointer 0.
        vmWriter.writePop(POINTER, 0);

        // '{' varDec* statement* '}' -> goes on to fill in allocated memory and execute statements
        compileSubroutineBody();

        // 0 or more subroutineDec in class -> recursive call
        compileSubroutineDec();
    }

    /* Method for compiling 0 or 1 parameterList within subroutineDec,
    i.e., a method for compiling the arguments of a subroutine.
    Def: ((type varName) (',' type varName)*)?
    ex., int x, int y, boolean flag */
    // OK
    private void compileParameterList() throws IOException {
        /* Immediately return if not type -> 0 parameterList */
        String token = tk.getCurrToken();
        TokenType type = tk.getCurrType();
        if (!PRIMITIVES.contains(token) && type != identifier) {
            return;
        }
        /* type: primitive or className */
        String dataType = token;
        tk.advance();

        /* varName */
        // Add first arg to subroutine symbol table
        subSymTable.define(tk.getCurrToken(), dataType, Scope.ARG);
        checkType(identifier);

        /* 0 or more (',' type varName) */
        while (tk.getCurrToken().equals(",")) {
            check(",");

            /* type: primitive or className */
            token = tk.getCurrToken(); type = tk.getCurrType();
            if (!PRIMITIVES.contains(token) && type != identifier) {
                throwRuntimeException("primitive type or className");
            }
            dataType = token;
            tk.advance();

            /* varName */
            // Add next args to subroutine symbol table
            subSymTable.define(tk.getCurrToken(), dataType, Scope.ARG);
            checkType(identifier);
        }
    }

    /* Method for compiling subroutine body, i.e., { varDec* statement* } */
    // OK
    private void compileSubroutineBody() throws IOException {
        check("{");
        compileVarDec(); // 0 or more varDec
        compileStatements(); // statements (0 or more statement)
        check("}");
    }

    /* Method for compiling var declarations within a subroutine.
    Def: 'var' type varName (',' varName)* ';' */
    private void compileVarDec() throws IOException {
        // Immediately return if not var
        if (!tk.getCurrToken().equals("var")) {
            return;
        }
        /* 'var' keyword */
        check("var");

        /* type: primitive tokenType or className */
        String dataType = tk.getCurrToken(); // all var decs are of this data type
        TokenType tokenType = tk.getCurrType();
        if (!PRIMITIVES.contains(dataType) && tokenType != identifier) {
            throwRuntimeException("primitive tokenType or className");
        }
        tk.advance();

        /* varName identifier */
        // Add var to subroutine symbol table
        subSymTable.define(tk.getCurrToken(), dataType, Scope.VAR);
        checkType(identifier);

        /* (',' varName)* */
        while (tk.getCurrToken().equals(",")) {
            check(",");

            /* varName identifier */
            // Add var to subroutine symbol table
            subSymTable.define(tk.getCurrToken(), dataType, Scope.VAR);
            checkType(identifier);
        }
        check(";");

        // 0 or more varDec in subroutineDec -> recursive call
        compileVarDec();
    }

    /* Compile a let, if, while, do, or return statement */
    // OK
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

    /* Def: 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws IOException {
        vmWriter.write("// let statement");
        check("let");

        /* varName identifier */
        String varName = tk.getCurrToken();
        checkType(identifier);

        /* 0 or 1 ('[' expression ']') */
        String token = tk.getCurrToken();
        boolean leftIsArray = false;

        if (!token.equals("=")) {
            // LHS is an array index -> push varName, compileExp, then add
            leftIsArray = true;
            vmWriter.writePush(
                    scopeToSegment.get(subSymTable.scopeOf(varName)),
                    subSymTable.indexOf(varName)
            );
            check("[");
            compileExpression();
            check("]");

            vmWriter.writeArithmetic(add);
        }
        // RHS: expression value is put on stack
        check("=");
        compileExpression();
        check(";");

        // VM code: LHS of let statement
        if (leftIsArray) {
            vmWriter.writePop(POINTER, 1);
            vmWriter.writePush(TEMP, 0);
            vmWriter.writePop(THAT, 0);
        }
        else {
            vmWriter.writePop(
                    scopeToSegment.get(subSymTable.scopeOf(varName)),
                    subSymTable.indexOf(varName)
            );
        }
    }

    /* 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
    private void compileIf() throws IOException {
        vmWriter.write("// if statement");

        // Define labels to jump to else block and end
        String elseLabel = "L" + labelCounter;
        String endLabel = "L" + (labelCounter + 1);
        labelCounter += 2;

        /* 'if' '(' expression ')' '{' statements '}' */
        check("if");
        check("(");
        compileExpression();
        check(")");
        vmWriter.writeArithmetic(not); // negate the resultant, which is at top of stack
        vmWriter.writeIf(elseLabel); // if-goto ELSE

        // Start of IF block
        check("{");
        compileStatements();
        check("}");
        vmWriter.writeGoto(endLabel); // goto END

        /* ('else' '{' statements '}')? */
        vmWriter.writeLabel(elseLabel); // label ELSE
        if (tk.getCurrToken().equals("else")) {
            check("else");
            check("{");
            compileStatements();
            check("}");
        }
        // Todo: evaluate need for these two filler instructions
        vmWriter.writePush(CONSTANT, 0);
        vmWriter.writePop(TEMP, 0);
        vmWriter.writeLabel(endLabel); // label END
    }

    /* 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws IOException {
        vmWriter.write("// while statement");

        // Set labels: one each for while and end
        String whileLabel = "L" + labelCounter;
        String endLabel = "L" + (labelCounter + 1);
        labelCounter += 2;

        vmWriter.writeLabel(whileLabel); // label A
        check("while");
        check("(");
        compileExpression();
        check(")");
        vmWriter.writeArithmetic(not); // negate resultant, which is currently on stack
        vmWriter.writeIf(endLabel); // goto B if true -> skip compileStatements() blk

        check("{");
        compileStatements(); // 0 or more statement
        check("}");

        vmWriter.writeGoto(whileLabel); // goto A -> return to expression
        vmWriter.writeLabel(endLabel); // label B
    }

    /* 'do' subroutineCall ';' */
    private void compileDo() throws IOException {
        vmWriter.write("// do statement");
        check("do");

        /* subroutineCall */
        String token = tk.getCurrToken();
        tk.advance();
        String nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken); // resultant placed on stack

        check(";");

        // VM: no return value, get rid of topmost value
        vmWriter.writePush(TEMP, 0);
    }

    /* 'return' expression? ';' */
    private void compileReturn() throws IOException {
        vmWriter.write("// return statement");
        check("return");

        /* expression? */
        if (!tk.getCurrToken().equals(";")) {
            compileExpression();
        }
        else { // no expression -> void method/function
            vmWriter.writePush(CONSTANT, 0);
        }
        check(";");

        // VM code: return
        vmWriter.writeReturn();
    }

    /* term (op term)* */
    private void compileExpression() throws IOException {
        /* term */
        compileTerm();

        /* 0 or more (op term) */
        String token = tk.getCurrToken();
        while (OP.contains(token)) {
            String op = token; // store the current op
            tk.advance();

            compileTerm();
            token = tk.getCurrToken(); // Prepare next token

            // VM code: write VM equivalent of current op
            switch (op) {
                case "*" -> vmWriter.writeCall("Math.multiply", 2);
                case "/" -> vmWriter.writeCall("Math.divide", 2);
                default -> vmWriter.writeArithmetic(OP_TO_COMMAND.get(op));
            }
        }
    }

    /* integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' |
    '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm() throws IOException {
        String token = tk.getCurrToken(); TokenType type = tk.getCurrType();

        if (type == integerConstant) {
            // VM: push the int to stack
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
            tk.advance();

            // VM: push exp; unary op
            compileTerm();
            vmWriter.writeArithmetic(UNARY_OP_TO_COMMAND.get(token));
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
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(token));
                vmWriter.writePush(segment, subSymTable.indexOf(token));

                check("[");
                compileExpression();
                check("]");

                vmWriter.writePop(TEMP, 0);
            }
            else { // varName
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(token));
                vmWriter.writePush(segment, subSymTable.indexOf(token));
            }
        }
        else {
            throwRuntimeException("integerConstant, stringConstant, keywordConstant, " +
                    "varName, '(', unaryOp, subroutineCall");
        }
    }

    /* Helper function for compileDo and compileTerm
    subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')' */
    private void compileSubroutineCall(String token, String nextToken) throws IOException {
        /*
        Def: subroutineName '(' expressionList ')'
        Has to be method operating on current object, i.e., 'this'
        ex: g(2, y, -5, -z) */
        if (nextToken.equals("(")) { // todo: review
            // VM: push 'this' onto stack
            String subroutineName = className + "." + token;
            vmWriter.writePush(POINTER, 0);

            // VM: resultants of compileExpressionList() are pushed on stack
            check("(");
            int nArgs = compileExpressionList();
            check(")");

            // VM: after all args are pushed on stack, call function
            vmWriter.writeCall(subroutineName, nArgs + 1);
        }
        /*
        Def: (className | varName) '.' subroutineName '(' expressionList ')'
        Can either be method, function, or constructor.
        A constructor is a special type of function
        ex: Memory.deAlloc(this), p1.distance(p2) */
        else if (nextToken.equals(".")) { // todo: review function calls
            boolean isClass = false;
            String calleeClassName = null;

            /* (varName | className) */
            if (subSymTable.contains(token)) {
                // varName -> VM: push varName onto stack
                Segment memSegment = scopeToSegment.get(subSymTable.scopeOf(token));
                vmWriter.writePush(memSegment, subSymTable.indexOf(token));
            }
            else {
                // className -> static subroutine, save its name
                isClass = true;
                calleeClassName = token;
            }
            check(".");

            /* subroutineName */
            token = tk.getCurrToken();
            String subroutineName = isClass ? calleeClassName + "." + token : subSymTable.dataTypeOf(token) + "." + token;
            checkType(identifier);

            // resultants of compileExpressionList() are pushed on stack
            check("(");
            int nArgs = compileExpressionList();
            check(")");

            // VM: after all args are pushed on stack, call function
            vmWriter.writeCall(subroutineName, nArgs + ((isClass) ? 0 : 1));
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
        while (tk.getCurrToken().equals(",")) {
            check(",");
            compileExpression();
            numExpressions += 1;
        }
        return numExpressions;
    }

    private void throwRuntimeException(String expected) throws IOException {
        String actual = tk.getCurrToken();
        TokenType actualType = tk.getCurrType();

        tk.close();
        writer.close();
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expected + " but found " + actualStr);
    }

    /* Helper method for writing specific symbols and keywords */
    private void check(String expected) throws IOException {
        String token = tk.getCurrToken();

        TokenType expectedType;
        if (Tokenizer.KEYWORDS.contains(expected)) {
            expectedType = keyword;
        } else {
            expectedType = symbol;
        }
        if (!token.equals(expected)) {
            throwRuntimeException("'" + expected + "' " + expectedType);
        }
        tk.advance();
    }

    private void checkType(TokenType expectedType) throws IOException {
        String token = tk.getCurrToken();
        TokenType currType = tk.getCurrType();
        if (currType != expectedType) {
            throw new RuntimeException("Expected " + expectedType + " but found " + token + " " + currType);
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