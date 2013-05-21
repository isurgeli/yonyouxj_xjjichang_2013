package nc.ui.xjjc.voucher;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.xjjc.voucher.BDInfo;
import nc.itf.xjjc.voucher.BillTemplateID;
import nc.itf.xjjc.voucher.ISubjAssMapDataService;
import nc.itf.xjjc.voucher.UsedFreeValue;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.ui.bd.ref.AbstractRefModel;
import nc.ui.bd.util.XTreePane;
import nc.ui.ml.NCLangRes;
import nc.ui.pub.ButtonObject;
import nc.ui.pub.ClientEnvironment;
import nc.ui.pub.ToftPanel;
import nc.ui.pub.beans.MessageDialog;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pub.beans.UIPanel;
import nc.ui.pub.beans.UIRefPane;
import nc.ui.pub.beans.UISplitPane;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillEditEvent;
import nc.ui.pub.bill.BillEditListener;
import nc.ui.pub.bill.BillItem;
import nc.ui.pub.bill.BillListPanel;
import nc.ui.xjjc.ref.voucher.OtherAircorpRefModel;
import nc.ui.xjjc.ref.voucher.OtherCustomerRefModel;
import nc.ui.xjjc.ref.voucher.OtherDeptRefModel;
import nc.ui.xjjc.ref.voucher.OtherVehicleTypeRefModel;
import nc.vo.ml.NCLangRes4VoTransl;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.util.tree.MethodGroup;
import nc.vo.util.tree.TreeDetail;
import nc.vo.util.tree.XTreeModel;
import nc.vo.util.tree.XTreeNode;
import nc.vo.xjjc.freevaluemap.AssValueMapVO;
import nc.vo.xjjc.freevaluemap.UsedFreeValueVO;

/**
 * 
 * 创建日期：(2013-4-17 13:33:00)
 * 
 * @author：李歆涛
 */
