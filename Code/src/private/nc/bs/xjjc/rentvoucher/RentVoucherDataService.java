package nc.bs.xjjc.rentvoucher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import nc.bd.glorgbook.GlorgbookCache;
import nc.bd.glorgbook.IGlorgbookAccessor;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.impl.xjjc.voucher.SubjAssValue;
import nc.itf.gl.voucher.IVoucher;
import nc.itf.uap.bd.accsubj.ISubjassQry;
import nc.itf.xjjc.rentvoucher.IRentVoucherDataService;
import nc.itf.xjjc.voucher.BDInfo;
import nc.itf.xjjc.voucher.VoucherBizType;
import nc.jdbc.framework.processor.VectorProcessor;
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

public class RentVoucherDataService implements IRentVoucherDataService {
	private Hashtable<String, RentItemDataValue> rentData = new Hashtable<String, RentItemDataValue>();
	private Hashtable<String, VoucherVO> voucherMap = new Hashtable<String, VoucherVO>();
	private Hashtable<String, RentAccsubjValue> subjMap = new Hashtable<String, RentAccsubjValue>();
	private Hashtable<String, SubjAssValue> subjAssMap = new Hashtable<String, SubjAssValue>();
	private Hashtable<String, String> errors = new Hashtable<String, String>();
	
