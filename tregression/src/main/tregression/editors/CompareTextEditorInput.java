package tregression.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import microbat.model.trace.TraceNode;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class CompareTextEditorInput implements IEditorInput {

	private TraceNode selectedNode;
	private PairList pairList;
	
	private String sourceFilePath;
	private String targetFilePath;

	private DiffMatcher matcher;

	public CompareTextEditorInput(TraceNode selectedNode, PairList pairList, String sourceFilePath,
			String targetFilePath, DiffMatcher matcher) {
		super();
		this.setSelectedNode(selectedNode);
		this.setPairList(pairList);
		this.sourceFilePath = sourceFilePath;
		this.targetFilePath = targetFilePath;
		this.matcher = matcher;
	}

	public DiffMatcher getMatcher() {
		return matcher;
	}

	public void setMatcher(DiffMatcher matcher) {
		this.matcher = matcher;
	}

	public String getSourceFilePath() {
		return sourceFilePath;
	}

	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}

	public String getTargetFilePath() {
		return targetFilePath;
	}

	public void setTargetFilePath(String targetFilePath) {
		this.targetFilePath = targetFilePath;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "compare";
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

	public TraceNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TraceNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}

}
