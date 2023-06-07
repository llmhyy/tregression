package regularproject;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		char result = main.method("test", 0);
                assertEquals('t', result);
	}
}
