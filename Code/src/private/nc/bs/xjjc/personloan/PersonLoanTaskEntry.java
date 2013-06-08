package nc.bs.xjjc.personloan;

import nc.bs.pub.taskcenter.BgWorkingContext;
import nc.bs.pub.taskcenter.IBackgroundWorkPlugin;
import nc.vo.logging.Debug;
import nc.vo.pub.BusinessException;

public class PersonLoanTaskEntry implements IBackgroundWorkPlugin{

	public String executeTask(BgWorkingContext bgwc) {
		XjjcPersonLoanImpl loanImpl = new XjjcPersonLoanImpl();
		try {
			loanImpl.refreshPersonLoan();
		} catch (BusinessException e) {
			Debug.error(e.getMessage(),e);
			return e.getMessage();
		}
		
		return "OK";
	}
}
