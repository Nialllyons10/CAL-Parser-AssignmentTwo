import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

public class ThreeAddressCoder implements CALParserVisitor {

    private String currentLable = "L0";
    private String previousLable;
    private int labelCount = 0;
    private LinkedHashMap<String, ArrayList<AddressCode>> addressCodes = new LinkedHashMap<>();
    private HashMap<String, String> jumpLables = new HashMap<>();

    @Override
    public Object visit(SimpleNode node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTProgramme node, Object data) {
        System.out.println("---- 3-address code representation ----");
        node.childrenAccept(this, data);

        Set keys = addressCodes.keySet();
        if(keys.size() > 0) {
            for (Object key : keys) {
                String s = (String) key;
                ArrayList<AddressCode> a = addressCodes.get(s);
                System.out.println(s);
                
                for (AddressCode addressCode : a) {
                    System.out.println(" " + addressCode.toString());
                }
            }
        } else {
            System.out.println("Nothing declared");
        }
        System.out.println("---- End 3-address code representation ----");

        return null;
    }

   

    @Override
    public Object visit(ASTMain node, Object data) {
        currentLable = "L" + (labelCount + 1);
        previousLable = currentLable;

        node.childrenAccept(this, data);

        currentLable = previousLable;
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }
        AddressCode endMain = new AddressCode();
        endMain.address1 = "END";
        currentAddressCodes.add(endMain);
        addressCodes.put(currentLable,currentAddressCodes);
        labelCount++;

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
        currentLable = "L" + (labelCount + 1);

        jumpLables.put((String) node.jjtGetChild(1).jjtAccept(this, null), currentLable);

        node.childrenAccept(this, data);

        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode returnAddressCode = new AddressCode();
        returnAddressCode.address1 = "return";

        currentAddressCodes.add(returnAddressCode);
        addressCodes.put(currentLable, currentAddressCodes);

        currentLable = previousLable;
        labelCount++;

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
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if (currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode ac = new AddressCode();
        ac.address1 = "="; 
        ac.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        ac.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        ac.address4 = (node.jjtGetChild(2).jjtAccept(this, null).toString());
        currentAddressCodes.add(ac);
        addressCodes.put(currentLable, currentAddressCodes);
        return null;
    }

    @Override
    public Object visit(ASTVarDeclaration node, Object data) {
        return null;
    }

