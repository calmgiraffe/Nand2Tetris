import Core.CompilationEngine;
import org.junit.Test;

import java.io.IOException;

public class UnitTestVMWriter {
    @Test
    public void simpleExpressionTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/VMWriterTests/SimpleExpression.jack");
        ce.compile();
    }
}
