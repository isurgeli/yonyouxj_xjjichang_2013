package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefModel;

public class OtherRentPrjRefModel extends AbstractRefModel {
	public OtherRentPrjRefModel() {
		setRefNodeName("×âÁÞÀàÐÍ");
	}

	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
		setFieldCode(new String[] { "code", "cname", "to_number(code)" });
		setFieldName(new String[] { "±àÂë", "Ãû³Æ", "PK" });
		setDefaultFieldCount(2);
		setTableName("xj_amdb.view_rent_chargeproject");
		setPkFieldCode("to_number(code)");
		setDataSource("xj_amdb");
		resetFieldName();
	}
}