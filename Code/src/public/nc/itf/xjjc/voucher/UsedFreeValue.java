package nc.itf.xjjc.voucher;

public enum UsedFreeValue {
	CUSTOMER("XJJC0000000000000004"),
	DEPARTMENT("XJJC0000000000000002"),
	AIRLINE("XJJC0000000000000001"),
	VEHICLE("XJJC0000000000000003");
	
	
	private final String value;
	 
	private UsedFreeValue(String value) {
	     this.value = value;
	}
	
	public String getValue() {
	     return value;
	}
}
