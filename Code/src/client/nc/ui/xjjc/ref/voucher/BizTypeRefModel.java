package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefModel;

public class BizTypeRefModel extends AbstractRefModel {
	public BizTypeRefModel(int type) { //1 �������� 2 ��������
		setRefNodeName("ҵ�����", type);
	}

	public void setRefNodeName(String refNodeName, int type) {
		m_strRefNodeName = refNodeName;
		setFieldCode(new String[] { "vcode", "vname", "pk_subjbiztype" });
		setFieldName(new String[] { "����", "����", "PK" });
		setDefaultFieldCount(2);
		setTableName("xjjc_bd_accsubjbiztype");
		setPkFieldCode("pk_subjbiztype");
		if (type==1)
			setWherePart("vcode>='0101' and vcode < '02'" );
		else
			setWherePart("vcode>='0201' and vcode < '03'" );
		resetFieldName();
	}
}