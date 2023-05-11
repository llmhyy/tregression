package outofboundsexception;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		int result = main.method(3);
		assertEquals(3, result);
	}
}
