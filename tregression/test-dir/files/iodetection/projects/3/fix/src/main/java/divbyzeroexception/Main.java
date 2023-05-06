package divbyzeroexception;

public class Main {
	public int method(int a) {
		return method1(a);
	}
	
	public int method1(int a) {
		return 100 / a;
	}
}
