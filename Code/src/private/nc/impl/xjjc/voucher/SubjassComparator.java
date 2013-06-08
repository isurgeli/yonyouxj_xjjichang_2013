package nc.impl.xjjc.voucher;

import java.util.Comparator;

import nc.vo.bd.b02.SubjassVO;


public class SubjassComparator implements Comparator<SubjassVO>{
	public int compare(SubjassVO a0, SubjassVO a1) {
		return a0.getPk_bdinfo().compareTo(a1.getPk_bdinfo());
	}
}
