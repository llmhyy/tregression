package tregression.views;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import microbat.model.trace.TraceNode;
import tregression.editors.CompareEditor;
import tregression.editors.CompareTextEditorInput;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.FilePairWithDiff;

public class CorrectTraceView extends TregressionTraceView {

	public static final String ID = "tregression.evalView.correctTraceView";
	
	public CorrectTraceView() {
	}
	
	@Override
	protected Action createControlMendingAction() {
		Action action = new Action() {
			public void run() {
				if (listViewer.getSelection().isEmpty()) {
					return;
				}

				if (listViewer.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
					TraceNode node = (TraceNode) selection.getFirstElement();
					
					BuggyTraceView buggyTraceView = TregressionViews.getBuggyTraceView();
					ClassLocation correspondingLocation = diffMatcher.findCorrespondingLocation(node.getBreakPoint(), true);
					TraceNode otherControlDom = new RootCauseFinder().findResponsibleControlDomOnOtherTrace(node, pairList, 
							buggyTraceView.getTrace(), true, correspondingLocation);
					
					if (otherControlDom != null) {
						buggyTraceView.otherViewsBehavior(otherControlDom);
					}
					
				}
				
			}
			
			public String getText() {
				return "control mend";
			}
		};
		
		return action;
	}
	
	private void openInCompare(CompareTextEditorInput input, TraceNode node) {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage workBenchPage = win.getActivePage();

		IEditorPart editPart = workBenchPage.findEditor(input);
		if(editPart != null){
			workBenchPage.activate(editPart);
			CompareEditor editor = (CompareEditor)editPart;
			editor.highLight(node);
		}
		else{
			try {
				workBenchPage.openEditor(input, CompareEditor.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		
	}

	class CompareFileName {
		String buggyFileName;
		String fixFileName;

		public CompareFileName(String buggyFileName, String fixFileName) {
			super();
			this.buggyFileName = buggyFileName;
			this.fixFileName = fixFileName;
		}

	}

	private CompareFileName generateCompareFile(BreakPoint breakPoint, DiffMatcher matcher) {
		String buggyFileName;
		String fixFileName;
		
		String buggyPath = getDiffMatcher().getBuggyPath() + File.separator + matcher.getSourceFolderName() + File.separator;
		String fixPath = getDiffMatcher().getFixPath() + File.separator + matcher.getSourceFolderName() + File.separator;

		FilePairWithDiff fileDiff = getDiffMatcher().findDiffByTargetFile(breakPoint);
		if (getDiffMatcher() == null || fileDiff == null) {
			String baseFileName = breakPoint.getDeclaringCompilationUnitName();
			baseFileName = baseFileName.replace(".", File.separator) + ".java";

			buggyFileName = buggyPath + baseFileName;
			fixFileName = fixPath + baseFileName;
		} else {
			String sourceBase = fileDiff.getSourceDeclaringCompilationUnit();
			sourceBase = sourceBase.replace(".", File.separator) + ".java";
			buggyFileName = buggyPath + sourceBase;

			String targetBase = fileDiff.getTargetDeclaringCompilationUnit();
			targetBase = targetBase.replace(".", File.separator) + ".java";
			fixFileName = fixPath + targetBase;
		}
		
		CompareFileName cfn = new CompareFileName(buggyFileName, fixFileName);
		return cfn;
	}

	@Override
	protected void markJavaEditor(TraceNode node) {
		BreakPoint breakPoint = node.getBreakPoint();
		
		CompareFileName cfn = generateCompareFile(breakPoint, getDiffMatcher());

		CompareTextEditorInput input = new CompareTextEditorInput(node, this.pairList, 
				cfn.buggyFileName, cfn.fixFileName, getDiffMatcher());

		openInCompare(input, node);

	}

	@Override
	protected void otherViewsBehavior(TraceNode correctNode) {
		if (this.refreshProgramState) {
			
			StepPropertyView stepPropertyView = null;
			try {
				stepPropertyView = (StepPropertyView)PlatformUI.getWorkbench().
						getActiveWorkbenchWindow().getActivePage().showView(StepPropertyView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
			
			TraceNodePair pair = pairList.findByAfterNode(correctNode);
			correctNode.toString();
			TraceNode buggyNode = null;
			if(pair != null){
				buggyNode = pair.getBeforeNode();
				if (buggyNode != null) {
					BuggyTraceView buggyTraceView = TregressionViews.getBuggyTraceView();
					buggyTraceView.jumpToNode(buggyTraceView.getTrace(), buggyNode.getOrder(), false);
				}
			}
			
			stepPropertyView.refresh(correctNode, buggyNode, diffMatcher, pairList);
		}

		markJavaEditor(correctNode);
	}

	public PairList getPairList() {
		return pairList;
	}

	public void setPairList(PairList pairList) {
		this.pairList = pairList;
	}

	public DiffMatcher getDiffMatcher() {
		return diffMatcher;
	}

	public void setDiffMatcher(DiffMatcher diffMatcher) {
		this.diffMatcher = diffMatcher;
	}

}
