package regularproject;

import static org.junit.Assert.*;

import org.junit.Test;

public class MainTest {
	@Test
	public void test() {
		Main main = new Main();
		MainUtils utils = new MainUtils();
		String strInput = MainUtils.INPUT;
		int intInput = utils.getInput();
		char result = main.method(strInput, intInput);
		assertEquals('n', result);
	}
}
