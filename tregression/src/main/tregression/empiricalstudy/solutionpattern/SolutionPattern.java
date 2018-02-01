package tregression.empiricalstudy.solutionpattern;

public class SolutionPattern {
	public final static int MISSING_ASSIGNMENT = 0;
	public final static int INCORRECT_CONDITION = 1;
	public final static int EXTRA_NESTED_IF_BLOCK = 2;
	public final static int MISSING_IF_BLOCK = 3;
	public final static int MISSING_IF_RETURN = 4;
	public final static int MISSING_IF_THROW = 5;
	
	private int type;
	
	public SolutionPattern(int type) {
		super();
		this.type = type;
	}
	
	public String toString(){
		return getTypeName();
	}

	public String getTypeName(){
		if(getType()==MISSING_ASSIGNMENT){
			return "missing assignment";
		}
		else if(getType()==INCORRECT_CONDITION){
			return "incorrect condition";
		}
		else if(getType()==EXTRA_NESTED_IF_BLOCK){
			return "extra nested if block";
		}
		else if(getType()==MISSING_IF_BLOCK){
			return "missing if block";
		}
		else if(getType()==MISSING_IF_RETURN){
			return "missing if return";
		}
		else if(getType()==MISSING_IF_THROW){
			return "missing if throw";
		}
		
		return "unknown";
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
