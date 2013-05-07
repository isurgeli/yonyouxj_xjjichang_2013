package nc.bs.xjjc.personloan;

import nc.bs.pub.taskcenter.BgWorkingContext;
import nc.bs.pub.taskcenter.IBackgroundWorkPlugin;
import nc.vo.pub.BusinessException;

public class PersonLoanTaskEntry implements IBackgroundWorkPlugin{

	@Override
	public String executeTask(BgWorkingContext bgwc) {
		XjjcPersonLoanImpl loanImpl = new XjjcPersonLoanImpl();
		try {
			loanImpl.refreshPersonLoan();
		} catch (BusinessException e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
		return "OK";
	}
}
