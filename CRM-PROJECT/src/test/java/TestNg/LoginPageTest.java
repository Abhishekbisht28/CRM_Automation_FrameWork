package TestNg;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPageTest {

    WebDriver driver;

    public LoginPageTest(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this); 
    }

    @FindBy(id = "user-name")
    WebElement un;

    @FindBy(id = "password")
    WebElement pwd;

    @FindBy(id = "login-button")
    WebElement loginBtn;

    public void login(String username, String password) {
        un.sendKeys(username);
        pwd.sendKeys(password);
        loginBtn.click();
    }
}
