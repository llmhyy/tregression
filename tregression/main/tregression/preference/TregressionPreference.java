package tregression.preference;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import microbat.Activator;
import tregression.autofeedback.AutoFeedbackMethod;

public class TregressionPreference extends PreferencePage implements IWorkbenchPreferencePage {

//	private Text buggyProjectPathText;
//	private Text correctProjectPathText;
	
	private Text projectPathText;
	private Text projectNameText;
	private Text bugIDText;
	private Text testCaseText;
	private Text defects4jFileText;
//	private Combo autoFeedbackCombo;
//	private Button manualFeedbackButton;
//	private Button useTestCaseIDButton;
//	private Text testCaseIDText;
//	private Text seedText;
//	private Text dropInFolderText;
//	private Text configPathText;

	
//	private String defaultBuggyProjectPath;
//	private String defaultCorrectProjectPath;
	
	private String defaultProjectPath;
	private String defaultProjectName;
	private String defaultBugID;
	private String defaultTestCase;
	private String defaultDefects4jFile;
//	private String defaultManualFeedback;
//	private String defaultUseTestCaseID;
//	private String defaultTestCaesID;
	
//	private String defaultSeed;
//	private String defaultDropInFolder;
//	private String defaultConfigPath;
	
//	public static final String BUGGY_PATH = "buggy_path";
//	public static final String CORRECT_PATH = "correct_path";
	
	public static final String REPO_PATH = "project_path";
	public static final String PROJECT_NAME = "project_name";
	public static final String BUG_ID = "bug_id";
	public static final String TEST_CASE = "test_case";
	public static final String DEFECTS4J_FILE = "defects4j_file";
	public static final String AUTO_FEEDBACK_METHOD = "autoFeedbackMethod";
//	public static final String MANUAL_FEEDBACK = "manualFeedback";
//	public static final String TEST_CASE_ID = "testCaseID";
//	public static final String USE_TEST_CASE_ID = "useTestCaseID";
	
//	public static final String SEED = "seed";
//	public static final String DROP_IN_FOLDER = "dropInFolder";
//	public static final String CONFIG_PATH = "configPath";

//	private int defaultAutoFeedbackMethod = AutoFeedbackMethod.RANDOM.ordinal();
	
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
//		this.defaultManualFeedback = Activator.getDefault().getPreferenceStore().getString(MANUAL_FEEDBACK);
//		this.defaultUseTestCaseID = Activator.getDefault().getPreferenceStore().getString(USE_TEST_CASE_ID);
//		this.defaultTestCaesID = Activator.getDefault().getPreferenceStore().getString(TEST_CASE_ID);
		
//		this.defaultSeed = Activator.getDefault().getPreferenceStore().getString(SEED);
//		this.defaultDropInFolder = Activator.getDefault().getPreferenceStore().getString(DROP_IN_FOLDER);
//		this.defaultConfigPath = Activator.getDefault().getPreferenceStore().getString(CONFIG_PATH);
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

//		this.createAutoFeedbackSettingGroup(compo);
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
//		preferences.put(AUTO_FEEDBACK_METHOD, this.autoFeedbackCombo.getText());
//		preferences.put(MANUAL_FEEDBACK, String.valueOf(this.manualFeedbackButton.getSelection()));
//		preferences.put(USE_TEST_CASE_ID, String.valueOf(this.useTestCaseIDButton.getSelection()));
//		preferences.put(TEST_CASE_ID, this.testCaseIDText.getText());
//		preferences.put(SEED, this.seedText.getText());
//		preferences.put(DROP_IN_FOLDER, this.dropInFolderText.getText());
//		preferences.put(CONFIG_PATH, this.configPathText.getText());
		
