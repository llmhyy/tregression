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

import microbat.model.variable.Variable;
import microbat.model.variable.LocalVar;
import microbat.model.value.PrimitiveValue;
import debuginfo.NodeFeedbacksPair;
import debuginfo.NodeVarPair;
import debuginfo.DebugInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.ChosenVariablesOption;
import microbat.recommendation.UserFeedback;

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
		
		GridData buttonLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		
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
		addInputButton.setToolTipText("Sets the selected fields as input");
		addInputButton.setLayoutData(buttonLayoutData);
		addInputButton.addMouseListener(new AddInputListener());
		
		Button addOutputButton = new Button(slicingGroup, SWT.NONE);
		addOutputButton.setText("Outputs");
		addOutputButton.setToolTipText("Sets the selected fields as output");
		addOutputButton.setLayoutData(buttonLayoutData);
		addOutputButton.addMouseListener(new AddOuputListener());
		
		Button clearVarsButton = new Button(slicingGroup, SWT.NONE);
		clearVarsButton.setText("clear");
		clearVarsButton.setToolTipText("Clears the input and output selected");
		clearVarsButton.setLayoutData(buttonLayoutData);
		clearVarsButton.addMouseListener(new ClearVarsListener());
		
		Button showIOButton = new Button(slicingGroup, SWT.NONE);
		showIOButton.setText("IO");
		showIOButton.setToolTipText("Shows the input and output fields set");
		showIOButton.setLayoutData(buttonLayoutData);
		showIOButton.addMouseListener(new showIOListener());
		
		Button manualFeedbackButton = new Button(slicingGroup, SWT.NONE);
		manualFeedbackButton.setText("Feedback");
		manualFeedbackButton.setLayoutData(buttonLayoutData);
		manualFeedbackButton.addMouseListener(new manualFeedbackListener());
		manualFeedbackButton.setToolTipText("Set the feedback on the selected trace node to be wrong-var, wrong-flow or correct");
	}
	
	private class AddInputListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
//			List<VarValue> inputs = getSelectedVars();
//			DebugInfo.addInputs(inputs);
			List<NodeVarPair> inputNodeVarPairs = getSelectedNodeVarPairs();
			DebugInfo.addInputNodeVarPairs(inputNodeVarPairs);
		}
		
		@Override
		public void mouseUp(MouseEvent e) {}
	}
	
	private class AddOuputListener implements MouseListener {

		@Override
		public void mouseDoubleClick(MouseEvent e) {}

		@Override
		public void mouseDown(MouseEvent e) {
			List<NodeVarPair> outputNodeVarPairs = new ArrayList<>();
			if (controlButton.getSelection()) {
				TraceNode controlDom = currentNode.getControlDominator();
				VarValue controlDomVar = controlDom.getConditionResult();
				outputNodeVarPairs.add(new NodeVarPair(currentNode, controlDomVar, controlDom.getOrder()));
				DebugInfo.addOutputNodeVarPairs(outputNodeVarPairs);
				
				UserFeedback feedback = new UserFeedback(UserFeedback.WRONG_PATH);
				NodeFeedbacksPair pair = new NodeFeedbacksPair(currentNode, feedback);
				DebugInfo.addNodeFeedbacksPair(pair);
			} else {
//				List<VarValue> outputs = getSelectedVars();
//				DebugInfo.addOutputs(outputs);
				outputNodeVarPairs.addAll(getSelectedNodeVarPairs());
				DebugInfo.addOutputNodeVarPairs(outputNodeVarPairs);
			}
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
			List<UserFeedback> feedbacks = new ArrayList<>();
			if (correctButton.getSelection()) {
				UserFeedback feedback = new UserFeedback();
				feedback.setFeedbackType(UserFeedback.CORRECT);
				feedbacks.add(feedback);
			} else if (controlButton.getSelection()) {
				UserFeedback feedback = new UserFeedback();
				feedback.setFeedbackType(UserFeedback.WRONG_PATH);
				feedbacks.add(feedback);
			} else {
				List<VarValue> selectedReadVars = getSelectedReadVars();
				List<VarValue> selectedWriteVars = getSelectedWriteVars();
				if (selectedReadVars.isEmpty() && selectedWriteVars.isEmpty()) {
					throw new RuntimeException("No selected variables");
				}
				for (VarValue readVar : selectedReadVars) {
					UserFeedback feedback = new UserFeedback();
					feedback.setFeedbackType(UserFeedback.WRONG_VARIABLE_VALUE);
					feedback.setOption(new ChosenVariableOption(readVar, null));
					feedbacks.add(feedback);
				}
			}
			DebugInfo.addNodeFeedbacksPair(currentNode, feedbacks);
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
	
	private List<NodeVarPair> getSelectedNodeVarPairs() {
		List<NodeVarPair> pairs = new ArrayList<>();
		List<VarValue> selectedVars = this.getSelectedVars();
		List<VarValue> variables = new ArrayList<>();
		variables.addAll(currentNode.getReadVariables());
		variables.addAll(currentNode.getWrittenVariables());
		variables.forEach((var) -> {
			if (selectedVars.contains(var)) {
				pairs.add(new NodeVarPair(currentNode, var));
			}
		});
		return pairs;
	}
	
//	public static void registerHandler(RequireIO handler) {
//		StepDetailIOUI.registeredHandlers.add(handler);
//	}
}
