package Core;


public class VMWriter {
    public enum Segment {
        CONSTANT, ARGUMENT, LOCAL, STATIC, THIS, THAT, POINTER, TEMP
    }
    public enum Command {
        add, sub, neg, eq, gt, lt, and, or, not
    }

    /** Writes a VM push command */
    public void writePush(Segment segment, int index) {

    }

    /** Writes a VM pop command */
    public void writePop(Segment segment, int index) {

    }

    /** Writes a VM arithmetic-logical command */
    public void writeArithmetic(Command command) {

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
}
