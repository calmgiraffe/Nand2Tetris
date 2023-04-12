import java.io.FileNotFoundException;

public class Main {
    // Main = syntax analyzer
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length == 0) {
            System.out.println("Must have at least one argument.");
        } else {
            // JackAnalyzer source
            // source is either a filename or the name of a folder
            for (String arg : args) {
                Tokenizer tokenizer = new Tokenizer(arg);

            }
        }
    }

}
