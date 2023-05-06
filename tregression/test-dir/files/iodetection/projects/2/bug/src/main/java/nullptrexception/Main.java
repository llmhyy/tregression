package nullptrexception;

import java.util.ArrayList;
import java.util.List;

public class Main {
	public List<Integer> method(int a) {
		List<Integer> ls = new ArrayList<Integer>();
		ls.add(a);
		return null;
	}
}
