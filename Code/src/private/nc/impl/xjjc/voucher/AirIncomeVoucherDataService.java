package nc.impl.xjjc.voucher;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.poi.hssf.record.formula.functions.Setvalue;

import nc.bd.glorgbook.GlorgbookCache;
import nc.bd.glorgbook.IGlorgbookAccessor;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.itf.gl.voucher.IVoucher;
import nc.itf.uap.bd.accsubj.IAccsubjDataQuery;
import nc.itf.uap.bd.accsubj.ISubjassQry;
import nc.itf.xjjc.voucher.BDInfo;
import nc.itf.xjjc.voucher.IAirIncomeVoucherDataService;
import nc.itf.xjjc.voucher.VoucherBizType;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.bd.b02.AccsubjVO;
import nc.vo.bd.b02.SubjassVO;
import nc.vo.bd.b54.GlorgbookVO;
import nc.vo.gl.pubvoucher.DetailVO;
import nc.vo.gl.pubvoucher.VoucherVO;
import nc.vo.glcom.ass.AssVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.xjjc.accsubjmap.SubjMapVO;
import nc.vo.xjjc.freevaluemap.AssValueMapVO;
import nc.vo.xjjc.voucher.AirCorpVO;
import nc.vo.xjjc.voucher.FakeVoucherVO;

