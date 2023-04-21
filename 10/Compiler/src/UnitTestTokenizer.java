import Core.Tokenizer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UnitTestTokenizer {
    private static String[] outputStrings1 = new String[]{
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
    private static String[] outputStrings2 = new String[]{
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
    private static String[] outputStrings3 = new String[]{
            "class", "Main", "{",
            "static", "boolean", "flagred", "=", "false", ";",
            "static", "boolean", "flagblue", "=", "true", ";",
            "static", "boolean", "flag2", "=", "(", "1", "<", "2", ")", ";"
    };

    @Test
    public void testEmptyFile() throws IOException {
        Tokenizer tk1 = new Tokenizer("./src/TestFiles/TokenizerTests/empty.jack");
        tk1.printToXML();
        tk1.close();
    }

    @Test
    public void testTokenization() throws IOException {
        Tokenizer tk1 = new Tokenizer("./src/TestFiles/TokenizerTests/Test1.jack");
        Tokenizer tk2 = new Tokenizer("./src/TestFiles/TokenizerTests/Test2.jack");
        Tokenizer tk3 = new Tokenizer("./src/TestFiles/TokenizerTests/Test3.jack");
        Tokenizer tk4 = new Tokenizer("./src/TestFiles/TokenizerTests/Test4.jack");

        for (String str : outputStrings1) {
            //System.out.println(tk1.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk1.getCurrToken());
            tk1.advance();
        }
        for (String str : outputStrings2) {
            Assert.assertEquals("Output strings do not match", str, tk2.getCurrToken());
            Assert.assertEquals("Output strings do not match", str, tk4.getCurrToken());
            tk2.advance();
            tk4.advance();
        }
        for (String str : outputStrings3) {
            Assert.assertEquals("Output strings do not match", str, tk3.getCurrToken());
            tk3.advance();
        }
        tk1.close();
        tk2.close();
        tk3.close();
        tk4.close();
    }

    @Test
    public void XMLArrayTest() throws IOException {
        Tokenizer tk1 = new Tokenizer("./src/TestFiles/ArrayTest/Main.jack");
        tk1.printToXML();
        tk1.close();
    }

    @Test
    public void XMLExpressionLessSquare() throws IOException {
        Tokenizer tk1 = new Tokenizer("./src/TestFiles/ExpressionLessSquare/Main.jack");
        Tokenizer tk2 = new Tokenizer("./src/TestFiles/ExpressionLessSquare/Square.jack");
        Tokenizer tk3 = new Tokenizer("./src/TestFiles/ExpressionLessSquare/SquareGame.jack");
        tk1.printToXML();
        tk2.printToXML();
        tk3.printToXML();
        tk1.close();
        tk2.close();
        tk3.close();
    }

    @Test
    public void XMLSquare() throws IOException {
        Tokenizer tk1 = new Tokenizer("./src/TestFiles/Square/Main.jack");
        Tokenizer tk2 = new Tokenizer("./src/TestFiles/Square/Square.jack");
        Tokenizer tk3 = new Tokenizer("./src/TestFiles/Square/SquareGame.jack");
        tk1.printToXML();
        tk2.printToXML();
        tk3.printToXML();
        tk1.close();
        tk2.close();
        tk3.close();
    }
}
