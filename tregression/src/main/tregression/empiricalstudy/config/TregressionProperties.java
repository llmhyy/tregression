package tregression.empiricalstudy.config;

import java.io.File;
import java.util.Properties;

public class TregressionProperties extends Properties {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1365654007157341215L;

	@Override
	public String getProperty(String key) {
		String raw = super.getProperty(key);
		String result = raw.replace("/", File.separator);
		return result;
	}
}
