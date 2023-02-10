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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;

import debuginfo.DebugInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.ChosenVariablesOption;
import microbat.recommendation.UserFeedback;
import microbat.recommendation.UserFeedback_M;
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
	
	private Button correctButton;
	
//	private static List<RequireIO> registeredHandlers = new ArrayList<>();
	
	public StepDetailIOUI(TregressionTraceView view, TraceNode node, boolean isOnBefore) {
		super(view, node, isOnBefore);
	}
	
	@Override
	protected void createSlicingGroup(Composite panel) {
		Group slicingGroup = new Group(panel, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.minimumHeight = 35;
		slicingGroup.setLayoutData(data);
		
		GridLayout gl = new GridLayout(4, true);
		slicingGroup.setLayout(gl);
		
		this.correctButton = new Button(slicingGroup, SWT.RADIO);
		this.correctButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		this.correctButton.setText("correct");
		
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
		
		Button manualFeedbackButton = new Button(slicingGroup, SWT.NONE);
		manualFeedbackButton.setText("Feedback");
		manualFeedbackButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		manualFeedbackButton.addMouseListener(new manualFeedbackListener());
		
		Button stopButton = new Button(slicingGroup, SWT.NONE);
		stopButton.setText("Stop");
		stopButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		stopButton.addMouseListener(new StopListener());
	}
	
	private class AddInputListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			List<VarValue> inputs = getSelectedVars();
			DebugInfo.addInputs(inputs);
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
			DebugInfo.addOutputs(outputs);
		}

		@Override
		public void mouseUp(MouseEvent e) {}
	}
	
	private class ClearVarsListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			DebugInfo.clearData();
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private class showIOListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			DebugInfo.printInputs();
			DebugInfo.printOutputs();
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private class manualFeedbackListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			UserFeedback_M feedback = new UserFeedback_M();
			if (correctButton.getSelection()) {
				feedback.setFeedbackType(UserFeedback.CORRECT);
			} else if (controlButton.getSelection()) {
				feedback.setFeedbackType(UserFeedback.WRONG_PATH);
			} else {
				feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
				List<VarValue> selectedReadVars = getSelectedReadVars();
				List<VarValue> selectedWriteVars = getSelectedWriteVars();
				if (selectedReadVars.isEmpty() && selectedWriteVars.isEmpty()) {
					throw new RuntimeException("No selected variables");
				}
				feedback.setOption(new ChosenVariablesOption(selectedReadVars, selectedWriteVars));
			}
			DebugInfo.addNodeFeedbackPair(currentNode, feedback);
//			BaselineHandler.setManualFeedback(feedback, currentNode);
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private class StopListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			DebugInfo.setStop(true);
		}

		@Override
		public void mouseUp(MouseEvent e) {	}
		
	}
	
	private List<VarValue> getSelectedVars() {
		List<VarValue> vars = new ArrayList<>();
		vars.addAll(this.getSelectedReadVars());
		vars.addAll(this.getSelectedWriteVars());
		return vars;
	}

	private List<VarValue> getSelectedReadVars() {
		List<VarValue> vars = new ArrayList<>();
		
		Object[] readObjList = this.readVariableTreeViewer.getCheckedElements();
		for (Object object : readObjList) {
			if (object instanceof VarValue) {
				VarValue input = (VarValue) object;
				vars.add(input);
			}
		}
		
		return vars;
	}
	
	private List<VarValue> getSelectedWriteVars() {
		List<VarValue> vars = new ArrayList<>();
		Object[] writeObjList = this.writtenVariableTreeViewer.getCheckedElements();
		for (Object object : writeObjList) {
			if (object instanceof VarValue) {
				VarValue output = (VarValue) object;
				vars.add(output);
			}
		}
		return vars;
	}
	
//	public static void registerHandler(RequireIO handler) {
//		StepDetailIOUI.registeredHandlers.add(handler);
//	}
}
