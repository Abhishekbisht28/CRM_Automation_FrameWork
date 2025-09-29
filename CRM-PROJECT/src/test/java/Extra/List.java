package Extra;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Test;


public class List{
	@Test(retryAnalyzer = Extra.Listener_imp_Retry.class)

	public void case1() {
		WebDriver driver = new ChromeDriver();
		driver.get("https://www.amazon.com");
		WebElement verify = driver.findElement(By.cssSelector("aria-label=\"Amazon\""));
		Assert.assertTrue(verify.isDisplayed());
	}

}