		Activator.getDefault().getPreferenceStore().putValue(REPO_PATH, this.projectPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(PROJECT_NAME, this.projectNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(BUG_ID, this.bugIDText.getText());
		Activator.getDefault().getPreferenceStore().putValue(TEST_CASE, this.testCaseText.getText());
		Activator.getDefault().getPreferenceStore().putValue(DEFECTS4J_FILE, this.defects4jFileText.getText());
//		Activator.getDefault().getPreferenceStore().putValue(AUTO_FEEDBACK_METHOD, this.autoFeedbackCombo.getText());
//		Activator.getDefault().getPreferenceStore().putValue(MANUAL_FEEDBACK, String.valueOf(this.manualFeedbackButton.getSelection()));
//		Activator.getDefault().getPreferenceStore().putValue(USE_TEST_CASE_ID, String.valueOf(this.useTestCaseIDButton.getSelection()));
//		Activator.getDefault().getPreferenceStore().putValue(TEST_CASE_ID, this.testCaseIDText.getText());
//		Activator.getDefault().getPreferenceStore().putValue(SEED, this.seedText.getText());
//		Activator.getDefault().getPreferenceStore().putValue(DROP_IN_FOLDER, this.dropInFolderText.getText());
//		Activator.getDefault().getPreferenceStore().putValue(CONFIG_PATH, this.configPathText.getText());

		
		return true;
	}
	
	private void createAutoFeedbackSettingGroup(Composite parent) {
//		Group autoFeedbackGroup = new Group(parent, SWT.NONE);
//		autoFeedbackGroup.setText("Auto Feedback Methods");
//		
//		GridData autoFeedbackGroupData = new GridData(SWT.FILL, SWT.FILL, true, true);
//		autoFeedbackGroupData.horizontalSpan = 3;
//		autoFeedbackGroup.setLayoutData(autoFeedbackGroupData);
//		
//		GridLayout layout = new GridLayout();
//		layout.numColumns = 2;
//		
//		autoFeedbackGroup.setLayout(layout);
//		
//		Label methodLabel = new Label(autoFeedbackGroup, SWT.NONE);
//		methodLabel.setText("Method: ");
//		
//		AutoFeedbackMethod[] methods = AutoFeedbackMethod.values();
//		String[] methodsName = new String[methods.length];
//		for(int i=0; i<methods.length; i++) {
//			methodsName[i] = methods[i].name();
//		}
//		this.autoFeedbackCombo = new Combo(autoFeedbackGroup, SWT.DROP_DOWN);
//		this.autoFeedbackCombo.setItems(methodsName);
//		this.autoFeedbackCombo.select(this.defaultAutoFeedbackMethod);
//		
//		this.manualFeedbackButton = new Button(autoFeedbackGroup, SWT.CHECK);
//		this.manualFeedbackButton.setText("Given feedback manually");
//		GridData manualFeedbackData = new GridData(SWT.FILL, SWT.FILL, true, false);
//		manualFeedbackData.horizontalSpan = 3;
//		this.manualFeedbackButton.setLayoutData(manualFeedbackData);
//		boolean manualFeedbackButtonSelected = this.defaultManualFeedback.equals("true");
//		this.manualFeedbackButton.setSelection(manualFeedbackButtonSelected);
//		
//		this.useTestCaseIDButton = new Button(autoFeedbackGroup, SWT.CHECK);
//		this.useTestCaseIDButton.setText("Use Test Case ID");
//		GridData useTestCaseIDData = new GridData(SWT.FILL, SWT.FILL, true, false);
//		useTestCaseIDData.horizontalSpan = 3;
//		this.useTestCaseIDButton.setLayoutData(useTestCaseIDData);
//		boolean useTestCaseIDDataSelected = this.defaultUseTestCaseID.equals("true");
//		this.useTestCaseIDButton.setSelection(useTestCaseIDDataSelected);
//		
//		Label testCaseIDLabel = new Label(autoFeedbackGroup, SWT.NONE);
//		testCaseIDLabel.setText("Test Case ID");
//		this.testCaseIDText = new Text(autoFeedbackGroup, SWT.NONE);
//		this.testCaseIDText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
//		this.testCaseIDText.setText(this.defaultTestCaesID);
		
//		Label seedLabel = new Label(autoFeedbackGroup, SWT.NONE);
//		seedLabel.setText("Seed: ");
//		this.seedText = new Text(autoFeedbackGroup, SWT.NONE);
//		this.seedText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
//		this.seedText.setText(this.defaultSeed);
//		
//		Label dropInFolderLabel = new Label(autoFeedbackGroup, SWT.NONE);
//		dropInFolderLabel.setText("Drop In Folder: ");
//		this.dropInFolderText = new Text(autoFeedbackGroup, SWT.NONE);
//		this.dropInFolderText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
//		this.dropInFolderText.setText(this.defaultDropInFolder);
//		
//		Label configPathLabel = new Label(autoFeedbackGroup, SWT.NONE);
//		configPathLabel.setText("Config Path: ");
//		this.configPathText = new Text(autoFeedbackGroup, SWT.NONE);
//		this.configPathText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
//		this.configPathText.setText(this.defaultConfigPath);
	}
}
