import java.util.*; 

public class Visitor implements CALParserVisitor {

	private HashMap<String, HashMap<String, Symbols>> symbolTable = new HashMap<>();
    private static final String PROGRAMME = "Programme";
    private static final String MAIN = "Main";
    private static final String BOOLEAN = "boolean";
    private static final String INTEGER = "integer";
    private String currentScope = PROGRAMME;
    private String previousScope;
    private List<ErrorMessage> theErrorsList = new ArrayList<>();
    private List<String> varsNotRead = new ArrayList<>();
    private List<String> varsNotWritten = new ArrayList<>();
    private int functionsNotCalled = 0;

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        symbolTable.put(currentScope, new HashMap<>());
        node.childrenAccept(this, data);

        System.out.println("***** START SYMBOL TABLE ***");
        Set scopes = symbolTable.keySet();
        for (Object scope : scopes) { 
            String scopeName = (String) scope;
            System.out.println("***** START " + scopeName + " SCOPE *****");
            Set symbols = symbolTable.get(scopeName).keySet();
            if (symbols.size() == 0) {
                System.out.println(" NO SYMBOL TREE!");
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
                        varsNotWritten.add(currentSymbol.getName().image); 
                    }
                    if (!currentSymbol.getIsRead()) {
                        varsNotRead.add(currentSymbol.getName().image);
                    }
                }
            }
            System.out.println("***** END " + scopeName + " SCOPE *****"); 
        }
        System.out.println("***** END SYMBOL TABLE *****");

        if (theErrorsList.size() == 0) { 
            isThereASemanticError();
            System.out.println("NO ERRORS HERE!");
            ThreeAddressCoder representer = new ThreeAddressCoder();
            node.jjtAccept(representer, symbolTable);
        } else { 
            printTheErrorList();
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
        HashMap<String, Symbols> theCurrentScopeST = symbolTable.get(currentScope);
        if (theCurrentScopeST == null) {
            theCurrentScopeST = new HashMap<>();
        }
        Symbols varSymbol = theCurrentScopeST.get(varName.image);
        if (varSymbol == null) {
            Token varType = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Symbols symbol = new Symbols();
            symbol.setName(varName);
            symbol.setType(varType);
            symbol.setScope(currentScope);
            symbol.setSymbolType(DataType.VAR);
            theCurrentScopeST.put(varName.image, symbol);
        } else {
            theErrorsList.add(new ErrorMessage(varName.beginLine, varName.beginColumn, "VAR \"" + varName.image + "\" already has been declared in  the currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, theCurrentScopeST);

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
        Symbols funcSymbol = functionSymbolTable.get(functionName.image);
        if (funcSymbol == null) {
            functionSymbolTable = symbolTable.get(PROGRAMME);
            funcSymbol = functionSymbolTable.get(functionName.image);
        }
        if (funcSymbol != null) {
            // go to ArgList
            if (node.jjtGetNumChildren() > 1) {
                node.jjtGetChild(1).jjtAccept(this, null);
            }
            funcSymbol.setIsCalled(true);
            updateTheSymbol(functionName.image, funcSymbol);
        } else {
            theErrorsList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" has not been declared in any scope"));
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
                theErrorsList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" has already been declared with " + symbol.getNumArgs() + " parameters"));
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
        HashMap<String, Symbols> theCurrentScopeST = symbolTable.get(currentScope);
        if (theCurrentScopeST == null) {
            theCurrentScopeST = new HashMap<>();
        }
        Token contantName = (Token) node.jjtGetChild(0).jjtAccept(this, null); 
        if (theCurrentScopeST.get(contantName.image) == null) { 
            Token constantType = (Token) node.jjtGetChild(1).jjtAccept(this, null); 
            Token contantValue = (Token) node.jjtGetChild(2).jjtAccept(this, null); 
            Symbols symbol = new Symbols(); 
            symbol.setName(contantName);
            symbol.setType(constantType);
            symbol.setScope(currentScope);
            symbol.setSymbolType(DataType.CONST);
            symbolTable.put(currentScope, theCurrentScopeST);
            theCurrentScopeST.put(contantName.image, symbol);

            if ((constantType.image.equals(INTEGER) && !isInt(contantValue.image)) || (constantType.image.equals(BOOLEAN) && !isBoolean(contantValue.image))) {
                theErrorsList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "There has been an invalid type assigned to constant \"" + contantName.image + "\""));
            } else {
                VarTypes variable = new VarTypes(symbol.getType().toString(), contantValue.image.toLowerCase());
                symbol.addValue(contantName.image, variable); 
            }
            theCurrentScopeST.put(contantName.image, symbol);
        } else {
            theErrorsList.add(new ErrorMessage(contantName.beginLine, contantName.beginColumn, "CONST \"" + contantName.image + "\" has already been declared in the currentScope \"" + currentScope + "\""));
        }
        symbolTable.put(currentScope, theCurrentScopeST); 
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
                if (returnedSymbol.getValues().size() == 0 && !returnedSymbol.getSymbolType().equals(DataType.PARAM)) {                   
                    theErrorsList.add(new ErrorMessage(returnedToken.beginLine, returnedToken.beginColumn, "Variable \"" + returnedToken.image + "\" has no value in the currentScope \"" + currentScope + "\""));
                }
                returnedSymbol.setIsRead(true);
                updateTheSymbol(returnedToken.image, returnedSymbol);
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
       	
        Symbols funcSymbol = currentSymbolTable.get(functionName.image);
        if (funcSymbol == null) {
            currentSymbolTable = symbolTable.get(PROGRAMME);
            funcSymbol = currentSymbolTable.get(functionName.image);
        }
        if (funcSymbol.getValues().containsKey(paramName.image)) { 
            theErrorsList.add(new ErrorMessage(paramName.beginLine, paramName.beginColumn, "There are duplicate parameter names, \"" + paramName.image + "\" for the function \"" + functionName.image + "\""));
        } else {
       
            VarTypes variable = new VarTypes(funcSymbol.getType().toString(), paramName.image);
            funcSymbol.addValue(paramName.image, variable);
            updateTheSymbol(functionName.image, funcSymbol);
        }
        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        Token functionName = (Token) node.jjtGetParent().jjtGetChild(0).jjtAccept(this, null);
        HashMap<String, Symbols> tempMapper = symbolTable.get(PROGRAMME);
        if (tempMapper != null) {
            Symbols funcSymbol = tempMapper.get(functionName.image);
            if (funcSymbol == null) {
                theErrorsList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "There has been no function called \"" + functionName.image + "\""));
            } else {             
                int numArgsDeclared = funcSymbol.getNumArgs();
                int numArgsPassed = node.jjtGetNumChildren();
                if (numArgsDeclared != numArgsPassed) {
                    theErrorsList.add(new ErrorMessage(functionName.beginLine, functionName.beginColumn, "Function \"" + functionName.image + "\" has an invalid number of arguments. It should have " + numArgsDeclared + " but it is called with " + numArgsPassed + " argument(s)"));
                } else if (numArgsDeclared > 0) {
                    Object[] keys =  funcSymbol.getValues().keySet().toArray();
                    String key = (String) keys[0];
                    String type = funcSymbol.getValues().get(key).get(0).getType();
                    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                        Token argumentName = (Token) node.jjtGetChild(i).jjtGetChild(0).jjtAccept(this, null);
                        Symbols argumentSymbol = tempMapper.get(argumentName.image);
                        if (argumentSymbol == null) {
                            tempMapper = symbolTable.get(currentScope);
                            argumentSymbol = tempMapper.get(argumentName.image);
                        }
                        if (!argumentSymbol.getType().image.equals(type)) {
                            theErrorsList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "\"" + argumentName.image + "\" is the wrong type for function \"" + functionName.image + "\""));
                        }
                        if (argumentSymbol.getSymbolType() != DataType.NOT_A_SYM) {
                            argumentSymbol.setIsRead(true);
                            updateTheSymbol(argumentName.image, argumentSymbol);
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
        HashMap<String, Symbols> theCurrentScopeST = symbolTable.get(currentScope);
        if (theCurrentScopeST == null) {
            theCurrentScopeST = new HashMap<>();
        }
        Token variableName = (Token) node.jjtGetChild(0).jjtAccept(this, null);
        Symbols variableSymbol = theCurrentScopeST.get(variableName.image);
        if (variableSymbol == null) { 
            variableSymbol = symbolTable.get(PROGRAMME).get(variableName.image);
        }
        if (variableSymbol != null) { 
            Token assignedValue = (Token) node.jjtGetChild(1).jjtAccept(this, null);
            Node assignedNode = node.jjtGetChild(1);
            if(variableSymbol.getSymbolType().equals(DataType.CONST)){ //constants can't be reassigned
                theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn,  " \"" + variableName + "\" can't be reassigned a value as it is of type \"" + variableSymbol.getSymbolType() + "\""));
            }
            if (assignedNode instanceof ASTID) {
                Symbols assignedSymbol = symbolTable.get(currentScope).get(assignedValue.image);
                if (assignedSymbol == null) {
                    assignedSymbol = symbolTable.get(PROGRAMME).get(assignedValue.image);
                }
                if (assignedSymbol == null) {
                    theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, variableSymbol.getSymbolType() + " \"" + assignedValue.image + "\" has not been declared in any scope \""));
                } else if (!variableSymbol.getType().image.equals(assignedSymbol.getType().image)) {
                    theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "\"" + variableName.image + "\" and \"" + assignedValue.image + "\" are not of the same type"));
                }
                else  {
                    VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    theCurrentScopeST.put(variableName.image, variableSymbol);
                    symbolTable.put(currentScope, theCurrentScopeST);
                    assignedSymbol.setIsRead(true);
                    updateTheSymbol(assignedValue.image, assignedSymbol);
                }
            } else if (assignedNode instanceof ASTDigit) {
                if (!variableSymbol.getType().image.equals(INTEGER)) {
                    theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign a type Digit to \"" + variableName.image + "\""));
                } else {
                    VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                    variableSymbol.addValue(variableName.image, variable);
                    updateTheSymbol(variableName.image, variableSymbol);
                }
            } else if (assignedNode instanceof ASTBoolean) {
                if (!variableSymbol.getType().image.equals(BOOLEAN)) {
                    theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Cannot assign a type boolean to \"" + variableName.image + "\""));
                }
                VarTypes variable = new VarTypes(variableSymbol.getType().toString(), assignedValue.toString());
                variableSymbol.addValue(variableName.image, variable);
                theCurrentScopeST.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, theCurrentScopeST);
            } else if (assignedNode instanceof ASTFunctionCall ) {
                Token functionToken = (Token) assignedNode.jjtGetChild(0).jjtAccept(this, null);
                VarTypes variable = new VarTypes(variableSymbol.getType().toString(), functionToken.toString());
                variableSymbol.addValue(functionToken.image, variable);
                theCurrentScopeST.put(variableName.image, variableSymbol);
                symbolTable.put(currentScope, theCurrentScopeST);
            }  else {
                node.childrenAccept(this, variableSymbol);
            }
        } else {
            theErrorsList.add(new ErrorMessage(variableName.beginLine, variableName.beginColumn, "Variable \"" + variableName.image + "\" is not declared in the currentScope \"" + currentScope + "\" or \"Programme\""));
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
            theErrorsList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "VAR or Const \"" + argumentName.image + "\" has not been declared in the currentScope \"" + currentScope + "\""));
        } else if (argumentSymbol.getValues().size() == 0 && !argumentSymbol.getSymbolType().equals(DataType.PARAM)) {
                theErrorsList.add(new ErrorMessage(argumentName.beginLine, argumentName.beginColumn, "VAR \"" + argumentName.image + "\" has been declared in currentScope \"" + currentScope + "\", but does not have a value"));
        } else {
            argumentSymbol.setIsRead(true);
            updateTheSymbol(argumentName.image, argumentSymbol);
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

    private void isThereASemanticError() {
        System.out.println("***** HERE ARE THE SEMANTIC CHECK RESULTS! ******");
        if (functionsNotCalled > 0) {
            System.out.println(functionsNotCalled + " functions are declared but have not been used.");
        }
        if (varsNotRead.size() > 0) {
            StringBuilder notReadVariables = new StringBuilder();
            System.out.println(varsNotRead.size() + " variables have not been accessed:");
            for(String variable: varsNotRead){
                notReadVariables.append(variable).append(",");
            }
            notReadVariables.deleteCharAt(notReadVariables.length()-1);
            System.out.println(notReadVariables.toString());
        }
        if (varsNotWritten.size() > 0) {
            StringBuilder notWrittenVariables = new StringBuilder();
            System.out.println(varsNotWritten.size() + " variables have not been initialised:");
            for(String variable: varsNotWritten){
                notWrittenVariables.append(variable).append(",");
            }
            notWrittenVariables.deleteCharAt(notWrittenVariables.length()-1);
            System.out.println(notWrittenVariables.toString());
        }
    }

    private void printTheErrorList() {
        Map<String, ErrorMessage> map = new LinkedHashMap<>();
        for (ErrorMessage errorMessage : theErrorsList) {
            map.put(errorMessage.errorMessage, errorMessage);
        }
        theErrorsList.clear();
        theErrorsList.addAll(map.values()); 
        System.out.println(theErrorsList.size() + " errors.");
        for (ErrorMessage errorMessage : theErrorsList) {
            System.out.println(errorMessage + "\n");
        }
    }

    private void updateTheSymbol(String symbolName, Symbols symbol) {
        HashMap<String, Symbols> temps;
        if (symbol.getScope().equals(currentScope)) {
            temps = symbolTable.get(currentScope);
            temps.put(symbolName, symbol);
            symbolTable.put(currentScope, temps);
        } else {
            temps = symbolTable.get(symbol.getScope());
            temps.put(symbolName, symbol);
            symbolTable.put(symbol.getScope(), temps);
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
            return "There is an error at line " + lineNumber + ", at column " + columnNumber + ":\n" + errorMessage;
        }
    }
}
