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
			throw new BusinessException("ָ�������ڼ䲻��һ�����¡�");
		
		try{
			//  �����������ݲ�ѯ-��ѯ����������
			initIncomeandPay(startDate.toString(), endDate.toString(), biztype);
			
			// ȷ����Ŀ�븨����
			RentItemDataValue[] contracts = rentData.values().toArray(new RentItemDataValue[0]);
			initSubjandAss(contracts);
			
			// ��ӦӦ����Ԥ�տ�Ŀ����ѯ
			initSubjBalance(contracts, startDate.toString());
			
			if (errors.size()>0){
				StringBuffer errmsg = new StringBuffer();
				Enumeration<String> e = errors.keys();

			    while(e.hasMoreElements())
			    	errmsg.append(e.nextElement()+"\n");
			    
				throw new BusinessException(errmsg.toString());
			}
			
			doTaxAdjust(contracts);
			
			// Ӧ�գ����룬Ԥ�գ��ֽ�䶯������
			for(RentItemDataValue contract : contracts){			
				if (contract.paid.doubleValue()>0){
					for(UFDouble paidItem : contract.paidItems)
						contract.cashSubjAmount.add(paidItem);
					
					contract.taxSubjAmount.add(contract.tax.multiply(-1));
				}
				
				if (contract.suspended.doubleValue()>0) { //�����ۼ�Ӧ��
					if (contract.paid.doubleValue() >= contract.suspended.add(contract.income).doubleValue()){ //���¸�������ۼ�Ӧ��+������Ӫ
						contract.suspendSubjAmount.add(contract.suspended.multiply(-1)); // �������Ӧ��
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // ������б�����Ӫ
						contract.advanceSubjAmount.add(contract.paid.sub(contract.suspended).sub(contract.income).multiply(-1)); // ʣ�������Ԥ��
					} else if (contract.paid.doubleValue() >= contract.suspended.doubleValue()){ //���¸�������ۼ�Ӧ��
						contract.suspendSubjAmount.add(contract.suspended.multiply(-1)); // �������Ӧ��
						contract.incomeSubjAmount.add(contract.paid.sub(contract.suspended).multiply(-1)); // ��ʣ�ึ����岿�ֱ�����Ӫ
						contract.incomeSubjAmount.add(contract.income.sub(contract.paid.sub(contract.suspended)).multiply(-1)); // ����ʣ����Ӫ
						contract.suspendSubjAmount.add(contract.income.sub(contract.paid.sub(contract.suspended))); // ����ʣ����Ӫ����Ӧ��
					} else {
						contract.suspendSubjAmount.add(contract.paid.multiply(-1)); // �������Ӧ��
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // ������Ӫ
						contract.suspendSubjAmount.add(contract.income); // ������Ӫ����Ӧ��
					}
				} else if (contract.advanced.doubleValue()>0) {//����Ԥ��
					if (contract.advanced.doubleValue() >= contract.income.doubleValue()){ //Ԥ�������ڱ�����Ӫ
						contract.advanceSubjAmount.add(contract.income); // ��Ԥ�ճ��������Ӫ
						contract.incomeSubjAmount.add(contract.income.multiply(-1));
						contract.advanceSubjAmount.add(contract.paid.multiply(-1)); // ����ȫ������Ԥ��
					} else if (contract.advanced.add(contract.paid).doubleValue() >= contract.income.doubleValue()){ //Ԥ�����+������ڱ�����Ӫ
						contract.advanceSubjAmount.add(contract.advanced); // ��ȫ��Ԥ�ճ岿����Ӫ
						contract.incomeSubjAmount.add(contract.advanced.multiply(-1));
						contract.incomeSubjAmount.add(contract.income.sub(contract.advanced).multiply(-1)); //�ø����ʣ����Ӫ
						contract.advanceSubjAmount.add(contract.paid.sub(contract.income.sub(contract.advanced)).multiply(-1));//ʣ�ึ�����Ԥ��
					} else {
						contract.advanceSubjAmount.add(contract.advanced); // ��ȫ��Ԥ�ճ岿����Ӫ
						contract.incomeSubjAmount.add(contract.advanced.multiply(-1));
						contract.incomeSubjAmount.add(contract.paid.multiply(-1)); // ��ȫ������岿����Ӫ
						contract.incomeSubjAmount.add(contract.income.sub(contract.advanced).sub(contract.paid).multiply(-1)); // ʣ����Ӫ����Ӧ��
						contract.suspendSubjAmount.add(contract.income.sub(contract.advanced).sub(contract.paid));
					}
				} else { //û��Ӧ����Ԥ��
					if (contract.paid.doubleValue() >= contract.income.doubleValue()){ //���¸�����ڱ�����Ӫ
						contract.incomeSubjAmount.add(contract.income.multiply(-1)); // �ø����������Ӫ
						contract.advanceSubjAmount.add(contract.paid.sub(contract.income).multiply(-1)); //ʣ�ึ�����Ԥ��
					} else {
						contract.incomeSubjAmount.add(contract.paid.multiply(-1)); // �ø���岿����Ӫ
						contract.incomeSubjAmount.add(contract.income.sub(contract.paid).multiply(-1)); // ʣ����Ӫ����Ӧ��
						contract.suspendSubjAmount.add(contract.income.sub(contract.paid));
					}
				}
			}
			
			// ������Ӧƾ֤��¼����Ϊ�裬��Ϊ��
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
		String explain = "("+contract.payer+")"+contract.payerName+contract.getAirStation()+contract.period+"�³������޷�("+contract.contractNo+")";
		
		for (UFDouble data : contract.cashSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_cashsubj, data, "�գ�"+explain);
				debitVO.setCheckstyle("0001A110000000000AIU"); // TODO ���㷽ʽ����
				debitVO.setCheckno("100001"); // TODO �����
				debitVO.setAccsubjcode(contract.subjs.getCashsubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.suspendSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_suspendsubj, data, "ת��"+explain);
				debitVO.setAccsubjcode(contract.subjs.getSuspendsubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.advanceSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_advancesubj, data, "ת��"+explain);
				debitVO.setAccsubjcode(contract.subjs.getAdvancesubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.incomeSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, contract.subjs.pk_incomesubj, data, "ת��"+explain);
				debitVO.setAccsubjcode(contract.subjs.getIncomesubjcode());
				initAssforDetail(debitVO, contract);
				voucher.addDetail(debitVO);
			}
		}
		
		for (UFDouble data : contract.taxSubjAmount){
			if (data.doubleValue()!=0){
				DetailVO debitVO = getNewDetailVO(voucher, "0001A110000000008CO4", data, "����˰");
				debitVO.setAccsubjcode("22210305");
				AssVO[] ass = new AssVO[1];
				ass[0] = new AssVO();
				ass[0].setPk_Checktype("0001A1100000000001GH");
				ass[0].setPk_Checkvalue("1022A1100000000000QO");
				ass[0].setCheckvaluecode("03");
				ass[0].setCheckvaluename("�����������");
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
		detail.setPk_detail(null); // ��¼����
		detail.setIsdifflag(new UFBoolean(false));
		detail.setPk_voucher(voucher.getPk_voucher()); // ƾ֤����
		detail.setPk_accsubj(pk_subj); // ��Ŀ����
		detail.setPk_currtype("00010000000000000001"); // ��������
		detail.setPk_sob(voucher.getPk_sob()); // �˲�����
		detail.setPk_corp(voucher.getPk_corp()); // ��˾����
		detail.setDetailindex(null); // ��¼��
		detail.setAssid(null); // ���������ʶ
		detail.setExplanation(explain); // ժҪ����
		detail.setPrice(new UFDouble(0)); // ����
		detail.setExcrate1(new UFDouble(0)); // ����1//�۸�����
		detail.setExcrate2(new UFDouble(1.0)); // ����2//�۱�����
		detail.setDebitquantity(new UFDouble(0)); // �跽����
		if (amount.doubleValue()>0){
			detail.setDebitamount(amount); // ԭ�ҽ跽���
			detail.setLocaldebitamount(amount); // ���ҽ跽���
			detail.setDirection("D");
		}else{
			detail.setDebitamount(new UFDouble(0)); // ԭ�ҽ跽���
			detail.setLocaldebitamount(new UFDouble(0)); // ���ҽ跽���
		}
		detail.setFracdebitamount(new UFDouble(0)); // ���ҽ跽���
		detail.setCreditquantity(new UFDouble(0)); // ��������
		if (amount.doubleValue()<0){
			detail.setCreditamount((amount.multiply(-1))); // ԭ�Ҵ������
			detail.setLocalcreditamount((amount.multiply(-1))); // ���Ҵ������
			detail.setDirection("C");
		}else{
			detail.setCreditamount(new UFDouble(0)); // ԭ�Ҵ������
			detail.setLocalcreditamount(new UFDouble(0)); // ���Ҵ������
		}
		detail.setFraccreditamount(new UFDouble(0)); // ���Ҵ������
		detail.setModifyflag("YYYYYYYYYYYYYYYY"); // �޸ı�־
		detail.setRecieptclass(null); // ���ݴ�����
		detail.setOppositesubj(null); // �Է���Ŀ
		detail.setContrastflag(null); // ���˱�־
		detail.setErrmessage(null); // �����Ϣ
		detail.setCheckstyle(null); // ���㷽ʽ����
		detail.setCheckno(null); // �����
		detail.setCheckdate(null); // ��������
		detail.setPk_innersob(null); // �ڲ��˲�����
		detail.setPk_innercorp(null); // �ڲ���λ����
		detail.setFree6(voucher.getFree1());

		detail.setPk_glorg(voucher.getPk_glorg());// �������
		detail.setPk_glbook(voucher.getPk_glbook());// ����˲�

		return detail;
	}

	private VoucherVO getRentPeriodVoucherVO(RentItemDataValue contract,
			String pk_voucherType, String explain, String pk_user, UFDate date) throws BusinessException {
		VoucherVO voucher = null;
		if (!voucherMap.containsKey(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0))){ // ƾ֤��������
			voucher = new VoucherVO();
			voucher.setPk_voucher(null); //ƾ֤����
			voucher.setPk_vouchertype(pk_voucherType); //ƾ֤�������
			voucher.setPk_sob(null); //�˲�����
			voucher.setPk_corp("1022"); //��˾����
			IGlorgbookAccessor glorg = new GlorgbookCache();
			GlorgbookVO[] books = glorg.getGLOrgBookVOsByPk_Corp2("1022");
			//period = period.substring(0, 4)+"-"+period.substring(4, 6)+"-01";
			voucher.setYear(date.toString().substring(0, 4)); // ������
			voucher.setPeriod(date.toString().substring(5, 7)); //����ڼ�
			voucher.setNo(null); //ƾ֤�Ŵ���
			voucher.setPrepareddate(date); //�Ƶ�����
			voucher.setTallydate(null); //��������
			voucher.setAttachment(0); //��������
			voucher.setPk_prepared(pk_user); //�Ƶ�������
			voucher.setPk_checked(null); //���������
			voucher.setPk_casher(null); //��������
			voucher.setPk_manager("N/A"); //����������
			voucher.setSignflag(new UFBoolean(false)); //ǩ�ֱ�־
			voucher.setModifyflag("YYY"); //ƾ֤�޸ı�־
			voucher.setDetailmodflag(new UFBoolean(true)); //��¼��ɾ��־
			voucher.setDiscardflag(new UFBoolean(false)); //���ϱ�־
			voucher.setPk_system("GL"); //�Ƶ�ϵͳ����
			voucher.setAddclass(null); //���ӽӿ���
			voucher.setModifyclass(null); //�޸Ľӿ���
			voucher.setDeleteclass(null); //ɾ���ӿ���
			voucher.setVoucherkind(0); //ƾ֤����
			voucher.setTotaldebit(new UFDouble(0)); //�跽�ϼ�
			voucher.setTotalcredit(new UFDouble(0)); //�����ϼ�
			if (explain!=null && explain.length()>0)
				voucher.setExplanation(explain); //ƾ֤ժҪ

			voucher.setFree10("VOUCHERNEWADD");
			voucher.setFree1(voucher.getPeriod());
			voucher.setContrastflag(null); //���˱�־
			voucher.setErrmessage(""); //�����Ϣ
			// ������������˵�����
			voucher.setPk_glorg(books[0].getPk_glorg()); //�������
			voucher.setPk_glbook(books[0].getPk_glbook()); //����˲�
			voucher.setPk_glorgbook(books[0].getPrimaryKey());
			
			voucherMap.put(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0), voucher); // ƾ֤����
		}else{ 
			voucher = voucherMap.get(contract.getAirStation()+String.valueOf(contract.subjs.taxRate.doubleValue()==0)); // ƾ֤����
		}
		
		return voucher;
	}

	private void initIncomeandPay(String sAccMonth, String eAccMonth, String biztype)	throws BusinessException{
		//  �����������ݲ�ѯ-��ѯ����������
		BaseDAO otherDao = new BaseDAO("xj_amdb");
		// ����ͻ�������վ¥
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> incomes = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_contract.contract_no, xj_amdb.view_rent_contract.payer_code, xj_amdb.view_rent_contract.charge_project_id, "
				+"xj_amdb.view_rent_settlement.amount, xj_amdb.view_payer.fname " 
				+"from xj_amdb.view_rent_settlement, xj_amdb.view_rent_contract, xj_amdb.view_payer " 
				+"where xj_amdb.view_rent_contract.contract_no=xj_amdb.view_rent_settlement.contract_no and xj_amdb.view_payer.iata_code=xj_amdb.view_rent_contract.payer_code and xj_amdb.view_payer.airport_code='URC' "
				+(biztype.equals("ALL")?"":"and xj_amdb.view_rent_contract.charge_project_id='"+biztype+"' ")
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'��',''), 'dd-mm-yy') >= to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'��',''), 'dd-mm-yy') <= to_date('"+eAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_contract.charge_project_id is not null", new VectorProcessor()); //ʱ��������ȷ��ֻ���һ���µ�
		
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
		// �����տ����ݲ�ѯ
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> pays = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_contract.contract_no, xj_amdb.view_rent_contract.payer_code, xj_amdb.view_rent_contract.charge_project_id, "
				+"xj_amdb.view_rent_paid.amount, xj_amdb.view_payer.fname " 
				+"from xj_amdb.view_rent_contract, xj_amdb.view_rent_paid, xj_amdb.view_payer " 
				+"where xj_amdb.view_rent_contract.contract_no=xj_amdb.view_rent_paid.contract_no and xj_amdb.view_payer.iata_code=xj_amdb.view_rent_contract.payer_code and xj_amdb.view_payer.airport_code='URC' "
				+(biztype.equals("ALL")?"":"and xj_amdb.view_rent_contract.charge_project_id='"+biztype+"' ")
				+"and xj_amdb.view_rent_paid.paid_date >= to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_paid.paid_date <= to_date('"+eAccMonth+"','yyyy-mm-dd') "
				+"and xj_amdb.view_rent_contract.charge_project_id is not null", new VectorProcessor()); //ʱ��������ȷ��ֻ���һ���µ�
		
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
				+"and to_date(replace(replace(substr(xj_amdb.view_rent_settlement.efficient_time,1,9),' ',''),'��',''), 'dd-mm-yy') < to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"group by view_rent_settlement.contract_no"
				, new VectorProcessor()); //���֮ǰ�ѷ���������
		
		for(Vector<Object> alreadyincome : alreadyincomes){
			if (rentData.containsKey(alreadyincome.get(0).toString())){
				rentData.get(alreadyincome.get(0).toString()).suspended = rentData.get(alreadyincome.get(0).toString()).suspended.add(
						new UFDouble(alreadyincome.get(1).toString()));
			}
		}
		// ������ʷ�տ����ݲ�ѯ
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> alreadypays = (Vector<Vector<Object>>)otherDao.executeQuery("select xj_amdb.view_rent_paid.contract_no, sum(xj_amdb.view_rent_paid.amount) "
				+"from xj_amdb.view_rent_paid "
				+"where xj_amdb.view_rent_paid.contract_no in "+insql.toString()
				+"and xj_amdb.view_rent_paid.paid_date < to_date('"+sAccMonth+"','yyyy-mm-dd') "
				+"group by xj_amdb.view_rent_paid.contract_no ", new VectorProcessor()); //���֮ǰ�ѷ����ĸ���
		
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
						throw new BusinessException("���޷�����Ŀ��["+contract.prjCode+"]û�����ÿ�Ŀ���ա�"); 
					
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
						throw new BusinessException("���޿�Ŀ���ֲ��š����̡���վ¥����ͬ����ĸ����ϵͳ�޷�����");
					
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
			text = "�ͻ�";
			sql = "select custcode, custname from bd_cubasdoc where pk_cubasdoc='PKVALUE'";
		}else if (pk_bdinfo.equals(BDInfo.DEPARTMENT.getValue())) {
			text = "����";
			sql = "select deptcode, deptname from bd_deptdoc where pk_deptdoc='PKVALUE'";
			needAirPortFilter = false;
		}else
			throw new BusinessException("��Ŀ���ֲ��š����̡���������ĸ����ϵͳ�޷�����");
		
		BaseDAO dao = new BaseDAO();
		@SuppressWarnings("unchecked")
		AssValueMapVO[] assValueMapVO = (AssValueMapVO[])dao.retrieveByClause(AssValueMapVO.class, "pk_bdinfo='"+pk_bdinfo
				+"' and pk_corp='1022' "+(needAirPortFilter?"and votherbiz='"+airport +"'":"")
				+" and vothercode='"+otherAssCode+"'").toArray(new AssValueMapVO[0]);
		if (assValueMapVO.length==0)
			throw new BusinessException("������["+airport+"]�µ�"+text+"���룺["+otherAssCode+"]û�����ø�������ա�"); 
		
		@SuppressWarnings("unchecked")
		Vector<Vector<Object>> codename = (Vector<Vector<Object>>)dao.executeQuery(sql.replace("PKVALUE", assValueMapVO[0].getPk_freevalue()), new VectorProcessor());
		if (codename == null || codename.size()==0)
			throw new BusinessException("������["+airport+"]�µ�"+text+"���룺["+otherAssCode+"]��Ӧ��������ڡ�");
		SubjAssValue assValue = new SubjAssValue();
		assValue.pk_freevalue = assValueMapVO[0].getPk_freevalue();
		assValue.code = codename.get(0).get(0).toString();
		assValue.name = codename.get(0).get(1).toString();
		
		subjAssMap.put(pk_bdinfo+otherAssCode, assValue);
		
		return assValue;
	}
}
