package nc.impl.xjjc.voucher;

import java.util.Comparator;

import nc.itf.xjjc.voucher.BDInfo;
import nc.vo.gl.pubvoucher.DetailVO;
import nc.vo.glcom.ass.AssVO;

public class AirDetailComparator implements Comparator<Object>{
	public int compare(Object a0, Object a1) {
		DetailVO arg0 = (DetailVO)a0;
		DetailVO arg1 = (DetailVO)a1;
		
		String s0 = (arg0.getDirection()?"A":"B")+arg0.getAccsubjcode();
		for (AssVO ass : arg0.getAss()){
			if (ass.getPk_Checktype().equals(BDInfo.VEHICLE.getValue())){
				s0 += ass.getCheckvaluecode();
				break;
			}
		}
		
		String s1 = (arg1.getDirection()?"A":"B")+arg1.getAccsubjcode();
		for (AssVO ass : arg1.getAss()){
			if (ass.getPk_Checktype().equals(BDInfo.VEHICLE.getValue())){
				s1 += ass.getCheckvaluecode();
				break;
			}
		}
		
		return s0.compareTo(s1);
	}
}