	public boolean genRentVOucher(String sAccMonth, String eAccMonth,
			String biztype, String pk_voucherType, String explain, String pk_user, UFDate date)
			throws BusinessException {
		
		rentData.clear();
		voucherMap.clear();
		errors.clear();
		subjMap.clear();
		subjAssMap.clear();
		
		UFDate startDate = new UFDate(sAccMonth+"-01");
		UFDate endDate = new UFDate(eAccMonth+"-01");
		endDate = endDate.getDateAfter(endDate.getDaysMonth()-endDate.getDay());
		if (startDate.getMonth()!=endDate.getMonth() || startDate.getDay()!=1 || endDate.getDateAfter(1).getDay()!=1)
			throw new BusinessException("指定生成期间不是一个整月。");
		
		try{
			//  租赁收入数据查询-查询条件与账期
			initIncomeandPay(startDate.toString(), endDate.toString(), biztype);
			
			// 确定科目与辅助项
			RentItemDataValue[] contracts = rentData.values().toArray(new RentItemDataValue[0]);
			initSubjandAss(contracts);
			
			// 对应应收与预收科目余额查询
			initSubjBalance(contracts, startDate.toString());
			
			if (errors.size()>0){
				StringBuffer errmsg = new StringBuffer();
				Enumeration<String> e = errors.keys();

			    while(e.hasMoreElements())
			    	errmsg.append(e.nextElement()+"\n");
			    
				throw new BusinessException(errmsg.toString());
			}
			
			doTaxAdjust(contracts);
			
			// 应收，收入，预收，现金变动金额汇总
			for(RentItemDataValue contract : contracts){			
				if (contract.paid.doubleValue()>0){
					for(UFDouble paidItem : contract.paidItems)
						contract.cashSubjAmount.add(paidItem);
					
					contract.taxSubjAmount.add(contract.tax.multiply(-1));
				}
				
				if (contract.suspended.doubleValue()>0) { //存在累计应收
					if (contract.paid.doubleValue() >= contract.suspended.add(contract.income).doubleValue()){ //本月付款大于累计应收+本月主营
						contract.suspendSubjAmount.add(contract.suspended.multiply(-1)); // 冲掉所有应收
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // 冲掉所有本期主营
						contract.advanceSubjAmount.add(contract.paid.sub(contract.suspended).sub(contract.income).multiply(-1)); // 剩余金额进入预收
					} else if (contract.paid.doubleValue() >= contract.suspended.doubleValue()){ //本月付款大于累计应收
						contract.suspendSubjAmount.add(contract.suspended.multiply(-1)); // 冲掉所有应收
						contract.incomeSubjAmount.add(contract.paid.sub(contract.suspended).multiply(-1)); // 用剩余付款金额冲部分本期主营
						contract.incomeSubjAmount.add(contract.income.sub(contract.paid.sub(contract.suspended)).multiply(-1)); // 处理剩余主营
						contract.suspendSubjAmount.add(contract.income.sub(contract.paid.sub(contract.suspended))); // 处理剩余主营进入应收
					} else {
						contract.suspendSubjAmount.add(contract.paid.multiply(-1)); // 冲掉部分应收
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // 处理主营
						contract.suspendSubjAmount.add(contract.income); // 处理主营进入应收
					}
				} else if (contract.advanced.doubleValue()>0) {//存在预收
					if (contract.advanced.doubleValue() >= contract.income.doubleValue()){ //预收余额大于本月主营
						contract.advanceSubjAmount.add(contract.income); // 用预收冲掉本期主营
						contract.incomeSubjAmount.add(contract.income.multiply(-1));
						contract.advanceSubjAmount.add(contract.paid.multiply(-1)); // 付款全部进入预收
					} else if (contract.advanced.add(contract.paid).doubleValue() >= contract.income.doubleValue()){ //预收余额+收入大于本月主营
						contract.advanceSubjAmount.add(contract.advanced); // 用全部预收冲部分主营
						contract.incomeSubjAmount.add(contract.advanced.multiply(-1));
						contract.incomeSubjAmount.add(contract.income.sub(contract.advanced).multiply(-1)); //用付款冲剩余主营
						contract.advanceSubjAmount.add(contract.paid.sub(contract.income.sub(contract.advanced)).multiply(-1));//剩余付款进入预收
					} else {
						contract.advanceSubjAmount.add(contract.advanced); // 用全部预收冲部分主营
						contract.incomeSubjAmount.add(contract.advanced.multiply(-1));
						contract.incomeSubjAmount.add(contract.paid.multiply(-1)); // 用全部付款冲部分主营
						contract.incomeSubjAmount.add(contract.income.sub(contract.advanced).sub(contract.paid).multiply(-1)); // 剩余主营进入应收
						contract.suspendSubjAmount.add(contract.income.sub(contract.advanced).sub(contract.paid));
					}
				} else { //没有应收与预收
					if (contract.paid.doubleValue() >= contract.income.doubleValue()){ //本月付款大于本月主营
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // 用付款冲所有主营
						contract.advanceSubjAmount.add(contract.paid.sub(contract.income).multiply(-1)); //剩余付款进入预收
					} else {
						contract.incomeSubjAmount.add(contract.paid.multiply(-1)); // 用付款冲部分主营
						contract.incomeSubjAmount.add(contract.income.sub(contract.paid).multiply(-1)); // 剩余主营进入应收
						contract.suspendSubjAmount.add(contract.income.sub(contract.paid));
					}
				}
			}
			
			// 生成相应凭证分录，正为借，负为贷
			for(RentItemDataValue contract : contracts){
				VoucherVO voucher = getRentPeriodVoucherVO(contract, pk_voucherType, explain, pk_user, date);
				ArrayList<UFDouble> allData = new ArrayList<UFDouble>();
				allData.addAll(contract.cashSubjAmount);
				allData.addAll(contract.suspendSubjAmount);
				allData.addAll(contract.advanceSubjAmount);
				allData.addAll(contract.incomeSubjAmount);
				allData.addAll(contract.taxSubjAmount);
				
				for (UFDouble data : allData){
					voucher.setTotaldebit(voucher.getTotaldebit().add(data.doubleValue()>0?data:new UFDouble(0)));
					voucher.setTotalcredit(voucher.getTotalcredit().add(data.doubleValue()<0?data.multiply(-1):new UFDouble(0)));
				}				
				initDetailVOforVoucher(voucher, contract);
			}
			
			VoucherVO[] vouchers = voucherMap.values().toArray(new VoucherVO[0]);
			IVoucher voucherBo = NCLocator.getInstance().lookup(IVoucher.class);
			for(VoucherVO voucher : vouchers){
				if (voucher.getExplanation() == null)
					voucher.setExplanation(((DetailVO)voucher.getDetail().get(0)).getExplanation());
				if (voucher.getDetail()!=null && voucher.getDetail().size()>0){
					//Collections.sort(voucher.getDetail(), new RentDetailComparator());
					voucherBo.save(voucher, true);
				}
			}
		}catch (BusinessException be){
			throw be;
		}catch(Exception e){
			Logger.error(e.getMessage(), e);
			throw new BusinessException(e);
		}
		return true;
	}