public class SubjAssMap extends ToftPanel implements TreeSelectionListener,
		BillEditListener, MouseListener {
	private static final String GET_VNAME = "getVname";

	private static final String GET_VCODE = "getVcode";

	private static final String FAKEOBJ_ID = "fakeobj_id";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* 窗体控件 */
	private XTreePane m_treePane;

	private UISplitPane m_splitPane;

	private UIPanel m_mainDataPanel;

	/* 操作控件 */
	private ButtonObject m_Add;

	private ButtonObject m_Edit;

	private ButtonObject m_Del;

	private ButtonObject m_Save;
	
	private ButtonObject m_Refresh;

	private ButtonObject m_Cancel;

	private ButtonObject[] m_Ary;

	private BillListPanel m_billListPanel;

	private BillCardPanel m_billCardPanel;

	/* 左树数据参照控件 */
	private TreeDetail m_treeDetail;

	private XTreeModel m_treeModel;

	/* 业务数据 */
	private String m_pkCorp;

	private String m_billType;

	private String m_busiType;

	private String m_userID;

	private String m_curPk_usedfreevalue;
	
	private String m_curPk_bdinfo;

	/* 功能切换控制 */
	private int m_State;

	private final static int _BROWSE_ = 0;

	private final static int _ADD_ = 1;

	private final static int _EDIT_ = 2;

	private final static String _CORP_SET_ = "0001"; /* 集团 */

	/**
	 * SubjAssMap 构造子注解。
	 */
	public SubjAssMap() {
		super();
		initialize();
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-5 9:41:41)
	 * @param e
	 *            nc.ui.pub.bill.BillEditEvent
	 */
	public void afterEdit(BillEditEvent e) {
		if (e.getKey().equals(AssValueMapVO.PK_FREEVALUE)) 
			getBillCardPanel().execHeadEditFormulas();
		else if (e.getKey().equals(FAKEOBJ_ID)) {
			UIRefPane ref = (nc.ui.pub.beans.UIRefPane)getBillCardPanel().getHeadItem(FAKEOBJ_ID).getComponent();
			Vector cur = ref.getRefModel().getSelectedData();
			if (cur!=null && cur.size()>0){
				getBillCardPanel().getHeadItem(AssValueMapVO.VOTHERCODE).setValue(((Vector)cur.get(0)).get(0));
				getBillCardPanel().getHeadItem(AssValueMapVO.VOTHERNAME).setValue(((Vector)cur.get(0)).get(1));
				if (((Vector)cur.get(0)).size()>3){
					getBillCardPanel().getHeadItem(AssValueMapVO.OBJ_ID).setValue(Integer.parseInt(((Vector)cur.get(0)).get(3).toString()));
					getBillCardPanel().getHeadItem(AssValueMapVO.VOTHERBIZ).setValue(((Vector)cur.get(0)).get(2));
				}else{
					getBillCardPanel().getHeadItem(AssValueMapVO.OBJ_ID).setValue(Integer.parseInt(((Vector)cur.get(0)).get(2).toString()));
				}
			}
		}
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-5 9:42:25)
	 * 
	 * @param e
	 *            nc.ui.pub.bill.BillEditEvent
	 */
	public void bodyRowChange(BillEditEvent e) {
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 20:15:45)
	 * 
	 * @return nc.ui.pub.beans.UIPanel
	 */
	private UIPanel createMainDataPanel() {
		BillListPanel tempBillListPanel = null;
		BillCardPanel tempBillCardPanel = null;

		if (m_mainDataPanel == null) {
			try {
				m_mainDataPanel = new UIPanel();
				m_mainDataPanel.setName("数据维护窗格");

				/* create 'bill list panel' */
				tempBillListPanel = getBillListPanel();

				/* create 'bill card panel' */
				tempBillCardPanel = getBillCardPanel();

				/* set 'CardLayout' to resolve refresh problem */
				m_mainDataPanel.setLayout(new CardLayout());

				/* add 'bill list panel' and 'bill card panel' */
				m_mainDataPanel.add(tempBillListPanel, tempBillListPanel
						.getName());
				m_mainDataPanel.add(tempBillCardPanel, tempBillCardPanel
						.getName());
				getBillListPanel().getHeadTable().addMouseListener(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m_mainDataPanel;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-3 15:01:03)
	 * 
	 * @return nc.ui.bd.util.XTreePane
	 */
	private XTreePane createTreePanel() {
		UsedFreeValueVO uFreeValueVOs[];

		if (m_treePane == null) {
			try {
				m_treePane = new XTreePane();
				m_treePane.setName("树形窗格");
				m_treePane.setTreeDetail(getTreeConfig());
				/* add tree listener */
				m_treePane.gettree().addTreeSelectionListener(this);
				/* build tree model */
				m_treeModel = (XTreeModel) m_treePane.gettree().getModel();
				uFreeValueVOs = getSubjAssMapDataService().queryUesdAssSubjByCorpPk(ClientEnvironment.getInstance().getCorporation().getPk_corp());
				m_treeModel.createTree(uFreeValueVOs);
				m_treePane.setPreferredSize(new Dimension(210, 300));
				m_treePane.setMinimumSize(new Dimension(150, 300));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m_treePane;
	}

	private ISubjAssMapDataService getSubjAssMapDataService() {
		ISubjAssMapDataService qry = (ISubjAssMapDataService) NCLocator.getInstance().lookup(ISubjAssMapDataService.class);
		return qry;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 20:02:24)
	 * 
	 * @return nc.ui.pub.beans.UISplitPane
	 */
	private UISplitPane createSplitPane() {
		try {
			if (m_splitPane == null) {
				m_splitPane = new UISplitPane(1);
				m_splitPane.setName("UISplitPane");
				m_splitPane.setDividerSize(3);
				m_splitPane.add(createTreePanel(), "left");
				m_splitPane.add(createMainDataPanel(), "right");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m_splitPane;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 15:33:40)
	 */
	private void doAdd() {
		BillCardPanel tempBillCardPanel = null;

		if (m_curPk_usedfreevalue == null || m_curPk_usedfreevalue.length()==0) {
			MessageDialog.showWarningDlg(this, null, "请先选中要增加对照的辅助项。");
			return;
		}

		/* 切换至‘增加’模式 */
		setState(_ADD_);
		m_treePane.gettree().setEnabled(false);
		/* set 'lock|unlock' relations between operational buttons */
		m_Add.setEnabled(false);
		m_Edit.setEnabled(false);
		m_Del.setEnabled(false);
		m_Save.setEnabled(true);
		m_Cancel.setEnabled(true);
		m_Refresh.setEnabled(false);
		updateButtons();

		/* 加载卡片模板 */
		//	if (m_pkCorp != null && m_billType != null && m_userID != null)
		//	{
		tempBillCardPanel = getBillCardPanel();

		/* 初始清空卡片模板 */
		tempBillCardPanel.addNew();

		/* 设置系统处理字段 */
		tempBillCardPanel.getHeadItem(AssValueMapVO.PK_CORP).setValue(getPKCorp());
		tempBillCardPanel.getHeadItem(AssValueMapVO.PK_USEDFREEVALUE).setValue(m_curPk_usedfreevalue);
		tempBillCardPanel.getHeadItem(AssValueMapVO.PK_BDINFO).setValue(m_curPk_bdinfo);

		/* 切换至卡片模板 */
		((CardLayout) m_mainDataPanel.getLayout()).show(m_mainDataPanel, tempBillCardPanel.getName());
		((UIRefPane) getBillCardPanel().getHeadItem(FAKEOBJ_ID).getComponent()).getUITextField().grabFocus();
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 15:59:13)
	 */
	private void doCancel() {
		/* switch to 'BROWSE' mode */
		setState(_BROWSE_);
		m_treePane.gettree().setEnabled(true);
		/* set 'lock|unlock' relations between operational buttons */
		m_Add.setEnabled(true);
		m_Edit.setEnabled(true);
		m_Del.setEnabled(true);
		m_Save.setEnabled(false);
		m_Cancel.setEnabled(false);
		m_Refresh.setEnabled(true);
		updateButtons();

		/* load bill list panel on 'BROWSE' mode */
		loadMainDataListPanel();
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 15:48:27)
	 */
	private void doDelete() {

		/* determine current selected row */
		AssValueMapVO map_vo = getSelectedVO();

		if(map_vo == null){
			showErrorMessage(nc.vo.bd.BDMsg.MSG_CHOOSE_DATA());
			return;
		}
		/* 删除确认 */
		if (MessageDialog.showOkCancelDlg(this,
				nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
						"UPPuifactory-000064")/* @res "档案删除" */,
				nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
						"UPPuifactory-000065")/* @res "是否确认要删除?" */
				, MessageDialog.ID_CANCEL) == UIDialog.ID_OK) {
			try {
				getSubjAssMapDataService().deleteMap(map_vo);
			} catch (Exception e) {
				e.printStackTrace();
				showErrorMessage(e.getMessage());
				//					return;
			}
		} else
			return;

		/* switch to 'BROWSE' mode */
		setState(_BROWSE_);

		/* set 'lock|unlock' relations between operational buttons */
		m_Add.setEnabled(true);
		m_Edit.setEnabled(true);
		m_Del.setEnabled(true);
		m_Save.setEnabled(false);
		m_Cancel.setEnabled(false);
		updateButtons();

		/* load bill list panel on 'BROWSE' mode */
		loadMainDataListPanel();
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 15:39:20)
	 */
	private void doEdit() {
		BillCardPanel tempBillCardPanel = null;
		AssValueMapVO map_vo = null;

		/* determine current selected row */
		map_vo = getSelectedVO();
		if (map_vo == null) {
			MessageDialog.showWarningDlg(this, null, nc.vo.bd.BDMsg
					.MSG_CHOOSE_DATA());
			return;
		}

		/* switch to 'EDIT' mode */
		setState(_EDIT_);
		m_treePane.gettree().setEnabled(false);
		/* set 'lock|unlock' relations between operational buttons */
		m_Add.setEnabled(false);
		m_Edit.setEnabled(false);
		m_Del.setEnabled(false);
		m_Save.setEnabled(true);
		m_Cancel.setEnabled(true);
		m_Refresh.setEnabled(false);
		updateButtons();

		/* load address BillCardPanel */
		//	if (m_pkCorp != null && m_billType != null && m_userID != null)
		//	{
		tempBillCardPanel = getBillCardPanel();

		/* clear contents */
		tempBillCardPanel.addNew();

		/* load bill data */
		tempBillCardPanel.getBillData().setHeaderValueVO((CircularlyAccessibleValueObject) map_vo);

		/* run formulas for item vos */
		tempBillCardPanel.execHeadLoadFormulas();

		/* add to address panel */
		((CardLayout) m_mainDataPanel.getLayout()).show(m_mainDataPanel,tempBillCardPanel.getName());
		getBillCardPanel().getHeadItem(FAKEOBJ_ID).setValue(getBillCardPanel().getHeadItem(AssValueMapVO.OBJ_ID).getValueObject().toString());
		((UIRefPane) getBillCardPanel().getHeadItem(FAKEOBJ_ID).getComponent()).getUITextField().grabFocus();
	}

	/**
	 * Invoked when the mouse has been clicked on a component.
	 */
	public void mouseClicked(java.awt.event.MouseEvent e) {
		if (e.getSource() == getBillListPanel().getHeadTable()) {
			AssValueMapVO map_vo = getSelectedVO();
			/* 下属公司不允许修改集团定义的地点档案 */
			if (getPKCorp() != null && !getPKCorp().equals(_CORP_SET_)) {
				/* 公司 */
				if (map_vo != null	&& map_vo.getPk_corp().equalsIgnoreCase(_CORP_SET_)) {
					m_Edit.setEnabled(false);
					m_Del.setEnabled(false);
					updateButtons();
					return;
				}
			}
			m_Edit.setEnabled(true);
			m_Del.setEnabled(true);
			updateButtons();
		}

	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	public void mouseEntered(java.awt.event.MouseEvent e) {
	}

	/**
	 * Invoked when the mouse exits a component.
	 */
	public void mouseExited(java.awt.event.MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
	public void mousePressed(java.awt.event.MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 */
	public void mouseReleased(java.awt.event.MouseEvent e) {
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 15:52:56)
	 * @throws Exception 
	 */
	private void doSave() throws Exception {
		AssValueMapVO map_vo = null;
		getBillCardPanel().stopEditing();
		/* get values filled in current bill card templete UI */
		map_vo = (AssValueMapVO) getBillCardPanel().getBillData().getHeaderValueVO(AssValueMapVO.class.getName());
		if (getSelectedVO() != null && getState() == _EDIT_) {
			map_vo.setTs(getSelectedVO().getTs());
		}
		if (map_vo == null)
			return;
		else
			map_vo.setPk_corp(getPKCorp());
		//非空校验
		getBillCardPanel().dataNotNullValidate();

		/* execute database operation according to current business mode */
//		try {
			switch (getState()) {
			case _ADD_: {
				getSubjAssMapDataService().insertMap(map_vo);
			}
				break;

			case _EDIT_: {
				getSubjAssMapDataService().updateMap(map_vo);
			}
				break;

			default:
				break;
			}/* "switch" */

		/* switch to 'BROWSE' mode */
		setState(_BROWSE_);
		m_treePane.gettree().setEnabled(true);
		/* set 'lock|unlock' relations between operational buttons */
		m_Add.setEnabled(true);
		m_Edit.setEnabled(true);
		m_Del.setEnabled(true);
		m_Save.setEnabled(false);
		m_Cancel.setEnabled(false);
		m_Refresh.setEnabled(true);
		updateButtons();

		/* load bill list panel on 'BROWSE' mode */
		loadMainDataListPanel();
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-3 15:13:21)
	 * 
	 * @return nc.vo.util.tree.TreeDetail
	 */
	private TreeDetail getTreeConfig() {
		if (m_treeDetail == null) {
			/* set construct rule */
			MethodGroup mg = new MethodGroup();
			try {
				mg.setKeyField(UsedFreeValueVO.class.getMethod(GET_VCODE, null));
				mg.setNameField(UsedFreeValueVO.class.getMethod(GET_VNAME,null));
				mg.setSortCodeFiled(UsedFreeValueVO.class.getMethod(GET_VCODE,null));
				mg.setHowDisplay(new boolean[] { false, true, true }); // 显示排序码和名称
				mg.setAimClass(UsedFreeValueVO.class);
				mg.setLevel(new int[]{2});
			} catch (Exception e) {
				e.printStackTrace();
			}

			/* set area tree config */
			m_treeDetail = new TreeDetail();
			m_treeDetail.setMg(new MethodGroup[] { mg });
			m_treeDetail.setRootname("辅助项目类别");
		}

		return m_treeDetail;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 19:38:28)
	 * 
	 * @return nc.ui.pub.bill.BillCardPanel
	 */
	private BillCardPanel getBillCardPanel() {
		if (m_billCardPanel == null) {
			try {
				m_billCardPanel = new BillCardPanel();
				m_billCardPanel.setName("数据卡片窗格");
				m_billCardPanel.setEnabled(true);
				/* load address templete */
				m_billCardPanel.loadTemplet(m_billType, m_busiType, m_userID,m_pkCorp);
				
				m_billCardPanel.addBillEditListenerHeadTail(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return m_billCardPanel;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 19:32:49)
	 * 
	 * @return nc.ui.pub.bill.BillListPanel
	 */
	private BillListPanel getBillListPanel() {
		if (m_billListPanel == null) {
			try {
				m_billListPanel = new BillListPanel();
				m_billListPanel.setName("数据列表窗格");
				m_billListPanel.setEnabled(false);

				/* load address templete */
				m_billListPanel.loadTemplet(m_billType, m_busiType, m_userID,m_pkCorp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m_billListPanel;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:32:22)
	 * 
	 * @return java.lang.String
	 */
	public String getBillType() {
		return m_billType;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:32:49)
	 * 
	 * @return java.lang.String
	 */
	public String getBusiType() {
		return m_busiType;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:31:58)
	 * 
	 * @return java.lang.String
	 */
	public String getPKCorp() {
		return m_pkCorp;
	}

	

	/**
	 * 此处插入方法说明。 创建日期：(2004-4-27 9:03:57)
	 * 
	 * @return nc.vo.bd.b202.AddressVO
	 * @throws Exception
	 */
	private AssValueMapVO getSelectedVO() {
		int rowNo = -1;
		AssValueMapVO[] map_vos = null;
		AssValueMapVO mao_vo = null;

		map_vos = (AssValueMapVO[]) getBillListPanel().getHeadBillModel().
				getBodyValueVOs(AssValueMapVO.class.getName());
		if (map_vos == null)
			return null;

		rowNo = ((JTable) getBillListPanel().getHeadTable()).getSelectedRow();
		if (rowNo >= 0 && rowNo < map_vos.length) {
			//			rowNoAfterSort = getBillListPanel().getHeadBillModel()
			//					.convertIntoModelRow(rowNo);
			mao_vo = map_vos[rowNo];
		}
		return mao_vo;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 17:54:31)
	 * 
	 * @return int
	 */
	public int getState() {
		return m_State;
	}

	/**
	 * 子类实现该方法，返回业务界面的标题。
	 * 
	 * @version (00-6-6 13:33:25)
	 * 
	 * @return java.lang.String
	 */
	public String getTitle() {
		return "辅助项对照";
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-4 12:55:26)
	 * 
	 * @return nc.vo.util.tree.XTreeModel
	 */
	public XTreeModel getTreeModel() {
		return m_treeModel;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:33:10)
	 * 
	 * @return java.lang.String
	 */
	public String getUserID() {
		return m_userID;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 13:58:06)
	 */
	public void initialize() {
		try {
			/* switch to 'BROWSE' mode */
			setState(_BROWSE_);

			/* initialize templete parameters */
			setPKCorp(getClientEnvironment().getCorporation().getPrimaryKey());
			setBillType(BillTemplateID.SUBJASSMAP.getValue());
			setBusiType(null);
			setUserID(getClientEnvironment().getUser().getPrimaryKey());

			setName("AssValueTempletUI");
			//		setLayout(new BorderLayout());
			setSize(774, 419);

			/* load business area panes */
			add(createSplitPane());

			/* load address panel */
			loadDefaultMainDataTemplete();
			/* load operational buttons and set 'lock|unlock' relationship */
			m_Add = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("10080803", "UC001-0000002")/* @res "增加" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000002"), 2, "增加"); /*-=notranslate=-*/
			m_Add.setEnabled(true);
			m_Edit = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("10080803", "UC001-0000045")/* @res "修改" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000045"), 2, "修改"); /*-=notranslate=-*/
			m_Edit.setEnabled(true);
			m_Del = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("10080803", "UC001-0000039")/* @res "删除" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000039"), 2, "删除"); /*-=notranslate=-*/
			m_Del.setEnabled(true);
			m_Save = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("10080803", "UC001-0000001")/* @res "保存" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000001"), 2, "保存"); /*-=notranslate=-*/
			m_Save.setEnabled(false);
			
			m_Refresh = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("common", "UC001-0000009")/* @res "保存" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000009"), 2, "刷新");
			m_Refresh.setEnabled(true);
			m_Cancel = new ButtonObject(nc.ui.ml.NCLangRes.getInstance()
					.getStrByID("10080803", "UC001-0000008")/* @res "取消" */,
					NCLangRes.getInstance().getStrByID("10080803",
							"UC001-0000008"), 2, "取消"); /*-=notranslate=-*/
			m_Cancel.setEnabled(false);

			m_Ary = new ButtonObject[] { m_Add, m_Edit, m_Del, m_Save,m_Refresh,m_Cancel };
			setButtons(m_Ary);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-9 10:36:16)
	 */
	private void loadMainDataListPanel() {
		BillListPanel tempBillListPanel = null;
		String strWhere = null;
		AssValueMapVO[] resultVOs = null;

		tempBillListPanel = getBillListPanel();

		strWhere = AssValueMapVO.PK_CORP+" = '" + getPKCorp() + "' and "+AssValueMapVO.PK_USEDFREEVALUE+" = '"
					+ m_curPk_usedfreevalue +"' order by "+AssValueMapVO.VOTHERBIZ+","+AssValueMapVO.VOTHERCODE;

		try {
			resultVOs = getSubjAssMapDataService().queryAllSubjAssMap(strWhere);
		} catch (Exception e) {
			e.printStackTrace();
		}

		tempBillListPanel
				.setHeaderValueVO((CircularlyAccessibleValueObject[]) resultVOs);

		/* run formulas for header vos */
		tempBillListPanel.getHeadBillModel().execLoadFormula();

		/* clear body of current bill list panel */
		tempBillListPanel.getBodyBillModel().clearBodyData();

		/* show transrela panel */
		((CardLayout) m_mainDataPanel.getLayout()).show(m_mainDataPanel, tempBillListPanel.getName());
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-3 12:59:55)
	 */
	private void loadDefaultMainDataTemplete() {
		BillListPanel tempBillListPanel = null;

		if (m_mainDataPanel == null) {
			/* report an error and exit */
			System.out
					.println("Main data panel not available, can not load default main data templete");
			return;
		}

		/* load address BillListPanel on‘BROWSE’mode */
		tempBillListPanel = getBillListPanel();

		/* show address panel */
		((CardLayout) m_mainDataPanel.getLayout()).show(m_mainDataPanel, tempBillListPanel.getName());
	}

	/**
	 * 子类实现该方法，响应按钮事件。
	 * 
	 * @version (00-6-1 10:32:59)
	 * 
	 * @param bo
	 *            ButtonObject
	 */
	public void onButtonClicked(ButtonObject bo) {
		if (bo == m_Add) {
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID(
							"uifactory", "UPPuifactory-000061")/*
																 * @res
																 * "开始进行增加单据，请等待......"
																 */);
			doAdd();
		} else if (bo == m_Edit) {
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000067")/*
													 * @res "开始进行编辑单据，请等待......"
													 */);
			doEdit();
			
		} else if (bo == m_Del) {
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000070")/*
													 * @res "开始进行档案删除，请等待......"
													 */);
			doDelete();
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000071")/* @res "档案删除完成,耗时(ms):" */
							);
		} else if (bo == m_Save) {
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000072")/*
													 * @res "开始进行单据保存，请等待......"
													 */);
			try {
				doSave();
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
				showErrorMessage(e.getMessage());
                return;
			}
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000073")/* @res "单据保存完成,耗时(ms):" */
							);
		} else if (bo == m_Cancel) {
			doCancel();
			
		}else if(bo == m_Refresh){
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000076")/*
													 * @res "开始进行刷新单据，请等待......"
													 */);
			doRefresh();
			showHintMessage(
					nc.ui.ml.NCLangRes.getInstance().getStrByID("uifactory",
							"UPPuifactory-000077")/* @res "单据刷新完成,耗时(ms):" */
							);
		}
	}

	private void doRefresh() {
		String usedFreeValuePk = m_curPk_usedfreevalue;
		TreeSelectionModel model = m_treePane.gettree().getSelectionModel();
		m_treePane.gettree().setSelectionModel(null);
		try {
			 UsedFreeValueVO[] uAssVOs = getSubjAssMapDataService().queryUesdAssSubjByCorpPk(ClientEnvironment
						.getInstance().getCorporation().getPk_corp());
			 getTreeModel().createTree(uAssVOs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		 m_treePane.gettree().setSelectionModel(model);
		if(usedFreeValuePk != null && usedFreeValuePk.length() > 0){
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)getTreeModel().getRoot();
			Enumeration enu = node.breadthFirstEnumeration();
			while(enu.hasMoreElements()){
				XTreeNode treeNode = (XTreeNode)enu.nextElement();
				if(usedFreeValuePk.equalsIgnoreCase(treeNode.getPrimaryKey())){
				   m_treePane.gettree().setSelectionPath(new TreePath(treeNode.getPath()));
				   break;
				}
			}
		}	
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:32:22)
	 * 
	 * @param newM_billType
	 *            java.lang.String
	 */
	public void setBillType(String newM_billType) {
		m_billType = newM_billType;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:32:49)
	 * 
	 * @param newM_busiType
	 *            java.lang.String
	 */
	public void setBusiType(String newM_busiType) {
		m_busiType = newM_busiType;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-3 16:41:43)
	 * 
	 * @param newM_curTreeNode
	 *            java.lang.String
	 */
	private void setCurTreeNode(UsedFreeValueVO treeVO) {
		if (treeVO==null){
			m_curPk_usedfreevalue = null;
			m_curPk_bdinfo = null;
			
			return;
		}
		m_curPk_usedfreevalue = treeVO.getPrimaryKey();
		m_curPk_bdinfo = treeVO.getPk_bdinfo();
		
		IUAPQueryBS dao = NCLocator.getInstance().lookup(IUAPQueryBS.class);
		try {
			Vector refNode = (Vector)dao.executeQuery("select refnodename from bd_bdinfo where pk_bdinfo='"+m_curPk_bdinfo+"'", new VectorProcessor());
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_FREEVALUE).setRefType(((Vector)refNode.get(0)).get(0).toString());
			
			getBillCardPanel().getHeadItem(FAKEOBJ_ID).getComponent().setEnabled(true);
			if(m_curPk_usedfreevalue.equals(UsedFreeValue.DEPARTMENT.getValue())) 
				getBillCardPanel().getHeadItem(FAKEOBJ_ID).setRefType("<"+OtherDeptRefModel.class.getName()+">"); //部门辅助项
			else if(m_curPk_usedfreevalue.equals(UsedFreeValue.AIRLINE.getValue()))
				getBillCardPanel().getHeadItem(FAKEOBJ_ID).setRefType("<"+OtherAircorpRefModel.class.getName()+">"); //航空公司辅助项
			else if(m_curPk_usedfreevalue.equals(UsedFreeValue.CUSTOMER.getValue()))
				getBillCardPanel().getHeadItem(FAKEOBJ_ID).setRefType("<"+OtherCustomerRefModel.class.getName()+">"); //客户辅助项
			else if(m_curPk_usedfreevalue.equals(UsedFreeValue.VEHICLE.getValue()))
				getBillCardPanel().getHeadItem(FAKEOBJ_ID).setRefType("<"+OtherVehicleTypeRefModel.class.getName()+">"); //车辆辅助项
			else 
				getBillCardPanel().getHeadItem(FAKEOBJ_ID).getComponent().setEnabled(false);
			
			String[] formulas;
			if (m_curPk_bdinfo.equals(BDInfo.CUSTOMER.getValue())) formulas= new String[]{ //客商辅助项
					"freevaluecode->getColValue(bd_cubasdoc, custcode, pk_cubasdoc, pk_freevalue)"
					,"freevaluename->getColValue(bd_cubasdoc, custname, pk_cubasdoc, pk_freevalue)"};
			else if (m_curPk_bdinfo.equals(BDInfo.DEPARTMENT.getValue())) formulas= new String[]{ //部门辅助项
					"freevaluecode->getColValue(bd_deptdoc, deptcode, pk_deptdoc, pk_freevalue)"
					,"freevaluename->getColValue(bd_deptdoc, deptname, pk_deptdoc, pk_freevalue)"};
			else if (m_curPk_bdinfo.equals(BDInfo.VEHICLE.getValue())) formulas= new String[]{ //车型辅助项
					"freevaluecode->getColValue(bd_jobbasfil,jobcode,pk_jobbasfil,pk_freevalue)"
					,"freevaluename->getColValue(bd_jobbasfil,jobname,pk_jobbasfil,pk_freevalue)"};
			else formulas = null;
				
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_FREEVALUE).setEditFormula(formulas);
			getBillListPanel().getHeadItem(AssValueMapVO.PK_FREEVALUE).setEditFormula(formulas);
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_FREEVALUE).setLoadFormula(formulas);
			getBillListPanel().getHeadItem(AssValueMapVO.PK_FREEVALUE).setLoadFormula(formulas);
		} catch (BusinessException e) {
			e.printStackTrace();
		}
		
		//freevaluecode->getcolvalue(tablename,fieldname,pk_field,pk_value ) 
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:31:58)
	 * 
	 * @param newM_pkCorp
	 *            java.lang.String
	 */
	public void setPKCorp(String newM_pkCorp) {
		m_pkCorp = newM_pkCorp;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-8 17:54:31)
	 * 
	 * @param newM_State
	 *            int
	 */
	public void setState(int newM_State) {
		m_State = newM_State;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-2 15:33:10)
	 * 
	 * @param newM_userID
	 *            java.lang.String
	 */
	public void setUserID(String newM_userID) {
		m_userID = newM_userID;
	}

	/**
	 * 此处插入方法说明。 创建日期：(2004-3-3 15:39:21)
	 * 
	 * @param e
	 *            javax.swing.event.TreeSelectionEvent
	 */
	public void valueChanged(TreeSelectionEvent e) {
		BillListPanel tempBillListPanel = null;
		XTreeNode node = null;
		String strWhere = null;
		BillItem item = null;
		AssValueMapVO[] resultVOs = null;

		TreePath tp = e.getPath();
		if (tp == null)
			return;

		node = (XTreeNode) tp.getLastPathComponent();
		UsedFreeValueVO treeVO = (UsedFreeValueVO)node.getValue();
		setCurTreeNode(treeVO);

		switch (getState()) {
		case _BROWSE_:
			tempBillListPanel = getBillListPanel();

			/* 加载数据 */
			strWhere = "("+AssValueMapVO.PK_CORP+" = '" + getPKCorp() +"') and "+AssValueMapVO.PK_USEDFREEVALUE+" = '" 
					+ m_curPk_usedfreevalue +"' order by "+AssValueMapVO.VOTHERBIZ+","+AssValueMapVO.VOTHERCODE;

			try {
				resultVOs = getSubjAssMapDataService().queryAllSubjAssMap(strWhere);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			tempBillListPanel
					.setHeaderValueVO((CircularlyAccessibleValueObject[]) resultVOs);

			/* run formulas for header vos */
			tempBillListPanel.getHeadBillModel().execLoadFormula();

			/* clear body of current bill list panel */
			tempBillListPanel.getBodyBillModel().clearBodyData();

			/* show transrela panel */
			((CardLayout) m_mainDataPanel.getLayout()).show(m_mainDataPanel,
					tempBillListPanel.getName());
			break;

		case _ADD_:
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_USEDFREEVALUE).setValue(m_curPk_usedfreevalue);
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_BDINFO).setValue(m_curPk_bdinfo);
			break;

		case _EDIT_:
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_USEDFREEVALUE).setValue(m_curPk_usedfreevalue);
			getBillCardPanel().getHeadItem(AssValueMapVO.PK_BDINFO).setValue(m_curPk_bdinfo);
			break;

		default:
			break;
		} /* "switch" */
	}
	@Override
	public boolean onClosing() {
		if (m_State == _EDIT_ || m_State == _ADD_) {
			int i = MessageDialog.showYesNoCancelDlg(this, null,
					NCLangRes4VoTransl.getNCLangRes().getStrByID("common",
							"UCH001"/*是否保存已修改的数据？*/), UIDialog.ID_CANCEL);
			switch (i) {
			case UIDialog.ID_YES: {
				try {
					doSave();
				} catch (Exception e) {
					Logger.error(e.getMessage(), e);
					showErrorMessage(e.getMessage());
					return false;
				}
				return true;
			}
			case UIDialog.ID_NO: {
				return true;
			}
			case UIDialog.ID_CANCEL: {
				return false;
			}

			default:
				return true;
			}

		} else {
			return true;
		}
	}
}