package nc.bs.xjjc.personloan;

import java.util.Hashtable;
import java.util.Vector;

import nc.bd.glorgbook.GlorgbookCache;
import nc.bd.glorgbook.IGlorgbookAccessor;
import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.itf.gl.pub.ICommAccBookPub;
import nc.itf.xjjc.personloan.IXjjcPersonLoan;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.bd.b54.GlorgbookVO;
import nc.vo.glcom.ass.AssVO;
import nc.vo.glcom.balance.GLQueryKey;
import nc.vo.glcom.balance.GlBalanceVO;
import nc.vo.glcom.balance.GlQueryVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;

public class XjjcPersonLoanImpl implements IXjjcPersonLoan {

	public void refreshPersonLoan() throws BusinessException {
		GlQueryVO glQVO = getGLQueryVO();
		
		ICommAccBookPub accBookPub =  NCLocator.getInstance().lookup(ICommAccBookPub.class);
		GlBalanceVO[] vos = accBookPub.getEndBalance(glQVO);
		doDatabaseWork(vos);
	}
	
	private void doDatabaseWork(GlBalanceVO[] datas) throws DAOException{
		Hashtable<String, UFDouble> mergebyPerson = new Hashtable<String, UFDouble>();
		BaseDAO dao = new BaseDAO();
		dao.executeUpdate("delete from xjjc_gl_personloan");
		String sql = "insert into xjjc_gl_personloan(pk_psnbasdoc,nloan) values(?,?)";
		for(int i=0;i<datas.length;i++){
			
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> assData = (Vector<Vector<Object>>)dao.executeQuery("select checkvalue from gl_freevalue where freevalueid='"+datas[i].getAssid()
					+"' AND checktype='00010000000000000001'", new VectorProcessor());
			if (assData.size()>0){
				UFDouble amount = datas[i].getDebitamount().sub(datas[i].getCreditamount());
				String pk_psndoc = assData.get(0).get(0).toString();
				
				if (mergebyPerson.containsKey(pk_psndoc)) {
					mergebyPerson.put(pk_psndoc, mergebyPerson.get(pk_psndoc).add(amount));
				}else{
					mergebyPerson.put(pk_psndoc, amount);
				}
			}
			
		}
		for(String pk_person : mergebyPerson.keySet()){
			if (mergebyPerson.get(pk_person).doubleValue()==0) continue;
			SQLParameter para = new SQLParameter();
			para.addParam(pk_person);
			para.addParam(mergebyPerson.get(pk_person).doubleValue());
			dao.executeUpdate(sql, para);
		}
	}

	private GlQueryVO getGLQueryVO() {
		GlQueryVO glQVO = new GlQueryVO();
		IGlorgbookAccessor glorg = new GlorgbookCache();
		GlorgbookVO[] books = glorg.getAllGlorgbookVOs();
		String[] pk_glorgbooks = new String[books.length];
		for(int i=0;i<books.length;i++) pk_glorgbooks[i]=books[i].getPrimaryKey();
		
		glQVO.setPk_glorgbook(pk_glorgbooks);//传递相应公司的账簿		
		glQVO.setAccsubjCode(new String[]{"122101"});
		
		String accYear = new UFDate().toString().substring(0, 4);
		String accMonth = new UFDate().toString().substring(5, 7);
		
		AssVO[] assvos = new AssVO[1];
		assvos[0] = new AssVO();
		assvos[0].setPk_Checktype("00010000000000000001");//辅助核算的name转化成pk
		assvos[0].setPk_Checkvalue(null);
		glQVO.setAssVos(assvos);
		
		glQVO.setPeriod(accMonth);//设置起始会计期间为当前期间		
		glQVO.setEndPeriod(accMonth);//结束期间
		glQVO.setYear(accYear);
				
		//排序字段
		glQVO.setGroupFields(new int[] { GLQueryKey.K_GLQRY_PK_GLORGBOOK, GLQueryKey.K_GLQRY_SUBJ, GLQueryKey.K_GLQRY_ASSID });
		
		glQVO.setQueryByPeriod(true);
		glQVO.getFormatVO().setShowHasBalanceButZeroOccur(true);
		glQVO.getFormatVO().setShowZeroAmountRec(false);
		glQVO.setShowZeroAmountRec(false);
		glQVO.setIncludeUnTallyed(true);//true 是否包含未记账凭证
		glQVO.setRealtimeVoucher(false);
		glQVO.setIncludeError(false);
		
		return glQVO;
	}
}