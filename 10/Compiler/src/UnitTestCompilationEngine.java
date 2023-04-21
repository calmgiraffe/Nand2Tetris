import Core.CompilationEngine;
import org.junit.Test;

import java.io.IOException;

public class UnitTestCompilationEngine {
    @Test
    public void basicClassTest() throws IOException {
        CompilationEngine ce = new CompilationEngine("./src/TestFiles/CompilationEngineTests/mainTest1.jack");
        ce.compile();
    }
}
