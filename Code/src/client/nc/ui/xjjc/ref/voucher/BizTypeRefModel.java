package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefModel;

public class BizTypeRefModel extends AbstractRefModel {
	public BizTypeRefModel() {
		setRefNodeName("ҵ�����");
	}

	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
		setFieldCode(new String[] { "vcode", "vname", "pk_subjbiztype" });
		setFieldName(new String[] { "����", "����", "PK" });
		setDefaultFieldCount(2);
		setTableName("xjjc_bd_accsubjbiztype");
		setPkFieldCode("pk_subjbiztype");
		setWherePart("vcode>='0101' and vcode < '02'" );
		resetFieldName();
	}
}