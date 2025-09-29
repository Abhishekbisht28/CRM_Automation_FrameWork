package Generic_Utility;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

public class PracticeUtility{
	WebDriver driver;
	Actions act;
	//  Constructor 
	PracticeUtility(WebDriver driver){
		this.driver= new ChromeDriver();
		this.act = new Actions(driver);
	}
	// Hover Method 
	public void Hover(WebElement Element) {
		act.moveToElement(Element).build().perform();
	}
	public void RightClick(WebElement Element) {
		act.contextClick(Element).build().perform();
	}
}