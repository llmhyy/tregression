package outofboundsexception;

public class Main {
	public int method(int a) {
		int[] arr = new int[] {0, 1, 2};
		int result = 0;
		for (int i = 0; i <= a; i++) {
			result += arr[i];
		}
		return result;
	}
}
