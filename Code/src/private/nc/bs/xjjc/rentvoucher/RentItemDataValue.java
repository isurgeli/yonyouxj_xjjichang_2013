package nc.bs.xjjc.rentvoucher;

import java.util.ArrayList;
import java.util.Hashtable;

import org.springframework.util.Assert;

import nc.bs.dao.BaseDAO;
import nc.vo.bd.b38.JobbasfilVO;
import nc.vo.glcom.ass.AssVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;

public class RentItemDataValue {
	public ArrayList<UFDouble> paidItems;
	public UFDouble income;
	public UFDouble tax;
	public UFDouble paid;
	public UFDouble suspended;
	public UFDouble advanced;
	
	public String contractNo;
	public String payer;
	public String prjCode;
	public String period;
	public String payerName;
	
	public ArrayList<UFDouble> cashSubjAmount;
	public ArrayList<UFDouble> taxSubjAmount;
	public ArrayList<UFDouble> incomeSubjAmount;
	public ArrayList<UFDouble> suspendSubjAmount;
	public ArrayList<UFDouble> advanceSubjAmount;
	
	public RentAccsubjValue subjs;
	public Hashtable<String, AssVO> ass = new Hashtable<String, AssVO>();
	
	private static Hashtable<String, JobbasfilVO> m_airStationAss = new Hashtable<String, JobbasfilVO>();
	private JobbasfilVO m_contractAss;
	
	public RentItemDataValue(String _contractNo, String _payer, String _prjCode, String _period	, String _payerName){
		contractNo = _contractNo;
		payer = _payer;
		prjCode = _prjCode;
		period = _period;
		payerName = _payerName;
		
		income = new UFDouble(0);
		paid = new UFDouble(0);
		suspended = new UFDouble(0);
		advanced = new UFDouble(0);
		
		cashSubjAmount = new ArrayList<UFDouble>();
		incomeSubjAmount = new ArrayList<UFDouble>();
		suspendSubjAmount = new ArrayList<UFDouble>();
		advanceSubjAmount = new ArrayList<UFDouble>();
		taxSubjAmount = new ArrayList<UFDouble>(); 
		
		paidItems = new ArrayList<UFDouble>();
		
		Assert.notNull(_period);
	}
	
	public String getAirStation() throws BusinessException{
		int prjnum = Integer.parseInt(prjCode);
		if (prjnum<=4) return "T1航站楼";
		else if (prjnum<=8) return "T2航站楼";
		else if (prjnum<=12) return "T3航站楼";
		else if (prjnum<=15) return "其它场地";
		else if (prjnum<=19) return "T4航站楼";
		else if (prjnum<=23) return "T5航站楼";
		else throw new BusinessException("["+prjCode+"]无法确定所属航站楼。");
	}
	
	public JobbasfilVO getAirStationAss() throws BusinessException{
		if(!m_airStationAss.containsKey(getAirStation())){
			BaseDAO dao = new BaseDAO();
			Object[] jobvo = dao.retrieveByClause(JobbasfilVO.class, "jobname='"+getAirStation()+"' and pk_jobtype='0001A1100000000001YE'").toArray();
			if (jobvo.length==0)
				throw new BusinessException(getAirStation()+"找不到对应辅助项。");
			m_airStationAss.put(getAirStation(), (JobbasfilVO)jobvo[0]);
		}
		return m_airStationAss.get(getAirStation());
	}
	
	public JobbasfilVO getContractAss() throws BusinessException{
		if(m_contractAss == null){
			BaseDAO dao = new BaseDAO();
			Object jobvo[] = dao.retrieveByClause(JobbasfilVO.class, "instr(jobname,'"+contractNo+"')=1 and pk_jobtype='0001A11000000000027Z'").toArray();
			if (jobvo.length==0)
				throw new BusinessException("合同：["+contractNo+"]找不到对应辅助项。");
			m_contractAss = (JobbasfilVO)jobvo[0];
		}
		return m_contractAss;
	}
}
