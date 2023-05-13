package Core;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static Core.SymbolTable.Scope.*;

public class SymbolTable {
    private record Data(String dataType, Scope scope, int index) {}
    public enum Scope {
        STATIC, FIELD, ARG, VAR
    }
    public static HashSet<String> DATA_TYPES = new HashSet<>(Set.of("int","char","boolean"));

    /* Map ScopeType to cumulative index */
    private final Map<Scope, Integer> scopeToRunningIndex = new HashMap<>() {{
        put(STATIC, 0);
        put(FIELD, 0);
        put(ARG, 0);
        put(VAR, 0);
    }};
    /* name -> data type, scope, index
    Note: In Jack and in most programming languages, variables cannot share
    the same name even if they are different data types */
    private final Map<String, Data> nameToData = new HashMap<>();
    private SymbolTable nextTable;

    /** Default constructor */
    SymbolTable() {}

    /** Can add pointer to next symbol table to check */
    SymbolTable(SymbolTable nextTable) {
        this.nextTable = nextTable;
    }

    /** Adds to the symbol table a new variable of the given name, dataType, and scope.
    Assigns to it the index value of that scope, and adds 1 to the index */
    public void define(String name, String dataType, Scope scope) {
        // Add to set of data types, includes primitive and user-defined
        DATA_TYPES.add(dataType);

        // Map name to new symbol table entry
        // Todo: handle case where name already exists by logging error
        Data data = new Data(dataType, scope, scopeToRunningIndex.get(scope));
        nameToData.put(name, data);

        // Increment data type's index by 1
        scopeToRunningIndex.replace(scope, scopeToRunningIndex.get(scope) + 1);
    }

    /** Returns the number of variables of the given scope */
    public int varCount(Scope scope) {
        if (scopeToRunningIndex.containsKey(scope)) {
            return scopeToRunningIndex.get(scope);
        }
        if (nextTable == null) {
            return 0;
        }
        return nextTable.varCount(scope);
    }

    /** Returns the data type (primitive or object) of the given variable.
    Can either be a primitive (int, boolean, char), built-in object, or user-defined object */
    public String dataTypeOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name).dataType;
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return "Unknown";
        }
        return nextTable.dataTypeOf(name);
    }

    /** Returns the scope of the named identifier */
    public Scope scopeOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name).scope;
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return null;
        }
        return nextTable.scopeOf(name);
    }

    /** Returns the index of the named variable */
    public int indexOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name).index;
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return -1;
        }
        return nextTable.indexOf(name);
    }

    public boolean contains(String name) {
        if (nameToData.containsKey(name)) {
            return true;
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return false;
        }
        return nextTable.contains(name);
    }

    public void printSymbolTable(PrintWriter writer, String header) {
        writer.println("<" + header + ">");
        for (String name : nameToData.keySet()) {
            writer.print(name);
            writer.print(" ");
            writer.print(dataTypeOf(name));
            writer.print(" ");
            writer.print(scopeOf(name));
            writer.print(" ");
            writer.println(indexOf(name));
        }
        writer.println("");
    }
}
