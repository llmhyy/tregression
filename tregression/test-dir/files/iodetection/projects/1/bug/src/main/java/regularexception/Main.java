package regularexception;

public class Main {
	public int method(int a) {
		if (a == 0) {
			throw new RuntimeException("Exception!");
		}
		return a;
	}
}
