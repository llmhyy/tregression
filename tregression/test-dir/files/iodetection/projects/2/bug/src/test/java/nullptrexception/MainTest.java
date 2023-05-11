package nullptrexception;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import nullptrexception.Main;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		List<Integer> resultLs = main.method(0);
		int actual = resultLs.get(0);
		assertEquals(0, actual);
	}
}
