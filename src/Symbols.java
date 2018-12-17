import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

class Symbols { 
	private Token name;
    private Token type;
    private DataType symbolType;
    private String scope;
    private LinkedHashMap<String, LinkedList<VarTypes>> values;
    private int numberOfArgs = -1;
    private boolean isRead = false;
    private boolean isCalled = false;

    Symbols() {
        values = new LinkedHashMap<>();
    }

    void setName(Token n0) {
        name = n0;
    }

    Token getName() {
        return name;
    }

    void setType(Token t0) {
        type = t0;
    }

    Token getType() {
        return type;
    }

    void setSymbolType(DataType d0) {
        symbolType = d0;
    }

    DataType getSymbolType() {
        return symbolType;
    }

    void setScope(String s0) {
        scope = s0;
    }

    String getScope() {
        return scope;
    }

    void setValues(LinkedHashMap<String, LinkedList<VarTypes>> v0) {
        values = v0;
    }

    LinkedHashMap<String, LinkedList<VarTypes>> getValues() {
        return values;
    }

    void setNumArgs(int na0) {
        numberOfArgs = na0;
    }

    int getNumArgs() {
        return numberOfArgs;
    }

    void setIsRead(boolean i0) {
        isRead = i0;
    }

    boolean getIsRead() {
        return isRead;
    }

    void setIsCalled(boolean i0) {
        isCalled = i0;
    }

    boolean getIsCalled() {
        return isCalled;
    }

    LinkedList<VarTypes> getValue(String name) {
        return values.get(name);
    }

    void addValue(String name, VarTypes value) {
        LinkedList<VarTypes> vars =  values.get(name);
        if(vars == null){
            vars = new LinkedList<>();
        }
        vars.add(value);
        values.put(name, vars);
    }

    String printValues(){
        StringBuilder buildTheString = new StringBuilder();
        buildTheString.append("{ ");
        for (Map.Entry<String, LinkedList<VarTypes>> entryType : values.entrySet()) {
            String key = entryType.getKey();
            for (VarTypes vars : entryType.getValue()) {
                buildTheString.append(vars.getValue()).append(": ").append(vars.getType()).append(", ");
            }
        }
        buildTheString.deleteCharAt(buildTheString.length()-1);
        buildTheString.append(" }");
        return buildTheString.toString();
    }

    String getSymbolString(){
        StringBuilder buildTheString = new StringBuilder();
        buildTheString.append("Name: ").append(getName()).append("\n");
        buildTheString.append("\t ").append("The SymbolType: ").append(getSymbolType()).append("\n");
        if(getSymbolType().equals(DataType.FUNC)){
            buildTheString.append("\t ").append("The Parameters: ").append(printValues()).append("\n");
            buildTheString.append("\t ").append("Is called?: ").append(getIsCalled()).append("\n");
        }
        else {
            if (getValues().size() > 0) {
                buildTheString.append("\t ").append("The Values: ").append(printValues()).append("\n");
            } else {
                buildTheString.append("\t ").append("The Values: No assignments made").append("\n");
            }
            buildTheString.append("\t ").append("It is written to: ").append(getValues().size() > 0).append("\n");
            buildTheString.append("\t ").append("It is read from: ").append(getIsRead()).append("\n");
        }

        return buildTheString.toString();

    }
}

