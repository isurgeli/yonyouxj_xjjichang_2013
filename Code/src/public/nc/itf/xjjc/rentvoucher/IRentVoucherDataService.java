package nc.itf.xjjc.rentvoucher;

import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;

public interface IRentVoucherDataService {
	public boolean genRentVOucher(String sAccMonth, String eAccMonth, String biztype, String pk_voucherType, String explain
			, String pk_user, UFDate date) throws BusinessException;
}
