package nc.impl.xjjc.voucher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import nc.bd.glorgbook.GlorgbookCache;
import nc.bd.glorgbook.IGlorgbookAccessor;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.bs.uap.lock.PKLock;
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
	private Hashtable<String, String> errors = new Hashtable<String, String>();
	

	public AirCorpVO[] getAirCorpNeedGenVoucher(String sAccMonth, String eAccMonth, String airPort) {
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		BaseDAO ncDao = new BaseDAO();
		Hashtable<String, AirCorpVO> retData = new Hashtable<String, AirCorpVO>();
		try {
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> invoices = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_payer.iata_code,xj_amdb.view_payer.fname,xj_amdb.view_payer.country_type, xj_amdb.view_invoice.obj_id "
					+"from xj_amdb.view_invoice, xj_amdb.view_payer where xj_amdb.view_invoice.payer_code = xj_amdb.view_payer.iata_code "
					+"and xj_amdb.view_invoice.airport_code = xj_amdb.view_payer.airport_code "
					+"and xj_amdb.view_invoice.amount <> 0 "
					+"and xj_amdb.view_invoice.account_month >= '"+sAccMonth.replace("-", "")
					+"' and xj_amdb.view_invoice.account_month <= '"+eAccMonth.replace("-", "")
					+"' and xj_amdb.view_invoice.airport_code = '"+airPort+"'", new VectorProcessor());
			
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> gened = (Vector<Vector<Object>>)ncDao.executeQuery("select xjjc_gl_fakevoucher.obj_id from xjjc_gl_fakevoucher, gl_voucher "
					+"where xjjc_gl_fakevoucher.pk_voucher=gl_voucher.pk_voucher and isnull(gl_voucher.dr,0)=0", new VectorProcessor());
			
			Hashtable<String, String> gendMap = new Hashtable<String, String>();
			for(Vector<Object> gendata : gened) gendMap.put(gendata.get(0).toString(), gendata.get(0).toString());
			// 增加已生成凭证的发票过滤-查询待生成发票
			
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
			Logger.error(e.getMessage(), e);
		}
		
		return retData.values().toArray(new AirCorpVO[0]);
	}

	public FakeVoucherVO[] genVoucherForAirCorp(String sAccMonth,
			String eAccMonth, String airPort, String airPortname, String pk_voucherType,
			String explain, String[] airlines, boolean useDollar, UFDouble raito, String pk_user, UFDate date) throws BusinessException {
		//先加动态锁
		String[] pks = new String[airlines.length];
		for(int i=0;i<airlines.length;i++)
			pks[i]=airPort+airlines[i];
		if (!PKLock.getInstance().addBatchDynamicLock(pks))
			throw new BusinessException("其它人正在对指定机场下的航空公司进行生成凭证处理。");
		
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		BaseDAO ncDao = new BaseDAO();
		ArrayList<FakeVoucherVO> retVO = new ArrayList<FakeVoucherVO>();
		voucherMap.clear();
		voucherInvoiceMap.clear();
		errors.clear();
		subjMap.clear();
		subjAssMap.clear();
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> gened = (Vector<Vector<Object>>)ncDao.executeQuery("select xjjc_gl_fakevoucher.obj_id from xjjc_gl_fakevoucher, gl_voucher "
				+"where xjjc_gl_fakevoucher.pk_voucher=gl_voucher.pk_voucher and isnull(gl_voucher.dr,0)=0", new VectorProcessor());
		
		Hashtable<String, String> gendMap = new Hashtable<String, String>();
		for(Vector<Object> gendata : gened) gendMap.put(gendata.get(0).toString(), gendata.get(0).toString());
		// 增加已生成凭证的发票过滤-生成凭证
		try {
			for(String airline : airlines){
				@SuppressWarnings("unchecked")
				Vector<Vector<Object>> invoices = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_invoice.obj_id, xj_amdb.view_invoice.name, xj_amdb.view_invoice.account_month, "
						+"xj_amdb.view_invoice.amount, xj_amdb.view_payer.fname, xj_amdb.view_payer.country_type "
						+"from xj_amdb.view_invoice, xj_amdb.view_payer "
						+"where xj_amdb.view_invoice.payer_code=xj_amdb.view_payer.iata_code "
						+"and xj_amdb.view_invoice.airport_code = xj_amdb.view_payer.airport_code "
						+"and xj_amdb.view_invoice.amount <> 0 "
						+" and xj_amdb.view_invoice.account_month >= '"+sAccMonth.replace("-", "")
						+"' and xj_amdb.view_invoice.account_month <= '"+eAccMonth.replace("-", "")
						+"' and xj_amdb.view_invoice.airport_code = '"+airPort
						+"' and xj_amdb.view_invoice.payer_code = '"+airline
						+"'", new VectorProcessor());
				
				
				
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
					
					VoucherVO voucher = getAirlinePeriodVoucherVO(airline, period, pk_voucherType, explain, invoiceInfo, invoiceAmount, pk_user, date);
					
					@SuppressWarnings("unchecked")
					Vector<Vector<Object>> items = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_chargeproject.code, xj_amdb.view_invoice_item.amount "
							+"from xj_amdb.view_invoice_item, xj_amdb.view_chargeproject "
							+"where xj_amdb.view_invoice_item.charge_project_id=xj_amdb.view_chargeproject.obj_id "
							+"and xj_amdb.view_invoice_item.amount <> 0 "
							+"and xj_amdb.view_invoice_item.invoice_id="+obj_id, new VectorProcessor());
					
					for(int itemIdx=0;itemIdx<items.size();itemIdx++){ 
						String chargePrjCode = items.get(itemIdx).get(0).toString();
						UFDouble itemAmount = new UFDouble(items.get(itemIdx).get(1).toString());
						initDetailVOforVoucher(voucher, airPort, airPortname, airline, airlineName, 
							invoiceName, period, chargePrjCode, itemAmount, useDollar&&foreignAirline, raito);
					}
				}
			}
			if (errors.size()>0){
				StringBuffer errmsg = new StringBuffer();
				Enumeration<String> e = errors.keys();

			    while(e.hasMoreElements())
			    	errmsg.append(e.nextElement()+"\n");
			    
				throw new BusinessException(errmsg.toString());
			}
			
			VoucherVO[] vouchers = voucherMap.values().toArray(new VoucherVO[0]);
			IVoucher voucherBo = NCLocator.getInstance().lookup(IVoucher.class);
			for(VoucherVO voucher : vouchers){	
				if (voucher.getExplanation() == null)
					voucher.setExplanation(((DetailVO)voucher.getDetail().get(0)).getExplanation());
				if (voucher.getDetail()!=null && voucher.getDetail().size()>0){
					for(int idx=0;idx<voucher.getDetail().size();idx++){
						voucher.getDetail(idx).setFreevalue30(null);
						voucher.getDetail(idx).setFreevalue29(null);
					}
					Collections.sort(voucher.getDetail(), new AirDetailComparator());
					doBalanceForVoucher(voucher);
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
			}
			
			ncDao.insertVOArray(retVO.toArray(new FakeVoucherVO[0]));
		}catch (BusinessException be){
			throw be;
		}catch (Exception e) {
			Logger.error(e.getMessage(), e);
			throw new BusinessException(e); 
		}
		
		return retVO.toArray(new FakeVoucherVO[0]);
	}

	private void doBalanceForVoucher(VoucherVO voucher) {
		if (voucher.getDetail()!=null && voucher.getDetail().size()>0){
			UFDouble creditAmount=new UFDouble(0);
			UFDouble debitAmount=new UFDouble(0);
			for(int idx=0;idx<voucher.getDetail().size();idx++){
				if (voucher.getDetail(idx).getDirection())
					debitAmount = debitAmount.add(voucher.getDetail(idx).getLocaldebitamount());
				else
					creditAmount = creditAmount.add(voucher.getDetail(idx).getLocalcreditamount());
			}
			if (creditAmount.compareTo(debitAmount)!=0){
				String pk_accsubj = "0001A110000000008CO4";
				AssVO[] ass = new AssVO[1];
				ass[0] = new AssVO();
				ass[0].setPk_Checktype("0001A1100000000001GH");
				ass[0].setPk_Checkvalue("1022A1100000000000QO");
				ass[0].setCheckvaluecode("03");
				ass[0].setCheckvaluename("收入管理中心");
				
				DetailVO taxDetail = getNewDetailVO(voucher, pk_accsubj, "销项税", ass, false, new UFDouble(1), null);
				taxDetail.setDirection("C");
				taxDetail.setAccsubjcode("22210305");
				taxDetail.setAss(ass);
				detailAddCreditAmount(debitAmount.sub(creditAmount), false, new UFDouble(1), taxDetail);
				
				voucher.addDetail(taxDetail);
			}
		}
	}

	private VoucherVO getAirlinePeriodVoucherVO(String airline, String period, String pk_voucherType, 
			String explain, InvoiceValue invoiceInfo, UFDouble amount, String pk_user, UFDate date) {
		
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
			voucher.setYear(date.toString().substring(0, 4)); // 会计年度
			voucher.setPeriod(date.toString().substring(5, 7)); //会计期间
			voucher.setNo(null); //凭证号处理
			voucher.setPrepareddate(date); //制单日期
			voucher.setTallydate(null); //记账日期
			voucher.setAttachment(0); //附单据数
			voucher.setPk_prepared(pk_user); //制单人主键
			voucher.setPk_checked(null); //审核人主键
			voucher.setPk_casher(null); //出纳主键
			voucher.setPk_manager("N/A"); //记账人主键
			voucher.setSignflag(new UFBoolean(false)); //签字标志
			voucher.setModifyflag("YYY"); //凭证修改标志
			voucher.setDetailmodflag(new UFBoolean(true)); //分录增删标志
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
			subjvalue.taxRate = subjMapVOs[0].getNtaxrate()==null?new UFDouble(0.06):subjMapVOs[0].getNtaxrate();
			
			subjMap.put(airport+chargePrjCode, subjvalue);
		}
	}

	private void initDetailVOforVoucher(VoucherVO voucher, String airport, String airPortName, String airline, 
			String airlineName, String invoiceName,	String period, String chargePrjCode, UFDouble amount, 
			boolean useDollar,	UFDouble raito) { 
		try{
			initSubjMapVo(airport, chargePrjCode);
			String pk_accsubjdebit = subjMap.get(airport+chargePrjCode).pk_debitsubj;
			String pk_accsubjcredit = subjMap.get(airport+chargePrjCode).pk_creditsubj;
			
			String accsubjdebitCode = subjMap.get(airport+chargePrjCode).debitSubjCode;
			String accsubjcreditCode = subjMap.get(airport+chargePrjCode).creditSubjCode;
			
			String creditExplain="转："+airlineName+period.substring(0,4)+"年"+period.substring(4,6)+"月"+airPortName+"机场 起降费";
			String debitExplain=creditExplain;
			boolean isClearCenter = invoiceName.indexOf("清算")>-1;
			if (isClearCenter)
				debitExplain+="（清算中心）";
			else
				debitExplain+="（航空公司）";
			
			UFDouble noTaxAmount = amount;
			if (!useDollar)
				noTaxAmount = amount.div(subjMap.get(airport+chargePrjCode).taxRate.add(1));
			{
				AssVO[] debitAss = initAssforDetail(pk_accsubjdebit, airport, airline, chargePrjCode, isClearCenter);
				DetailVO debitVO = findDebitDetailbyAccsubjExplain(voucher, pk_accsubjdebit, debitExplain, debitAss, invoiceName);
				
				if (debitVO==null){
					debitVO = getNewDetailVO(voucher, pk_accsubjdebit, debitExplain, debitAss, useDollar, raito, invoiceName);
					debitVO.setDirection("D");
					debitVO.setAccsubjcode(accsubjdebitCode);
					debitVO.setAss(debitAss);
					detailAddDebitAmount(amount, useDollar, raito, debitVO);
					voucher.insertDetail(debitVO, 0);
				//	debitVO = findDetailbyAccsubjExplain(voucher, pk_accsubjdebit, debitExplain, debitAss, amount); //因为插入的是clone后的VO
				}else{
					detailAddDebitAmount(amount, useDollar, raito, debitVO);
				}
			}
			{
				AssVO[] creditAss = initAssforDetail(pk_accsubjcredit, airport, airline, chargePrjCode, isClearCenter);
				DetailVO creditVO = findCreditDetailbyAccsubjExplain(voucher, pk_accsubjcredit, creditExplain, creditAss, noTaxAmount);
				
				if (creditVO==null){
					creditVO = getNewDetailVO(voucher, pk_accsubjcredit, creditExplain, creditAss, useDollar, raito, "");
					creditVO.setDirection("C");
					creditVO.setAccsubjcode(accsubjcreditCode);
					creditVO.setAss(creditAss);
					detailAddCreditAmount(noTaxAmount, useDollar, raito, creditVO);
					voucher.addDetail(creditVO);
				//	creditVO = findDetailbyAccsubjExplain(voucher, pk_accsubjcredit, creditExplain, creditAss, amount);
				}else{
					detailAddCreditAmount(noTaxAmount, useDollar, raito, creditVO);
				}
			}
		}
		catch(BusinessException be){
			if (!errors.containsKey(be.getMessage()))
				errors.put(be.getMessage(),"");
		}
	}

	private void detailAddCreditAmount(UFDouble amount, boolean useDollar,
			UFDouble raito, DetailVO creditVO) {
		// 合并分录金额处理，特别是外币
		if (useDollar)
			creditVO.setCreditamount(creditVO.getCreditamount().add(amount.div(raito))); // 原币贷方金额
		else
			creditVO.setCreditamount(creditVO.getCreditamount().add(amount)); // 原币贷方金额
		creditVO.setLocalcreditamount(creditVO.getLocalcreditamount().add(amount)); // 本币贷方金额	
	}

	private void detailAddDebitAmount(UFDouble amount, boolean useDollar,
			UFDouble raito, DetailVO debitVO) {
		// 合并分录金额处理，特别是外币
		if (useDollar)
			debitVO.setDebitamount(debitVO.getDebitamount().add(amount.div(raito))); // 原币借方金额
		else
			debitVO.setDebitamount(debitVO.getDebitamount().add(amount)); // 原币借方金额
		debitVO.setLocaldebitamount(debitVO.getLocaldebitamount().add(amount)); // 本币借方金额
	}

	private String getAssFullID(AssVO[] ass) {
		StringBuffer sb = new StringBuffer();
		for(AssVO assItem : ass){
			sb.append(assItem.getPk_Checktype());
			sb.append(assItem.getCheckvaluecode());
		}
		
		return sb.toString();
	}

	private AssVO[] initAssforDetail(String pk_accsubj, String airport,
			String airline, String chargePrjCode, boolean isClearCenter) throws BusinessException {
		ISubjassQry assQry = NCLocator.getInstance().lookup(ISubjassQry.class);
		SubjassVO[] assVOs = assQry.queryBDInfo(pk_accsubj);
		
		Arrays.sort(assVOs, new SubjassComparator());
		
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

		return ass;
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
		if (codename == null || codename.size()==0)
			throw new BusinessException("机场：["+airport+"]下的"+text+"编码：["+otherAssCode+"]对应辅助项不存在。");
		SubjAssValue assValue = new SubjAssValue();
		assValue.pk_freevalue = assValueMapVO[0].getPk_freevalue();
		assValue.code = codename.get(0).get(0).toString();
		assValue.name = codename.get(0).get(1).toString();
		
		subjAssMap.put(pk_bdinfo+otherAssCode, assValue);
		
		return assValue;
	}

	private DetailVO findCreditDetailbyAccsubjExplain(VoucherVO voucher, String pk_accsubj, String explain, AssVO[] ass, UFDouble amount) {
		boolean haveDebit = false;
		String assId = getAssFullID(ass);
		int i=0;
		for (i=0;i<voucher.getNumDetails();i++){
			if (voucher.getDetail(i).getPk_accsubj().equals(pk_accsubj) 
					&& voucher.getDetail(i).getExplanation().equals(explain)
					&& voucher.getDetail(i).getFreevalue30().equals(assId)
					&& (voucher.getDetail(i).getDirection()?voucher.getDetail(i).getDebitamount():voucher.getDetail(i).getCreditamount())
					.multiply(amount).doubleValue()>0){
				haveDebit = true;
				break;
			}
		}
		if (!haveDebit)
			return null;
		else
			return voucher.getDetail(i);
	}
	
	private DetailVO findDebitDetailbyAccsubjExplain(VoucherVO voucher, String pk_accsubj, String explain, AssVO[] ass, String invoiceName) {
		boolean haveDebit = false;
		String assId = getAssFullID(ass);
		int i=0;
		for (i=0;i<voucher.getNumDetails();i++){
			if (voucher.getDetail(i).getPk_accsubj().equals(pk_accsubj) 
					&& voucher.getDetail(i).getExplanation().equals(explain)
					&& voucher.getDetail(i).getFreevalue30().equals(assId)
					&& voucher.getDetail(i).getFreevalue29().equals(invoiceName)){
				haveDebit = true;
				break;
			}
		}
		if (!haveDebit)
			return null;
		else
			return voucher.getDetail(i);
	}
	
	private DetailVO getNewDetailVO(VoucherVO voucher, String pk_accsubj, String explian, AssVO[] ass, boolean useDollar, UFDouble raito, String invoiceName){
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
		
		String assId = getAssFullID(ass);
		detail.setFreevalue30(assId);
		detail.setFreevalue29(invoiceName);
		return detail;
	}
}
