package regularproject;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		int[][] arr = new int[][] {{1, 2}, {3, 4}};
		Main main = new Main();
		int result = main.method(arr);
		assertEquals(10, result);
	}
}
