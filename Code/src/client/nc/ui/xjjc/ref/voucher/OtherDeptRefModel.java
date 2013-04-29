package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefModel;

public class OtherDeptRefModel extends AbstractRefModel {
	public OtherDeptRefModel(String refNodeName) {
		setRefNodeName(refNodeName);
	}
	
	public OtherDeptRefModel() {
		setRefNodeName("收入系统机场");
	}

	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
		setFieldCode(new String[] { "code", "sname", "obj_id" });
		setFieldName(new String[] { "编码", "名称", "PK" });
		setDefaultFieldCount(2);
		setTableName("xj_amdb.view_airportunit");
		setPkFieldCode("obj_id");
		setDataSource("xj_amdb");
		resetFieldName();
	}
}