package tregression.empiricalstudy.training;

import java.util.ArrayList;
import java.util.List;

public class DED {
	DeadEndData trueData;
	List<DeadEndData> falseDatas;
	
	public DED(DeadEndData trueData, List<DeadEndData> falseDatas) {
		super();
		this.trueData = trueData;
		this.falseDatas = falseDatas;
	}

	public DeadEndData getTrueData() {
		return trueData;
	}

	public void setTrueData(DeadEndData trueData) {
		this.trueData = trueData;
	}

	public List<DeadEndData> getFalseDatas() {
		return falseDatas;
	}

	public void setFalseDatas(List<DeadEndData> falseDatas) {
		this.falseDatas = falseDatas;
	}
	
	public List<DeadEndData> getAllData(){
		List<DeadEndData> deadEndList = new ArrayList<>();
		if(trueData!=null){
			deadEndList.add(trueData);			
		}
		deadEndList.addAll(falseDatas);
		return deadEndList;
	}

}