    @Override
    public Object visit(ASTStatementBlock node, Object data) {
     
        if(data instanceof ASTIf || data instanceof ASTWhile){
            currentLable = "L" + (labelCount + 1);
            labelCount++;
        }
        node.childrenAccept(this, data);
        if(data instanceof ASTIf || data instanceof ASTWhile){
            if (node.jjtGetNumChildren() > 0) {
                String hash = node.jjtGetChild(0).hashCode() + "";
                jumpLables.put(hash,currentLable);
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
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode returnAddressCode = new AddressCode();
        returnAddressCode.address1 = "if";
        currentAddressCodes.add(returnAddressCode);
        labelCount++;
        String currentIfLable = currentLable;
        int currentLableCount = labelCount;
        node.childrenAccept(this, node);

        currentLable = currentIfLable;
        String ifJumpLable = "L" + (currentLableCount + 1);
        String elseJumpLable = "L" + (currentLableCount + 2);
        String jumpToLable = "L" + (currentLableCount + 3);
        addressCodes.put(currentLable, currentAddressCodes);
        AddressCode gotoIf = new AddressCode();
        gotoIf.address1 = "goto";
        gotoIf.address2 = (jumpLables.get(node.jjtGetChild(1).jjtGetChild(0).hashCode() + ""));
        AddressCode gotoElse = new AddressCode();
        gotoElse.address1 = "goto";
        gotoElse.address2 = (jumpLables.get(node.jjtGetChild(2).jjtGetChild(0).hashCode() + ""));


        AddressCode endIf = new AddressCode();
        endIf.address1 = (jumpToLable);

        AddressCode gotoEnd = new AddressCode();
        gotoEnd.address1 = "goto";
        gotoEnd.address2 = (jumpToLable);

        currentAddressCodes.add(gotoIf);
        currentAddressCodes.add(gotoElse);
        currentAddressCodes.add(endIf);
        addressCodes.put(currentLable, currentAddressCodes);

        currentAddressCodes = addressCodes.get(ifJumpLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }
        currentAddressCodes.add(gotoEnd);
        addressCodes.put(ifJumpLable, currentAddressCodes);
        currentAddressCodes = addressCodes.get(elseJumpLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }
        currentAddressCodes.add(gotoEnd);
        addressCodes.put(elseJumpLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTWhile node, Object data) {
        previousLable = currentLable;
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }
        labelCount++;
        String startWhile = "L" + (labelCount + 1);
        AddressCode startWhileAddressCode = new AddressCode();
        startWhileAddressCode.address1 = (startWhile);
        AddressCode returnAddressCode = new AddressCode();
        returnAddressCode.address1 = "while";
        currentAddressCodes.add(startWhileAddressCode);
        currentAddressCodes.add(returnAddressCode);
        labelCount++;
        node.childrenAccept(this, node);

        currentLable = previousLable;
        String ifJumpLable = "L" + (labelCount);
        labelCount++;
        String jumpToLable = "L" + (labelCount);
        addressCodes.put(currentLable, currentAddressCodes);
        AddressCode gotoIf = new AddressCode();
        gotoIf.address1 = "goto";
        gotoIf.address2 =(jumpLables.get(node.jjtGetChild(1).jjtGetChild(0).hashCode() + ""));

        AddressCode endIf = new AddressCode();
        endIf.address1 = (jumpToLable);

        AddressCode gotoStart = new AddressCode();
        gotoStart.address1 = "goto";
        gotoStart.address2 = (startWhile);

        currentAddressCodes.add(gotoIf);
        addressCodes.put(currentLable, currentAddressCodes);

        currentAddressCodes = addressCodes.get(ifJumpLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }
        currentAddressCodes.add(gotoStart);
        addressCodes.put(ifJumpLable, currentAddressCodes);

        currentLable = previousLable;
        labelCount++;

        return null;
    }



    
    @Override
    public Object visit(ASTFunctionCall node, Object data) {
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode functionCallAddressCode = new AddressCode();
        functionCallAddressCode.address1 = ("functionCall");
        functionCallAddressCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        if(node.jjtGetNumChildren() > 1) {
            functionCallAddressCode.address3 = (node.jjtGetChild(1).jjtGetChild(0).jjtGetChild(0).jjtAccept(this, null).toString());
        }
        currentAddressCodes.add(functionCallAddressCode);

        AddressCode gotoAddressCode = new AddressCode();
        gotoAddressCode.address1 = ("goto");
        gotoAddressCode.address2 = (jumpLables.get(node.jjtGetChild(0).jjtAccept(this,null)));

        currentAddressCodes.add(gotoAddressCode);
        addressCodes.put(currentLable, currentAddressCodes);

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
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode orAddressCode = new AddressCode();
        orAddressCode.address1 = "||";
        if(node.jjtGetChild(0) instanceof ASTDigit || node.jjtGetChild(0) instanceof ASTBoolean || node.jjtGetChild(0) instanceof ASTID) {
            orAddressCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        }
        else{
            node.childrenAccept(this, data);
        }
        if(node.jjtGetChild(1) instanceof ASTDigit || node.jjtGetChild(1) instanceof ASTBoolean || node.jjtGetChild(1) instanceof ASTID) {
            orAddressCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }
        else {
            node.childrenAccept(this, data);
        }

        currentAddressCodes.add(orAddressCode);
        addressCodes.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTAnd node, Object data) {
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode andAddressCode = new AddressCode();
        andAddressCode.address1 = "&&";
        if(node.jjtGetChild(0) instanceof ASTDigit || node.jjtGetChild(0) instanceof ASTBoolean || node.jjtGetChild(0) instanceof ASTID) {
            andAddressCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        }
        else{
            node.childrenAccept(this, data);
        }
        if(node.jjtGetChild(1) instanceof ASTDigit || node.jjtGetChild(1) instanceof ASTBoolean || node.jjtGetChild(1) instanceof ASTID) {
            andAddressCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }
        else {
            node.childrenAccept(this, data);
        }

        currentAddressCodes.add(andAddressCode);
        addressCodes.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTBoolean node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    

    @Override
    public Object visit(ASTAssignment node, Object data) {
        ArrayList<AddressCode> currentAddressCodes = addressCodes.get(currentLable);
        if(currentAddressCodes == null) {
            currentAddressCodes = new ArrayList<>();
        }

        AddressCode asignAddressCode = new AddressCode();
        asignAddressCode.address1 = "=";
        asignAddressCode.address2 = (node.jjtGetChild(0).jjtAccept(this, null).toString());
        

        if(!(node.jjtGetChild(1) instanceof ASTID) && !(node.jjtGetChild(1) instanceof ASTDigit) && !(node.jjtGetChild(1) instanceof ASTBoolean)){
            node.childrenAccept(this, data);
            currentAddressCodes = addressCodes.get(currentLable);
        }
        else{
            asignAddressCode.address3 = (node.jjtGetChild(1).jjtAccept(this, null).toString());
        }

        currentAddressCodes.add(asignAddressCode);
        addressCodes.put(currentLable, currentAddressCodes);

        return null;
    }

    @Override
    public Object visit(ASTComparing node, Object data) {
        return ((Token) node.jjtGetValue()).image;
    }

    //Get boolean operators for the notAddressCode
    private String getBooleanOperator(Node node){
        if (node instanceof ASTAnd) {
            return "&&";
        } else if (node instanceof ASTOr) {
            return "||";
        }
        return "";
    }
}
