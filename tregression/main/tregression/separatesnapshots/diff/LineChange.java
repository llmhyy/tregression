package tregression.separatesnapshots.diff;

public class LineChange {
	
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int UNCHANGE = 2;
	
	private int index;
	private int type;
	
	private String lineContent;

	public LineChange(int index, int type, String lineContent) {
		super();
		this.index = index;
		this.type = type;
		this.lineContent = lineContent;
	}
	
	public String toString() {
		return this.lineContent;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getLineContent() {
		return lineContent;
	}

	public void setLineContent(String lineContent) {
		this.lineContent = lineContent;
	}
	
	
}
