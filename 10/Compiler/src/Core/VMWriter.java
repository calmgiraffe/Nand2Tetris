package Core;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static Core.VMWriter.Arithmetic.*;
import static Core.VMWriter.Arithmetic.not;

public class VMWriter {
    public static Map<String, Arithmetic> OP_TO_COMMAND = new HashMap<>() {{
        put("+", add);
        put("-", sub);
        put("=", eq);
        put(">", lt);
        put("<", gt);
        put("|", or);
    }};
    public static Map<String, Arithmetic> UNARY_OP_TO_COMMAND = new HashMap<>() {{
        put("-", neg); // same as minus in OP_TO_COMMAND
        put("~", not);
    }};
    public enum Segment {
        CONSTANT, ARGUMENT, LOCAL, STATIC, THIS, THAT, POINTER, TEMP
    }
    public enum Arithmetic {
        add, sub, neg, eq, gt, lt, and, or, not
    }

    private final PrintWriter writer;

    VMWriter(String prefix) throws IOException {
        writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + "_output.vm")));
    }
    public void write(String string) {
        writer.println(string);
    }

    /** Writes a VM push command */
    public void writePush(Segment segment, int index) {
        writer.println("push " + segment.toString().toLowerCase() + " " + index);
    }

    /** Writes a VM pop command */
    public void writePop(Segment segment, int index) {
        writer.println("pop " + segment.toString().toLowerCase() + " " + index);
    }

    /** Writes a VM arithmetic-logical command */
    public void writeArithmetic(Arithmetic command) {
        writer.println(command);
    }

    /** Writes a VM label command */
    public void writeLabel(String label) {}

    /** Writes a VM goto command */
    public void writeGoto(String label) {}

    /** Writes a VM if-goto command */
    public void writeIf(String label) {}

    /** Writes a VM call command */
    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    /** Writes a VM function command */
    public void writeFunction(String name, int nArgs) {}

    /** Writes a VM return command */
    public void writeReturn() {
        writer.println("return");
    }

    public void close() {
        writer.close();
    }
}
