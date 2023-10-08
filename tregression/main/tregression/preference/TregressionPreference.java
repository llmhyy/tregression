package tregression.preference;

import java.util.stream.Stream;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import microbat.Activator;
import tregression.auto.AutoSimulationMethod;

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
	
	/* Simulator Setting*/
	protected Text inputFolderText;
	protected Text outputFolderText;
	protected Text mistakeProbabilityText;
	protected Combo simulateMethodTypeCombo;
	protected AutoSimulationMethod autoSimulationMethod = AutoSimulationMethod.DEBUG_PILOT;
	protected Text timeLimitText;
	
//	private String defaultBuggyProjectPath;
//	private String defaultCorrectProjectPath;
	
	private String defaultProjectPath;
	private String defaultProjectName;
	private String defaultBugID;
	private String defaultTestCase;
	private String defaultDefects4jFile;
	
	protected String defaultInputFolder;
	protected String defaultOutputPath;
	protected double defaultMistakeProbability;
	protected double defaultTimeLimit;
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
	
	public static final String INPUT_FOLDER_KEY = "input_folder_key";
	public static final String OUTPUT_PATH_KEY = "output_path_key";
	public static final String MISTAKE_PROBABILITY_KEY = "mistake_probability_key";
	public static final String AUTO_SIMULATION_METHOD_KEY = "auto_simulation_method_key";
	public static final String TIME_LIMIT_KEY = "time_limit_key";
	
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
		
		this.defaultInputFolder = Activator.getDefault().getPreferenceStore().getString(INPUT_FOLDER_KEY);
		this.defaultOutputPath = Activator.getDefault().getPreferenceStore().getString(OUTPUT_PATH_KEY);
		
		String mistakeProbStr = Activator.getDefault().getPreferenceStore().getString(MISTAKE_PROBABILITY_KEY);
		this.defaultMistakeProbability = mistakeProbStr == null || mistakeProbStr.isEmpty() ? 0.0d : Double.valueOf(mistakeProbStr);
		
		String timeLimitStr = Activator.getDefault().getPreferenceStore().getString(MISTAKE_PROBABILITY_KEY);
		this.defaultTimeLimit = timeLimitStr == null || timeLimitStr.isEmpty() ? 120.0d : Double.valueOf(timeLimitStr);
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
		this.createSimulationSettingGroup(compo);
		return compo;
	}

	@Override
	public boolean performOk(){
		this.autoSimulationMethod = AutoSimulationMethod.valueOf(this.simulateMethodTypeCombo.getText());
		
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("tregression.preference");
		preferences.put(REPO_PATH, this.projectPathText.getText());
		preferences.put(PROJECT_NAME, this.projectNameText.getText());
		preferences.put(BUG_ID, this.bugIDText.getText());
		preferences.put(TEST_CASE, this.testCaseText.getText());
		preferences.put(DEFECTS4J_FILE, this.defects4jFileText.getText());
		preferences.put(INPUT_FOLDER_KEY, this.inputFolderText.getText());
		preferences.put(OUTPUT_PATH_KEY, this.outputFolderText.getText());
		preferences.put(MISTAKE_PROBABILITY_KEY, this.mistakeProbabilityText.getText());
		preferences.put(AUTO_SIMULATION_METHOD_KEY, this.autoSimulationMethod.name());
		preferences.put(TIME_LIMIT_KEY, this.timeLimitText.getText());
	
		
		Activator.getDefault().getPreferenceStore().putValue(REPO_PATH, this.projectPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(PROJECT_NAME, this.projectNameText.getText());
		Activator.getDefault().getPreferenceStore().putValue(BUG_ID, this.bugIDText.getText());
		Activator.getDefault().getPreferenceStore().putValue(TEST_CASE, this.testCaseText.getText());
		Activator.getDefault().getPreferenceStore().putValue(DEFECTS4J_FILE, this.defects4jFileText.getText());
		Activator.getDefault().getPreferenceStore().putValue(INPUT_FOLDER_KEY, this.inputFolderText.getText());
		Activator.getDefault().getPreferenceStore().putValue(OUTPUT_PATH_KEY, this.outputFolderText.getText());
		Activator.getDefault().getPreferenceStore().putValue(MISTAKE_PROBABILITY_KEY, this.mistakeProbabilityText.getText());
		Activator.getDefault().getPreferenceStore().putValue(AUTO_SIMULATION_METHOD_KEY, this.autoSimulationMethod.name());
		Activator.getDefault().getPreferenceStore().putValue(TIME_LIMIT_KEY, this.timeLimitText.getText());
		
		return true;
	}
	
	protected void createSimulationSettingGroup(Composite parent) {
		Group simulationGroup = new Group(parent, SWT.NONE);
		simulationGroup.setText("Simulation Setting");
		
		GridData simulationData = new GridData(SWT.FILL, SWT.FILL, true, true);
		simulationData.horizontalSpan = 2;
		simulationGroup.setLayoutData(simulationData);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		simulationGroup.setLayout(layout);
		
		Label inputFolderLabel = new Label(simulationGroup, SWT.NONE);
		inputFolderLabel.setText("Input Folder: ");
		this.inputFolderText = new Text(simulationGroup, SWT.NONE);
		this.inputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		this.inputFolderText.setText(this.defaultInputFolder);
		
		Label outputPathLabel = new Label(simulationGroup, SWT.NONE);
		outputPathLabel.setText("Output Folder: ");
		this.outputFolderText = new Text(simulationGroup, SWT.NONE);
		this.outputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		this.outputFolderText.setText(this.defaultOutputPath);
		
		Label mistakeProbabilityLabel = new Label(simulationGroup, SWT.NONE);
		mistakeProbabilityLabel.setText("Mistake Probability: ");
		this.mistakeProbabilityText = new Text(simulationGroup, SWT.NONE);
		this.mistakeProbabilityText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		this.mistakeProbabilityText.setText(String.valueOf(this.defaultMistakeProbability));
		
		
		Label timeLimitLabel = new Label(simulationGroup, SWT.NONE);
		timeLimitLabel.setText("Time Limit (Mins): ");
		this.timeLimitText = new Text(simulationGroup, SWT.NONE);
		this.timeLimitText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		this.timeLimitText.setText(String.valueOf(this.defaultTimeLimit));
		
		Label simulationMethodLabel = new Label(simulationGroup, SWT.NONE);
		simulationMethodLabel.setText("Testing Approach: ");
		final String[] autoSimulationMethodNames = Stream.of(AutoSimulationMethod.values()).map(Enum::name).toArray(String[]::new);
		this.simulateMethodTypeCombo = new Combo(simulationGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		this.simulateMethodTypeCombo.setItems(autoSimulationMethodNames);
		this.simulateMethodTypeCombo.select(this.autoSimulationMethod.ordinal());
		
		
	}
	
}