	private void doTaxAdjust(RentItemDataValue[] contracts) {
		for(RentItemDataValue contract : contracts){
			contract.tax = UFDouble.ZERO_DBL;
			if (contract.subjs.taxRate.doubleValue() > 0) {
				contract.tax = contract.paid.div(contract.subjs.taxRate.add(1)).multiply(contract.subjs.taxRate);
				contract.paid = contract.paid.div(contract.subjs.taxRate.add(1));
				contract.advanced = contract.advanced.div(contract.subjs.taxRate.add(1));
				contract.suspended = contract.suspended.div(contract.subjs.taxRate.add(1));
				contract.income = contract.income.div(contract.subjs.taxRate.add(1)); 
			}
		}
	}

	private void initDetailVOforVoucher(VoucherVO voucher, RentItemDataValue contract) throws BusinessException {
		String explain = "("+contract.payer+")"+contract.payerName+contract.getAirStation()+contract.period+"月场地租赁费("+contract.contractNo+")";
		
		for (UFDouble data : contract.cashSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_cashsubj, data, "收："+explain);
				debitVO.setCheckstyle("0001A110000000000AIU"); // TODO 结算方式主键
				debitVO.setCheckno("100001"); // TODO 结算号
				debitVO.setAccsubjcode(contract.subjs.getCashsubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.suspendSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_suspendsubj, data, "转："+explain);
				debitVO.setAccsubjcode(contract.subjs.getSuspendsubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.advanceSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_advancesubj, data, "转："+explain);
				debitVO.setAccsubjcode(contract.subjs.getAdvancesubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.incomeSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_incomesubj, data, "转："+explain);
				debitVO.setAccsubjcode(contract.subjs.getIncomesubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.taxSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, "0001A110000000008CO4", data, "销项税");
				debitVO.setAccsubjcode("22210305");
				AssVO[] ass = new AssVO[1];
				ass[0] = new AssVO();
				ass[0].setPk_Checktype("0001A1100000000001GH");
				ass[0].setPk_Checkvalue("1022A1100000000000QO");
				ass[0].setCheckvaluecode("03");
				ass[0].setCheckvaluename("收入管理中心");
				debitVO.setAss(ass);
				voucher.addDetail(debitVO);
			}
		}
	}

	private void initAssforDetail(DetailVO debitVO, RentItemDataValue contract) throws BusinessException {
		ISubjassQry assQry = NCLocator.getInstance().lookup(ISubjassQry.class);
		SubjassVO[] subjAssVOs = assQry.queryBDInfo(debitVO.getPk_accsubj());
		
		AssVO[] ass = new AssVO[subjAssVOs.length];
		for (int i = 0; i < subjAssVOs.length; i++) {
			ass[i] = contract.ass.get(subjAssVOs[i].getPk_bdinfo());
		}
		debitVO.setAss(ass);
	}

	private DetailVO getNewDetailVO(VoucherVO voucher, String pk_subj,	UFDouble amount, String explain) {
		DetailVO detail = new DetailVO();
		detail.setPk_detail(null); // 分录主键
		detail.setIsdifflag(new UFBoolean(false));
		detail.setPk_voucher(voucher.getPk_voucher()); // 凭证主键
		detail.setPk_accsubj(pk_subj); // 科目主键
		detail.setPk_currtype("00010000000000000001"); // 币种主键
		detail.setPk_sob(voucher.getPk_sob()); // 账簿主键
		detail.setPk_corp(voucher.getPk_corp()); // 公司主键
		detail.setDetailindex(null); // 分录号
		detail.setAssid(null); // 辅助核算标识
		detail.setExplanation(explain); // 摘要内容
		detail.setPrice(new UFDouble(0)); // 单价
		detail.setExcrate1(new UFDouble(0)); // 汇率1//折辅汇率
		detail.setExcrate2(new UFDouble(1.0)); // 汇率2//折本汇率
		detail.setDebitquantity(new UFDouble(0)); // 借方数量
		if (amount.doubleValue()>0){
			detail.setDebitamount(amount); // 原币借方金额
			detail.setLocaldebitamount(amount); // 本币借方金额
			detail.setDirection("D");
		}else{
			detail.setDebitamount(new UFDouble(0)); // 原币借方金额
			detail.setLocaldebitamount(new UFDouble(0)); // 本币借方金额
		}
		detail.setFracdebitamount(new UFDouble(0)); // 辅币借方金额
		detail.setCreditquantity(new UFDouble(0)); // 贷方数量
		if (amount.doubleValue()<0){
			detail.setCreditamount((amount.multiply(-1))); // 原币贷方金额
			detail.setLocalcreditamount((amount.multiply(-1))); // 本币贷方金额
			detail.setDirection("C");
		}else{
			detail.setCreditamount(new UFDouble(0)); // 原币贷方金额
			detail.setLocalcreditamount(new UFDouble(0)); // 本币贷方金额
		}
		detail.setFraccreditamount(new UFDouble(0)); // 辅币贷方金额
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

	private VoucherVO getRentPeriodVoucherVO(RentItemDataValue contract,
			String pk_voucherType, String explain, String pk_user, UFDate date) throws BusinessException {
		VoucherVO voucher = null;
		if (!voucherMap.containsKey(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0))){ // 凭证生成条件
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
			voucher.setTotaldebit(new UFDouble(0)); //借方合计
			voucher.setTotalcredit(new UFDouble(0)); //贷方合计
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
			
			voucherMap.put(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0), voucher); // 凭证条件
		}else{ 
			voucher = voucherMap.get(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0)); // 凭证条件
		}
		
		return voucher;
	}

	private void initIncomeandPay(String sAccMonth, String eAccMonth, String biztype)	throws BusinessException{
		//  租赁收入数据查询-查询条件与账期
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		// 处理客户名，航站楼
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> incomes = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_contract.contract_no, xj_amdb.view_rent_contract.payer_code, xj_amdb.view_rent_contract.charge_project_id, "
				+"xj_amdb.view_rent_settlement.amount, xj_amdb.view_payer.fname " 
				+"from xj_amdb.view_rent_settlement, xj_amdb.view_rent_contract, xj_amdb.view_payer " 
				+"where xj_amdb.view_rent_contract.contract_no=xj_amdb.view_rent_settlement.contract_no and xj_amdb.view_payer.iata_code=xj_amdb.view_rent_contract.payer_code and xj_amdb.view_payer.airport_code='URC' "
				+(biztype.equals("ALL")?"":"and xj_amdb.view_rent_contract.charge_project_id='"+biztype+"' ")
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'月',''), 'dd-mm-yy') >= to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'月',''), 'dd-mm-yy') <= to_date('"+eAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_contract.charge_project_id is not null", new VectorProcessor()); //时间限制需确保只查出一个月的
		
		for(Vector<Object> income : incomes){
			if (!rentData.containsKey(income.get(0).toString())){
				RentItemDataValue itemValue = new RentItemDataValue(income.get(0).toString(),
						income.get(1).toString(),income.get(2).toString(),sAccMonth.substring(0,7),
						income.get(4).toString());
				itemValue.income = new UFDouble(income.get(3).toString());
				rentData.put(income.get(0).toString(), itemValue);
			}else{
				rentData.get(income.get(0).toString()).income = new UFDouble(income.get(3).toString());
			}
		}
		// 租赁收款数据查询
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> pays = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_contract.contract_no, xj_amdb.view_rent_contract.payer_code, xj_amdb.view_rent_contract.charge_project_id, "
				+"xj_amdb.view_rent_paid.amount, xj_amdb.view_payer.fname " 
				+"from xj_amdb.view_rent_contract, xj_amdb.view_rent_paid, xj_amdb.view_payer " 
				+"where xj_amdb.view_rent_contract.contract_no=xj_amdb.view_rent_paid.contract_no and xj_amdb.view_payer.iata_code=xj_amdb.view_rent_contract.payer_code and xj_amdb.view_payer.airport_code='URC' "
				+(biztype.equals("ALL")?"":"and xj_amdb.view_rent_contract.charge_project_id='"+biztype+"' ")
				+"and xj_amdb.view_rent_paid.paid_date >= to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_paid.paid_date <= to_date('"+eAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_contract.charge_project_id is not null", new VectorProcessor()); //时间限制需确保只查出一个月的
		
		for(Vector<Object> pay : pays){
			if (!rentData.containsKey(pay.get(0).toString())){
				RentItemDataValue itemValue = new RentItemDataValue(pay.get(0).toString(),
						pay.get(1).toString(),pay.get(2).toString(),sAccMonth.substring(0,7),
						pay.get(4).toString());
				itemValue.paid = itemValue.paid.add(new UFDouble(pay.get(3).toString()));
				itemValue.paidItems.add(new UFDouble(pay.get(3).toString()));
				rentData.put(pay.get(0).toString(), itemValue);
			}else{
				rentData.get(pay.get(0).toString()).paid = rentData.get(pay.get(0).toString()).paid.add(
						new UFDouble(pay.get(3).toString()));
				rentData.get(pay.get(0).toString()).paidItems.add(new UFDouble(pay.get(3).toString()));
			}
		}
	}

	private void initSubjBalance(RentItemDataValue[] contracts, String sAccMonth) throws DAOException {
		StringBuilder insql = new StringBuilder();
		insql.append("(");
		for(int i=1;i<contracts.length;i++){
			insql.append("'"+contracts[i].contractNo+"',");
		}
		insql.deleteCharAt(insql.length()-1);
		insql.append(") ");
		
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> alreadyincomes = (Vector<Vector<Object>>)otherDao.executeQuery("select view_rent_settlement.contract_no, sum(xj_amdb.view_rent_settlement.amount) " 
				+"from xj_amdb.view_rent_settlement " 
				+"where xj_amdb.view_rent_settlement.contract_no in "+insql.toString()
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'月',''), 'dd-mm-yy') < to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"group by view_rent_settlement.contract_no"
				, new VectorProcessor()); //查出之前已发生的收入
		
		for(Vector<Object> alreadyincome : alreadyincomes){
			if (rentData.containsKey(alreadyincome.get(0).toString())){
				rentData.get(alreadyincome.get(0).toString()).suspended = rentData.get(alreadyincome.get(0).toString()).suspended.add(
						new UFDouble(alreadyincome.get(1).toString()));
			}
		}
		// 租赁历史收款数据查询
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> alreadypays = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_paid.contract_no, sum(xj_amdb.view_rent_paid.amount) "
				+"from xj_amdb.view_rent_paid "
				+"where xj_amdb.view_rent_paid.contract_no in "+insql.toString()
				+"and xj_amdb.view_rent_paid.paid_date < to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"group by xj_amdb.view_rent_paid.contract_no ", new VectorProcessor()); //查出之前已发生的付款
		
		for(Vector<Object> alreadypay : alreadypays){
			if (rentData.containsKey(alreadypay.get(0).toString())){
				rentData.get(alreadypay.get(0).toString()).suspended = rentData.get(alreadypay.get(0).toString()).suspended.sub(
						new UFDouble(alreadypay.get(1).toString()));
			}
		}
		
		for(RentItemDataValue contract : contracts){
			if (contract.suspended.doubleValue()<0) {
				contract.advanced = contract.suspended.multiply(-1);
				contract.suspended = new UFDouble(0);
			}
		}
	}

	private void initSubjandAss(RentItemDataValue[] contracts) throws BusinessException {
		BaseDAO dao = new BaseDAO();
		for(RentItemDataValue contract : contracts){
			try{
				if (!subjMap.containsKey(contract.prjCode)){
					@SuppressWarnings("unchecked")
					SubjMapVO[] subjMapVOs = (SubjMapVO[])dao.retrieveByClause(SubjMapVO.class, "pk_subjbiz='"+VoucherBizType.RENTREVED.getValue()
							+"' and pk_corp='1022"
							+"' and vothercode='"+contract.prjCode+"'").toArray(new SubjMapVO[0]);
					if (subjMapVOs.length<1)
						throw new BusinessException("租赁费用项目：["+contract.prjCode+"]没有配置科目对照。"); 
					
					RentAccsubjValue subjvalue = new RentAccsubjValue();
					subjvalue.pk_cashsubj = subjMapVOs[0].getPk_debitsubj();
					subjvalue.pk_suspendsubj = subjMapVOs[0].getPk_suspendsubj();
					subjvalue.pk_advancesubj = subjMapVOs[0].getPk_advancesubj();
					subjvalue.pk_incomesubj = subjMapVOs[0].getPk_creditsubj();
					subjvalue.taxRate = subjMapVOs[0].getNtaxrate()==null?new UFDouble(0):subjMapVOs[0].getNtaxrate();
				
					subjMap.put(contract.prjCode, subjvalue);
				}
				contract.subjs = subjMap.get(contract.prjCode);
			}catch(BusinessException be)			{
				if (!errors.containsKey(be.getMessage()))
					errors.put(be.getMessage(),"");
			}
		}
	
		for(RentItemDataValue contract : contracts){
			
			ISubjassQry assQry = NCLocator.getInstance().lookup(ISubjassQry.class);
			ArrayList<SubjassVO> subjAssVOs = new ArrayList<SubjassVO>();
			subjAssVOs.addAll(Arrays.asList(assQry.queryBDInfo(contract.subjs.pk_cashsubj)));
			subjAssVOs.addAll(Arrays.asList(assQry.queryBDInfo(contract.subjs.pk_incomesubj)));
			subjAssVOs.addAll(Arrays.asList(assQry.queryBDInfo(contract.subjs.pk_suspendsubj)));
			subjAssVOs.addAll(Arrays.asList(assQry.queryBDInfo(contract.subjs.pk_advancesubj)));
				
			for (SubjassVO subjAssVO : subjAssVOs) {
				try{
					if (contract.ass.containsKey(subjAssVO.getPk_bdinfo())) continue;
					
					String otherAssCode;
					AssVO assVO = new AssVO();
					assVO.setPk_Checktype(subjAssVO.getPk_bdinfo());
					if (subjAssVO.getPk_bdinfo().equals(BDInfo.CUSTOMER.getValue()))
						otherAssCode = contract.payer;
					else if (subjAssVO.getPk_bdinfo().equals(BDInfo.DEPARTMENT.getValue()))
						otherAssCode = contract.prjCode;
					else if (subjAssVO.getPk_bdinfo().equals(BDInfo.CONTRACT.getValue())){
						otherAssCode = null;
						assVO.setPk_Checkvalue(contract.getContractAss().getPrimaryKey());
						assVO.setCheckvaluecode(contract.getContractAss().getJobcode());
						assVO.setCheckvaluename(contract.getContractAss().getJobclName());
					}
					else if (subjAssVO.getPk_bdinfo().equals(BDInfo.TERMINAL.getValue())){
						otherAssCode = null;
						assVO.setPk_Checkvalue(contract.getAirStationAss().getPrimaryKey());
						assVO.setCheckvaluecode(contract.getAirStationAss().getJobcode());
						assVO.setCheckvaluename(contract.getAirStationAss().getJobclName());
					}
					else
						throw new BusinessException("租赁科目出现部门、客商、航站楼、合同以外的辅助项，系统无法处理。");
					
					if (otherAssCode !=null){
						SubjAssValue assValue = getSubjAssValue(subjAssVO.getPk_bdinfo(), "URC", otherAssCode);
						
						assVO.setPk_Checkvalue(assValue.pk_freevalue);
						assVO.setCheckvaluecode(assValue.code);
						assVO.setCheckvaluename(assValue.name);
					}
					contract.ass.put(subjAssVO.getPk_bdinfo(), assVO);
				}catch(BusinessException be) {
					if (!errors.containsKey(be.getMessage()))
						errors.put(be.getMessage(),"");
				}
			}
		}
	}
	
	private SubjAssValue getSubjAssValue(String pk_bdinfo, String airport, String otherAssCode) throws BusinessException {
		if (subjAssMap.containsKey(pk_bdinfo+otherAssCode))
			return subjAssMap.get(pk_bdinfo+otherAssCode);
		
		String text,sql;
		boolean needAirPortFilter = true;
		if (pk_bdinfo.equals(BDInfo.CUSTOMER.getValue())) {
			text = "客户";
			sql = "select custcode, custname from bd_cubasdoc where pk_cubasdoc='PKVALUE'";
		}else if (pk_bdinfo.equals(BDInfo.DEPARTMENT.getValue())) {
			text = "部门";
			sql = "select deptcode, deptname from bd_deptdoc where pk_deptdoc='PKVALUE'";
			needAirPortFilter = false;
		}else
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
}
