package regularproject;

public class Main {
	public int method(int[][] arr) {
		int result = 0;
		for (int i = 0; i < arr.length; i++) {
			int[] curr = arr[i];
			for (int j = 0; j < curr.length; j++) {
				result += curr[j];
			}
		}
		return result;
	}
}
