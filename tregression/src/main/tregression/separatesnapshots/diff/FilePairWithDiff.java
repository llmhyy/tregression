package tregression.separatesnapshots.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The class represents a pair of files matched by Git. 
 * @author linyun
 *
 */
public class FilePairWithDiff {
	
	private HashMap<Integer, List<Integer>> sourceToTargetMap = new HashMap<>();
	private HashMap<Integer, List<Integer>> targetToSourceMap = new HashMap<>();
	
	private String sourceFile;
	private String targetFile;
	
	private String sourceFolderName;
	
	private List<DiffChunk> chunks = new ArrayList<>();

	public FilePairWithDiff(String sourceFile, String targetFile, String sourceFolderName) {
		super();
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.setSourceFolderName(sourceFolderName);
	}

	public FilePairWithDiff(){}
	
	public String getSourceDeclaringCompilationUnit(){
		return getDeclaringCompilationUnit(sourceFile, getSourceFolderName());
	}
	
	public String getTargetDeclaringCompilationUnit(){
		return getDeclaringCompilationUnit(targetFile, getSourceFolderName());
	}
	
	public String getDeclaringCompilationUnit(String path, String sourceFolderName){

		String path0 = path.substring(path.indexOf(sourceFolderName)+sourceFolderName.length()+1, path.indexOf(".java"));
		String qualifier = path0.replace(File.separatorChar, '.');
		
		if(qualifier.startsWith(".")){
			qualifier = qualifier.substring(qualifier.indexOf(".")+1, qualifier.length());
		}
		
		return qualifier;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public List<DiffChunk> getChunks() {
		return chunks;
	}

	public void setChunks(List<DiffChunk> chunks) {
		this.chunks = chunks;
	}

	public HashMap<Integer, List<Integer>> getSourceToTargetMap() {
		return sourceToTargetMap;
	}

	public void setSourceToTargetMap(HashMap<Integer, List<Integer>> sourceToTargetMap) {
		this.sourceToTargetMap = sourceToTargetMap;
	}

	public HashMap<Integer, List<Integer>> getTargetToSourceMap() {
		return targetToSourceMap;
	}

	public void setTargetToSourceMap(HashMap<Integer, List<Integer>> targetToSourceMap) {
		this.targetToSourceMap = targetToSourceMap;
	}

	public String getSourceFolderName() {
		return sourceFolderName;
	}

	public void setSourceFolderName(String sourceFolderName) {
		this.sourceFolderName = sourceFolderName;
	}
	
	
}
