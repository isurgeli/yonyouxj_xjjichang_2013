package nc.itf.xjjc.voucher;

public enum BillTemplateID {
	SUBJASSMAP("10081831"),
	ACCSUBJMAP("10081832");
		
	private final String value;
	 
	private BillTemplateID(String value) {
	     this.value = value;
	}
	
	public String getValue() {
	     return value;
	}
}
