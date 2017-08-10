package tregression.separatesnapshots;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import tregression.StepChangeType;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.DiffParser;
import tregression.separatesnapshots.diff.FilePairWithDiff;
import tregression.separatesnapshots.diff.LineChange;

public class DiffMatcher {
	
	private String sourceFolderName;
	private String testFolderName;
	
	private String buggyPath;
	private String fixPath;
	
	private List<FilePairWithDiff> fileDiffList;
	
	public DiffMatcher(String sourceFolderName, String testFolderName, String buggyPath, String fixPath) {
		super();
		this.sourceFolderName = sourceFolderName;
		this.testFolderName = testFolderName;
		this.buggyPath = buggyPath;
		this.fixPath = fixPath;
	}
	
	public boolean checkSourceDiff(BreakPoint breakPoint, boolean isOnBeforeTrace) {
		if (isOnBeforeTrace) {
			FilePairWithDiff diff = findDiffBySourceFile(breakPoint);
			if(diff != null){
				for (DiffChunk chunk : diff.getChunks()) {
					int start = chunk.getStartLineInSource();
					int end = start + chunk.getChunkLengthInSource() - 1;
					int type = findLineChange(breakPoint, chunk, start, end, isOnBeforeTrace);
					if(type == StepChangeType.SRC){
						return true;
					}
					else if(type == -1){
						break;
					}
				}
			}
		} else {
			FilePairWithDiff diff = findDiffByTargetFile(breakPoint);
			if(diff != null){
				for (DiffChunk chunk : diff.getChunks()) {
					int start = chunk.getStartLineInTarget();
					int end = start + chunk.getChunkLengthInTarget() - 1;
					int type = findLineChange(breakPoint, chunk, start, end, isOnBeforeTrace);
					if(type == StepChangeType.SRC){
						System.currentTimeMillis();
						return true;
					}
					else if(type == -1){
						break;
					}
				}
				
			}
		}
		
		return false;
	}

	/**
	 * return SRC if the code of step is contained in chunk and the code is a diff
	 * return -1 if the code of step is contained in chunk and the code is not a diff
	 * return -2 if the code of step is not contained in chunk.
	 * 
	 * @param step
	 * @param chunk
	 * @param start
	 * @param end
	 * @return
	 */
	private int findLineChange(BreakPoint breakPoint, DiffChunk chunk, int start, int end, boolean isOnBeforeTrace) {
		int stepLineNo = breakPoint.getLineNumber();
		if (start <= stepLineNo && stepLineNo <= end) {
			int count = 0;
			for (int i = 0; i < chunk.getChangeList().size(); i++) {
				LineChange lineChange = chunk.getChangeList().get(i);
				if(isOnBeforeTrace){
					if(lineChange.getType() != LineChange.ADD){
						count++;
					}
				}
				else{
					if(lineChange.getType() != LineChange.REMOVE){
						count++;
					}
				}
				
				int currentLineNo = start + count - 1;
				if (stepLineNo == currentLineNo) {
					if(lineChange.getType() != LineChange.UNCHANGE){
						return StepChangeType.SRC;
					}
					else{
						return -1;
					}
				}
			}
		}
		
		return -2;
	}
	
