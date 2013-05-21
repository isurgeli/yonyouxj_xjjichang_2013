package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefGridTreeModel;

public class OtherVehicleTypeRefModel extends AbstractRefGridTreeModel {
	public OtherVehicleTypeRefModel() {
		setRefNodeName("车辆类型");
	}
	
	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
	
		setRootName("机场");
		setClassFieldCode(new String[] { "xj_amdb.view_airportunit.code", "xj_amdb.view_airportunit.sname"});
		setClassJoinField("xj_amdb.view_airportunit.code");
		setClassTableName("xj_amdb.view_airportunit ");
		setCodingRule("3");
		
		setFieldCode(new String[] { "xj_amdb.view_chargeproject.code",	"xj_amdb.view_chargeproject.cname", 
				"xj_amdb.view_chargeproject.airport_code", "xj_amdb.view_chargeproject.obj_id"});
		setFieldName(new String[] { "编码", "名称", "机场", "PK" });
		setTableName("xj_amdb.view_chargeproject");
		setPkFieldCode("xj_amdb.view_chargeproject.obj_id");
		setWherePart("(xj_amdb.view_chargeproject.code >= '3101' And xj_amdb.view_chargeproject.code <= '3121')");
		setDocJoinField("xj_amdb.view_chargeproject.airport_code");
		setOrderPart("code");
		setDefaultFieldCount(3);
		
		setDataSource("xj_amdb");
		resetFieldName();
	}
}
