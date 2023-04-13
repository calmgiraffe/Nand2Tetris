
import java.io.IOException;

public class Main {

    // Main = syntax analyzer
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.out.println("Must have at least one argument.");
        } else {
            // JackAnalyzer source
            // source is either a filename or the name of a folder
            for (String arg : args) {
                Tokenizer tk = new Tokenizer(arg);
                tk.advance();
            }
        }
    }

}
