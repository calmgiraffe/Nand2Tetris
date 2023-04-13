import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UnitTest {

    @Test
    public void testTokenizer() throws IOException, InterruptedException {
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
        String[] outputStrings2 = new String[]{
                "class", "Main", "{",
                "function", "void", "more", "(", ")", "{",
                "var", "int", "i", ",", "j", ";",
                "var", "String", "s", ";",
                "var", "Array", "a", ";",
                "if", "(", "false", ")", "{",
                "let", "s", "=", "string constant", ";",
                "let", "s", "=", "null", ";",
                "let", "a", "[", "1", "]", "=", "a", "[", "2", "]", ";",
                "}", "else", "{",
                "let", "i", "=", "i", "*", "(", "-", "j", ")", ";",
                "let", "j", "=", "j", "/", "(", "-", "2", ")", ";",
                "let", "i", "=", "i", "|", "j", ";",
                "}",
                "return", ";",
                "}",
                "}"
        };
        String[] outputStrings3 = new String[]{
                "class", "Main", "{",
                "static", "boolean", "flagred", "=", "false", ";",
                "static", "boolean", "flagblue", "=", "true", ";",
                "static", "boolean", "flag2", "=", "(", "1", "<", "2", ")", ";"
        };
        Tokenizer tk1 = new Tokenizer("./src/Test1.jack");
        Tokenizer tk2 = new Tokenizer("./src/Test2.jack");
        Tokenizer tk3 = new Tokenizer("./src/Test3.jack");
        Tokenizer tk4 = new Tokenizer("./src/Test4.jack");

        for (String str : outputStrings1) {
            tk1.advance();
            //System.out.println(tk1.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk1.getCurrToken());
        }
        for (String str : outputStrings2) {
            tk2.advance();
            tk4.advance();
            //System.out.println(tk2.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk2.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk4.getCurrToken());
        }
        for (String str : outputStrings3) {
            tk3.advance();
            //System.out.println(tk2.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk3.getCurrToken());
        }
    }

}
