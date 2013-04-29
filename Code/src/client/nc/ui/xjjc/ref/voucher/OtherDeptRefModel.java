package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefModel;

public class OtherDeptRefModel extends AbstractRefModel {
	public OtherDeptRefModel(String refNodeName) {
		setRefNodeName(refNodeName);
	}
	
	public OtherDeptRefModel() {
		setRefNodeName("����ϵͳ����");
	}

	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
		setFieldCode(new String[] { "code", "sname", "obj_id" });
		setFieldName(new String[] { "����", "����", "PK" });
		setDefaultFieldCount(2);
		setTableName("xj_amdb.view_airportunit");
		setPkFieldCode("obj_id");
		setDataSource("xj_amdb");
		resetFieldName();
	}
}