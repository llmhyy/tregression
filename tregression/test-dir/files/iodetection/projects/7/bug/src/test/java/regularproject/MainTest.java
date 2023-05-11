package regularproject;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		int input = main.increment(1);
		int result = main.method(input, 1);
		assertEquals(3, result);
	}
}
