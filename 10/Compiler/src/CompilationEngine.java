import java.io.*;

/* The CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private Tokenizer tk;
    private PrintWriter writer;


    /* Build the list of output files */
    CompilationEngine(String source) throws IOException {
        tk = new Tokenizer(source);
        String prefix = source.substring(0, source.length() - 5);
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(prefix + ".xml")));
    }

    /** Analyze the grammar of the source file and output a structured representation to an XML file */
    public void printToXML() throws IOException {
        tk.advance();
        Tokenizer.TokenType type = tk.getTokenType();


    }
    private void compile() throws IOException {
        tk.advance();
    }

    private void compileClass() {

    }
}
