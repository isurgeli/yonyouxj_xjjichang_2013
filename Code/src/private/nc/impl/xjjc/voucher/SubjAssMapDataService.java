package nc.impl.xjjc.voucher;

import java.util.ArrayList;
import java.util.Vector;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.itf.xjjc.voucher.ISubjAssMapDataService;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.pub.BusinessException;
import nc.vo.xjjc.freevaluemap.AssValueMapVO;
import nc.vo.xjjc.freevaluemap.UsedFreeValueVO;

public class SubjAssMapDataService implements ISubjAssMapDataService {

	private BaseDAO getDao() {
		BaseDAO dao = new BaseDAO();
		return dao;
	}
	
	public UsedFreeValueVO[] queryUesdAssSubjByCorpPk(String pk_corp) throws BusinessException {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<UsedFreeValueVO> data = (ArrayList<UsedFreeValueVO>)getDao().
					retrieveByClause(UsedFreeValueVO.class, "pk_corp='"+pk_corp+"' or pk_corp='0001'");
			return data.toArray(new UsedFreeValueVO[0]);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public AssValueMapVO[] queryAllSubjAssMap(String strWhere) throws BusinessException  {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<AssValueMapVO> data = (ArrayList<AssValueMapVO>)getDao().
					retrieveByClause(AssValueMapVO.class, strWhere);
			return data.toArray(new AssValueMapVO[0]);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void deleteMap(AssValueMapVO map_vo) throws BusinessException  {
		try {
			getDao().deleteVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
	
	public void insertMap(AssValueMapVO map_vo) throws BusinessException  {
		try {
			checkUniqueCode(map_vo);
			getDao().insertVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}

	public void updateMap(AssValueMapVO map_vo) throws BusinessException  {
		try {
			checkUniqueCode(map_vo);
			getDao().updateVO(map_vo);
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
	
	private void checkUniqueCode(AssValueMapVO map_vo) throws BusinessException  {
		try {
			Vector count = (Vector)getDao().executeQuery("select count(1) from XJJC_BD_FREEVALUEMAP where vothercode='"+map_vo.getVothercode()
					+"' and pk_freevaluemap<>'"+map_vo.getPrimaryKey()+"' and pk_corp='"+map_vo.getPk_corp()+"'", new VectorProcessor());
			if (!((Vector)count.get(0)).get(0).equals(0))
				throw new BusinessException("外系统编码：["+map_vo.getVothercode()+"]，已被使用。");
		} catch (DAOException e) {
			throw new BusinessException(e);
		}
	}
}
