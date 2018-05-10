package tregression.preference;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import microbat.Activator;

public class TregressionPreference extends PreferencePage implements IWorkbenchPreferencePage {

//	private Text buggyProjectPathText;
//	private Text correctProjectPathText;
	
	private Text projectPathText;
	private Text projectNameText;
	private Text bugIDText;
	private Text testCaseText;
	private Text defects4jFileText;
	
//	private String defaultBuggyProjectPath;
//	private String defaultCorrectProjectPath;
	
	private String defaultProjectPath;
	private String defaultProjectName;
	private String defaultBugID;
	private String defaultTestCase;
	private String defaultDefects4jFile;
	
//	public static final String BUGGY_PATH = "buggy_path";
//	public static final String CORRECT_PATH = "correct_path";
	
	public static final String REPO_PATH = "project_path";
	public static final String PROJECT_NAME = "project_name";
	public static final String BUG_ID = "bug_id";
	public static final String TEST_CASE = "test_case";
	public static final String DEFECTS4J_FILE = "defects4j_file";
	
	
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
		this.defaultProjectPath = Activator.getDefault().getPreferenceStore().getString(REPO_PATH);
		this.defaultProjectName = Activator.getDefault().getPreferenceStore().getString(PROJECT_NAME);
		this.defaultBugID = Activator.getDefault().getPreferenceStore().getString(BUG_ID);
		this.defaultTestCase = Activator.getDefault().getPreferenceStore().getString(TEST_CASE);
		this.defaultDefects4jFile = Activator.getDefault().getPreferenceStore().getString(DEFECTS4J_FILE);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite compo = new Composite(parent, SWT.NONE);
		compo.setLayout(new GridLayout(2, false));
		
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
		defects4jFileLabel.setText("Defects4j benchmark: ");
		defects4jFileText = new Text(compo, SWT.NONE);
		defects4jFileText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		defects4jFileText.setText(this.defaultDefects4jFile);
		return compo;
	}

	@Override
	public boolean performOk(){
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("tregression.preference");
		preferences.put(REPO_PATH, this.projectPathText.getText());
		preferences.put(PROJECT_NAME, this.projectNameText.getText());
		preferences.put(BUG_ID, this.bugIDText.getText());
		preferences.put(TEST_CASE, this.testCaseText.getText());
		preferences.put(DEFECTS4J_FILE, this.defects4jFileText.getText());
		
		Activator.getDefault().getPreferenceStore().putValue(REPO_PATH, this.projectPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(PROJECT_NAME, this.projectNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(BUG_ID, this.bugIDText.getText());
		Activator.getDefault().getPreferenceStore().putValue(TEST_CASE, this.testCaseText.getText());
		Activator.getDefault().getPreferenceStore().putValue(DEFECTS4J_FILE, this.defects4jFileText.getText());
		
		return true;
	}
}
