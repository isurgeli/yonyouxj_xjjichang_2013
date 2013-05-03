package nc.itf.xjjc.voucher;

import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDouble;
import nc.vo.xjjc.voucher.AirCorpVO;
import nc.vo.xjjc.voucher.FakeVoucherVO;

public interface IAirIncomeVoucherDataService {
	AirCorpVO[] getAirCorpNeedGenVoucher(String sAccMonth, String eAccMonth, String airPort);
	FakeVoucherVO[] genVoucherForAirCorp(String sAccMonth, String eAccMonth, String airPort, String airPortname, String pk_voucherType, 
			String explain, String[] airlines, boolean useDollar, UFDouble raito, String pk_user) throws BusinessException;
}
