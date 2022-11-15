package tregression.preference;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import microbat.Activator;
import tregression.constants.Dataset;

public class TregressionPreference extends PreferencePage implements IWorkbenchPreferencePage {

//	private Text buggyProjectPathText;
//	private Text correctProjectPathText;
	
	private Combo datasetCombo;
	private Text projectPathText;
	private Text projectNameText;
	private Text bugIDText;
	private Text testCaseText;
	private Text resultsFileText;
	
//	private String defaultBuggyProjectPath;
//	private String defaultCorrectProjectPath;
	
	private String defaultDatasetName;
	private String defaultProjectPath;
	private String defaultProjectName;
	private String defaultBugID;
	private String defaultTestCase;
	private String defaultResultsFile;
	
//	public static final String BUGGY_PATH = "buggy_path";
//	public static final String CORRECT_PATH = "correct_path";

	public static final String DATASET_NAME = "dataset_name";
	public static final String REPO_PATH = "project_path";
	public static final String PROJECT_NAME = "project_name";
	public static final String BUG_ID = "bug_id";
	public static final String TEST_CASE = "test_case";
	public static final String RESULTS_FILE = "result_file";
	
	
	public TregressionPreference() {
	}

	public TregressionPreference(String title) {
		super(title);
	}

	public TregressionPreference(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		this.defaultDatasetName = Activator.getDefault().getPreferenceStore().getString(DATASET_NAME);
		this.defaultProjectPath = Activator.getDefault().getPreferenceStore().getString(REPO_PATH);
		this.defaultProjectName = Activator.getDefault().getPreferenceStore().getString(PROJECT_NAME);
		this.defaultBugID = Activator.getDefault().getPreferenceStore().getString(BUG_ID);
		this.defaultTestCase = Activator.getDefault().getPreferenceStore().getString(TEST_CASE);
		this.defaultResultsFile = Activator.getDefault().getPreferenceStore().getString(RESULTS_FILE);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite compo = new Composite(parent, SWT.NONE);
		compo.setLayout(new GridLayout(2, false));
		
		Label projectLabel = new Label(compo, SWT.NONE);
		projectLabel.setText("Dataset Name: ");
		
		datasetCombo = new Combo(compo, SWT.NONE);
		datasetCombo.setItems(Dataset.DEFECTS4J.getName(), Dataset.REGS4J.getName(), Dataset.MUTATION_FRAMEWORK.getName());
		datasetCombo.setText(this.defaultDatasetName);
		
		GridData comboData = new GridData(SWT.FILL, SWT.LEFT, true, false);
		datasetCombo.setLayoutData(comboData);
		
		Label projectPathLabel = new Label(compo, SWT.NONE);
		projectPathLabel.setText("Repository Path: ");
		projectPathText = new Text(compo, SWT.NONE);
		projectPathText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		projectPathText.setText(this.defaultProjectPath);
		
		Label projectNameLabel = new Label(compo, SWT.NONE);
		projectNameLabel.setText("Project Name: ");
		projectNameText = new Text(compo, SWT.NONE);
		projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		projectNameText.setText(this.defaultProjectName);
		
		Label bugIDLabel = new Label(compo, SWT.NONE);
		bugIDLabel.setText("Bug ID: ");
		bugIDText = new Text(compo, SWT.NONE);
		bugIDText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		bugIDText.setText(this.defaultBugID);
		
		Label testcaseLabel = new Label(compo, SWT.NONE);
		testcaseLabel.setText("Test Case: ");
		testCaseText = new Text(compo, SWT.NONE);
		testCaseText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		testCaseText.setText(this.defaultTestCase);
		
		Label defects4jFileLabel = new Label(compo, SWT.NONE);
		defects4jFileLabel.setText("Dataset benchmark: ");
		resultsFileText = new Text(compo, SWT.NONE);
		resultsFileText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		resultsFileText.setText(this.defaultResultsFile);
		return compo;
	}

	@Override
	public boolean performOk(){
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("tregression.preference");
		preferences.put(DATASET_NAME, this.datasetCombo.getText());
		preferences.put(REPO_PATH, this.projectPathText.getText());
		preferences.put(PROJECT_NAME, this.projectNameText.getText());
		preferences.put(BUG_ID, this.bugIDText.getText());
		preferences.put(TEST_CASE, this.testCaseText.getText());
		preferences.put(RESULTS_FILE, this.resultsFileText.getText());
		
		Activator.getDefault().getPreferenceStore().putValue(DATASET_NAME, this.datasetCombo.getText());
		Activator.getDefault().getPreferenceStore().putValue(REPO_PATH, this.projectPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(PROJECT_NAME, this.projectNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(BUG_ID, this.bugIDText.getText());
		Activator.getDefault().getPreferenceStore().putValue(TEST_CASE, this.testCaseText.getText());
		Activator.getDefault().getPreferenceStore().putValue(RESULTS_FILE, this.resultsFileText.getText());
		
		return true;
	}
}
