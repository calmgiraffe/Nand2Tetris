import Core.CompilationEngine;
import org.junit.Test;

import java.io.IOException;

public class UnitTestCompilationEngine {
    @Test
    public void simpleClassTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/CompilationEngineTests/simpleClass.jack");
        ce.compile();
    }

    @Test
    public void classVarDecTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/CompilationEngineTests/classVarDec.jack");
        ce.compile();
    }

    @Test
    public void subroutineDecAndParameterListTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/CompilationEngineTests/subroutineDec.jack");
        ce.compile();
    }

    @Test
    public void varDecTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/CompilationEngineTests/varDec.jack");
        ce.compile();
    }

    @Test
    public void arrayTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/ArrayTest/Main.jack");
        ce.compile();
    }

    @Test
    public void expressionlessSquareTest() throws IOException {
        CompilationEngine ce1 = new CompilationEngine("./src/TestFiles/ExpressionLessSquare/Main.jack");
        CompilationEngine ce2 = new CompilationEngine("./src/TestFiles/ExpressionLessSquare/Square.jack");
        CompilationEngine ce3 = new CompilationEngine("./src/TestFiles/ExpressionLessSquare/SquareGame.jack");
        ce1.compile();
        ce2.compile();
        ce3.compile();
    }
}
