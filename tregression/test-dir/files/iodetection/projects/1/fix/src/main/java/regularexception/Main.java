package regularexception;

public class Main {
	public int method(int a) {
		if (a == 1) {
			throw new RuntimeException("Exception!");
		}
		return a;
	}
}
