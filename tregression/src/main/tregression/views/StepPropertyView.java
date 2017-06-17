package tregression.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import microbat.views.MicroBatViews;
import microbat.views.TraceView;

public class StepPropertyView extends ViewPart {

	public StepPropertyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout parentLayout = new GridLayout(1, true);
		parent.setLayout(parentLayout);
		
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(data);
		
		Composite leftPanel = new Composite(sashForm, SWT.NONE);
		BuggyTraceView buggyView = null;
		try {
			buggyView = (BuggyTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(BuggyTraceView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		createPanel(leftPanel, buggyView);
		
		Composite rightPanel = new Composite(sashForm, SWT.NONE);
		CorrectTraceView correctView = null;
		try {
			correctView = (CorrectTraceView)PlatformUI.getWorkbench().
					getActiveWorkbenchWindow().getActivePage().showView(CorrectTraceView.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		createPanel(rightPanel, correctView);

		sashForm.setWeights(new int[]{50, 50});
	}

	private void createPanel(Composite panel, TraceView view) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);
		
		
		StepDetailUI detailUI = new StepDetailUI(view, null);
		detailUI.createDetails(panel);
		
	}
	
	@Override
	public void setFocus() {

	}

}
