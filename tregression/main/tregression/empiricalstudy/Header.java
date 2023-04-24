package tregression.empiricalstudy;

public enum Header {
	PROJECT ("project"), 
	BUG_ID ("bug_ID"),
	MUTATION_TYPE ("mutation type"),
	TESTCASE("test case"),
	EXECTION_TIME("excution time"),
	FOUND_CAUSE ("found cause"),
	GENERAL_CAUSE ("general cause"),
	
	BUGGY_TRACE_LENGTH ("buggy trace length"),
	CORRECT_TRACE_LENGTH ("correct trace length"),
	TRACE_COLLECTION_TIME ("trace collection time"),
	TRACE_MATCH_TIME ("trace match time"),
	SIMULATION_TIME ("simulation time"),
	EXPLANATION_SIZE ("explanation size"),
	REGRESSION_EXPLANATION ("regression explanation nodes"),
	CORRECT_EXPLANATION ("correct explanation nodes"),
	
	TYPE ("type"),
	OVERSKIP ("overskip steps"),
	CHECK_LIST ("checklist"),
	
	EXCEPTION ("exception"),
	MULTI_THREAD ("multi thread"),
	
	BREAK_TO_BUG ("break to bug"),

	DEADEND_TYPE ("type"),
	DEADEND_OCCUR ("occur"),
	DEADEND ("dead end"),
	DEADEND_BREAK ("break"),
	DEADEND_SOLUTION ("solution"),
	VAR_TYPE ("var type");
	
	private String title;
	private Header(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public int getIndex() {
		return ordinal();
	}
}
