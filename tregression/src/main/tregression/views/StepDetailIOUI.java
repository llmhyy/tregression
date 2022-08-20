package tregression.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import tregression.handler.BaselineHandler;

/**
 * Do everything the same as StepDetailUI.
 * 
 * But for StepDetailIOUI, it enable user to specific the input and output
 * for baseline probability encoder
 * @author David
 *
 */
public class StepDetailIOUI extends StepDetailUI {

	public StepDetailIOUI(TregressionTraceView view, TraceNode node, boolean isOnBefore) {
		super(view, node, isOnBefore);
	}
	
	@Override
	protected void createSlicingGroup(Composite panel) {
		Group slicingGroup = new Group(panel, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.minimumHeight = 35;
		slicingGroup.setLayoutData(data);
		
		GridLayout gl = new GridLayout(3, true);
		slicingGroup.setLayout(gl);
		
		dataButton = new Button(slicingGroup, SWT.RADIO);
		dataButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		dataButton.setText("data ");
		
		
		controlButton = new Button(slicingGroup, SWT.RADIO);
		controlButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		controlButton.setText("control ");
		
		Button submitButton = new Button(slicingGroup, SWT.NONE);
		submitButton.setText("Go");
		submitButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		FeedbackSubmitListener fListener = new FeedbackSubmitListener();
		submitButton.addMouseListener(fListener);
		
		Button addInputButton = new Button(slicingGroup, SWT.NONE);
		addInputButton.setText("Inputs");
		addInputButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		addInputButton.addMouseListener(new AddInputListener());
		
		Button addOutputButton = new Button(slicingGroup, SWT.NONE);
		addOutputButton.setText("Outputs");
		addOutputButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		addOutputButton.addMouseListener(new AddOuputListener());
		
		Button clearVarsButton = new Button(slicingGroup, SWT.NONE);
		clearVarsButton.setText("clear");
		clearVarsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		clearVarsButton.addMouseListener(new ClearVarsListener());
		
		Button showIOButton = new Button(slicingGroup, SWT.NONE);
		showIOButton.setText("IO");
		showIOButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		showIOButton.addMouseListener(new showIOListener());
	}
	
	private class AddInputListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			List<VarValue> inputs = getSelectedVars();
			BaselineHandler.addInputs(inputs);
		}
		
		@Override
		public void mouseUp(MouseEvent e) {}
	}
	
	private class AddOuputListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			List<VarValue> outputs = getSelectedVars();
			BaselineHandler.addOutpus(outputs);
		}

		@Override
		public void mouseUp(MouseEvent e) {}
	}
	
	private class ClearVarsListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			BaselineHandler.clearIO();
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private class showIOListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			BaselineHandler.printIO();
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private List<VarValue> getSelectedVars() {
		List<VarValue> vars = new ArrayList<>();
		
		Object[] readObjList = this.readVariableTreeViewer.getCheckedElements();
		for (Object object : readObjList) {
			if (object instanceof VarValue) {
				VarValue input = (VarValue) object;
				vars.add(input);
			}
		}
		
		Object[] writeObjList = this.writtenVariableTreeViewer.getCheckedElements();
		for (Object object : writeObjList) {
			if (object instanceof VarValue) {
				VarValue output = (VarValue) object;
				vars.add(output);
			}
		}
		
		return vars;
	}

}
