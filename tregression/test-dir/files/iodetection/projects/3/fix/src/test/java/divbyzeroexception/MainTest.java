package divbyzeroexception;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		int result = main.method(1);
		assertEquals(100, result);
	}
}