public class AirIncomeVoucherDataService implements
		IAirIncomeVoucherDataService {
	
	private Hashtable<String, VoucherVO> voucherMap = new Hashtable<String, VoucherVO>();
	private Hashtable<VoucherVO, ArrayList<InvoiceValue>> voucherInvoiceMap = new Hashtable<VoucherVO, ArrayList<InvoiceValue>>();
	private Hashtable<String, AccSubjValue> subjMap = new Hashtable<String, AccSubjValue>();
	private Hashtable<String, SubjAssValue> subjAssMap = new Hashtable<String, SubjAssValue>();
	

	@Override
	public AirCorpVO[] getAirCorpNeedGenVoucher(String sAccMonth, String eAccMonth, String airPort) {
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		BaseDAO ncDao = new BaseDAO();
		Hashtable<String, AirCorpVO> retData = new Hashtable<String, AirCorpVO>();
		try {
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> invoices = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_payer.iata_code,xj_amdb.view_payer.fname,xj_amdb.view_payer.country_type, xj_amdb.view_invoice.obj_id "
					+"from xj_amdb.view_invoice, xj_amdb.view_payer where xj_amdb.view_invoice.payer_code = xj_amdb.view_payer.iata_code "
					+"and xj_amdb.view_invoice.airport_code = xj_amdb.view_payer.airport_code "
					+"and xj_amdb.view_invoice.amount > 0 "
					+"and xj_amdb.view_invoice.account_month >= '"+sAccMonth.replace("-", "")
					+"' and xj_amdb.view_invoice.account_month <= '"+eAccMonth.replace("-", "")
					+"' and xj_amdb.view_invoice.airport_code = '"+airPort+"'", new VectorProcessor());
			
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> gened = (Vector<Vector<Object>>)ncDao.executeQuery("select xjjc_gl_fakevoucher.obj_id from xjjc_gl_fakevoucher, gl_voucher "
					+"where xjjc_gl_fakevoucher.pk_voucher=gl_voucher.pk_voucher and isnull(gl_voucher.dr,0)=0", new VectorProcessor());
			
			Hashtable<String, String> gendMap = new Hashtable<String, String>();
			for(Vector<Object> gendata : gened) gendMap.put(gendata.get(0).toString(), gendata.get(0).toString());
			// TODO 增加已生成凭证的发票过滤-查询待生成发票
			
			for(Vector<Object> invoice : invoices){
				if (!gendMap.containsKey(invoice.get(3).toString())){ // 过滤掉已生成凭证的发票
					if (!retData.containsKey(invoice.get(0).toString())){ // 过滤掉已记录的航空公司
						AirCorpVO tmp = new AirCorpVO();
						tmp.setCode(invoice.get(0).toString());
						tmp.setName(invoice.get(1).toString());
						tmp.setType(Integer.valueOf(invoice.get(2).toString()));
						retData.put(tmp.getCode(), tmp);
					}
				}
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		
		return retData.values().toArray(new AirCorpVO[0]);
	}

	@Override
	public FakeVoucherVO[] genVoucherForAirCorp(String sAccMonth,
			String eAccMonth, String airPort, String airPortname, String pk_voucherType,
			String explain, String[] airlines, boolean useDollar, UFDouble raito, String pk_user) throws BusinessException {
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		BaseDAO ncDao = new BaseDAO();
		ArrayList<FakeVoucherVO> retVO = new ArrayList<FakeVoucherVO>();
		voucherMap.clear();
		voucherInvoiceMap.clear();
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> gened = (Vector<Vector<Object>>)ncDao.executeQuery("select xjjc_gl_fakevoucher.obj_id from xjjc_gl_fakevoucher, gl_voucher "
				+"where xjjc_gl_fakevoucher.pk_voucher=gl_voucher.pk_voucher and isnull(gl_voucher.dr,0)=0", new VectorProcessor());
		
		Hashtable<String, String> gendMap = new Hashtable<String, String>();
		for(Vector<Object> gendata : gened) gendMap.put(gendata.get(0).toString(), gendata.get(0).toString());
		try {
			for(String airline : airlines){
				@SuppressWarnings("unchecked")
				Vector<Vector<Object>> invoices = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_invoice.obj_id, xj_amdb.view_invoice.name, xj_amdb.view_invoice.account_month, "
						+"xj_amdb.view_invoice.amount, xj_amdb.view_payer.fname, xj_amdb.view_payer.country_type "
						+"from xj_amdb.view_invoice, xj_amdb.view_payer "
						+"where xj_amdb.view_invoice.payer_code=xj_amdb.view_payer.iata_code "
						+"and xj_amdb.view_invoice.airport_code = xj_amdb.view_payer.airport_code "
						+"and xj_amdb.view_invoice.amount > 0 "
						+" and xj_amdb.view_invoice.account_month >= '"+sAccMonth.replace("-", "")
						+"' and xj_amdb.view_invoice.account_month <= '"+eAccMonth.replace("-", "")
						+"' and xj_amdb.view_invoice.airport_code = '"+airPort
						+"' and xj_amdb.view_invoice.payer_code = '"+airline
						+"'", new VectorProcessor());
				//TODO 增加已生成凭证的发票过滤-生成凭证
				
				
				for(int invoiceIdx=0;invoiceIdx<invoices.size();invoiceIdx++){
					Integer obj_id = Integer.parseInt(invoices.get(invoiceIdx).get(0).toString());
					if (gendMap.containsKey(obj_id.toString()))
						continue; // 过滤掉已生成凭证的发票
					String period = invoices.get(invoiceIdx).get(2).toString();
					String invoiceName = invoices.get(invoiceIdx).get(1).toString();
					UFDouble invoiceAmount = new UFDouble(invoices.get(invoiceIdx).get(3).toString());
					
					String airlineName = invoices.get(invoiceIdx).get(4).toString();
					boolean foreignAirline = invoices.get(invoiceIdx).get(5).toString().equals("2");
					
					InvoiceValue invoiceInfo = new InvoiceValue();
					invoiceInfo.setObj_id(obj_id);
					invoiceInfo.setNinvoamount(invoiceAmount);
					invoiceInfo.setVinvoaircorp(airlineName);
					invoiceInfo.setVinvoairport(airPortname);
					invoiceInfo.setVinvoname(invoiceName);
					invoiceInfo.setVinvoperiod(period);
					
					VoucherVO voucher = getAirlinePeriodVoucherVO(airline, period, pk_voucherType, explain, invoiceInfo, invoiceAmount, pk_user);
					
					@SuppressWarnings("unchecked")
					Vector<Vector<Object>> items = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_chargeproject.code, xj_amdb.view_invoice_item.amount "
							+"from xj_amdb.view_invoice_item, xj_amdb.view_chargeproject "
							+"where xj_amdb.view_invoice_item.charge_project_id=xj_amdb.view_chargeproject.obj_id "
							+"and xj_amdb.view_invoice_item.invoice_id="+obj_id, new VectorProcessor());
					
					for(int itemIdx=0;itemIdx<items.size();itemIdx++){ 
						String chargePrjCode = items.get(itemIdx).get(0).toString();
						UFDouble itemAmount = new UFDouble(items.get(itemIdx).get(1).toString());
						initDetailVOforVoucher(voucher, airPort, airPortname, airline, airlineName, 
							invoiceName, period, chargePrjCode, itemAmount, useDollar&&foreignAirline, raito);
					}
				}
			}
			VoucherVO[] vouchers = voucherMap.values().toArray(new VoucherVO[0]);
			IVoucher voucherBo = NCLocator.getInstance().lookup(IVoucher.class);
			for(VoucherVO voucher : vouchers){
				voucherBo.save(voucher, true);
				ArrayList<InvoiceValue> invoiceInfos = voucherInvoiceMap.get(voucher);
				for(InvoiceValue invo : invoiceInfos){
					FakeVoucherVO vouinvo = new FakeVoucherVO();
					vouinvo.setPk_voucher(voucher.getPk_voucher());
					vouinvo.setVinvoname(invo.getVinvoname());
					vouinvo.setObj_id(invo.getObj_id());
					vouinvo.setNinvoamount(invo.getNinvoamount());
					vouinvo.setVinvoperiod(invo.getVinvoperiod());
					vouinvo.setVinvoaircorp(invo.getVinvoaircorp());
					vouinvo.setVinvoairport(invo.getVinvoairport());
					vouinvo.setPk_corp("1022");
					retVO.add(vouinvo);
				}
			}
			
			ncDao.insertVOArray(retVO.toArray(new FakeVoucherVO[0]));
		} catch (Exception e) {
			throw new BusinessException(e); 
		}
		
		return retVO.toArray(new FakeVoucherVO[0]);
	}

	private VoucherVO getAirlinePeriodVoucherVO(String airline, String period, String pk_voucherType, 
			String explain, InvoiceValue invoiceInfo, UFDouble amount, String pk_user) {
		
		VoucherVO voucher = null;
		if (!voucherMap.containsKey(airline+period)){
			voucher = new VoucherVO();
			voucher.setPk_voucher(null); //凭证主键
			voucher.setPk_vouchertype(pk_voucherType); //凭证类别主键
			voucher.setPk_sob(null); //账簿主键
			voucher.setPk_corp("1022"); //公司主键
			IGlorgbookAccessor glorg = new GlorgbookCache();
			GlorgbookVO[] books = glorg.getGLOrgBookVOsByPk_Corp2("1022");
			//period = period.substring(0, 4)+"-"+period.substring(4, 6)+"-01";
			voucher.setYear(new UFDate(new Date()).toString().substring(0, 4)); // 会计年度
			voucher.setPeriod(new UFDate(new Date()).toString().substring(5, 7)); //会计期间
			voucher.setNo(null); //凭证号处理
			voucher.setPrepareddate(new UFDate()); //制单日期
			voucher.setTallydate(null); //记账日期
			voucher.setAttachment(0); //附单据数
			voucher.setPk_prepared(pk_user); //制单人主键
			voucher.setPk_checked(null); //审核人主键
			voucher.setPk_casher(null); //出纳主键
			voucher.setPk_manager("N/A"); //记账人主键
			voucher.setSignflag(new UFBoolean(false)); //签字标志
			voucher.setModifyflag("YYY"); //凭证修改标志
			voucher.setDetailmodflag(new UFBoolean(false)); //分录增删标志
			voucher.setDiscardflag(new UFBoolean(false)); //作废标志
			voucher.setPk_system("GL"); //制单系统主键
			voucher.setAddclass(null); //增加接口类
			voucher.setModifyclass(null); //修改接口类
			voucher.setDeleteclass(null); //删除接口类
			voucher.setVoucherkind(0); //凭证类型
			voucher.setTotaldebit(amount); //借方合计
			voucher.setTotalcredit(amount); //贷方合计
			if (explain!=null && explain.length()>0)
				voucher.setExplanation(explain); //凭证摘要
			else
				voucher.setExplanation("00010000000000000001");
			voucher.setFree10("VOUCHERNEWADD");
			voucher.setFree1(voucher.getPeriod());
			voucher.setContrastflag(null); //对账标志
			voucher.setErrmessage(""); //标错信息
			// 处理存在两个账的问题
			voucher.setPk_glorg(books[0].getPk_glorg()); //会计主体
			voucher.setPk_glbook(books[0].getPk_glbook()); //会计账簿
			voucher.setPk_glorgbook(books[0].getPrimaryKey());
			
			voucherMap.put(airline+period, voucher);
		}else{
			voucher = voucherMap.get(airline+period);
			voucher.setTotaldebit(voucher.getTotaldebit().add(amount)); //借方合计
			voucher.setTotalcredit(voucher.getTotalcredit().add(amount)); //贷方合计
		}
		
		if(voucherInvoiceMap.containsKey(voucher)){
			voucherInvoiceMap.get(voucher).add(invoiceInfo);
		}else{
			ArrayList<InvoiceValue> invoiceInfos = new ArrayList<InvoiceValue>();
			voucherInvoiceMap.put(voucher, invoiceInfos);
			invoiceInfos.add(invoiceInfo);
		}
		
		return voucher;
	}

	private void initSubjMapVo(String airport, String chargePrjCode) throws BusinessException {
		if (!subjMap.containsKey(airport+chargePrjCode)){
			BaseDAO dao = new BaseDAO();
			@SuppressWarnings("unchecked")
			SubjMapVO[] subjMapVOs = (SubjMapVO[])dao.retrieveByClause(SubjMapVO.class, "pk_subjbiz='"+VoucherBizType.LIFTLANDFEE.getValue()
					+"' and pk_corp='1022' and votherbiz='"+airport
					+"' and vothercode='"+chargePrjCode+"'").toArray(new SubjMapVO[0]);
			if (subjMapVOs.length==0)
				throw new BusinessException("机场：["+airport+"]下费用项目：["+chargePrjCode+"]没有配置科目对照。");
			
			IAccsubjDataQuery subjQry = NCLocator.getInstance().lookup(IAccsubjDataQuery.class);
			AccsubjVO debitSubjVO = subjQry.findAccsubjVOByPrimaryKey(subjMapVOs[0].getPk_debitsubj());
			AccsubjVO creditSubjVO = subjQry.findAccsubjVOByPrimaryKey(subjMapVOs[0].getPk_creditsubj());
			
			AccSubjValue subjvalue = new AccSubjValue();
			subjvalue.pk_creditsubj = subjMapVOs[0].getPk_creditsubj();
			subjvalue.pk_debitsubj = subjMapVOs[0].getPk_debitsubj();
			subjvalue.creditSubjCode = creditSubjVO.getSubjcode();
			subjvalue.debitSubjCode = debitSubjVO.getSubjcode();
			
			subjMap.put(airport+chargePrjCode, subjvalue);
		}
	}

	private void initDetailVOforVoucher(VoucherVO voucher, String airport, String airPortName, String airline, 
			String airlineName, String invoiceName,	String period, String chargePrjCode, UFDouble amount, 
			boolean useDollar,	UFDouble raito) throws BusinessException{
		initSubjMapVo(airport, chargePrjCode);
		String pk_accsubjdebit = subjMap.get(airport+chargePrjCode).pk_debitsubj;
		String pk_accsubjcredit = subjMap.get(airport+chargePrjCode).pk_creditsubj;
		
		String creditExplain="转："+airlineName+period.substring(0,4)+"年"+period.substring(4,6)+"月"+airPortName+"机场 起降费";
		String debitExplain=creditExplain;
		boolean isClearCenter = invoiceName.indexOf("清算")>-1;
		if (isClearCenter)
			debitExplain+="（清算中心）";
		else
			debitExplain+="（航空公司）";
		
		DetailVO debitVO = findDetailbyAccsubjExplain(voucher, pk_accsubjdebit, debitExplain);
		
		if (debitVO==null){
			debitVO = getNewDetailVO(voucher, pk_accsubjdebit, debitExplain, useDollar, raito);
			debitVO.setDirection("D");
			initAssforDetail(debitVO, airport, airline, chargePrjCode, isClearCenter);
			voucher.insertDetail(debitVO, 0);
			debitVO = findDetailbyAccsubjExplain(voucher, pk_accsubjdebit, debitExplain);
		}
		// 合并分录金额处理，特别是外币
		if (useDollar)
			debitVO.setDebitamount(debitVO.getDebitamount().add(amount.div(raito))); // 原币借方金额
		else
			debitVO.setDebitamount(debitVO.getDebitamount().add(amount)); // 原币借方金额
		debitVO.setLocaldebitamount(debitVO.getLocaldebitamount().add(amount)); // 本币借方金额
		
		DetailVO creditVO = findDetailbyAccsubjExplain(voucher, pk_accsubjcredit, creditExplain);
		
		if (creditVO==null){
			creditVO = getNewDetailVO(voucher, pk_accsubjcredit, creditExplain, useDollar, raito);
			creditVO.setDirection("C");
			initAssforDetail(creditVO, airport, airline, chargePrjCode, isClearCenter);
			voucher.addDetail(creditVO);
			creditVO = findDetailbyAccsubjExplain(voucher, pk_accsubjcredit, creditExplain);
		}
		// 合并分录金额处理，特别是外币
		if (useDollar)
			creditVO.setCreditamount(creditVO.getCreditamount().add(amount.div(raito))); // 原币贷方金额
		else
			creditVO.setCreditamount(creditVO.getCreditamount().add(amount)); // 原币贷方金额
		creditVO.setLocalcreditamount(creditVO.getLocalcreditamount().add(amount)); // 本币贷方金额	
	}

	private void initAssforDetail(DetailVO debitVO, String airport,
			String airline, String chargePrjCode, boolean isClearCenter) throws BusinessException {
		ISubjassQry assQry = NCLocator.getInstance().lookup(ISubjassQry.class);
		SubjassVO[] assVOs = assQry.queryBDInfo(debitVO.getPk_accsubj());
		
		AssVO[] ass = new AssVO[assVOs.length];
		for (int i = 0; i < assVOs.length; i++) {
			String otherAssCode;
			
			if (assVOs[i].getPk_bdinfo().equals(BDInfo.CUSTOMER.getValue()))
				otherAssCode = airline;
			else if (assVOs[i].getPk_bdinfo().equals(BDInfo.DEPARTMENT.getValue()))
				otherAssCode = airport;
			else if (assVOs[i].getPk_bdinfo().equals(BDInfo.VEHICLE.getValue()))
				otherAssCode = chargePrjCode;
			else if (assVOs[i].getPk_bdinfo().equals(BDInfo.CLEAROBJECT.getValue()))
				otherAssCode = null;
			else
				throw new BusinessException("科目：["+chargePrjCode+"]出现部门、客商、车型以外的辅助项，系统无法处理。");
			ass[i] = new AssVO();
			ass[i].setPk_Checktype(assVOs[i].getPk_bdinfo());
			if (otherAssCode !=null){
				SubjAssValue assValue = getSubjAssValue(assVOs[i].getPk_bdinfo(), airport, otherAssCode);
				
				ass[i].setPk_Checkvalue(assValue.pk_freevalue);
				ass[i].setCheckvaluecode(assValue.code);
				ass[i].setCheckvaluename(assValue.name);
			}else{
				if(isClearCenter){
					ass[i].setPk_Checkvalue("1022A11000000000019L");
					ass[i].setCheckvaluecode("01");
					ass[i].setCheckvaluename("清算中心");
				}else{
					ass[i].setPk_Checkvalue("1022A11000000000019N");
					ass[i].setCheckvaluecode("02");
					ass[i].setCheckvaluename("非清算中心");
				}
			}
		}
		debitVO.setAss(ass);
	}

	private SubjAssValue getSubjAssValue(String pk_bdinfo, String airport, String otherAssCode) throws BusinessException {
		if (subjAssMap.containsKey(pk_bdinfo+otherAssCode))
			return subjAssMap.get(pk_bdinfo+otherAssCode);
		
		String text,sql;
		boolean needAirPortFilter = true;
		if (pk_bdinfo.equals(BDInfo.CUSTOMER.getValue())) {
			text = "航空公司";
			sql = "select custcode, custname from bd_cubasdoc where pk_cubasdoc='PKVALUE'";
		}
		else if (pk_bdinfo.equals(BDInfo.DEPARTMENT.getValue())) {
			text = "部门";
			sql = "select deptcode, deptname from bd_deptdoc where pk_deptdoc='PKVALUE'";
			needAirPortFilter = false;
		}
		else if (pk_bdinfo.equals(BDInfo.VEHICLE.getValue())) {
			text = "车型";
			sql = "select jobcode, jobname from bd_jobbasfil where pk_jobbasfil='PKVALUE'";
		}
		else
			throw new BusinessException("科目出现部门、客商、车型以外的辅助项，系统无法处理。");
		
		BaseDAO dao = new BaseDAO();
		@SuppressWarnings("unchecked")
		AssValueMapVO[] assValueMapVO = (AssValueMapVO[])dao.retrieveByClause(AssValueMapVO.class, "pk_bdinfo='"+pk_bdinfo
				+"' and pk_corp='1022' "+(needAirPortFilter?"and votherbiz='"+airport +"'":"")
				+" and vothercode='"+otherAssCode+"'").toArray(new AssValueMapVO[0]);
		if (assValueMapVO.length==0)
			throw new BusinessException("机场：["+airport+"]下的"+text+"编码：["+otherAssCode+"]没有配置辅助项对照。");
		
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> codename = (Vector<Vector<Object>>)dao.executeQuery(sql.replace("PKVALUE", assValueMapVO[0].getPk_freevalue()), new VectorProcessor());
		SubjAssValue assValue = new SubjAssValue();
		assValue.pk_freevalue = assValueMapVO[0].getPk_freevalue();
		assValue.code = codename.get(0).get(0).toString();
		assValue.name = codename.get(0).get(1).toString();
		
		subjAssMap.put(pk_bdinfo+otherAssCode, assValue);
		
		return assValue;
	}

	private DetailVO findDetailbyAccsubjExplain(VoucherVO voucher, String pk_accsubj, String explain) {
		boolean haveDebit = false;
		int i=0;
		for (i=0;i<voucher.getNumDetails();i++){
			if (voucher.getDetail(i).getPk_accsubj().equals(pk_accsubj) 
					&& voucher.getDetail(i).getExplanation().equals(explain)){
				haveDebit = true;
				break;
			}
		}
		if (!haveDebit)
			return null;
		else
			return voucher.getDetail(i);
	}
	
	private DetailVO getNewDetailVO(VoucherVO voucher, String pk_accsubj, String explian, boolean useDollar, UFDouble raito){
		DetailVO detail = new DetailVO();
		detail.setPk_detail(null); // 分录主键
		detail.setIsdifflag(new UFBoolean(false));
		detail.setPk_voucher(voucher.getPk_voucher()); // 凭证主键
		detail.setPk_accsubj(pk_accsubj); // 科目主键
		if (useDollar)
			detail.setPk_currtype("00010000000000000002"); // 币种主键
		else
			detail.setPk_currtype("00010000000000000001"); // 币种主键
		
		detail.setPk_sob(voucher.getPk_sob()); // 账簿主键
		detail.setPk_corp(voucher.getPk_corp()); // 公司主键
		detail.setDetailindex(null); // 分录号
		detail.setAssid(null); // 辅助核算标识
		detail.setExplanation(explian); // 摘要内容
		detail.setPrice(new UFDouble(0)); // 单价
		detail.setExcrate1(new UFDouble(0)); // 汇率1//折辅汇率
		if (useDollar)
			detail.setExcrate2(raito); // 汇率2//折本汇率
		else
			detail.setExcrate2(new UFDouble(1.0)); // 汇率2//折本汇率
		detail.setDebitquantity(new UFDouble(0)); // 借方数量
		detail.setDebitamount(new UFDouble(0)); // 原币借方金额
		detail.setFracdebitamount(new UFDouble(0)); // 辅币借方金额
		detail.setLocaldebitamount(new UFDouble(0)); // 本币借方金额
		detail.setCreditquantity(new UFDouble(0)); // 贷方数量
		detail.setCreditamount(new UFDouble(0)); // 原币贷方金额
		detail.setFraccreditamount(new UFDouble(0)); // 辅币贷方金额
		detail.setLocalcreditamount(new UFDouble(0)); // 本币贷方金额
		detail.setModifyflag("YYYYYYYYYYYYYYYY"); // 修改标志
		detail.setRecieptclass(null); // 单据处理类
		detail.setOppositesubj(null); // 对方科目
		detail.setContrastflag(null); // 对账标志
		detail.setErrmessage(null); // 标错信息
		detail.setCheckstyle(null); // 结算方式主键
		detail.setCheckno(null); // 结算号
		detail.setCheckdate(null); // 结算日期
		detail.setPk_innersob(null); // 内部账簿主键
		detail.setPk_innercorp(null); // 内部单位主键
		detail.setFree6(voucher.getFree1());

		detail.setPk_glorg(voucher.getPk_glorg());// 会计主体
		detail.setPk_glbook(voucher.getPk_glbook());// 会计账簿

		return detail;
	}
}
