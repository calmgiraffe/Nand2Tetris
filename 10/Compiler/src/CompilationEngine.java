import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/* The CompilationEngine should grab tokens from the tokenizer one-by-one, analyze the grammar,
and emit a structured representation of the source code in a xml file */
public class CompilationEngine {
    private final ArrayDeque<String> outputFiles = new ArrayDeque<>();

    CompilationEngine(String source) {
        /* Build the list of output files */
        if (source.endsWith(".jack")) { // source is .jack file -> one xml file
            String prefix = source.substring(0, source.length() - ".jack".length());
            outputFiles.add(prefix + ".xml");
        }
        else { // source is file direction -> multiple xml files
            File directoryPath = new File("./" + source);
            FilenameFilter fileFilter = (dir, name) -> {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".jack");
            };
            outputFiles.addAll(List.of(Objects.requireNonNull(directoryPath.list(fileFilter))));
        }}
}
