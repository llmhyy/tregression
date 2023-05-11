package regularproject;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		int[] arr = main.method();
		assertEquals(0, arr[0]);
	}
}
