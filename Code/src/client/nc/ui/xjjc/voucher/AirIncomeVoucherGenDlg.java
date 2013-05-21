package nc.ui.xjjc.voucher;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;

import nc.bs.framework.common.NCLocator;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.xjjc.voucher.IAirIncomeVoucherDataService;
import nc.itf.xjjc.voucher.VoucherBizType;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIButton;
import nc.ui.pub.beans.UICheckBox;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pub.beans.UILabel;
import nc.ui.pub.beans.UIList;
import nc.ui.pub.beans.UIPanel;
import nc.ui.pub.beans.UIRadioButton;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.UIScrollPane;
import nc.ui.pub.beans.UITextField;
import nc.ui.pub.tools.BannerDialog;
import nc.ui.xjjc.ref.voucher.BizTypeRefModel;
import nc.ui.xjjc.ref.voucher.CheckListManager;
import nc.ui.xjjc.ref.voucher.OtherDeptRefModel;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.xjjc.voucher.AirCorpVO;
import nc.vo.xjjc.voucher.FakeVoucherVO;

public class AirIncomeVoucherGenDlg extends UIDialog implements ActionListener, Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private UIButton btnOk;
	private UIButton btnCacnel;
	private UIButton btnQuery;
	private UIRefPane comboAirport;
	private UIRefPane refVoucherType;
	private UITextField txtExplain;
	private UIRefPane comboBizType;
	private UIRefPane refEndPeriod;
	private UIRefPane refStartPeriod;
	private UIRadioButton rbAccPeriod;
	private UIRefPane refEndDate;
	private UIRefPane refStartDate;
	private UIRadioButton rbDate;
	private UICheckBox ckSelectAll;
	private UIList listAirline;
	private ButtonGroup bgOnly;
	private CheckListManager checkListManager;
	
	private String sAccMonth,eAccMonth,airPort,airPortname;
	private AirCorpVO[] airCorps;
	private UICheckBox ckUseDollar;
	private UITextField txtDollarRatio;
	private UILabel labelDollarRatio;
	private String m_userID;
	
	public FakeVoucherVO[] retData;

	public void setM_userID(String m_userID) {
		this.m_userID = m_userID;
	}


	public AirIncomeVoucherGenDlg(Container parent) {
		super(parent, "航空收入凭证生成");
		initUI();
	}


	private void initUI() {
		setSize(670, 300);
		setLayout(new BorderLayout());
		UIPanel centerPanel = getCenterPanel();
		add(centerPanel, BorderLayout.WEST);
		UIPanel rightPanel = getRightPanel();
		add(rightPanel, BorderLayout.CENTER);
		add(Box.createHorizontalStrut(6), BorderLayout.EAST);

		UIPanel buttonPanel = new UIPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
		buttonPanel.add(getBtnQuery());
		buttonPanel.add(getBtnOk());
		buttonPanel.add(getBtnCacnel());
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private UIPanel getRightPanel() {
		UIPanel rightPanel = new UIPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.add(Box.createVerticalStrut(6));
		UIScrollPane panel1 = new UIScrollPane();
		panel1.setViewportView(getAirlineList());
		panel1.setPreferredSize(new Dimension(193, 180));
		rightPanel.add(panel1); 
		//rightPanel.add(Box.createVerticalStrut(25));
		UIPanel panel2 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
		panel2.add(getSelectAll()); 
		panel2.add(getUseDollar());
		panel2.add(getDollarRatioLabel());
		panel2.add(getDollarRatio());
		rightPanel.add(panel2); 
		
		return rightPanel;
	}

	public UICheckBox getUseDollar() {
		if(ckUseDollar == null){
			ckUseDollar = new UICheckBox("外航使用美元");
			ckUseDollar.setVisible(false);
		}
		return ckUseDollar;
	}
	
	public UITextField getDollarRatio() {
		if(txtDollarRatio == null){
			txtDollarRatio = new UITextField("汇率");
			txtDollarRatio.setVisible(false);
		}
		return txtDollarRatio;
	}
	
	public UILabel getDollarRatioLabel() {
		if(labelDollarRatio == null){
			labelDollarRatio = new UILabel("汇率 ");
			labelDollarRatio.setVisible(false);
		}
		return labelDollarRatio;
	}

	private UICheckBox getSelectAll() {
		if(ckSelectAll == null){
			ckSelectAll = new UICheckBox("全选");
			ckSelectAll.setSize(50, 22);
			ckSelectAll.setPreferredSize(new java.awt.Dimension(50, 22));
			ckSelectAll.setMaximumSize(new java.awt.Dimension(50, 22));
			ckSelectAll.setMinimumSize(new java.awt.Dimension(50, 22));
			ckSelectAll.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED){
						checkListManager.getSelectionModel().addSelectionInterval(0, listAirline.getModel().getSize());
						listAirline.repaint();
					}
					else{
						checkListManager.getSelectionModel().removeSelectionInterval(0, listAirline.getModel().getSize());
						listAirline.repaint();
					}
				}
			});
		}
		return ckSelectAll;
	}


	private UIList getAirlineList() {
		if (listAirline==null){
			listAirline = new UIList();
			checkListManager = new CheckListManager(listAirline); 
		}
		return listAirline;
	}


	private UIPanel getCenterPanel() { 
		UIPanel centerPanel = new UIPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		
		UIPanel panel1 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel1.add(getRadioBtnDate()); 
		panel1.add(new UILabel("开始日期")); 
		panel1.add(getRefStartDate()); 
		panel1.add(new UILabel("结束日期")); 
		panel1.add(getRefEndDate());
		centerPanel.add(panel1);
		
		UIPanel panel2 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel2.add(getRadioBtnAccPeriod()); 
		panel2.add(new UILabel("开始期间"));
		panel2.add(getRefStartPeriod());
		panel2.add(new UILabel("结束期间"));
		panel2.add(getRefEndPeriod());
		centerPanel.add(panel2);
		
		UIPanel panel3 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel3.add(Box.createHorizontalStrut(20));
		panel3.add(new UILabel("业务类别"));
		panel3.add(getComboBizType());
		centerPanel.add(panel3);
		
		UIPanel panel4 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel4.add(Box.createHorizontalStrut(20));
		panel4.add(new UILabel("默认摘要"));
		panel4.add(getTextExplain());
		centerPanel.add(panel4);
		
		UIPanel panel5 = new UIPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		panel5.add(Box.createHorizontalStrut(20));
		panel5.add(new UILabel("凭证类别"));
		panel5.add(getRefVoucherType());
		panel5.add(new UILabel("开票机场"));
		panel5.add(getComboAirport());
		centerPanel.add(panel5);
		
		return centerPanel;
	}


	private UIRefPane getComboAirport() {
		if (comboAirport==null){
			comboAirport = new UIRefPane();
			comboAirport.setRefModel(new OtherDeptRefModel());
			comboAirport.setPK(2);
		}
		return comboAirport;
	}

	public UIRefPane getRefVoucherType() {
		if (refVoucherType==null){
			refVoucherType = new UIRefPane("凭证类别");
			refVoucherType.setPK("0001DEFAULT000000001");
		}
		return refVoucherType;
	}

	public UITextField getTextExplain() {
		if (txtExplain==null){
			txtExplain = new UITextField();
			txtExplain.setSize(300, 22);
			txtExplain.setPreferredSize(new java.awt.Dimension(300, 22));
			txtExplain.setMaximumSize(new java.awt.Dimension(300, 22));
			txtExplain.setMinimumSize(new java.awt.Dimension(300, 22));
		}
		return txtExplain;
	}

	private UIRefPane getComboBizType() {
		if (comboBizType==null){
			comboBizType = new UIRefPane();
			comboBizType.setRefModel(new BizTypeRefModel(1));
			comboBizType.setPK(VoucherBizType.LIFTLANDFEE.getValue());
		}
		return comboBizType;
	}

	private UIRefPane getRefEndPeriod() {
		if (refEndPeriod==null){
			refEndPeriod = new UIRefPane("会计期间");
			refEndPeriod.setPK(refStartPeriod.getRefPK());
		}
		return refEndPeriod;
	}

	private UIRefPane getRefStartPeriod() {
		if (refStartPeriod==null){
			refStartPeriod = new UIRefPane("会计期间");
			IUAPQueryBS dao = NCLocator.getInstance().lookup(IUAPQueryBS.class);
			UFDate today = new UFDate();
			try {
				@SuppressWarnings("unchecked")
				Vector<Vector<Object>> data = (Vector<Vector<Object>>)dao.executeQuery("select pk_accperiodmonth from bd_accperiodmonth where begindate<'"+today.toString()+
						"' and enddate>'"+today.toString()+"'", new VectorProcessor());
				if (data.size()>0) refStartPeriod.setPK(data.get(0).get(0).toString());
			} catch (BusinessException e) {
				e.printStackTrace();
			}
		}
		return refStartPeriod;
	}

	private UIRadioButton getRadioBtnAccPeriod() {
		if (rbAccPeriod==null){
			rbAccPeriod = new UIRadioButton();
			rbAccPeriod.setSize(20, 22);
			rbAccPeriod.setPreferredSize(new java.awt.Dimension(20, 22));
			rbAccPeriod.setMaximumSize(new java.awt.Dimension(20, 22));
			rbAccPeriod.setMinimumSize(new java.awt.Dimension(20, 22));
			
			ButtonGroup bg = getButtonGroup();
			bg.add(rbAccPeriod);
		}
		return rbAccPeriod;
	}

	private ButtonGroup getButtonGroup() {
		if (bgOnly==null){
			bgOnly = new ButtonGroup();
		}
		return bgOnly;
	}


	private UIRefPane getRefEndDate() {
		if (refEndDate==null){
			refEndDate = new UIRefPane("日历");
			UFDate today = new UFDate();
			refEndDate.setValue(today.getDateAfter(today.getDaysMonth()-today.getDay()).toString());
		}
		return refEndDate;
	}

	private UIRefPane getRefStartDate() {
		if (refStartDate==null){
			refStartDate = new UIRefPane("日历");
			UFDate today = new UFDate();
			refStartDate.setValue(today.getDateBefore(today.getDay()-1).toString());
		}
		return refStartDate;
	}

	private UIRadioButton getRadioBtnDate() {
		if (rbDate==null){
			rbDate = new UIRadioButton("se",true);
			rbDate.setSize(20, 22);
			rbDate.setPreferredSize(new java.awt.Dimension(20, 22));
			rbDate.setMaximumSize(new java.awt.Dimension(20, 22));
			rbDate.setMinimumSize(new java.awt.Dimension(20, 22));
			
			ButtonGroup bg = getButtonGroup();
			bg.add(rbDate);
		}
		return rbDate;
	}
	
	private UIButton getBtnCacnel() {
		if (btnCacnel == null) {
			btnCacnel = new UIButton("取消");
			btnCacnel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeCancel();
				}
			});
		}
		return btnCacnel;
	}

	private UIButton getBtnOk() {
		if (btnOk == null) {
			btnOk = new UIButton("确定");
			btnOk.addActionListener(this);
		}
		return btnOk;
	}
	
	private UIButton getBtnQuery() {
		if (btnQuery == null) {
			btnQuery = new UIButton("获得开票对象");
			btnQuery.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getDollarRatio().setVisible(false);
					getDollarRatioLabel().setVisible(false);
					getUseDollar().setVisible(false);
					getUseDollar().setSelected(false);
					if(getRadioBtnDate().isSelected()){
						sAccMonth = getRefStartDate().getRefPK().substring(0, 7);
						eAccMonth = getRefEndDate().getRefPK().substring(0, 7);
					}else{
						sAccMonth = getRefStartPeriod().getRefName();
						eAccMonth = getRefEndPeriod().getRefName();
					}
					airPort = getComboAirport().getRefCode();
					airPortname = getComboAirport().getRefName();
					
					IAirIncomeVoucherDataService service = NCLocator.getInstance().lookup(IAirIncomeVoucherDataService.class);
					airCorps = service.getAirCorpNeedGenVoucher(sAccMonth, eAccMonth, airPort);
					
					listAirline.setListData(airCorps);
					
					boolean haveForeign=false;
					for(int i=0;i<airCorps.length;i++){
						if (airCorps[i].getType()==2) {
							haveForeign = true;
							break;
						}
					}
					if (haveForeign){
						getDollarRatio().setVisible(true);
						getDollarRatioLabel().setVisible(true);
						getUseDollar().setVisible(true);
					}
				}
			});
		}
		return btnQuery;
	}

	public void actionPerformed(ActionEvent e) {
		new Thread(this).start();
		//genrateTheVoucher();
	}

	private void genrateTheVoucher() {
		UFDouble raito = new UFDouble(1.0);
		if (getUseDollar().isSelected()){
			try{
				raito = new UFDouble(getDollarRatio().getText());
			}
			catch (Exception ex){
				MessageDialog.showWarningDlg(null, null, "请填写正确的汇率。");
				return;
			}
		}
		
		IAirIncomeVoucherDataService service = NCLocator.getInstance().lookup(IAirIncomeVoucherDataService.class);
		String pk_voucherType = getRefVoucherType().getRefPK();
		String explain = getTextExplain().getText();
		
		boolean haveForeign = false;
		ArrayList<String> selectAirCorp = new ArrayList<String>();
		for(int i=0;i<airCorps.length;i++)
			if (checkListManager.getSelectionModel().isSelectedIndex(i)){
				selectAirCorp.add(airCorps[i].getCode());
				if (airCorps[i].getType()==2) haveForeign = true;
			}
		
		boolean useDollar = false;
		
		if (haveForeign){
			useDollar = getUseDollar().isSelected();
			raito = useDollar?new UFDouble(getDollarRatio().getText()):new UFDouble(1);
		}
		
		try {
			retData = service.genVoucherForAirCorp(sAccMonth, eAccMonth, airPort, airPortname, 
					pk_voucherType, explain, selectAirCorp.toArray(new String[0]), useDollar, raito, m_userID);
		} catch (BusinessException ex) {
			MessageDialog.showWarningDlg(this, "错误", ex.getMessage());
			ex.printStackTrace();
			return;
		}
		
		closeOK();
	}

	public void run() {
		BannerDialog dialog = new BannerDialog(this);
		dialog.setStartText("凭证生成中。。。请稍候！");
		try {
			dialog.start();
			Thread.sleep(500); //等待1秒
			genrateTheVoucher();
		}catch (Exception e) {
			MessageDialog.showHintDlg(null, "错误", e.getMessage());
		} 
		
		dialog.end();
	}
}
