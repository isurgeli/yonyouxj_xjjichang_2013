package nc.bs.xjjc.rentvoucher;

import java.util.Vector;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.logging.Debug;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;

public class RentAccsubjValue {
	public String pk_cashsubj;
	public String pk_incomesubj;
	public String pk_suspendsubj;
	public String pk_advancesubj;
	
	public UFDouble taxRate;
	
	private String cashsubjcode;
	private String incomesubjcode;
	private String suspendsubjcode;
	private String advancesubjcode;
	
	public String getCashsubjcode() throws BusinessException {
		if (cashsubjcode==null) initSubjCode();
		return cashsubjcode;
	}
	
	public String getIncomesubjcode() throws BusinessException {
		if (incomesubjcode==null) initSubjCode();
		return incomesubjcode;
	}
	public String getSuspendsubjcode() throws BusinessException {
		if (suspendsubjcode==null) initSubjCode();
		return suspendsubjcode;
	}
	public String getAdvancesubjcode() throws BusinessException {
		if (advancesubjcode==null) initSubjCode();
		return advancesubjcode;
	}
	
	private void initSubjCode() throws BusinessException {
		BaseDAO dao = new BaseDAO();
		try {
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> data = (Vector<Vector<Object>>)dao.executeQuery("select pk_accsubj, subjcode from bd_accsubj where pk_accsubj in ('"
					+pk_cashsubj+"','"
					+pk_incomesubj+"','"
					+pk_suspendsubj+"','"
					+pk_advancesubj+"')", new VectorProcessor());
			
			for (Vector<Object> item : data){
				if (item.get(0).toString().equals(pk_cashsubj))
					cashsubjcode = item.get(1).toString();
				else if (item.get(0).toString().equals(pk_incomesubj))
					incomesubjcode = item.get(1).toString();
				else if (item.get(0).toString().equals(pk_suspendsubj))
					suspendsubjcode = item.get(1).toString();
				else if (item.get(0).toString().equals(pk_advancesubj))
					advancesubjcode = item.get(1).toString();
			}
		} catch (DAOException e) {
			Debug.error(e.getMessage(),e);
			throw new BusinessException(e.getMessage());
		}
	}
}
