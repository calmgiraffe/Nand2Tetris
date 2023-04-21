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
}
