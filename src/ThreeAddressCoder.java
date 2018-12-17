import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class ThreeAddressCoder implements CALParserVisitor {

    private String currentLable = "L0";
    private String previousLable;
    private int theLabelCounter = 0;
    private LinkedHashMap<String, ArrayList<AddressCode>> theAddrCodes = new LinkedHashMap<>();
    private HashMap<String, String> theJumpLables = new HashMap<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        String filename = "TAC.ir";
        try {
            PrintStream out = new PrintStream(new FileOutputStream(filename)); 
            System.setOut(out);
        }
        catch (FileNotFoundException e) {
                e.printStackTrace();
        }

        System.out.println("****** THREE-ADDRESS CODE REPRESENTATION *****");
        node.childrenAccept(this, data);

        Set keys = theAddrCodes.keySet();
        if(keys.size() > 0) {
            for (Object key : keys) {
                String s = (String) key;
                ArrayList<AddressCode> a = theAddrCodes.get(s);
                System.out.println(s);
                
                for (AddressCode addressCode : a) {
                    System.out.println(" " + addressCode.toString());
                }
            }
        } else {
            System.out.println("NO THREE-ADDRESS CODE REPRESENTATION");
        }
        System.out.println("***** END THREE-ADDRESS CODE REPRESENTATION *****");

        return null;
    }

   

    @Override
    public Object visit(ASTMain node, Object data) {
        currentLable = "L" + (theLabelCounter + 1);
        previousLable = currentLable;

        node.childrenAccept(this, data);

        currentLable = previousLable;
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }
        AddressCode endOfMain = new AddressCode();
        endOfMain.address1 = "END";
        currAddrCodes.add(endOfMain);
        theAddrCodes.put(currentLable,currAddrCodes);
        theLabelCounter++;

        return null;
    }

    @Override
    public Object visit(ASTFunctionList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
        previousLable = currentLable;
        currentLable = "L" + (theLabelCounter + 1);

        theJumpLables.put((String) node.jjtGetChild(1).jjtAccept(this, null), currentLable);

        node.childrenAccept(this, data);

        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode retAddrCode = new AddressCode();
        retAddrCode.address1 = "return";

        currAddrCodes.add(retAddrCode);
        theAddrCodes.put(currentLable, currAddrCodes);

        currentLable = previousLable;
        theLabelCounter++;

        return null;
    }

    @Override
    public Object visit(ASTFunctionBody node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTParamList node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTParams node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTDeclarationList node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTConstDeclaration node, Object data) {
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if (currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode ac = new AddressCode();
        ac.address1 = " = "; 
        ac.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        ac.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        ac.address4 = (node.jjtGetChild(2).jjtAccept(this, null).toString());
        currAddrCodes.add(ac);
        theAddrCodes.put(currentLable, currAddrCodes);
        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTStatementBlock node, Object data) {
     
        if(data instanceof ASTIf || data instanceof ASTWhile){
            currentLable = "L" + (theLabelCounter + 1);
            theLabelCounter++;
        }
        node.childrenAccept(this, data);
        if(data instanceof ASTIf || data instanceof ASTWhile){
            if (node.jjtGetNumChildren() > 0) {
                String hash = node.jjtGetChild(0).hashCode() + "";
                theJumpLables.put(hash,currentLable);
            }
        }
    
        return null;
    }

    @Override
    public Object visit(ASTCondition node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIf node, Object data) {
        previousLable = currentLable;
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode retAddrCode = new AddressCode();
        retAddrCode.address1 = "if";
        currAddrCodes.add(retAddrCode);
        theLabelCounter++;
        String currentIfLable = currentLable;
        int currentLableCount = theLabelCounter;
        node.childrenAccept(this, node);

        currentLable = currentIfLable;
        String ifJumpLable = "L" + (currentLableCount + 1);
        String elseJumpLable = "L" + (currentLableCount + 2);
        String jumpToThisLables = "L" + (currentLableCount + 3);
        theAddrCodes.put(currentLable, currAddrCodes);
        AddressCode goToTheIf = new AddressCode();
        goToTheIf.address1 = "goto";
        goToTheIf.address2 = (theJumpLables.get(node.jjtGetChild(1).jjtGetChild(0).hashCode() + ""));
        AddressCode gotoElse = new AddressCode();
        gotoElse.address1 = "goto";
        gotoElse.address2 = (theJumpLables.get(node.jjtGetChild(2).jjtGetChild(0).hashCode() + ""));


        AddressCode enderIf = new AddressCode();
        enderIf.address1 = (jumpToThisLables);

        AddressCode gotoEnd = new AddressCode();
        gotoEnd.address1 = "goto";
        gotoEnd.address2 = (jumpToThisLables);

        currAddrCodes.add(goToTheIf);
        currAddrCodes.add(gotoElse);
        currAddrCodes.add(enderIf);
        theAddrCodes.put(currentLable, currAddrCodes);

        currAddrCodes = theAddrCodes.get(ifJumpLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }
        currAddrCodes.add(gotoEnd);
        theAddrCodes.put(ifJumpLable, currAddrCodes);
        currAddrCodes = theAddrCodes.get(elseJumpLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }
        currAddrCodes.add(gotoEnd);
        theAddrCodes.put(elseJumpLable, currAddrCodes);

        return null;
    }

    @Override
    public Object visit(ASTWhile node, Object data) {
        previousLable = currentLable;
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }
        theLabelCounter++;
        String startWhile = "L" + (theLabelCounter + 1);
        AddressCode startWhileAddressCode = new AddressCode();
        startWhileAddressCode.address1 = (startWhile);
        AddressCode retAddrCode = new AddressCode();
        retAddrCode.address1 = "while";
        currAddrCodes.add(startWhileAddressCode);
        currAddrCodes.add(retAddrCode);
        theLabelCounter++;
        node.childrenAccept(this, node);

        currentLable = previousLable;
        String ifJumpLable = "L" + (theLabelCounter);
        theLabelCounter++;
        String jumpToThisLables = "L" + (theLabelCounter);
        theAddrCodes.put(currentLable, currAddrCodes);
        AddressCode goToTheIf = new AddressCode();
        goToTheIf.address1 = "goto";
        goToTheIf.address2 =(theJumpLables.get(node.jjtGetChild(1).jjtGetChild(0).hashCode() + ""));

        AddressCode enderIf = new AddressCode();
        enderIf.address1 = (jumpToThisLables);

        AddressCode gotoTheStart = new AddressCode();
        gotoTheStart.address1 = "goto";
        gotoTheStart.address2 = (startWhile);

        currAddrCodes.add(goToTheIf);
        theAddrCodes.put(currentLable, currAddrCodes);

        currAddrCodes = theAddrCodes.get(ifJumpLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }
        currAddrCodes.add(gotoTheStart);
        theAddrCodes.put(ifJumpLable, currAddrCodes);

        currentLable = previousLable;
        theLabelCounter++;

        return null;
    }
    
    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode funcCallAddrCode = new AddressCode();
        funcCallAddrCode.address1 = ("functionCall");
        funcCallAddrCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        if(node.jjtGetNumChildren() > 1) {
            funcCallAddrCode.address3 = (node.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null).toString());
        }
        currAddrCodes.add(funcCallAddrCode);

        AddressCode goToThisAddrCode = new AddressCode();
        goToThisAddrCode.address1 = ("goto");
        goToThisAddrCode.address2 = (theJumpLables.get(node.jjtGetChild(0).jjtAccept(this,null)));

        currAddrCodes.add(goToThisAddrCode);
        theAddrCodes.put(currentLable, currAddrCodes);

        return null;
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTArg node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTID node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTDigit node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTTypeValue node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTOr node, Object data) {
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode orAddrCode = new AddressCode();
        orAddrCode.address1 = "||";
        if(node.jjtGetChild(0) instanceof ASTDigit || node.jjtGetChild(0) instanceof ASTBoolean || node.jjtGetChild(0) instanceof ASTID) {
            orAddrCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        }
        else{
            node.childrenAccept(this, data);
        }
        if(node.jjtGetChild(1) instanceof ASTDigit || node.jjtGetChild(1) instanceof ASTBoolean || node.jjtGetChild(1) instanceof ASTID) {
            orAddrCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }
        else {
            node.childrenAccept(this, data);
        }

        currAddrCodes.add(orAddrCode);
        theAddrCodes.put(currentLable, currAddrCodes);

        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode andAddrCode = new AddressCode();
        andAddrCode.address1 = "&&";
        if(node.jjtGetChild(0) instanceof ASTDigit || node.jjtGetChild(0) instanceof ASTBoolean || node.jjtGetChild(0) instanceof ASTID) {
            andAddrCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        }
        else{
            node.childrenAccept(this, data);
        }
        if(node.jjtGetChild(1) instanceof ASTDigit || node.jjtGetChild(1) instanceof ASTBoolean || node.jjtGetChild(1) instanceof ASTID) {
            andAddrCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }
        else {
            node.childrenAccept(this, data);
        }

        currAddrCodes.add(andAddrCode);
        theAddrCodes.put(currentLable, currAddrCodes);

        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
        ArrayList<AddressCode> currAddrCodes = theAddrCodes.get(currentLable);
        if(currAddrCodes == null) {
            currAddrCodes = new ArrayList<>();
        }

        AddressCode assignmentAddrCode = new AddressCode();
        assignmentAddrCode.address1 = "=";
        assignmentAddrCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        

        if(!(node.jjtGetChild(1) instanceof ASTID) && !(node.jjtGetChild(1) instanceof ASTDigit) && !(node.jjtGetChild(1) instanceof ASTBoolean)){
            node.childrenAccept(this, data);
            currAddrCodes = theAddrCodes.get(currentLable);
        }
        else{
            assignmentAddrCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }

        currAddrCodes.add(assignmentAddrCode);
        theAddrCodes.put(currentLable, currAddrCodes);

        return null;
    }

    @Override
    public Object visit(ASTComparing node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

}