	private List<String> getRawDiffContent(){
		List<String> cmdList = new ArrayList<>();
		
		cmdList.add("git");
		cmdList.add("diff");
		cmdList.add("--no-index");
		
		String buggySourcePath = buggyPath + File.separator + sourceFolderName;
		cmdList.add(buggySourcePath);
		String fixSourcePath = fixPath + File.separator + sourceFolderName;
		cmdList.add(fixSourcePath);
		
		String[] cmds = cmdList.toArray(new String[0]);
		try {
			Process proc = Runtime.getRuntime().exec(cmds, new String[]{});
			
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			
			List<String> diffContent = new ArrayList<>();
			String line = null;
			while ( (line = br.readLine()) != null)
				diffContent.add(line);

			return diffContent;
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	public boolean isMatch(BreakPoint srcPoint, BreakPoint targetPoint){
		if(!srcPoint.isSourceVersion()){
			BreakPoint tmp = srcPoint;
			srcPoint = targetPoint;
			targetPoint = tmp;
		}
		
		FilePairWithDiff fileDiff = findDiffBySourceFile(srcPoint);
		if(fileDiff==null){
			boolean isSameFile = srcPoint.getDeclaringCompilationUnitName().equals(targetPoint.getDeclaringCompilationUnitName());
			boolean isSameLocation = srcPoint.getLineNumber()==targetPoint.getLineNumber();
			
			return isSameFile && isSameLocation;
		}
		else{
			List<Integer> targetLines = fileDiff.getSourceToTargetMap().get(srcPoint.getLineNumber());
			if(fileDiff.getTargetDeclaringCompilationUnit().equals(targetPoint.getDeclaringCompilationUnitName())){
				if(targetLines.contains(targetPoint.getLineNumber())){
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	
	public FilePairWithDiff findDiffByTargetFile(String targetFile){
		for(FilePairWithDiff diff: this.fileDiffList){
			if(diff.getTargetFile().equals(targetFile)){
				return diff;
			}
		}
		
		return null;
	}
	
	public FilePairWithDiff findDiffBySourceFile(String sourceFile){
		for(FilePairWithDiff diff: this.fileDiffList){
			if(diff.getSourceFile().equals(sourceFile)){
				return diff;
			}
		}
		
		return null;
	}

	public FilePairWithDiff findDiffBySourceFile(BreakPoint srcPoint) {
		for(FilePairWithDiff diff: this.fileDiffList){
			if(diff.getSourceDeclaringCompilationUnit().equals(srcPoint.getDeclaringCompilationUnitName())){
				return diff;
			}
		}
		
		return null;
	}

	public void matchCode(){
		
		List<String> diffContent = getRawDiffContent();
		diffContent.add("diff end");
		List<FilePairWithDiff> fileDiffs = new DiffParser().parseDiff(diffContent, sourceFolderName);

		for(FilePairWithDiff fileDiff: fileDiffs){
			HashMap<Integer, List<Integer>> sourceToTargetMap = new HashMap<>();
			HashMap<Integer, List<Integer>> targetToSourceMap = new HashMap<>();
			
			constructMapping(fileDiff, sourceToTargetMap, targetToSourceMap);
			
			fileDiff.setSourceToTargetMap(sourceToTargetMap);
			fileDiff.setTargetToSourceMap(targetToSourceMap);
		}
		
		this.fileDiffList = fileDiffs;
		
	}

	private int countLineNumber(String fileName){
		LineNumberReader lnr;
		try {
			lnr = new LineNumberReader(new FileReader(new File(fileName)));
			lnr.skip(Long.MAX_VALUE);
			int count = lnr.getLineNumber() + 1; //Add 1 because line index starts at 0
			// Finally, the LineNumberReader object should be closed to prevent resource leak
			lnr.close();
			
			return count;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	private void constructMapping(FilePairWithDiff fileDiff, HashMap<Integer, List<Integer>> sourceToTargetMap,
			HashMap<Integer, List<Integer>> targetToSourceMap) {
		int sourceLineCursor = 1;
		int targetLineCursor = 1;
		
		for(DiffChunk chunk: fileDiff.getChunks()){
			int startLineInSource = chunk.getStartLineInSource();
			int startLineInTarget = chunk.getStartLineInTarget();
			
			while(sourceLineCursor<startLineInSource && targetLineCursor<startLineInTarget){
				mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
				sourceLineCursor++;
				targetLineCursor++;
			}
			
			for(int index=0; index<chunk.getChangeList().size(); ){
				LineChange line = chunk.getChangeList().get(index);
				if(line.getType()==LineChange.UNCHANGE){
					mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
					sourceLineCursor++;
					targetLineCursor++;
					index++;
				}
				else if(line.getType()==LineChange.REMOVE){
					int successiveRemoveLines = findSuccessiveRemoveLines(chunk, line.getIndex());
					System.currentTimeMillis();
					boolean followByAdd = checkFollowByAdd(chunk, line.getIndex(), successiveRemoveLines);
					
					if(followByAdd){
						int successiveAddLines = findSuccessiveAddLines(chunk, line.getIndex()+successiveRemoveLines);
						for(int i=sourceLineCursor; i<sourceLineCursor+successiveRemoveLines; i++){
							for(int j=targetLineCursor; j<targetLineCursor+successiveAddLines; j++){
								mapAdditionalLine(sourceToTargetMap, i, j);
								mapAdditionalLine(targetToSourceMap, j, i);
							}
						}
						
						sourceLineCursor += successiveRemoveLines;
						targetLineCursor += successiveAddLines;
						
						index += successiveAddLines + successiveRemoveLines;
					}
					else{
						sourceLineCursor += successiveRemoveLines;
						index += successiveRemoveLines;
					}
				}
				else{
					targetLineCursor++;
					index++;
				}
			}
			
		}
		
		int totalSoureLineNumber = countLineNumber(fileDiff.getSourceFile());
		int totalTargetLineNumber = countLineNumber(fileDiff.getTargetFile());
		while(sourceLineCursor<totalSoureLineNumber && targetLineCursor<totalTargetLineNumber){
			mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
			sourceLineCursor++;
			targetLineCursor++;
		}
	}
	
	private int findSuccessiveAddLines(DiffChunk chunk, int startIndex) {
		int count = 0;
		for(int i=startIndex; i<chunk.getChangeList().size(); i++){
			LineChange line = chunk.getChangeList().get(i);
			if(line.getType()==LineChange.ADD){
				count++;
			}
			else{
				break;
			}
		}
		
		return count;
	}

	private boolean checkFollowByAdd(DiffChunk chunk, int startIndex, int successiveRemoveLines) {
		int index = startIndex+successiveRemoveLines;
		if(index <= chunk.getChangeList().size()){
			return chunk.getChangeList().get(index).getType()==LineChange.ADD;
		}
		
		return false;
	}

	private int findSuccessiveRemoveLines(DiffChunk chunk, int startIndex) {
		int count = 0;
		for(int i=startIndex; i<chunk.getChangeList().size(); i++){
			LineChange line = chunk.getChangeList().get(i);
			if(line.getType()==LineChange.REMOVE){
				count++;
			}
			else{
				break;
			}
		}
		
		return count;
	}

	private void mapAdditionalLine(HashMap<Integer, List<Integer>> sourceToTargetMap, int sourceLineCursor,
			int targetLineCursor) {
		List<Integer> targetLines = sourceToTargetMap.get(sourceLineCursor);
		if(targetLines == null){
			targetLines = new ArrayList<>();
		}
		
		if(!targetLines.contains(targetLineCursor)){
			targetLines.add(targetLineCursor);			
		}
		
		sourceToTargetMap.put(sourceLineCursor, targetLines);
	}

	private void mapLine(HashMap<Integer, List<Integer>> sourceToTargetMap, 
			HashMap<Integer, List<Integer>> targetToSourceMap, int sourceLineCursor, int targetLineCursor){
		List<Integer> targetLines = new ArrayList<>();
		targetLines.add(targetLineCursor);
		List<Integer> sourceLines = new ArrayList<>();
		sourceLines.add(sourceLineCursor);
		
		sourceToTargetMap.put(sourceLineCursor, targetLines);
		targetToSourceMap.put(targetLineCursor, sourceLines);
	}

	public String getBuggyPath() {
		return buggyPath;
	}

	public void setBuggyPath(String buggyPath) {
		this.buggyPath = buggyPath;
	}

	public String getFixPath() {
		return fixPath;
	}

	public void setFixPath(String fixPath) {
		this.fixPath = fixPath;
	}
	
	public String getSourceFolderName(){
		return this.sourceFolderName;
	}

	public String getTestFolderName() {
		return testFolderName;
	}

	public void setTestFolderName(String testFolderName) {
		this.testFolderName = testFolderName;
	}

	public FilePairWithDiff findDiffByTargetFile(BreakPoint tarPoint) {
		for(FilePairWithDiff diff: this.fileDiffList){
			if(diff.getTargetDeclaringCompilationUnit().equals(tarPoint.getDeclaringCompilationUnitName())){
				return diff;
			}
		}
		
		return null;
	}

	public ClassLocation findCorrespondingLocation(BreakPoint breakPoint, boolean isSourceVersion) {
		if(isSourceVersion){
			return getCorrespondentLocationForSource(breakPoint);
		}
		else{
			return getCorrespondentLocationForTarget(breakPoint);
		}
	}

	private ClassLocation getCorrespondentLocationForSource(BreakPoint breakPoint) {
		FilePairWithDiff diff = findDiffBySourceFile(breakPoint);
		if(diff == null){
			return (BreakPoint) breakPoint.clone();
		}
		else{
			List<Integer> lines = diff.getTargetToSourceMap().get(breakPoint.getLineNumber());
			ClassLocation location = new ClassLocation(diff.getSourceDeclaringCompilationUnit(), null, lines.get(0));
			return location;
//			for(DiffChunk chunk: diff.getChunks()){
//				int startLine = chunk.getStartLineInSource();
//				int endLine = startLine + chunk.getChunkLengthInSource() - 1;
//				
//				if(startLine<=breakPoint.getLineNumber() && breakPoint.getLineNumber()<=endLine){
//					int count = 0;
//					for(int i=0; i<chunk.getChangeList().size(); i++){
//						LineChange change = chunk.getChangeList().get(i); 
//						if(change.getType() != LineChange.ADD){
//							int currentLine = startLine + count - 1;
//							
//							if(currentLine==breakPoint.getLineNumber()){
//								
//								List<Integer> lines = diff.getSourceToTargetMap().get(currentLine);
//								ClassLocation location = 
//										new ClassLocation(diff.getTargetDeclaringCompilationUnit(), null, lines.get(0));
//								return location;
//							}
//							
//							count++;
//						}
//					}
//				}
//			}
		}
		
//		return null;
	}
	
	
	private ClassLocation getCorrespondentLocationForTarget(BreakPoint breakPoint) {
		FilePairWithDiff diff = findDiffByTargetFile(breakPoint);
		if(diff == null){
			return (BreakPoint) breakPoint.clone();
		}
		else{
			List<Integer> lines = diff.getSourceToTargetMap().get(breakPoint.getLineNumber());
			ClassLocation location = 
					new ClassLocation(diff.getTargetDeclaringCompilationUnit(), null, lines.get(0));
			return location;
			
//			for(DiffChunk chunk: diff.getChunks()){
//				int startLine = chunk.getStartLineInTarget();
//				int endLine = startLine + chunk.getChunkLengthInSource() - 1;
//				
//				if(startLine<=breakPoint.getLineNumber() && breakPoint.getLineNumber()<=endLine){
//					int count = 0;
//					for(int i=0; i<chunk.getChangeList().size(); i++){
//						LineChange change = chunk.getChangeList().get(i); 
//						if(change.getType() != LineChange.REMOVE){
//							int currentLine = startLine + count - 1;
//							
//							if(currentLine==breakPoint.getLineNumber()){
//								
//								List<Integer> lines = diff.getTargetToSourceMap().get(currentLine);
//								ClassLocation location = 
//										new ClassLocation(diff.getSourceDeclaringCompilationUnit(), null, lines.get(0));
//								return location;
//							}
//							
//							count++;
//						}
//					}
//				}
//			}
		}
		
//		return null;
	}
	
}
