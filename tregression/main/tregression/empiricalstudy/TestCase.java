package tregression.empiricalstudy;

import sav.common.core.SavRtException;
import sav.common.core.utils.ClassUtils;

public class TestCase {
	public String testClass;
	public String testMethod;

	public TestCase(String testClass, String testMethod) {
		super();
		this.testClass = testClass;
		this.testMethod = testMethod;
	}
	
	public TestCase(String tc) {
		String[] tcFrags = tc.split("#");
		if (tcFrags.length == 2) {
			testClass = tcFrags[0];
			testMethod = tcFrags[1];
		} else {
			throw new SavRtException("Invalid testcase format: " + tc);
		}
	} 
	
	public String getClassSimpleName() {
		return ClassUtils.getSimpleName(testClass);
	}
	
	@Override
	public String toString(){
		return getName();
	}

	public String getName() {
		return testClass + "#" + testMethod;
	}
}
