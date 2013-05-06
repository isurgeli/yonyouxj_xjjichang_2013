package nc.itf.xjjc.voucher;

public enum BDInfo {
	CUSTOMER("00010000000000000073"),
	DEPARTMENT("00010000000000000002"),
	VEHICLE("0001A11000000000022E"),
	CLEAROBJECT("0001A1100000000002AM"),
	CONTRACT("0001A110000000000299"),
	TERMINAL("0001A1100000000001ZO");
	
	
	private final String value;
	 
	private BDInfo(String value) {
	     this.value = value;
	}
	
	public String getValue() {
	     return value;
	}
}
