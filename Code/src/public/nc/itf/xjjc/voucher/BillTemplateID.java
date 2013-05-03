package nc.itf.xjjc.voucher;

public enum BillTemplateID {
	SUBJASSMAP("10081831"),
	ACCSUBJMAP("10081832"),
	AIRINCOMEVOUCHER("20021070"),
	VOUCHERQUERY("20021025");
		
	private final String value;
	 
	private BillTemplateID(String value) {
	     this.value = value;
	}
	
	public String getValue() {
	     return value;
	}
}
