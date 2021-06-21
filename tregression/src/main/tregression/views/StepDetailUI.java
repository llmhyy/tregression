package tregression.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import microbat.algorithm.graphdiff.GraphDiff;
import microbat.model.BreakPointValue;
import microbat.model.UserInterestedVariables;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import microbat.model.value.VirtualValue;
import microbat.model.variable.Variable;
import microbat.recommendation.ChosenVariableOption;
import microbat.recommendation.UserFeedback;
import microbat.util.JavaUtil;
import microbat.util.Settings;
import microbat.util.TempVariableInfo;
import microbat.views.ImageUI;
import sav.common.core.Pair;
import sav.strategies.dto.AppJavaClassPath;
import tregression.StepChangeType;

public class StepDetailUI {
	
	public static final String RW = "rw";
	public static final String STATE = "state";
	
	public UserInterestedVariables interestedVariables = new UserInterestedVariables();
	
	private UserFeedback feedback = new UserFeedback();

	class FeedbackSubmitListener implements MouseListener{
		public void mouseUp(MouseEvent e) {}
		public void mouseDoubleClick(MouseEvent e) {}
		
		private void openChooseFeedbackDialog(){
			MessageBox box = new MessageBox(PlatformUI.getWorkbench()
					.getDisplay().getActiveShell());
			box.setMessage("Please tell me whether this step is correct or not!");
			box.open();
		}
		
		public void mouseDown(MouseEvent e) {
			
			//for(int i=66644; i<=67830; i++){
				TraceNode n = traceView.getTrace().getExecutionList().get(1);
				for(VarValue var: n.getReadVariables()){
					if(var.getVarName().contains("code")){
						//System.out.println(n.getOrder()+":" + var.getVarName());
					}
				}
			//}
			
			if (feedback == null) {
				openChooseFeedbackDialog();
			} 
			else {
				Trace trace = traceView.getTrace();
				
				TraceNode suspiciousNode = null;
				if(dataButton.getSelection()){
					Object[] objList = readVariableTreeViewer.getCheckedElements();
					if(objList.length!=0) {
						Object obj = objList[0];
						if(obj instanceof VarValue) {
							VarValue readVar = (VarValue)obj;
							suspiciousNode = trace.findDataDependency(currentNode, readVar);
						}
					}
				}
				else if(controlButton.getSelection()){
					suspiciousNode = currentNode.getInvocationMethodOrDominator();
				}
				
				if(suspiciousNode != null){
					traceView.recordVisitedNode(currentNode);
					jumpToNode(trace, suspiciousNode);	
				}
			}
		}
		
		private void jumpToNode(Trace trace, TraceNode suspiciousNode) {
			traceView.jumpToNode(trace, suspiciousNode.getOrder(), true);
		}
	}
	
	@SuppressWarnings("unchecked")
	class RWVariableContentProvider implements ITreeContentProvider {
		/**
		 * rw is true means read, and rw is false means write.
		 */
		boolean rw;

		public RWVariableContentProvider(boolean rw) {
			this.rw = rw;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ArrayList) {
				ArrayList<VarValue> elements = (ArrayList<VarValue>) inputElement;
				return elements.toArray(new VarValue[0]);
			}

