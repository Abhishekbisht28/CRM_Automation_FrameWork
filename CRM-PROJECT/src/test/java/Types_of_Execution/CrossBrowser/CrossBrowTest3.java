package Types_of_Execution.CrossBrowser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class CrossBrowTest3 {
	
	@Parameters("bro")
	@Test
	public void case1(String browser) throws InterruptedException {
		WebDriver driver=null;
		
		if(browser.equals("chrome")) {
			driver=new ChromeDriver();
		}else if(browser.equals("edge")) {
			driver=new EdgeDriver();
		}else if(browser.equals("firefox")) {
			driver=new ChromeDriver();
		}else {
			driver=new ChromeDriver();
		}
		
		driver.get("https://x.com/");
		Thread.sleep(4000);
		driver.quit();
	}

}