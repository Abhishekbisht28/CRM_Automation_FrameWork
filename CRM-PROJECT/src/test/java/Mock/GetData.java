package Mock;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GetData{
	public static void main(String[] args) throws IOException {
		FileInputStream fis = new FileInputStream("C:\\Users\\Abhishek Bisht\\git\\CRM-PROJECT\\CRM-PROJECT\\src\\test\\resources\\CommonData.properties");
		Properties pObj = new Properties();
		pObj.load(fis);
		
		String  Browser = pObj.getProperty("bro");
		System.out.println(Browser);
		}
		
}