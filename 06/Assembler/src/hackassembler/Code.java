package hackassembler;

import java.util.HashMap;
import java.util.Map;

/*
Class to translate the fields (symbolic mnemonics) into binary codes.
 */
public class Code {
    public static final int MIN_INT = 0;
    public static final int MAX_INT = 32767;
    public static final Map<String, String> compCodes = new HashMap<>(){
        {
            put("0","0101010");
            put("1","0111111");
            put("-1","0111010");
            put("D", "0001100");
            put("A","0110000");
            put("M","1110000");
            put("!D","0001101");
            put("!A","0110001");
            put("!M","1110001");
            put("-D","0001111");
            put("-A","0110011");
            put("-M","1110011");
            put("D+1","0011111");
            put("A+1","0110111");
            put("M+1","1110111");
            put("D-1","0001110");
            put("A-1","0110010");
            put("M-1","1110010");
            put("D+A","0000010");
            put("D+M","1000010");
            put("D-A","0010011");
            put("D-M","1010011");
            put("A-D","0000111");
            put("M-D","1000111");
            put("D&A","0000000");
            put("D&M","1000000");
            put("D|A","0010101");
            put("D|M","1010101");
        }
    };
    public static final Map<String, String> destCodes = new HashMap<>(){
        {
            put("M","001");
            put("D","010");
            put("DM","011");
            put("MD","011");
            put("A", "100");
            put("AM","101");
            put("MA","101");
            put("AD","110");
            put("DA","110");
            put("ADM","111");
            put("AMD","111");
            put("DAM","111");
            put("DMA","111");
            put("MAD","111");
            put("MDA","111");
        }
    };
    public static final Map<String, String> jumpCodes = new HashMap<>(){
        {
            put("JGT","001");
            put("JEQ","010");
            put("JGE","011");
            put("JLT", "100");
            put("JNE","101");
            put("JLE","110");
            put("JMP","111");
        }
    };

    /**
     * Converts an integer to its binary value.
     */
    public static String intToBinary(int num) {
        if (num < MIN_INT || num > MAX_INT) {
            throw new IllegalArgumentException("Number must be between 0 and 32767");
        }
        return Integer.toBinaryString(num);
    }

    public static String generateAInstruct(int num) {
        String lower = intToBinary(num);
        StringBuilder upper = new StringBuilder();
        int upperLen = 16 - lower.length();

        upper.append("0".repeat(Math.max(0, upperLen)));
        upper.append(lower);
        return upper.toString();
    }

    public static String generateCInstruct(String dest, String comp, String jump) {
        String d;
        String j;

        // Get dest portion, if it exists
        if (dest == null) {
            d = "000";
        } else {
            d = destCodes.get(dest);
        }
        // Get comp portion
        String c = compCodes.get(comp);

        // Get jump portion, if it exists
        if (jump == null) {
            j = "000";
        } else {
            j = jumpCodes.get(jump);
        }
        return "111" + c + d + j;
    }
}
