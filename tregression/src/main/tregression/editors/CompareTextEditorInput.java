package tregression.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import tregression.separatesnapshots.DiffMatcher;

public class CompareTextEditorInput implements IEditorInput {

	private String sourceFilePath;
	private String targetFilePath;

	private DiffMatcher matcher;

	public CompareTextEditorInput(String sourceFilePath, String targetFilePath, DiffMatcher diffMatcher) {
		super();
		this.sourceFilePath = sourceFilePath;
		this.targetFilePath = targetFilePath;
		this.matcher = diffMatcher;
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

}
