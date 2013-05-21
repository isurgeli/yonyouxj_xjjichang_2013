package nc.ui.xjjc.ref.voucher;

import nc.ui.bd.ref.AbstractRefGridTreeModel;

public class OtherAircorpRefModel extends AbstractRefGridTreeModel {
	public OtherAircorpRefModel() {
		setRefNodeName("���չ�˾");
	}
	
	public void setRefNodeName(String refNodeName) {
		m_strRefNodeName = refNodeName;
	
		setRootName("����");
		setClassFieldCode(new String[] { "xj_amdb.view_airportunit.code", "xj_amdb.view_airportunit.sname"});
		setClassJoinField("xj_amdb.view_airportunit.code");
		setClassTableName("xj_amdb.view_airportunit ");
		setCodingRule("3");
		
		setFieldCode(new String[] { "xj_amdb.view_payer.iata_code",	"xj_amdb.view_payer.fname", "xj_amdb.view_payer.airport_code", "xj_amdb.view_payer.obj_id"});
		setFieldName(new String[] { "����", "����", "����", "PK" });
		setTableName("xj_amdb.view_payer");
		setPkFieldCode("xj_amdb.view_payer.obj_id");
		setWherePart("xj_amdb.view_payer.airlines='1'");
		setDocJoinField("xj_amdb.view_payer.airport_code");
		setOrderPart("iata_code");
		setDefaultFieldCount(3);
		
		setDataSource("xj_amdb");
		resetFieldName();
	}
}
