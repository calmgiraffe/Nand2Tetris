
import java.io.IOException;

public class Main {

    // Main = syntax analyzer
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 1) {
            System.out.println("Must have only one argument.");
        } else {
            // source is either a filename or the name of a folder
            Tokenizer tk = new Tokenizer(args[0]);

            // arg[0] is passed to compilationengine, and arg[0] can either be a directory or jack file
            //
            // CompilationEngine determines whether arg is file or directory
            // if directly, builds a list of output files

        }
    }

}
