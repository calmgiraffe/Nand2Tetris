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
    enum SubroutineType {
        METHOD, CONSTRUCTOR, FUNCTION
    }
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
    private final SymbolTable classSymTable;
    private SymbolTable subSymTable;
    private String className;
    private int labelCounter = 0;

    /** Build the list of output files */
    public CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        vmWriter = new VMWriter(prefix);
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + "_SymTable.txt")));
        classSymTable = new SymbolTable(writer);
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void compile() throws IOException {
        compileClass();
        tk.close();
        writer.close();
        vmWriter.close();
    }

    /** Method for compiling a class, ex., class Main {...} <p>
     * Def: "class" className '{' classVarDec* subroutineDec* ')' */
    private void compileClass() throws IOException {
        /* 'class' keyword */
        check("class");

        /* className identifier */
        className = tk.getCurrToken(); // all subroutines have an implicit instance of this type
        verifyType(identifier);

        check("{");
        compileClassVarDec(); // classVarDec*
        classSymTable.printSymbolTable(className); // todo: preprocessor directive to enable/disable this

        compileSubroutineDec(); // subroutineDec*
        check("}");
    }

    /** Method for compiling static or field (non-static) variables of object. Consists only of
     * class symbol table entry adding. <p>
     * Def: ('static' | 'field') type varName (',' varName)* ';' <p>
     * ex., field int x, y, z; <br> */
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
        verifyType(identifier);

        /* Zero or more (',' varName) */
        while (tk.getCurrToken().equals(",")) {
            check(",");

            /* varName */
            // Add to class symbol table - ('static' | 'field') (primitive | className)
            classSymTable.define(tk.getCurrToken(), dataTypeOf, scopeOf);
            verifyType(identifier);
        }
        check(";");

        // 0 or more classVarDec in class -> recursive call
        compileClassVarDec();
    }

    /** Method for compiling object routines like constructor, static/non-static methods. <p> Def:
     * ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')'
     * subroutineBody <p>
     * ex., method void draw(int x, int y) */
    private void compileSubroutineDec() throws IOException {
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
        // This is the only time a new subroutine symbol table is made; link class symbol table
        subSymTable = new SymbolTable(writer, classSymTable);
        tk.advance();

        /* type: void, primitive type, or identifier */
        String token = tk.getCurrToken();
        TokenType type = tk.getCurrType();
        if (!token.equals("void") && !PRIMITIVES.contains(token) && type != identifier) {
            throwRuntimeException("'void', primitive type, or className");
        }
        tk.advance();

        /* subroutineName */
        String subroutineName = className + "." + tk.getCurrToken();
        verifyType(identifier);

        /* '(' parameterList ')' -> compiling the arguments of subroutine */
        check("(");
        if (subType == SubroutineType.METHOD) {
            // Add 'this' to subroutine symbol table, if subroutine is method
            // According to method call contract, arg 0 is current object
            subSymTable.define("this", className, Scope.ARG);
        }
        compileParameterList();
        check(")");

        // '{' varDec* statement* '}' -> declare local vars and execute statements
        compileSubroutineBody(subroutineName, subType);
        subSymTable.printSymbolTable(subroutineName);

        // 0 or more subroutineDec in class -> recursive call
        compileSubroutineDec();
    }

    /** Method for compiling 0 or 1 parameterList within subroutineDec i.e., a method for compiling
     * the arguments of a subroutine. <p> Def: ((type varName) (',' type varName)*)? <br>
     * ex., int x, int y, boolean flag */
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
        verifyType(identifier);

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
            verifyType(identifier);
        }
    }

    /** Method for compiling a subroutine body, the local variable declarations and statements. <br>
     * Def: { varDec* statement* } */
    private void compileSubroutineBody(String subroutineName, SubroutineType subType) throws IOException {
        check("{");
        compileVarDec(); // 0 or more varDec -> sub symbol table filled with vars

        // VM code: function functionName nVars
        // nVars = number of local variables
        int nVars = subSymTable.varCount(Scope.VAR);
        vmWriter.writeFunction(subroutineName, nVars);

        if (subType == SubroutineType.CONSTRUCTOR) {
            // Allocate enough words for object instance variables.
            // The allocated size is equal to the number of field variables of current class.
            int objectSize = classSymTable.varCount(Scope.FIELD);
            vmWriter.writePush(CONSTANT, objectSize);

            // Memory.alloc pushes base address to stack, which is then popped to POINTER 0
            // Now, this segment is properly aligned with newly created object
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(POINTER, 0);
        }
        else if (subType == SubroutineType.METHOD) {
            // For methods, push the first argument (this) to the stack and pop it to pointer 0.
            vmWriter.writePush(ARGUMENT, 0);
            vmWriter.writePop(POINTER, 0);
        }
        compileStatements(); // statements (0 or more statement)
        check("}");
    }

    /** Method for compiling variable declarations within a subroutine. <p>
     * Def: 'var' type varName (',' varName)* ';' */
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
        verifyType(identifier);

        /* (',' varName)* */
        while (tk.getCurrToken().equals(",")) {
            check(",");

            /* varName identifier */
            // Add var to subroutine symbol table
            subSymTable.define(tk.getCurrToken(), dataType, Scope.VAR);
            verifyType(identifier);
        }
        check(";");

        // 0 or more varDec in subroutineDec -> recursive call
        compileVarDec();
    }

    /** Compile a series of let, if, while, do, or return statements. */
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

    /** Compile a let statement. Also handles case where LHS is an array, such as a[1]. <p>
     * Def: 'let' varName ('[' expression ']')? '=' expression ';' */
    private void compileLet() throws IOException {
        vmWriter.write("// let statement");
        check("let");

        /* varName identifier: save a copy of it */
        String varName = tk.getCurrToken();
        verifyType(identifier);

        /* ( '[' expression ']' )? */
        boolean leftIsArray = false;
        if (!tk.getCurrToken().equals("=")) {
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
            vmWriter.writePop(TEMP, 0); // push RHS to temporary storage
            vmWriter.writePop(POINTER, 1); // set THAT to point to array index
            vmWriter.writePush(TEMP, 0); //  push RHS back to stack
            vmWriter.writePop(THAT, 0); // remove from stack and write to the specified array index
        }
        else {
            vmWriter.writePop(
                    scopeToSegment.get(subSymTable.scopeOf(varName)),
                    subSymTable.indexOf(varName)
            );
        }
    }

    /** Compile an if statement. <p>
     * Def: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')? */
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
        // Todo: evaluate need for these two filler instructions, eventually test on user made VM emulator
        vmWriter.writePush(CONSTANT, 0);
        vmWriter.writePop(TEMP, 0);
        vmWriter.writeLabel(endLabel); // label END
    }

    /** Compile a while statement. <p> Def: 'while' '(' expression ')' '{' statements '}' */
    private void compileWhile() throws IOException {
        vmWriter.write("// while statement");

        // Set labels: one each for while and end
        String whileLabel = "L" + labelCounter;
        String endLabel = "L" + (labelCounter + 1);
        labelCounter += 2;

        vmWriter.writeLabel(whileLabel); // label A
        check("while");
        check("(");
        compileExpression(); // Should be a boolean statement
        check(")");
        vmWriter.writeArithmetic(not); // negate resultant, which is currently on stack
        vmWriter.writeIf(endLabel); // goto B if true -> skip compileStatements() block

        check("{");
        compileStatements(); // 0 or more statement
        check("}");

        vmWriter.writeGoto(whileLabel); // goto A -> return to expression
        vmWriter.writeLabel(endLabel); // label B
    }

    /** Compile a do statement. <p> Def: 'do' subroutineCall ';' */
    private void compileDo() throws IOException {
        vmWriter.write("// do statement");
        check("do");

        /* subroutineCall */
        String token = tk.getCurrToken();
        tk.advance();
        String nextToken = tk.getCurrToken();
        compileSubroutineCall(token, nextToken); // resultant placed on stack

        check(";");

        // VM: no return value, get rid of resultant
        vmWriter.writePop(TEMP, 0);
    }

    /** Compile a return statement. <p> Def: 'return' expression? ';' */
    private void compileReturn() throws IOException {
        vmWriter.write("// return statement");
        check("return");

        /* expression? */
        if (!tk.getCurrToken().equals(";")) {
            compileExpression();
        }
        else { // no expression -> void method/function. Simply push null value and return this
            vmWriter.writePush(CONSTANT, 0);
        }
        check(";");

        // VM code: return
        vmWriter.writeReturn();
    }

    /** Compile an expression. Def: term (op term)* */
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

    /** Compile a term. <p> Def: integerConstant | stringConstant | keywordConstant | varName |
     varName '[' expression ']' | '(' expression ')' | (unaryOp term) | subroutineCall */
    private void compileTerm() throws IOException {
        String currToken = tk.getCurrToken(); TokenType type = tk.getCurrType();

        if (type == integerConstant) {
            // VM: push the int to stack
            vmWriter.writePush(CONSTANT, Integer.parseInt(currToken));
            tk.advance();
        }
        else if (type == stringConstant) {
            // VM: call String constructor, then initialize the new object with the String chars
            // by generating a sequence of calls to the String method appendChar, one for each char
            vmWriter.writePush(CONSTANT, currToken.length());
            vmWriter.writeCall("String.new", 1);

            for (int i = 0; i < currToken.length(); i++) {
                vmWriter.writePush(CONSTANT, currToken.charAt(i));
                vmWriter.writeCall("String.appendChar", 2);
            }
            tk.advance();
        }
        else if (KEYWORD_CONST.contains(currToken)) { // true, false, null, this
            switch (currToken) {
                case "false", "null" -> vmWriter.writePush(CONSTANT, 0);
                case "this" -> vmWriter.writePush(POINTER, 0);
                case "true" ->  {
                    vmWriter.writePush(CONSTANT, 1);
                    vmWriter.writeArithmetic(neg);
                }
            }
            tk.advance();
        }
        else if (currToken.equals("(")) { // '(' expression ')'
            check("(");
            compileExpression();
            check(")");
        }
        else if (UNARY_OP.contains(currToken)) { // (unaryOp term)
            String unaryOp = currToken;
            tk.advance();

            // VM: push exp; unary op
            compileTerm();
            vmWriter.writeArithmetic(UNARY_OP_TO_COMMAND.get(unaryOp));
        }
        else if (type == identifier) { // varName | varName '[' expression ']' | subroutineCall
            // Peek ahead to determine next steps
            tk.advance();

            String nextToken = tk.getCurrToken();
            if (nextToken.equals("(") || nextToken.equals(".")) {
                // if next token is '(' or '.', must be a subroutine call
                compileSubroutineCall(currToken, nextToken);
            }
            else if (nextToken.equals("[")) {
                // If next token is '[', must be varName '[' expression ']'
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(currToken));
                vmWriter.writePush(segment, subSymTable.indexOf(currToken));

                check("[");
                compileExpression();
                check("]");

                vmWriter.writeArithmetic(add);
                vmWriter.writePop(POINTER, 1);
                vmWriter.writePush(THAT, 0);
            }
            else {
                // else, must be varName
                Segment segment = scopeToSegment.get(subSymTable.scopeOf(currToken));
                vmWriter.writePush(segment, subSymTable.indexOf(currToken));
            }
        }
        else {
            throwRuntimeException("integerConstant, stringConstant, keywordConstant, " +
                    "varName, '(', unaryOp, subroutineCall");
        }
    }

    /** Compiles a subroutine, used by compileDo and compileTerm. <p> Def: subroutineName
     * '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')' */
    private void compileSubroutineCall(String currToken, String nextToken) throws IOException {
        /*
        Def: subroutineName '(' expressionList ')'
        Has to be method operating on current object, i.e., 'this'
        ex: g(2, y, -5, -z)
        */
        if (nextToken.equals("(")) {
            // VM: push 'this' onto stack
            String subroutineName = className + "." + currToken;
            vmWriter.writePush(POINTER, 0);

            /* '(' expressionList ')' */
            // The resultants of compileExpressionList() are pushed on stack.
            // The expression list is the inputted arguments of the subroutine.
            check("(");
            int nArgs = compileExpressionList();
            check(")");

            // VM: after all args are pushed on stack, call function
            vmWriter.writeCall(subroutineName, nArgs + 1);
        }
        /*
        Def: (className | varName) '.' subroutineName '(' expressionList ')'
        Can either be method, function, or constructor.
        A constructor is a special type of function.
        ex: Memory.deAlloc(this), p1.distance(p2)
        */
        else if (nextToken.equals(".")) {
            // If in symbol table, must be varName
            boolean isVarName = subSymTable.contains(currToken);
            String calleeClassName;

            /* (varName | className) */
            if (isVarName) {
                // VM: push varName onto stack
                Segment memSegment = scopeToSegment.get(subSymTable.scopeOf(currToken));
                vmWriter.writePush(memSegment, subSymTable.indexOf(currToken));
                calleeClassName = subSymTable.dataTypeOf(currToken);
            }
            else {
                // VM: function (i.e., static method), save its name
                calleeClassName = currToken;
            }
            check(".");

            /* subroutineName */
            currToken = tk.getCurrToken();
            String subroutineName = calleeClassName + "." + currToken;
            verifyType(identifier);

            /* '(' expressionList ')' */
            // The resultants of compileExpressionList() are pushed on stack.
            // The expression list is the inputted arguments of the subroutine.
            check("(");
            int nArgs = compileExpressionList();
            check(")");

            // VM: after all args are pushed on stack, call function
            // if isVarName, need extra arg for the varName object
            vmWriter.writeCall(subroutineName, nArgs + ((isVarName) ? 1 : 0));
        }
    }

    /** Compile an expression list. Only found when inputting arguments into a subroutine. <p>
     * Def: ( expression (',' expression)* )? */
    private int compileExpressionList() throws IOException {
        // Immediately return if closing bracket (no expressions)
        if (tk.getCurrToken().equals(")")) {
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

    /** Helper method for checking for specific strings; throws exception if mismatch. */
    private void throwRuntimeException(String expected) throws IOException {
        String actual = tk.getCurrToken();
        TokenType actualType = tk.getCurrType();

        tk.close();
        writer.close();
        String actualStr = actual + " " + actualType;
        throw new RuntimeException("Expected " + expected + " but found " + actualStr);
    }

    /** Helper method for writing specific symbols and keywords.
     * Also advances the tokenizer by one. */
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

    /** Helper method for checking if a token is the correct TokenType
     * Like check, advances the tokenizer by one. */
    private void verifyType(TokenType expectedType) throws IOException {
        String token = tk.getCurrToken();
        TokenType currType = tk.getCurrType();
        if (currType != expectedType) {
            throw new RuntimeException("Expected " + expectedType + " but found " + token + " " + currType);
        }
        tk.advance();
    }
}