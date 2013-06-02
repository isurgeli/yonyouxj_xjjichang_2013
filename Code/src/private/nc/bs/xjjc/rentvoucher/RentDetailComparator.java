package nc.bs.xjjc.rentvoucher;

import java.util.Comparator;

import nc.itf.xjjc.voucher.BDInfo;
import nc.vo.gl.pubvoucher.DetailVO;
import nc.vo.glcom.ass.AssVO;

public class RentDetailComparator implements Comparator<Object>{
	public int compare(Object a0, Object a1) {
		DetailVO arg0 = (DetailVO)a0;
		DetailVO arg1 = (DetailVO)a1;
		
		String s0 = (arg0.getDirection()?"A":"B");
		for (AssVO ass : arg0.getAss()){
			if (ass.getPk_Checktype().equals(BDInfo.CONTRACT.getValue())){
				s0 += ass.getCheckvaluecode();
				break;
			}
		}
		if(s0.length()==1) s0+="0000000000";
		s0+=arg0.getAccsubjcode();
		
		String s1 = (arg1.getDirection()?"A":"B");
		for (AssVO ass : arg1.getAss()){
			if (ass.getPk_Checktype().equals(BDInfo.CONTRACT.getValue())){
				s1 += ass.getCheckvaluecode();
				break;
			}
		}
		if(s1.length()==1) s1+="0000000000";
		s1+=arg1.getAccsubjcode();
		
		return s0.compareTo(s1);
	}
}