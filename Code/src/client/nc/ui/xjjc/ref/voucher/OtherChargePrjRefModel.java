package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefGridTreeModel;

public class OtherChargePrjRefModel extends AbstractRefGridTreeModel {
	public OtherChargePrjRefModel() {
		setRefNodeName("�շ�����");
	}
	
	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
	
		setRootName("����");
		setClassFieldCode(new String[] { "xj_amdb.view_airportunit.code", "xj_amdb.view_airportunit.sname"});
		setClassJoinField("xj_amdb.view_airportunit.code");
		setClassTableName("xj_amdb.view_airportunit ");
		setCodingRule("3");
		
		setFieldCode(new String[] { "xj_amdb.view_chargeproject.code",	"xj_amdb.view_chargeproject.cname", 
				"xj_amdb.view_chargeproject.obj_id"});
		setFieldName(new String[] { "����", "����", "PK" });
		setHiddenFieldCode(new String[] { "xj_amdb.view_chargeproject.airport_code", "xj_amdb.view_chargeproject.currency"});
		setTableName("xj_amdb.view_chargeproject");
		setPkFieldCode("xj_amdb.view_chargeproject.obj_id");
		setWherePart("(xj_amdb.view_chargeproject.code <> 'ZZZZ')");
		setDocJoinField("xj_amdb.view_chargeproject.airport_code");
		setOrderPart("code");
		setDefaultFieldCount(2);
		
		setDataSource("xj_amdb");
		resetFieldName();
	}
}
