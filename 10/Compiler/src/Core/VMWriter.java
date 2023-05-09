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
    public static Map<String, Arithmetic> opToCommand = new HashMap<>() {{
        put("+", add);
        put("-", sub);
        put("=", eq);
        put(">", lt);
        put("<", gt);
        put("|", or);
    }};
    public static Map<String, Arithmetic> unaryOpToCommand = new HashMap<>() {{
        put("-", neg);
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

    /** Writes a VM push command */
    public void writePush(Segment segment, String index) {
        writer.println("push " + segment.toString().toLowerCase() + " " + index);
    }

    /** Writes a VM pop command */
    public void writePop(Segment segment, String index) {

    }

    /** Writes a VM arithmetic-logical command */
    public void writeArithmetic(String command) {
        writer.println(opToCommand.get(command));
    }

    /** Writes a one of VM neg or not */
    public void writeUnaryOp(String command) {
        writer.println(unaryOpToCommand.get(command));
    }

    public void write(Arithmetic command) {

    }

    /** Writes a VM label command */
    public void writeLabel(String label) {

    }

    /** Writes a VM goto command */
    public void writeGoto(String label) {

    }

    /** Writes a VM if-goto command */
    public void writeIf(String label) {

    }

    /** Writes a VM call command */
    public void writeCall(String name, int nArgs) {

    }

    /** Writes a VM function command */
    public void writeFunction(String name, int nArgs) {

    }

    /** Writes a VM return command */
    public void writeReturn() {

    }

    public void close() {
        writer.close();
    }
}
