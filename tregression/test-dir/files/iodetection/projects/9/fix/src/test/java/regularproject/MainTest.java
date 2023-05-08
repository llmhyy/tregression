package regularproject;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		List<Integer> ls = main.method();
		int result = ls.get(0);
		assertEquals(0, result);
	}
}
