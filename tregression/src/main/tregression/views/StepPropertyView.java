package tregression.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.views.TraceView;
import tregression.StepChangeType;
import tregression.StepChangeTypeChecker;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;

public class StepPropertyView extends ViewPart {

	public static final String ID = "tregression.view.stepProperty";
	
	public StepPropertyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout parentLayout = new GridLayout(1, true);
		parent.setLayout(parentLayout);
		
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(data);
		
		createScrolledComposite(sashForm, BuggyTraceView.ID);
		createScrolledComposite(sashForm, CorrectTraceView.ID);

		sashForm.setWeights(new int[]{50, 50});
	}
	
	private void createScrolledComposite(SashForm sashForm, String viewID){
		ScrolledComposite panel = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		panel.setExpandHorizontal(true);
		panel.setExpandVertical(true);
		TraceView view = null;
		
		if(viewID.equals(BuggyTraceView.ID)){
			try {
				view = (BuggyTraceView)PlatformUI.getWorkbench().
						getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		else{
			try {
				view = (CorrectTraceView)PlatformUI.getWorkbench().
						getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		
		Composite comp = createPanel(panel, view);
		panel.setContent(comp);
//		panel.setAlwaysShowScrollBars(true);
//		panel.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		Point point = comp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		panel.setMinHeight(point.y);
	}

	private Composite createPanel(Composite panel, TraceView view) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);
		
		if(view instanceof BuggyTraceView){
			buggyDetailUI = new StepDetailUI(view, null);			
			return buggyDetailUI.createDetails(panel);
		}
		else if(view instanceof CorrectTraceView){
			correctDetailUI = new StepDetailUI(view, null);
			return correctDetailUI.createDetails(panel);
		}
		
		return null;
	}
	
	private StepDetailUI buggyDetailUI;
	private StepDetailUI correctDetailUI;
	
	public void refresh(TraceNode correctNode, TraceNode buggyNode, DiffMatcher diffMatcher, PairList pairList){
		Trace buggyTrace = TregressionViews.getBuggyTraceView().getTrace();
		Trace correctTrace = TregressionViews.getCorrectTraceView().getTrace();
		StepChangeTypeChecker checker = new StepChangeTypeChecker(buggyTrace, correctTrace);
		
		if(buggyDetailUI != null && buggyNode != null){
			StepChangeType changeType = checker.getType(buggyNode, true, pairList, diffMatcher);
			buggyDetailUI.refresh(buggyNode, changeType);
		}
		
		if(correctDetailUI != null && correctNode != null){
			StepChangeType changeType = checker.getType(correctNode, false, pairList, diffMatcher);
			correctDetailUI.refresh(correctNode, changeType);
		}
	}
	
	@Override
	public void setFocus() {

	}

}
