package Core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SymbolTable {
    public static final Set<String> SCOPE_TYPES = Set.of("static","field","arg","var");
    public static HashSet<String> DATA_TYPES = new HashSet<>(Set.of("int","char","boolean"));

    /* Map ScopeType to cumulative index */
    private final Map<String, Integer> scopeToRunningIndex = new HashMap<>() {{
        put("static", 0);
        put("field", 0);
        put("arg", 0);
        put("var", 0);
    }};
    /* name -> data type, scope, index
    Note: In Jack and in most programming languages, variables cannot share
    the same name even if they are different data types */
    private final Map<String, String[]> nameToData = new HashMap<>();
    private SymbolTable nextTable;

    /** Default constructor */
    SymbolTable() {}

    /** Can add pointer to next symbol table to check */
    SymbolTable(SymbolTable nextTable) {
        this.nextTable = nextTable;
    }

    /** Adds to the symbol table a new variable of the given name, dataType, and scope.
    Assigns to it the index value of that scope, and adds 1 to the index */
    public void define(String name, String dataType, String scope) {
        /* If custom class, add to set of data types */
        DATA_TYPES.add(dataType);

        // Add new row to symbol table
        String[] data = new String[3];
        data[0] = dataType;
        data[1] = scope;    // scope will always be one of static, field, arg, var
        data[2] = String.valueOf(scopeToRunningIndex.get(scope));
        nameToData.put(name, data); // Todo: handle case where name already exists by logging error

        // Increment data type's index by 1
        scopeToRunningIndex.replace(scope, scopeToRunningIndex.get(scope) + 1);
    }

    /** Returns the number of variables of the given scope */
    public int varCount(String scope) {
        if (!SCOPE_TYPES.contains(scope)) {
            // Todo: handle cases where invalid scope
            return -1;
        }
        if (scopeToRunningIndex.containsKey(scope)) {
            return scopeToRunningIndex.get(scope);
        }
        return nextTable.varCount(scope);
    }

    /** Returns the data type (primitive or object) of the given variable.
    Can either be a primitive (int, boolean, char), built-in object, or user-defined object */
    public String dataTypeOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[0];
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return "Unknown";
        }
        return nextTable.dataTypeOf(name);
    }

    /** Returns the scope of the named identifier */
    public String scopeOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[1];
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return "Unknown";
        }
        return nextTable.scopeOf(name);
    }

    /** Returns the index of the named variable */
    public String indexOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[2];
        }
        if (nextTable == null) {
            // Handle the case where name is not found in any table
            return "Unknown";
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
}
