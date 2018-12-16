import java.util.*; 

public class Visitor implements CALParserVisitor {

	private HashMap<String, HashMap<String, Symbols>> symbolTable = new HashMap<>();
    private static final String PROGRAMME = "Programme";
    private static final String MAIN = "Main";
    private static final String INTEGER = "integer";
    private static final String BOOLEAN = "boolean";
    private String currentScope = PROGRAMME;
    private String previousScope;
    private int functionsNotCalled = 0;
    private List<ErrorMessage> errorList = new ArrayList<>();
    private List<String> variablesNotRead = new ArrayList<>();
    private List<String> variablesNotWritten = new ArrayList<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        symbolTable.put(currentScope, new HashMap<>());
        node.childrenAccept(this, data);

        System.out.println("-----Start Symbol Table-----");
        Set scopes = symbolTable.keySet();
        for (Object scope : scopes) { 
            String scopeName = (String) scope;
            System.out.println("-----Start " + scopeName + " Scope-----");
            Set symbols = symbolTable.get(scopeName).keySet();
            if (symbols.size() == 0) {
                System.out.println(" Nothing declared");
            }
            for (Object symbol : symbols) {
                String symbolName = (String) symbol;
                Symbols currentSymbol = symbolTable.get(scopeName).get(symbolName);
                System.out.println(currentSymbol.getSymbolString());
                if (currentSymbol.getSymbolType() == DataType.FUNC) { 
                    if (!currentSymbol.getIsCalled()) {
                        functionsNotCalled++; 
                    }
                } else {
                    if (currentSymbol.getValues().size() == 0 && !currentSymbol.getSymbolType().equals(DataType.PARAM)) {                    
                        variablesNotWritten.add(currentSymbol.getName().image); 
                    }
                    if (!currentSymbol.getIsRead()) {
                        variablesNotRead.add(currentSymbol.getName().image);
                    }
                }
            }
            System.out.println("-----End " + scopeName + " Scope-----"); 
        }
        System.out.println("-----End Symbol Table-----");

        if (errorList.size() == 0) { 
            isSemanticError();
            System.out.println("No errors found");
            ThreeAddressCoder representer = new ThreeAddressCoder();
            node.jjtAccept(representer, symbolTable);
        } else { 
            printErrorList();
        }

