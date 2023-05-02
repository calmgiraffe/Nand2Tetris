package Core;

import java.util.HashMap;
import java.util.Map;

import static Core.SymbolTable.ScopeType.*;

public class SymbolTable {
    public enum ScopeType {
        STATIC,
        FIELD,
        ARG,
        VAR
    }
    /* Map ScopeType to cumulative index */
    private final Map<ScopeType, Integer> scopeToIndex = new HashMap<>() {{
        put(STATIC, 0);
        put(FIELD, 0);
        put(ARG, 0);
        put(VAR, 0);
    }};

    /* Cols: data type, scope, index
    Note: In Jack and in most programming languages, variables cannot share
    the same name even if they are different data types */
    private final Map<String, String[]> nameToData = new HashMap<>();

    /* Empties the symbol table, and resets the four indexes to 0.
    // Todo: probably don't need */
    public void reset() {}

    /* Adds to the symbol table a new variable of the given name, type, and scope.
    Assigns to it the index value of that scope, and adds 1 to the index */
    public void define(String name, String type, ScopeType scope) {
        String[] data = new String[3];
        data[0] = type;
        data[1] = String.valueOf(scope);
        data[2] = String.valueOf(scopeToIndex.get(scope));

        nameToData.put(name, data); // Todo: potentially handle case where name already exists by logging error
        scopeToIndex.replace(scope, scopeToIndex.get(scope) + 1);
    }

    /* Returns the number of variables of the given scope */
    public int varCount(ScopeType scope) {
        return scopeToIndex.get(scope);
    }

    /* Returns the data type (primitive or object) of the given variable.
    Can either be a primitive (int, boolean, char), built-in object, or user-defined object */
    public String dataTypeOf(String name) {
        return nameToData.get(name)[0];
    }

    /* Returns the scope of the named identifier */
    public String scopeOf(String name) {
        return nameToData.get(name)[1];
    }

    /* Returns the index of the named variable */
    public String indexOf(String name) {
        return nameToData.get(name)[2];
    }
}
