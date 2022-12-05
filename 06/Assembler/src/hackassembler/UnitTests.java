package hackassembler;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;

public class UnitTests {

    @Test
    public void testSymbolTable() throws IOException {
        Parser p = new Parser("Fill.asm");
        p.makeSymbolTable();
        System.out.println(p.symbolTable);

        p.bufferedReader.close();
    }

    @Test
    public void testInstructionType() throws IOException {
        Parser p = new Parser("Fill.asm");
        p.advance();

        while (p.hasMoreLines()) {
            System.out.println(p.currInstruction + "    " +
                    Parser.type(p.currInstruction));
            p.advance();
        }
        p.bufferedReader.close();
    }

    @Test
    public void testIntToBinary() {
        Assert.assertEquals("1", Code.intToBinary(1));
        Assert.assertEquals("10000", Code.intToBinary(16));
        Assert.assertEquals("111111111111111", Code.intToBinary(32767));
    }

    @Test
    public void testGenAInstruct() {
        Random r = new Random();
        for (int i = 1; i < 10000; i += 1) {
            int randNum = r.nextInt(1, 32767 + 1);
            Assert.assertEquals(randNum, Integer.parseInt(Code.generateAInstruct(randNum), 2));
        }
    }

    @Test
    public void testAssemble() throws IOException {
        Parser p = new Parser("Fill.asm");
        p.makeSymbolTable();
        p.assemble();
    }
}