        return null;
	}

	@Override
    public Object visit(ASTMain node, Object data) {
        previousScope = currentScope;
        currentScope = MAIN;
        symbolTable.put(currentScope, new HashMap<>()); 
        node.childrenAccept(this, data);

        currentScope = previousScope;
        previousScope = null;
        return null;
    }

    private static boolean isBoolean(String s) {
        return Boolean.parseBoolean(s);
    }

    
    @Override
    public Object visit(ASTDeclarationList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }
    
    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        Token varName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbols> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Symbols varSymbol = currentScopeSymbolTable.get(varName.image);
        if (varSymbol == null) {
            Token varType = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbols symbol = new Symbols();
            symbol.setName(varName);
            symbol.setType(varType);
            symbol.setScope(currentScope);
            symbol.setSymbolType(DataType.VAR);
            currentScopeSymbolTable.put(varName.image, symbol);
        } else {
            errorList.add(new ErrorMessage(varName.beginLine, varName.beginColumn, "VAR \"" + varName.image + "\" already declared in currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, currentScopeSymbolTable);

        return null;
    }

    private static boolean isInt(String s) {
        try {
            // is integer
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public Object visit(ASTFunctionList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        Token functionName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        // check if function has been declared
        HashMap<String, Symbols> functionSymbolTable = symbolTable.get(currentScope);
        if (functionSymbolTable == null) {
            functionSymbolTable = symbolTable.get(PROGRAMME);
        }
        Symbols functionSymbol = functionSymbolTable.get(functionName.image);
        if (functionSymbol == null) {
            functionSymbolTable = symbolTable.get(PROGRAMME);
            functionSymbol = functionSymbolTable.get(functionName.image);
        }
        if (functionSymbol != null) {
            // go to ArgList
            if (node.jjtGetNumChildren() > 1) {
                node.jjtGetChild(1).jjtAccept(this, null);
            }
            functionSymbol.setIsCalled(true);
            updateSymbol(functionName.image, functionSymbol);
        } else {
            errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" not declared in any scope"));
        }
        return null;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
        HashMap<String, Symbols> currentHashScopeMap = symbolTable.get(currentScope);
        if (currentHashScopeMap == null) {
            currentHashScopeMap = new HashMap<>();
        }

        Token functionType = (Token) node.jjtGetChild(0).jjtAccept(this, null); 
        Token functionName = (Token) node.jjtGetChild(1).jjtAccept(this, null); 
        Symbols symbol = new Symbols(); 
        symbol.setName(functionName);
        symbol.setType(functionType);
        symbol.setScope(currentScope);
        symbol.setSymbolType(DataType.FUNC);
        symbol.setNumArgs(node.jjtGetChild(2).jjtGetNumChildren()); 

        
        if (currentHashScopeMap.containsKey(functionName.image)) {
                errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" already declared with " + symbol.getNumArgs() + " parameters"));
        } else {
            
            currentHashScopeMap.put(functionName.image, symbol);
            symbolTable.put(currentScope, currentHashScopeMap);
        }

        previousScope = currentScope;
        currentScope = functionName.image;

        node.jjtGetChild(2).jjtAccept(this, null);
        node.jjtGetChild(3).jjtAccept(this, null);

        currentScope = previousScope;
        previousScope = null;

        return null;
    }

    @Override
    public Object visit(ASTFunctionBody node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTComparing node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTTypeValue node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTConstDeclaration node, Object data) {
        HashMap<String, Symbols> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Token contantName = (Token) node.jjtGetChild(0).jjtAccept(this, null); //get the constant name
        if (currentScopeSymbolTable.get(contantName.image) == null) { //Check that the constant hasn't been defined in the current scope
            Token constantType = (Token) node.jjtGetChild(1).jjtAccept(this, null); //get the constant type (eg. integer)
            Token contantValue = (Token) node.jjtGetChild(2).jjtAccept(this, null); //get the constant value
            Symbols symbol = new Symbols(); //create a symbol for the constant
            symbol.setName(contantName);
            symbol.setType(constantType);
            symbol.setScope(currentScope);
            symbol.setSymbolType(DataType.CONST);
            symbolTable.put(currentScope, currentScopeSymbolTable);
            currentScopeSymbolTable.put(contantName.image, symbol);

            if ((constantType.image.equals(INTEGER) && !isInt(contantValue.image)) || (constantType.image.equals(BOOLEAN) && !isBoolean(contantValue.image))) {
                errorList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "Invalid type assigned to constant \"" + contantName.image + "\""));
            } else {
                VarTypes variable = new VarTypes(symbol.getType().toString(), contantValue.image.toLowerCase());
                symbol.addValue(contantName.image, variable); 
            }
            currentScopeSymbolTable.put(contantName.image, symbol);
        } else {
            errorList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "CONST \"" + contantName.image + "\" already declared in currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, currentScopeSymbolTable); 
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        if (node.jjtGetNumChildren() > 0) {
            Token returnedToken = (Token) node.jjtGetChild(0).jjtAccept(this, null);
            Node returnedNode = node.jjtGetChild(0);
            if (returnedNode instanceof ASTID) {
                HashMap<String, Symbols> currentSymbolTable = symbolTable.get(currentScope);
                Symbols returnedSymbol = currentSymbolTable.get(returnedToken.image);
                if (returnedSymbol == null) {
                    currentSymbolTable = symbolTable.get(PROGRAMME);
                    returnedSymbol = currentSymbolTable.get(returnedToken.image);
                }
                System.out.println("PRINNNNT" + returnedToken.image);
                if (returnedSymbol.getValues().size() == 0 && !returnedSymbol.getSymbolType().equals(DataType.PARAM)) {                   
                    errorList.add(new ErrorMessage(returnedToken.beginLine, returnedToken.beginColumn, "Variable \"" + returnedToken.image + "\" has no value in currentScope \"" + currentScope + "\""));
                }
                returnedSymbol.setIsRead(true);
                updateSymbol(returnedToken.image, returnedSymbol);
            }
        }
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTParamList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTParams node, Object data) {
        HashMap<String, Symbols> currentSymbolTable = symbolTable.get(currentScope);
        if (currentSymbolTable == null) {
            currentSymbolTable = new HashMap<>();
        }

        Token paramName = (Token) node.jjtGetChild(0).jjtAccept(this, null); 
        Token paramType = (Token) node.jjtGetChild(1).jjtAccept(this, null); 
        Symbols symbol = new Symbols(); 
        symbol.setName(paramName);
        symbol.setType(paramType);
        symbol.setScope(currentScope);
        symbol.setSymbolType(DataType.PARAM);
        currentSymbolTable.put(paramName.image, symbol);
        symbolTable.put(currentScope, currentSymbolTable);

        Token functionName = (Token) node.jjtGetParent().jjtGetParent().jjtGetChild(1).jjtAccept(this, null);
        currentSymbolTable = symbolTable.get(currentScope);
       	
        Symbols functionSymbol = currentSymbolTable.get(functionName.image);
        if (functionSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            functionSymbol = currentSymbolTable.get(functionName.image);
        }
        if (functionSymbol.getValues().containsKey(paramName.image)) { 
            errorList.add(new ErrorMessage(paramName.beginLine, paramName.beginColumn, "Duplicate parameter names, \"" + paramName.image + "\" for function \"" + functionName.image + "\""));
        } else {
       
            VarTypes variable = new VarTypes(functionSymbol.getType().toString(), paramName.image);
            functionSymbol.addValue(paramName.image, variable);
            updateSymbol(functionName.image, functionSymbol);
        }
        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        Token functionName = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbols> mapTemp = symbolTable.get(PROGRAMME);
        if (mapTemp != null) {
            Symbols functionSymbol = mapTemp.get(functionName.image);
            if (functionSymbol == null) {
                // error, no such function
                errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "No function called \"" + functionName.image + "\""));
            } else {             
                int numArgsDeclared = functionSymbol.getNumArgs();
                int numArgsPassed = node.jjtGetNumChildren();
                if (numArgsDeclared != numArgsPassed) {
                    errorList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" has invalid number of arguments. Should have " + numArgsDeclared + " but called with " + numArgsPassed + " argument(s)"));
                } else if (numArgsDeclared > 0) {
                    Object[] keys =  functionSymbol.getValues().keySet().toArray();
                    String key = (String) keys[0];
                    String type = functionSymbol.getValues().get(key).get(0).getType();
                    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                        Token argumentName = (Token) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, null);
                        Symbols argumentSymbol = mapTemp.get(argumentName.image);
                        if (argumentSymbol == null) {
                            mapTemp = symbolTable.get(currentScope);
                            argumentSymbol = mapTemp.get(argumentName.image);
                        }
                        if (!argumentSymbol.getType().image.equals(type)) {
                            errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "\"" + argumentName.image + "\" is of the wrong type for function \"" + functionName.image + "\""));
                        }
                        if (argumentSymbol.getSymbolType() != DataType.NAS) {
                            argumentSymbol.setIsRead(true);
                            updateSymbol(argumentName.image, argumentSymbol);
                        }
                    }
                }
            }
        }
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        HashMap<String, Symbols> currentScopeSymbolTable = symbolTable.get(currentScope);
        if (currentScopeSymbolTable == null) {
            currentScopeSymbolTable = new HashMap<>();
        }
        Token variableName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Symbols variableSymbol = currentScopeSymbolTable.get(variableName.image);
        if (variableSymbol == null) { 
            variableSymbol = symbolTable.get(PROGRAMME).get(variableName.image);
        }
        if (variableSymbol != null) { 
            Token assignedValue = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Node assignedNode = node.jjtGetChild(1);
            if(variableSymbol.getSymbolType().equals(DataType.CONST)){ //constants can't be reassigned
                errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn,  " \"" + variableName + "\" cannot be reassigned a value as it is of type \"" + variableSymbol.getSymbolType() + "\""));
            }
            if (assignedNode instanceof ASTID) {
                Symbols assignedSymbol = symbolTable.get(currentScope).get(assignedValue.image);
                if (assignedSymbol == null) {
                    assignedSymbol = symbolTable.get(PROGRAMME).get(assignedValue.image);
                }
                if (assignedSymbol == null) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, variableSymbol.getSymbolType() + " \"" + assignedValue.image + "\" not declared in any scope \""));
                } else if (!variableSymbol.getType().image.equals(assignedSymbol.getType().image)) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "\"" + variableName.image + "\" and \"" + assignedValue.image + "\" are not of same type"));
                }
                else  {
                    VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    currentScopeSymbolTable.put(variableName.image, variableSymbol);
                    symbolTable.put(currentScope, currentScopeSymbolTable);
                    assignedSymbol.setIsRead(true);
                    updateSymbol(assignedValue.image, assignedSymbol);
                }
            } else if (assignedNode instanceof ASTDigit) {
                if (!variableSymbol.getType().image.equals(INTEGER)) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign type Digit to \"" + variableName.image + "\""));
                } else {
                    VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    updateSymbol(variableName.image, variableSymbol);
                }
            } else if (assignedNode instanceof ASTBoolean) {
                if (!variableSymbol.getType().image.equals(BOOLEAN)) {
                    errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign type boolean to \"" + variableName.image + "\""));
                }
                VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                variableSymbol.addValue(variableName.image, variable);
                currentScopeSymbolTable.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, currentScopeSymbolTable);
            } else if (assignedNode instanceof ASTFunctionCall ) {
                Token functionToken = (Token) assignedNode.jjtGetChild(0).jjtAccept(this, null);
                VarTypes variable = new VarTypes(variableSymbol.getType().toString(), functionToken.toString());
                variableSymbol.addValue(functionToken.image, variable);
                currentScopeSymbolTable.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, currentScopeSymbolTable);
            }  else {
                node.childrenAccept(this, variableSymbol);
            }
        } else {
            errorList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Variable \"" + variableName.image + "\" not declared in currentScope \"" + currentScope + "\" or \"Programme\""));
        }
        return null;
    }


    @Override
    public Object visit(ASTArg node, Object data) {
        Token argumentName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbols> currentSymbolTable = symbolTable.get(currentScope);
        Symbols argumentSymbol = currentSymbolTable.get(argumentName.image);
        if (argumentSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            argumentSymbol = currentSymbolTable.get(argumentName.image);
        }
        if (argumentSymbol == null) {
            errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "VAR or Const \"" + argumentName.image + "\" has not been declared in currentScope \"" + currentScope + "\""));
        } else if (argumentSymbol.getValues().size() == 0 && !argumentSymbol.getSymbolType().equals(DataType.PARAM)) {
                errorList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "VAR \"" + argumentName.image + "\" has been declared in currentScope \"" + currentScope + "\", but has no value"));
        } else {
            argumentSymbol.setIsRead(true);
            updateSymbol(argumentName.image, argumentSymbol);
        }
        return null;
    }

	@Override
    public Object visit(ASTBoolean node, Object data) {
        return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTStatementBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTDigit node, Object data){ 
    	return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTID node, Object data){ 
    	return node.jjtGetValue();
    }

    @Override
    public Object visit(ASTIf node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTWhile node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCondition node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    private void isSemanticError() {
        System.out.println("Semantic Check Results");
        if (functionsNotCalled > 0) {
            System.out.println(functionsNotCalled + " function(s) are declared but not used.");
        }
        if (variablesNotWritten.size() > 0) {
            StringBuilder notWrittenVariables = new StringBuilder();
            System.out.println(variablesNotWritten.size() + " variable(s) have not been initialised:");
            for(String variable: variablesNotWritten){
                notWrittenVariables.append(variable).append(",");
            }
            notWrittenVariables.deleteCharAt(notWrittenVariables.length()-1);
            System.out.println(notWrittenVariables.toString());
        }
        if (variablesNotRead.size() > 0) {
            StringBuilder notReadVariables = new StringBuilder();
            System.out.println(variablesNotRead.size() + " variable(s) have not been accessed:");
            for(String variable: variablesNotRead){
                notReadVariables.append(variable).append(",");
            }
            notReadVariables.deleteCharAt(notReadVariables.length()-1);
            System.out.println(notReadVariables.toString());
        }
    }

    private void printErrorList() {
        Map<String, ErrorMessage> map = new LinkedHashMap<>();
        for (ErrorMessage errorMessage : errorList) {
            map.put(errorMessage.errorMessage, errorMessage);
        }
        errorList.clear();
        errorList.addAll(map.values()); 
        System.out.println(errorList.size() + " error(s).");
        for (ErrorMessage errorMessage : errorList) {
            System.out.println(errorMessage + "\n");
        }
    }

    private void updateSymbol(String symbolName, Symbols symbol) {
        HashMap<String, Symbols> tempMap;
        if (symbol.getScope().equals(currentScope)) {
            tempMap = symbolTable.get(currentScope);
            tempMap.put(symbolName, symbol);
            symbolTable.put(currentScope, tempMap);
        } else {
            tempMap = symbolTable.get(symbol.getScope());
            tempMap.put(symbolName, symbol);
            symbolTable.put(symbol.getScope(), tempMap);
        }
    }

	private class ErrorMessage {
        int lineNumber;
        int columnNumber;
        String errorMessage;

        ErrorMessage(int lineNumber, int columnNumber, String errorMessage) {
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "Error at line " + lineNumber + ", column " + columnNumber + ":\n" + errorMessage;
        }
    }
}
