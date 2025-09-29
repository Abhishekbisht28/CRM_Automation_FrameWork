package Generic_Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WebDriverUtility {

     WebDriver driver;
     WebDriverWait wait;
    Actions act;
     JavascriptExecutor jse;
    // Constructor
    public WebDriverUtility(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.act = new Actions(driver);
    }
    public static WebDriver startDriver(String url) {
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get(url);
        return driver;
    }
    // ================== File Utilities ==================
    public String getDataFromPropertiesFile(String key) throws IOException {
        FileInputStream fis = new FileInputStream("C:\\Users\\Abhishek Bisht\\git\\CRM-PROJECT\\CRM-PROJECT\\src\\test\\resources\\CommonData.properties");
        Properties pObj = new Properties();
        pObj.load(fis);
        return pObj.getProperty(key);
    }

    public String getDataFromExcelFile(String sheetName, int rowNum, int cellNum) throws IOException {
        FileInputStream fis1 = new FileInputStream("C:\\Users\\Abhishek Bisht\\git\\CRM-PROJECT\\CRM-PROJECT\\src\\test\\resources\\testScriptData.xlsx");
        Workbook book = WorkbookFactory.create(fis1);
        Sheet sheet = book.getSheet(sheetName);
        return sheet.getRow(rowNum).getCell(cellNum).getStringCellValue();
    }

    public int getNumDataFromExcelFile(String sheetName, int rowNum, int cellNum) throws IOException {
        FileInputStream fis1 = new FileInputStream("C:\\Users\\Abhishek Bisht\\git\\CRM-PROJECT\\CRM-PROJECT\\src\\test\\resources\\testScriptData.xlsx");
        Workbook book = WorkbookFactory.create(fis1);
        Sheet sheet = book.getSheet(sheetName);
        return (int) sheet.getRow(rowNum).getCell(cellNum).getNumericCellValue();
    }

    // ================== Synchronization ==================
    public void setImplicitWait(int timeInSeconds) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeInSeconds));
    }

    public WebElement waitForElementToBeVisible(By locator, int timeInSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeInSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForElementToBeClickable(By locator, int timeInSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeInSeconds))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean waitForTitleContains(String title, int timeInSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeInSeconds))
                .until(ExpectedConditions.titleContains(title));
    }

    public WebElement fluentWait(final By locator, int timeoutInSeconds, int pollingInMillis) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                .pollingEvery(Duration.ofMillis(pollingInMillis))
                .ignoring(NoSuchElementException.class);

        return wait.until(driver -> driver.findElement(locator));
    }

    // ================== Select Class ==================
    public void selectByIndex(WebElement element, int index) {
        new Select(element).selectByIndex(index);
    }

    public void selectByVisibleText(WebElement element, String text) {
        new Select(element).selectByVisibleText(text);
    }

    public void selectByValue(WebElement element, String value) {
        new Select(element).selectByValue(value);
    }

    public void deSelectByIndex(WebElement element, int index) {
        new Select(element).deselectByIndex(index);
    }

    public void deSelectByVisibleText(WebElement element, String text) {
        new Select(element).deselectByVisibleText(text);
    }

    public void deSelectByValue(WebElement element, String value) {
        new Select(element).deselectByValue(value);
    }

    public void deSelectAll(WebElement element) {
        new Select(element).deselectAll();
    }

    public WebElement getFirstSelectedOption(WebElement element) {
        return new Select(element).getFirstSelectedOption();
    }

    public java.util.List<WebElement> getAllSelectedOptions(WebElement element) {
        return new Select(element).getAllSelectedOptions();
    }

    public java.util.List<WebElement> getOptions(WebElement element) {
        return new Select(element).getOptions();
    }

    public boolean isMultiple(WebElement element) {
        return new Select(element).isMultiple();
    }
    // ================== Action Class ==================
    
    // ================== Mouse Action ==================

    
    // Hover Method 
 	public void Hover(WebElement Element) {
 		act.moveToElement(Element).build().perform();
 	}
 	// Right Click
 	public void RightClick(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}// Drag and Drop
 	public void DragDrop(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}// Double click
 	public void DoubleClick(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}// Click and Hold
 	public void ClickHold(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}// SCroll to Element 
 	public void ScrollToElement(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}// Scroll By Amount
 	public void ScrollByAmount(WebElement Element) {
 		act.contextClick(Element).build().perform();
 	}
    // ================== KeyBoard Action ==================
 	public void SendKeys(String Locator) {
 		act.sendKeys(Locator);
 	}// KeyUp
 	public void KeyUp(String name) {
 		act.keyUp(name);
 	}// KeyDown
 	public void KeyDown(String name) {
 		act.keyDown(name);
 	}
 	
 // ---------------- PopUp Handling ---------------- //
 	// Accept alert
 	public void acceptAlert(WebDriver driver) {
 		Alert alt = driver.switchTo().alert();
 		alt.accept();
 	}

 	// Dismiss alert
 	public void dismissAlert(WebDriver driver) {
 		Alert alt = driver.switchTo().alert();
 		alt.dismiss();
 	}

 	// Get text from alert
 	public String getAlertText(WebDriver driver) {
 		Alert alt = driver.switchTo().alert();
 		return alt.getText();
 	}

 	// ---------------- Frame Handling ---------------- //
 	// Switch by index
 	public void switchToFrame(WebDriver driver, int index) {
 		driver.switchTo().frame(index);
 	}

 	// Switch by name/id
 	public void switchToFrame(WebDriver driver, String nameOrId) {
 		driver.switchTo().frame(nameOrId);
 	}

 	// Switch by WebElement
 	public void switchToFrame(WebDriver driver, WebElement frameElement) {
 		driver.switchTo().frame(frameElement);
 	}

 	// Switch back to parent frame
 	public void switchToParentFrame(WebDriver driver) {
 		driver.switchTo().parentFrame();
 	}

 	// Switch back to default page
 	public void switchToDefaultContent(WebDriver driver) {
 		driver.switchTo().defaultContent();
 	}
 // ---------------- Manage Methods ---------------- //
 	public void maximizeWindow(WebDriver driver) {
 		driver.manage().window().maximize();
 	}

 	public void minimizeWindow(WebDriver driver) {
 		driver.manage().window().minimize();
 	}

 	public void fullscreenWindow(WebDriver driver) {
 		driver.manage().window().fullscreen();
 	}

 	public void deleteAllCookies(WebDriver driver) {
 		driver.manage().deleteAllCookies();
 	}

 	// ---------------- Navigation ---------------- //
 	public void navigateTo(WebDriver driver, String url) {
 		driver.navigate().to(url);
 	}

 	public void navigateBack(WebDriver driver) {
 		driver.navigate().back();
 	}

 	public void navigateForward(WebDriver driver) {
 		driver.navigate().forward();
 	}

 	public void navigateRefresh(WebDriver driver) {
 		driver.navigate().refresh();
 	}
 	
 	// ---------------- JavaScript Executor ---------------- //
	// Scroll down by pixels
	public void scrollDown(WebDriver driver, int pixels) {
		((JavascriptExecutor) driver).executeScript("window.scrollBy(0," + pixels + ")");
	}

	// Scroll up by pixels
	public void scrollUp(WebDriver driver, int pixels) {
		((JavascriptExecutor) driver).executeScript("window.scrollBy(0,-" + pixels + ")");
	}

	// Scroll to element
	public void scrollToElement(WebDriver driver, WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
	}

	// Click using JS
	public void clickElementByJS(WebDriver driver, WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
	}

	// Send keys using JS
	public void sendKeysByJS(WebDriver driver, WebElement element, String value) {
		((JavascriptExecutor) driver).executeScript("arguments[0].value='" + value + "';", element);
	}

	// Get page title using JS
	public String getTitleByJS(WebDriver driver) {
		return (String) ((JavascriptExecutor) driver).executeScript("return document.title;");
	}

	// ---------------- Screenshot Methods ---------------- //
	// Take screenshot and save in given path
	public String takeWebScreenshot(WebDriver driver, String screenshotName) throws IOException {
		TakesScreenshot ts = (TakesScreenshot) driver;
		File src = ts.getScreenshotAs(OutputType.FILE);
		String path = System.getProperty("path") + "/Screenshots/" + screenshotName + ".png";
		File dest = new File(path);
		FileUtils.copyFile(src, dest);
		return path; // returns screenshot path
	}

	// Take screenshot of element
	public String takeElementScreenshot(WebElement element, String screenshotName) throws IOException {
		File src = element.getScreenshotAs(OutputType.FILE);
		String path = System.getProperty("user.dir") + "/Screenshots/" + screenshotName + ".png";
		File dest = new File(path);
		FileUtils.copyFile(src, dest);
		return path;
	}
}

 






 	
 


