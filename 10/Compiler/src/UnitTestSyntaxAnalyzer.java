import Core.CompilationEngine;
import org.junit.Test;

import java.io.IOException;

public class UnitTestSyntaxAnalyzer {
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

    @Test
    public void squareTest() throws IOException {
        CompilationEngine ce1 = new CompilationEngine("./src/TestFiles/Square/Main.jack");
        CompilationEngine ce2 = new CompilationEngine("./src/TestFiles/Square/Square.jack");
        CompilationEngine ce3 = new CompilationEngine("./src/TestFiles/Square/SquareGame.jack");
        ce1.compile();
        ce2.compile();
        ce3.compile();
    }
}
