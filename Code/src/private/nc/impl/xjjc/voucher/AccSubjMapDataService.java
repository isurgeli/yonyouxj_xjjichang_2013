package nc.impl.xjjc.voucher;

import java.util.ArrayList;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.itf.xjjc.voucher.IAccSubjMapDataService;
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
			getDao().insertVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void updateMap(SubjMapVO map_vo) throws BusinessException {
		try {
			getDao().updateVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

}
