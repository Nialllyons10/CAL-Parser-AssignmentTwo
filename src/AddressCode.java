public class AddressCode extends Object {
    public String address1 = "";
    public String address2 = "";
    public String address3 = "";
    public String address4 = "";

    AddressCode() {}

    public AddressCode(String a0, String b0, String c0, String d0){ 
    	address1 = a0; 
    	address2 = b0;
    	address3 = c0;
    	address4 = d0;
    }

    public String toString(){ 
    	return address1 + " " + address2 + " " + address3 + " " + address4;

    }
}