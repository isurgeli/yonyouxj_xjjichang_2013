package nc.impl.xjjc.voucher;

import java.util.ArrayList;
import java.util.Vector;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.itf.xjjc.voucher.IAccSubjMapDataService;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.pub.BusinessException;
import nc.vo.xjjc.accsubjmap.BizTypeVO;
import nc.vo.xjjc.accsubjmap.SubjMapVO;

public class AccSubjMapDataService implements IAccSubjMapDataService {

	private BaseDAO getDao() {
		BaseDAO dao = new BaseDAO();
		return dao;
	}
	
	public BizTypeVO[] queryBizType() throws BusinessException {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<BizTypeVO> data = (ArrayList<BizTypeVO>)getDao().
					retrieveAll(BizTypeVO.class);
			return data.toArray(new BizTypeVO[0]);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public SubjMapVO[] queryAllSubjMap(String strWhere) throws BusinessException {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<SubjMapVO> data = (ArrayList<SubjMapVO>)getDao().
					retrieveByClause(SubjMapVO.class, strWhere);
			return data.toArray(new SubjMapVO[0]);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void deleteMap(SubjMapVO map_vo) throws BusinessException {
		try {
			getDao().deleteVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void insertMap(SubjMapVO map_vo) throws BusinessException {
		try {
			checkUniqueCode(map_vo);
			getDao().insertVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void updateMap(SubjMapVO map_vo) throws BusinessException {
		try {
			checkUniqueCode(map_vo);
			getDao().updateVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
	
	private void checkUniqueCode(SubjMapVO map_vo) throws BusinessException  {
		try {
			
			StringBuffer sql = new StringBuffer();
			sql.append("select count(1) from xjjc_bd_subjmap where vothercode='"+map_vo.getVothercode()+"' ");
			sql.append(map_vo.getPk_subjmap()!=null?" and pk_subjmap<>'"+map_vo.getPk_subjmap()+"' ":"");
			sql.append(" and pk_corp='"+map_vo.getPk_corp()+"' ");
			sql.append(" and votherbiz"+(map_vo.getVotherbiz()!=null?"='"+map_vo.getVotherbiz()+"'":" is null"));
			@SuppressWarnings("unchecked")
			Vector<Vector<Object>> count = (Vector<Vector<Object>>)getDao().executeQuery(sql.toString(), new VectorProcessor());
			if (!count.get(0).get(0).equals(0))
				throw new BusinessException((map_vo.getVotherbiz()!=null?"机场：["+map_vo.getVotherbiz()+"]下，":"")
						+"外系统编码：["+map_vo.getVothercode()+"]，已被使用。");
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
}
