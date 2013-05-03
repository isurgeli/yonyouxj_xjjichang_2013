package nc.itf.xjjc.voucher;

public enum VoucherBizType {
	LIFTLANDFEE("XJJC0000000000000006"),
	//LIFTLANDFEEIN("XJJC0000000000000007"),
	RENTREVED("XJJC0000000000000009"),
	RENTRECEIVABLE("XJJC0000000000000010"),
	ADVANCEREVCHANGE("XJJC0000000000000011");

	private final String value;
	 
	private VoucherBizType(String value) {
	     this.value = value;
	}
	
	public String getValue() {
	     return value;
	}
}
