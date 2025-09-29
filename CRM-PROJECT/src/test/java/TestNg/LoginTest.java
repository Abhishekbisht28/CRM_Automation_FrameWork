package TestNg;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import Generic_Utility.WebDriverUtility;

public class LoginTest {

    WebDriver driver;

    @Test(dataProvider = "getData")
    public void loginTest(String username, String password) {
    	driver = WebDriverUtility.startDriver("https://www.saucedemo.com/v1/");
        LoginPageTest loginPage = new LoginPageTest(driver);
        loginPage.login(username, password);
        driver.quit();
    }

    @DataProvider
    public Object[][] getData() {
        Object[][] cred = new Object[5][2]; 

        cred[0][0] = "standard_user";
        cred[0][1] = "secret_sauce";

        cred[1][0] = "locked_out_user";
        cred[1][1] = "secret_sauce";

        cred[2][0] = "problem_user";
        cred[2][1] = "secret_sauce";

        cred[3][0] = "performance_glitch_user";
        cred[3][1] = "secret_sauce";

        cred[4][0] = "invalid_user";
        cred[4][1] = "invalid_pass";

        return cred;
    }
    }

