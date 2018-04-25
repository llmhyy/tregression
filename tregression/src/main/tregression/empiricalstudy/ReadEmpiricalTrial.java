package tregression.empiricalstudy;

public class ReadEmpiricalTrial {
	private String project;
	private String bugID;
	private int rootcauseNode;
	private String debugType;
	private String deadEndType;
	private String exception;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bugID == null) ? 0 : bugID.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadEmpiricalTrial other = (ReadEmpiricalTrial) obj;
		if (bugID == null) {
			if (other.bugID != null)
				return false;
		} else if (!bugID.equals(other.bugID))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		return true;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getBugID() {
		return bugID;
	}

	public void setBugID(String bugID) {
		this.bugID = bugID;
	}

	public int getRootcauseNode() {
		return rootcauseNode;
	}

	public void setRootcauseNode(int rootcauseNode) {
		this.rootcauseNode = rootcauseNode;
	}

	public String getDebugType() {
		return debugType;
	}

	public void setDebugType(String debugType) {
		this.debugType = debugType;
	}

	public String getDeadEndType() {
		return deadEndType;
	}

	public void setDeadEndType(String deadEndType) {
		this.deadEndType = deadEndType;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

}
