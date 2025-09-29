package TestNg.Assertion;

import org.testng.Assert;
import org.testng.annotations.Test;

public class WorkingWithHardAssert {
	@Test
	public void case1() {

	boolean a = true;
		boolean b = false;

		if (b) {
			System.out.println("it true");
		} else {
			System.out.println("its false");
		}

		Assert.assertTrue(a);
		Assert.assertFalse(a);

		String x = "abc";
		String y = "abc";
		String z = "xyz";

		Assert.assertEquals(x, y);
		Assert.assertEquals(y, z);

		Assert.assertNotEquals(x, z);

		Assert.assertEquals("x","y");

		Object obj1 = null;
		Object obj2 = new Object();
		Assert.assertNull(obj2);
		Assert.assertNotNull(obj1);
		Assert.assertNull(new Object());

	}
}