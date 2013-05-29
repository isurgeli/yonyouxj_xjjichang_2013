package nc.ui.xjjc.voucher;

import java.util.ArrayList;

import javax.swing.JTable;

import nc.bs.framework.common.NCLocator;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.xjjc.voucher.BillTemplateID;
import nc.itf.xjjc.voucher.IAirIncomeVoucherDataService;
import nc.jdbc.framework.processor.BeanListProcessor;
import nc.ui.pub.ButtonObject;
import nc.ui.pub.ToftPanel;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillEditListener;
import nc.ui.pub.bill.BillListPanel;
import nc.ui.pub.linkoperate.ILinkMaintainData;
import nc.ui.trade.query.HYQueryConditionDLG;
import nc.ui.trade.query.INormalQuery;
import nc.ui.uap.sf.SFClientUtil;
import nc.ui.xjjc.rentvoucher.RentIncomeVoucherGenDlg;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.lang.UFDouble;
import nc.vo.querytemplate.TemplateInfo;
import nc.vo.xjjc.voucher.AirCorpVO;
import nc.vo.xjjc.voucher.FakeVoucherVO;

public class AirIncomeVoucher extends ToftPanel implements BillEditListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* 操作控件 */
	private ButtonObject m_Query;

	private ButtonObject m_GenVoucher;
	
	private ButtonObject m_ShowVoucher;
	
	private ButtonObject m_RentVoucher;

	private ButtonObject[] m_Ary;

	private BillListPanel m_billListPanel;
	
	/* 业务数据 */
	private String m_pkCorp;
	
	private String m_userID;

	private String m_billType;
	
	public AirIncomeVoucher() {
		super();
		initialize();
	}
	
	public void initialize() {
		try {
			/* initialize templete parameters */
			setPKCorp(getClientEnvironment().getCorporation().getPrimaryKey());
			setBillType(BillTemplateID.AIRINCOMEVOUCHER.getValue());
			setUserID(getClientEnvironment().getUser().getPrimaryKey());

			setName("AirVoucgerTempletUI");
			//		setLayout(new BorderLayout());
			setSize(774, 419);

			/* load business area panes */
			add(getBillListPanel());

			/* load operational buttons and set 'lock|unlock' relationship */
			m_Query = new ButtonObject("查询" ,"查询", 2, "查询"); /*-=notranslate=-*/
			m_Query.setEnabled(true);
			m_GenVoucher = new ButtonObject("生成凭证" ,"生成凭证", 2, "生成凭证");
			m_GenVoucher.setEnabled(true);
			m_ShowVoucher = new ButtonObject("查看凭证" ,"查看凭证", 2, "查看凭证");
			m_ShowVoucher.setEnabled(true);
			m_RentVoucher = new ButtonObject("转租赁凭证" ,"转租赁凭证", 2, "转租赁凭证");
			m_RentVoucher.setEnabled(true);

			m_Ary = new ButtonObject[] { m_Query, m_GenVoucher, m_ShowVoucher, m_RentVoucher};
			setButtons(m_Ary);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setPKCorp(String newM_pkCorp) {
		m_pkCorp = newM_pkCorp;
	}
	
	public void setBillType(String newM_billType) {
		m_billType = newM_billType;
	}
	
	public void setUserID(String newM_userID) {
		m_userID = newM_userID;
	}
	
	private BillListPanel getBillListPanel() {
		if (m_billListPanel == null) {
			try {
				m_billListPanel = new BillListPanel();
				m_billListPanel.setName("数据列表窗格");
				m_billListPanel.setEnabled(false);

				/* load address templete */
				m_billListPanel.loadTemplet(m_billType, null, m_userID,m_pkCorp);
				m_billListPanel.getHeadItem("totaldebit").setDecimalDigits(2);
				m_billListPanel.getHeadItem("totalcredit").setDecimalDigits(2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m_billListPanel;
	}
	
	public void afterEdit(BillEditEvent e) {
	}

	public void bodyRowChange(BillEditEvent e) {
	}

	public String getTitle() {
		return "航空收入凭证";
	}

	@Override
	public void onButtonClicked(ButtonObject bo) {
		if(bo == m_Query){
			showHintMessage("开始进行查询，请等待......");
			doQuery();
			showHintMessage("凭证查询完成,耗时(ms):");
		} else if(bo == m_ShowVoucher){
			showHintMessage("开始查看凭证。");
			doShowVoucher();
			showHintMessage("结束查看凭证。");
		} else if(bo == m_GenVoucher){
			showHintMessage("开始生成业务凭证。");
			doGenVoucher();
			showHintMessage("结束生成业务凭证。");
		} else if(bo == m_RentVoucher){
			showHintMessage("开始生成租赁业务凭证。");
			doGenRentVoucher();
			showHintMessage("结束生成租赁业务凭证。");
		}
	}

	private void doGenRentVoucher() {
		RentIncomeVoucherGenDlg dlg = new RentIncomeVoucherGenDlg(this);
		dlg.setM_userID(m_userID);
		dlg.showModal();
	}

	private void doGenVoucher() {
		AirIncomeVoucherGenDlg dlg = new AirIncomeVoucherGenDlg(this);
		dlg.setM_userID(m_userID);
		dlg.showModal();
		if (dlg.getResult()==UIDialog.ID_OK){
			if (dlg.retData!=null)
			getBillListPanel().getHeadBillModel().clearBodyData();
			getBillListPanel().setHeaderValueVO((CircularlyAccessibleValueObject[]) dlg.retData);
			getBillListPanel().getHeadBillModel().execLoadFormula();
		}
	}

	@SuppressWarnings("restriction")
	private void doShowVoucher() {
		FakeVoucherVO vo=null;
		
		FakeVoucherVO[] vos = (FakeVoucherVO[]) getBillListPanel().getHeadBillModel().getBodyValueVOs(FakeVoucherVO.class.getName());
		int rowNo = ((JTable) getBillListPanel().getHeadTable())
				.getSelectedRow();

		if (rowNo >= 0 && rowNo < vos.length) {
			vo = vos[rowNo];
			final String billID = vo.getPk_voucher();
			ILinkMaintainData data = new ILinkMaintainData() {
				public String getBillID() {
					return billID;
				}

				public Object getUserObject() {
					return null;
				}
			};
			SFClientUtil.openLinkedMaintainDialog(BillTemplateID.VOUCHERMAKE.getValue(), this, data);
		} else {
			MessageDialog.showWarningDlg(this, null, "请先选中需要查看的凭证。");
		}
	}

	private void doQuery() {
		TemplateInfo tempinfo = new TemplateInfo();
		tempinfo.setPk_Org(m_pkCorp);
		tempinfo.setCurrentCorpPk(m_pkCorp);
		tempinfo.setFunNode(BillTemplateID.AIRINCOMEVOUCHER.getValue());
		tempinfo.setUserid(m_userID);
		tempinfo.setBusiType(null);
		tempinfo.setNodekey(null);
		
		StringBuffer sql = new StringBuffer();
		sql.append("select gl_voucher.pk_voucher, vinvoname,vinvoperiod,ninvoamount,vinvoaircorp,vinvoairport "
				+"from xjjc_gl_fakevoucher, gl_voucher where xjjc_gl_fakevoucher.pk_voucher=gl_voucher.pk_voucher and ");

		HYQueryConditionDLG querydialog = new HYQueryConditionDLG(this, null, tempinfo);
		
		if (querydialog.showModal() != UIDialog.ID_OK)
			return;
		
		INormalQuery query = (INormalQuery) querydialog;

		String strWhere = query.getWhereSql();
		if(strWhere!=null){
			strWhere = strWhere.replace("pk_voucher", "gl_voucher.pk_voucher");
			strWhere = strWhere.replace("gl_voucherbackup", "gl_voucher");
		}
		
		if (strWhere == null || strWhere.trim().length()==0)
			strWhere = "1=1";
		
		strWhere = "(" + strWhere + ") and (isnull(gl_voucher.dr,0)=0) and xjjc_gl_fakevoucher.pk_corp='"+m_pkCorp+"' order by gl_voucher.no";
		
		sql.append(strWhere);
		
		IUAPQueryBS dao = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		
		try {
			@SuppressWarnings("unchecked")
			ArrayList<FakeVoucherVO> resultVOs = (ArrayList<FakeVoucherVO>) dao.executeQuery(sql.toString(), new BeanListProcessor(FakeVoucherVO.class));
			getBillListPanel().getHeadBillModel().clearBodyData();
			getBillListPanel().setHeaderValueVO((CircularlyAccessibleValueObject[]) resultVOs.toArray(new CircularlyAccessibleValueObject[0]));
			getBillListPanel().getHeadBillModel().execLoadFormula();
		} catch (BusinessException e) {
			e.printStackTrace();
		}	
	}
}
