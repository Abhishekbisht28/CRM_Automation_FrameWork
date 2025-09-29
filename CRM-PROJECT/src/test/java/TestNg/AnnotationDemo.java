package TestNg;

import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

public class AnnotationDemo {
	
	@BeforeSuite
	public void dbConnection() {
		Reporter.log("Connecting the Database + Report Configuration");
	}
	
	@BeforeTest
	public void precondtion() {
		Reporter.log("PreCondition");
	}
	
	@BeforeClass
	public void openBro() {
		Reporter.log("Open Browser",true);
	}
	
	@BeforeMethod
	public void login() {
		Reporter.log("Login",true);
	}
	
	@AfterMethod
	public void logOut() {
		Reporter.log("Logout",true);
	}
	
	@AfterClass
	public void closeBro() {
		Reporter.log("Close Browser",true);
	}
	
	@AfterTest
	public void postcondtion() {
		Reporter.log("PostCondition");
	}
	
	@AfterSuite
	public void dbClose() {
		Reporter.log("Database connectivity close + Report Backup");
	}
	

}