package tregression.empiricalstudy.solutionpattern;

public class SolutionPattern {
	public final static int MISSING_ASSIGNMENT = 0;
	public final static int INCORRECT_CONDITION = 1;
	public final static int EXTRA_NESTED_IF_BLOCK = 2;
	public final static int MISSING_IF_BLOCK = 3;
	public final static int MISSING_IF_RETURN = 4;
	public final static int MISSING_IF_THROW = 5;
	public final static int INCORRECT_ASSIGNMENT = 6;
	public final static int INVOKE_DIFFERENT_METHOD = 7;
	public final static int MISS_EVALUATED_CONDITION = 8;
	public final static int INVOKE_NEW_METHOD = 9;
	
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
		else if(getType()==INCORRECT_ASSIGNMENT){
			return "incorrect assignment";
		}
		else if(getType()==INVOKE_DIFFERENT_METHOD){
			return "invoke different method";
		}
		else if(getType()==MISS_EVALUATED_CONDITION){
			return "miss-evaluated condition";
		}
		else if(getType()==INVOKE_NEW_METHOD){
			return "invoke new method";
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
