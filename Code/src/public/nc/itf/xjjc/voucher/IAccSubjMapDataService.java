package nc.itf.xjjc.voucher;

import nc.vo.pub.BusinessException;
import nc.vo.xjjc.accsubjmap.BizTypeVO;
import nc.vo.xjjc.accsubjmap.SubjMapVO;

public interface IAccSubjMapDataService {
	BizTypeVO[] queryBizType() throws BusinessException;

	SubjMapVO[] queryAllSubjMap(String strWhere) throws BusinessException;

	void deleteMap(SubjMapVO map_vo) throws BusinessException;

	void insertMap(SubjMapVO map_vo) throws BusinessException;

	void updateMap(SubjMapVO map_vo) throws BusinessException;
}
