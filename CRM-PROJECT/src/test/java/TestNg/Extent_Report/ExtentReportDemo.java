package TestNg.Extent_Report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class ExtentReportDemo {

	WebDriver driver = new ChromeDriver();

	@BeforeMethod
	public void setup() {
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(15));
	}

	@Test
	public void generateReport() throws InterruptedException {
		driver.get("https://www.facebook.com/");

		// Generate unique time stamp to append to the report name
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyy_HH-mm-ss");
		String time = now.format(dtf);

		// generate the report
		ExtentSparkReporter spark = new ExtentSparkReporter("C:\\Users\\Abhishek Bisht\\git\\CRM-PROJECT\\CRM-PROJECT\\src\\test\\java\\Reports\\" + time + ".html");
		// Configure the report using the spark reporter
		spark.config().setDocumentTitle("Demo Automation Report");
		spark.config().setReportName("First Demo Report");
		spark.config().setTheme(Theme.DARK);

		// Create object of ExtentReports to attach the ExtentSparkReporter
		ExtentReports report = new ExtentReports(); 
		report.attachReporter(spark); 
		report.setSystemInfo("Operating System", "Windows 11");
		report.setSystemInfo("Environment", "Staging_Bed"); 
		report.setSystemInfo("Build Number", "0.1.0"); 

		// Create a test in the report. These will be used in @Listeners class
		ExtentTest test = report.createTest("Login Test"); 
		test.log(Status.INFO, "Info message"); 
		test.log(Status.PASS, "Pass message"); 
		test.log(Status.WARNING, "Warning message"); 
		test.log(Status.FAIL, "Fail message"); 
		test.log(Status.SKIP, "Skip message");

	
		TakesScreenshot tks = (TakesScreenshot) driver;
		String failSS = tks.getScreenshotAs(OutputType.BASE64); // get screenshot as base64 string
		test.addScreenCaptureFromBase64String(failSS, "Failed Test Screenshot"); // attach screenshot to the report

		report.flush(); // to write or update the report Very important step
		Thread.sleep(2000);
		driver.quit();
	}
}