package tregression.separatesnapshots.diff;

import java.util.ArrayList;
import java.util.List;

public class DiffChunk {
	private int startLineInSource;
	private int chunkLengthInSource;
	
	private int startLineInTarget;
	private int chunkLengthInTarget;
	
	private List<LineChange> changeList = new ArrayList<>();

	public DiffChunk(int startLineInSource, int chunkLengthInSource, int startLineInTarget, int chunkLengthInTarget) {
		super();
		this.startLineInSource = startLineInSource;
		this.chunkLengthInSource = chunkLengthInSource;
		this.startLineInTarget = startLineInTarget;
		this.chunkLengthInTarget = chunkLengthInTarget;
	}

	public int getStartLineInSource() {
		return startLineInSource;
	}

	public void setStartLineInSource(int startLineInSource) {
		this.startLineInSource = startLineInSource;
	}

	public int getChunkLengthInSource() {
		return chunkLengthInSource;
	}

	public void setChunkLengthInSource(int chunkLengthInSource) {
		this.chunkLengthInSource = chunkLengthInSource;
	}

	public int getStartLineInTarget() {
		return startLineInTarget;
	}

	public void setStartLineInTarget(int startLineInTarget) {
		this.startLineInTarget = startLineInTarget;
	}

	public int getChunkLengthInTarget() {
		return chunkLengthInTarget;
	}

	public void setChunkLengthInTarget(int chunkLengthInTarget) {
		this.chunkLengthInTarget = chunkLengthInTarget;
	}

	public List<LineChange> getChangeList() {
		return changeList;
	}

	public void setChangeList(List<LineChange> changeList) {
		this.changeList = changeList;
	}

	public int getLineNumberInSource(LineChange line) {
		int count = this.getStartLineInSource();
		for(int i=0; i<changeList.size(); i++){
			LineChange lineChange = changeList.get(i);
			if(lineChange.getIndex()==line.getIndex()){
				return count;
			}
			
			if(lineChange.getType()==LineChange.UNCHANGE){
				count++;
			}
			else if(lineChange.getType()==LineChange.REMOVE){
				count++;
			}
		}
		
		return -1;
	}
	
	public int getLineNumberInTarget(LineChange line) {
		int count = this.getStartLineInTarget();
		for(int i=0; i<changeList.size(); i++){
			LineChange lineChange = changeList.get(i);
			if(lineChange.getIndex()==line.getIndex()){
				return count;
			}
			
			if(lineChange.getType()==LineChange.UNCHANGE){
				count++;
			}
			else if(lineChange.getType()==LineChange.ADD){
				count++;
			}
		}
		
		return -1;
	}
	
	
}
