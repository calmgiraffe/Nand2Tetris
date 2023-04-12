import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UnitTest {

    @Test
    public void testTokenizer() throws IOException, InterruptedException {
        Tokenizer tk = new Tokenizer("./src/Test1.jack");

        String[] outputStrings1 = new String[]{
                "class", "Main", "{",
                "static", "boolean", "test", ";",
                "function", "void", "main", "(", ")", "{",
                "var", "SquareGame", "game", ";",
                "let", "game", "=", "SquareGame", ".", "new", "(", ")", ";",
                "do", "game", ".", "run", "(", ")", ";",
                "do", "game", ".", "dispose", "(", ")", ";",
                "return", ";",
                "}",
                "}"
        };
        for (String str : outputStrings1) {
            tk.advance();
            Assert.assertEquals("Output strings do not match", str, tk.getCurrToken());
        }
    }

}
