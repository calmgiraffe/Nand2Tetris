package Core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SymbolTable {
    public static final Set<String> SCOPE_TYPES = Set.of(
            "static","field","arg","var");
    public static HashSet<String> DATA_TYPES = new HashSet<>(Set.of(
            "int","char","boolean"));

    /* Map ScopeType to cumulative index */
    private final Map<String, Integer> scopeToIndex = new HashMap<>() {{
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
        if (!DATA_TYPES.contains(dataType)) {
            DATA_TYPES.add(dataType);
        }
        // Add new row to symbol table
        String[] data = new String[3];
        data[0] = dataType;
        data[1] = scope;
        data[2] = String.valueOf(scopeToIndex.get(scope));
        nameToData.put(name, data); // Todo: handle case where name already exists by logging error

        // Increment data type's index by 1
        scopeToIndex.replace(scope, scopeToIndex.get(scope) + 1);
    }

    /** Returns the number of variables of the given scope */
    public int varCount(String scope) {
        // Todo: handle cases where invalid scope
        if (scopeToIndex.containsKey(scope)) {
            return scopeToIndex.get(scope);
        }
        return nextTable.varCount(scope);
    }

    /** Returns the data type (primitive or object) of the given variable.
    Can either be a primitive (int, boolean, char), built-in object, or user-defined object */
    public String dataTypeOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[0];
        }
        if (nextTable != null) {
            return nextTable.dataTypeOf(name);
        }
        return null;
    }

    /** Returns the scope of the named identifier */
    public String scopeOf(String name) {
        // Todo: handle cases where invalid name
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[1];
        }
        if (nextTable != null) {
            return nextTable.dataTypeOf(name);
        }
        return null;
    }

    /** Returns the index of the named variable */
    public String indexOf(String name) {
        if (nameToData.containsKey(name)) {
            return nameToData.get(name)[2];
        }
        if (nextTable != null) {
            return nextTable.dataTypeOf(name);
        }
        return null;
    }

    public boolean contains(String name) {
        if (nameToData.containsKey(name)) {
            return true;
        }
        if (nextTable != null) {
            return nextTable.contains(name);
        }
        return false;
    }
}
