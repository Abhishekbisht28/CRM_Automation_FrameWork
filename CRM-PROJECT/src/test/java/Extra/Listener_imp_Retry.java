package Extra;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Listener_imp_Retry implements IRetryAnalyzer{
	int count = 0;
	int initcount = 7;
	@Override
	public boolean retry(ITestResult result) {
		if(count<initcount) {
			count++;
			return true;
		}else {
		return false;
		}
	}
	
}