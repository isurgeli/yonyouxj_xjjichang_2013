package nc.itf.xjjc.voucher;

import nc.vo.pub.BusinessException;
import nc.vo.xjjc.freevaluemap.AssValueMapVO;
import nc.vo.xjjc.freevaluemap.UsedFreeValueVO;

public interface ISubjAssMapDataService {
	UsedFreeValueVO[] queryUesdAssSubjByCorpPk(String pk_corp) throws BusinessException;

	AssValueMapVO[] queryAllSubjAssMap(String strWhere) throws BusinessException;

	void deleteMap(AssValueMapVO map_vo) throws BusinessException;

	void insertMap(AssValueMapVO map_vo) throws BusinessException;

	void updateMap(AssValueMapVO map_vo) throws BusinessException;
}
