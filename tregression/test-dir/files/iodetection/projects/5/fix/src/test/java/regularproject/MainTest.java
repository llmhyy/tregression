package regularproject;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		int firstValue = 1;
		int result = main.method(firstValue, 1);
		assertEquals(2, result);
	}
}
