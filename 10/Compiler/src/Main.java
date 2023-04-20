import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 1) {
            System.out.println("Must have only one argument that is either a .jack file or directory.");
            exit(-1);
        }
        ArrayDeque<String> sourceFiles = new ArrayDeque<>();
        if (args[0].endsWith(".jack")) { // If the argument is a .jack file, add it to the deque
            sourceFiles.add(args[0]);

        } else { // If the argument is a directory, add all .jack files in the directory to the deque
            File directory = new File(args[0]);
            FilenameFilter jackFilter = (dir, name) -> name.toLowerCase().endsWith(".jack");
            sourceFiles.addAll(List.of(Objects.requireNonNull(directory.list(jackFilter))));
        }
        /* Iterate through sourceFiles and compile */
        for (String file : sourceFiles) {
            CompilationEngine compiler = new CompilationEngine(file);
        }
    }
}