			return null;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ReferenceValue) {
				ReferenceValue refValue = (ReferenceValue)parentElement;
				
				if(refValue.getChildren()==null){
					return null;
				}
				
				return refValue.getChildren().toArray(new VarValue[0]);
			}

			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			if (children == null || children.length == 0) {
				return false;
			} else {
				return true;
			}
		}

	}

	class RWVarListener implements ICheckStateListener {
		private String RWType;

		public RWVarListener(String RWType) {
			this.RWType = RWType;
		}

		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			Object obj = event.getElement();
			VarValue value = null;

			if (obj instanceof VarValue) {
				Trace trace = traceView.getTrace();

				value = (VarValue) obj;
				String varID = value.getVarID();

				if (!interestedVariables.contains(value)) {
					interestedVariables.add(trace.getCheckTime(), value);

					ChosenVariableOption option = feedback.getOption();
					if (option == null) {
						option = new ChosenVariableOption(null, null);
					}

					if (this.RWType.equals(Variable.READ)) {
						option.setReadVar(value);
					}
					if (this.RWType.equals(Variable.WRITTEN)) {
						option.setWrittenVar(value);
					}
					feedback.setOption(option);

					TempVariableInfo.variableOption = option;
					TempVariableInfo.line = currentNode.getLineNumber();
					String cuName = currentNode.getBreakPoint().getDeclaringCompilationUnitName();
					TempVariableInfo.cu = JavaUtil.findCompilationUnitInProject(cuName, traceView.getTrace().getAppJavaClassPath());
				} else {
					interestedVariables.remove(value);
				}

				setChecks(writtenVariableTreeViewer, RW);
				setChecks(readVariableTreeViewer, RW);
				//setChecks(stateTreeViewer, STATE);

				writtenVariableTreeViewer.refresh();
				readVariableTreeViewer.refresh();
				//stateTreeViewer.refresh();

			}

		}
	}

	class VariableCheckStateProvider implements ICheckStateProvider {

		@Override
		public boolean isChecked(Object element) {

			VarValue value = null;
			if (element instanceof VarValue) {
				value = (VarValue) element;
			} else if (element instanceof GraphDiff) {
				value = (VarValue) ((GraphDiff) element).getChangedNode();
			}

			if (currentNode != null) {
				if (interestedVariables.contains(value)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isGrayed(Object element) {
			return false;
		}

	}

	class VariableContentProvider implements ITreeContentProvider {
		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof BreakPointValue) {
				BreakPointValue value = (BreakPointValue) inputElement;
				return value.getChildren().toArray(new VarValue[0]);
			} else if (inputElement instanceof ReferenceValue) {
				ReferenceValue value = (ReferenceValue) inputElement;
				VarValue[] list = value.getChildren().toArray(new VarValue[0]);
				if (list.length != 0) {
					return list;
				} else {
					return null;
				}
			}

			return null;
		}

		public Object[] getChildren(Object parentElement) {
			return getElements(parentElement);
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof ReferenceValue) {
				ReferenceValue rValue = (ReferenceValue) element;
				List<VarValue> children = rValue.getChildren();
				return children != null && !children.isEmpty();
			}
			return false;
		}

	}

	class VariableLabelProvider implements ITableLabelProvider {
		private StepChangeType changeType;
		private boolean isOnBefore;
		
		public VariableLabelProvider(StepChangeType changeType, boolean isOnBefore) {
			this.changeType = changeType;
			this.isOnBefore = isOnBefore;
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (element instanceof VarValue && columnIndex==0) {
				VarValue varValue = (VarValue) element;
				if(changeType.getType()==StepChangeType.DAT) {
					for(Pair<VarValue, VarValue> pair: changeType.getWrongVariableList()) {
						VarValue var = (this.isOnBefore) ? pair.first() : pair.second();
						if(var.getVarID().equals(varValue.getVarID())) {
							return Settings.imageUI.getImage(ImageUI.QUESTION_MARK);
						}
					}
				}
			}
			
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof VarValue) {
				VarValue varValue = (VarValue) element;
				switch (columnIndex) {
				case 0:
					String type = varValue.getType();
					if (type.contains(".")) {
						type = type.substring(type.lastIndexOf(".") + 1, type.length());
					}
					return type;
				case 1:
					String name = varValue.getVarName();
					if (varValue instanceof VirtualValue) {
						String methodName = name.substring(name.indexOf(":") + 1);
						name = "return from " + methodName + "()";
					}
					return name;
				case 2:
					String value = varValue.getStringValue();
					
					return value;
				case 3:
					String id = varValue.getVarID();
					String aliasVarID = varValue.getAliasVarID();
					if(aliasVarID != null){
						return id + (" aliasID:" + aliasVarID);
					}
					return id;
				}
			}

			return null;
		}

	}

	//private CheckboxTreeViewer stateTreeViewer;
	private CheckboxTreeViewer writtenVariableTreeViewer;
	private CheckboxTreeViewer readVariableTreeViewer;

	private ITreeViewerListener treeListener;
	private TregressionTraceView traceView;
	private boolean isOnBefore;
	
	public StepDetailUI(TregressionTraceView view, TraceNode node, boolean isOnBefore){
		this.traceView = view;
		this.currentNode = node;
		this.isOnBefore = isOnBefore;
	}

	private void addListener() {

		treeListener = new ITreeViewerListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {

				setChecks(readVariableTreeViewer, RW);
				setChecks(writtenVariableTreeViewer, RW);
				//setChecks(stateTreeViewer, STATE);

				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						readVariableTreeViewer.refresh();
						writtenVariableTreeViewer.refresh();
						//stateTreeViewer.refresh();
					}
				});

			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {

			}
		};

		this.readVariableTreeViewer.addTreeListener(treeListener);
		this.writtenVariableTreeViewer.addTreeListener(treeListener);
		//this.stateTreeViewer.addTreeListener(treeListener);

		this.writtenVariableTreeViewer.addCheckStateListener(new RWVarListener(Variable.WRITTEN));
		this.readVariableTreeViewer.addCheckStateListener(new RWVarListener(Variable.READ));
		//this.stateTreeViewer.addCheckStateListener(new RWVarListener(Variable.READ));

	}
	
	private CheckboxTreeViewer createVarGroup(Composite variableForm, String groupName) {
		Group varGroup = new Group(variableForm, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.minimumHeight = 100;
		varGroup.setLayoutData(data);
		
		varGroup.setText(groupName);
		varGroup.setLayout(new FillLayout());

		Tree tree = new Tree(varGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.CHECK);		
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		TreeColumn typeColumn = new TreeColumn(tree, SWT.LEFT);
		typeColumn.setAlignment(SWT.LEFT);
		typeColumn.setText("Variable Type");
		typeColumn.setWidth(100);
		
		TreeColumn nameColumn = new TreeColumn(tree, SWT.LEFT);
		nameColumn.setAlignment(SWT.LEFT);
		nameColumn.setText("Variable Name");
		nameColumn.setWidth(100);
		
		TreeColumn valueColumn = new TreeColumn(tree, SWT.LEFT);
		valueColumn.setAlignment(SWT.LEFT);
		valueColumn.setText("Variable Value");
		valueColumn.setWidth(150);
		
		TreeColumn idColumn = new TreeColumn(tree, SWT.LEFT);
		idColumn.setAlignment(SWT.LEFT);
		idColumn.setText("ID");
		idColumn.setWidth(200);

		CheckboxTreeViewer viewer = new CheckboxTreeViewer(tree);
		createContextMenu(viewer);
		return viewer;
	}
	
	/**
	 * Creates the context menu
	 *
	 * @param viewer
	 */
	protected void createContextMenu(final Viewer viewer) {
	    MenuManager contextMenu = new MenuManager("#ViewerMenu"); //$NON-NLS-1$
	    contextMenu.setRemoveAllWhenShown(true);
	    contextMenu.addMenuListener(new IMenuListener() {
	        @Override
	        public void menuAboutToShow(IMenuManager mgr) {
	            fillContextMenu(mgr, viewer);
	        }
	    });

	    Menu menu = contextMenu.createContextMenu(viewer.getControl());
	    viewer.getControl().setMenu(menu);
	}

	/**
	 * Fill dynamic context menu
	 *
	 * @param contextMenu
	 */
	protected void fillContextMenu(IMenuManager contextMenu, final Viewer viewer) {
	    contextMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

	    contextMenu.add(new Action("Search next step reading this variable") {
	        @Override
	        public void run() {
	        	IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
	        	Object obj = selection.getFirstElement();
	        	if(obj instanceof VarValue){
	        		VarValue value = (VarValue)obj;
	        		Trace trace = traceView.getTrace();
	        		List<TraceNode> nodes = trace.findNextReadingTraceNodes(value, currentNode.getOrder());
	        		if(!nodes.isEmpty()){
	        			TraceNode n = nodes.get(0);
	        			traceView.jumpToNode(trace, n.getOrder(), true);
	        		}
//	        		traceView.jumpToNode(expression, true);
	        	}
	        }
	    });
	    
	    contextMenu.add(new Action("Search previous step reading this variable") {
	        @Override
	        public void run() {
	        	IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
	        	Object obj = selection.getFirstElement();
	        	if(obj instanceof VarValue){
	        		VarValue value = (VarValue)obj;
	        		Trace trace = traceView.getTrace();
	        		List<TraceNode> nodes = trace.findPrevReadingTraceNodes(value, currentNode.getOrder());
	        		if(!nodes.isEmpty()){
	        			TraceNode n = nodes.get(0);
	        			traceView.jumpToNode(trace, n.getOrder(), true);
	        		}
	        	}
	        }
	    });
	    
	}

	public Composite createDetails(Composite panel) {
		Composite comp = new Composite(panel, SWT.NONE);
		
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(1, true));
		
		createSlicingGroup(comp);
		
		SashForm sashForm = new SashForm(comp, SWT.VERTICAL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(data);
		
		this.readVariableTreeViewer = createVarGroup(sashForm, "Read Variables: ");
		this.writtenVariableTreeViewer = createVarGroup(sashForm, "Written Variables: ");
		//this.stateTreeViewer = createVarGroup(comp, "States: ");
		
		sashForm.setWeights(new int[]{50, 50});
		
		
		
		addListener();
		
		return comp;
	}
	
	private Button dataButton;
	private Button controlButton;
	
	
	
	private void createSlicingGroup(Composite panel) {
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
	}
	
	private void createWrittenVariableContent(List<VarValue> writtenVariables, StepChangeType changeType) {
		this.writtenVariableTreeViewer.setContentProvider(new RWVariableContentProvider(false));
		this.writtenVariableTreeViewer.setLabelProvider(new VariableLabelProvider(changeType, isOnBefore));
		this.writtenVariableTreeViewer.setInput(writtenVariables);	
		
		setChecks(this.writtenVariableTreeViewer, RW);

		this.writtenVariableTreeViewer.refresh(true);
		
	}

	private void createReadVariableContect(List<VarValue> readVariables, StepChangeType changeType) {
		this.readVariableTreeViewer.setContentProvider(new RWVariableContentProvider(true));
		this.readVariableTreeViewer.setLabelProvider(new VariableLabelProvider(changeType, isOnBefore));
		this.readVariableTreeViewer.setInput(readVariables);	
		
		setChecks(this.readVariableTreeViewer, RW);

		this.readVariableTreeViewer.refresh(true);
	}

//	private void createStateContent(BreakPointValue value){
//		this.stateTreeViewer.setContentProvider(new VariableContentProvider());
//		this.stateTreeViewer.setLabelProvider(new VariableLabelProvider());
//		this.stateTreeViewer.setInput(value);	
//		
//		setChecks(this.stateTreeViewer, STATE);
//
//		this.stateTreeViewer.refresh(true);
//	}
	
	private void setChecks(CheckboxTreeViewer treeViewer, String type){
		Tree tree = treeViewer.getTree();
		for(TreeItem item: tree.getItems()){
			setChecks(item, type);
		}
	}
	
	private void setChecks(TreeItem item, String type){
		Object element = item.getData();
		if(element == null){
			return;
		}
		
		VarValue ev = (VarValue)element;
//		String varID = ev.getVarID();
		
		if(interestedVariables.contains(ev)){
			item.setChecked(true);
		}
		else{
			item.setChecked(false);
		}

		for(TreeItem childItem: item.getItems()){
			setChecks(childItem, type);
		}
	}
	
	private TraceNode currentNode;

	private void sortVars(List<VarValue> vars){
		List<VarValue> readVars = vars;
		Collections.sort(readVars, new Comparator<VarValue>() {
			@Override
			public int compare(VarValue o1, VarValue o2) {
				return o1.getVarName().compareTo(o2.getVarName());
			}
		});
	}
	
	public void refresh(TraceNode node, StepChangeType changeType){
		this.currentNode = node;
		
		if(node != null){
			//BreakPointValue thisState = node.getProgramState();
			//createStateContent(thisState);
			sortVars(node.getWrittenVariables());
			sortVars(node.getReadVariables());
			
			createWrittenVariableContent(node.getWrittenVariables(), changeType);
			createReadVariableContect(node.getReadVariables(), changeType);	
		}
		else{
			//createStateContent(null);
			createWrittenVariableContent(null, changeType);
			createReadVariableContect(null, changeType);	
		}
		
		this.controlButton.setSelection(false);
		this.dataButton.setSelection(false);
		if(changeType.getType()==StepChangeType.CTL){
			this.controlButton.setSelection(true);
		}
		else if(changeType.getType()==StepChangeType.DAT){
			this.dataButton.setSelection(true);
		}
		
	}

}
