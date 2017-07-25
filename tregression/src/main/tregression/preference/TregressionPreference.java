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

	private Text buggyProjectPathText;
	private Text correctProjectPathText;
	
	private String defaultBuggyProjectPath;
	private String defaultCorrectProjectPath;
	
	public static final String BUGGY_PATH = "buggy_path";
	public static final String CORRECT_PATH = "correct_path";
	
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
		this.defaultBuggyProjectPath = Activator.getDefault().getPreferenceStore().getString(BUGGY_PATH);
		this.defaultCorrectProjectPath = Activator.getDefault().getPreferenceStore().getString(CORRECT_PATH);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite compo = new Composite(parent, SWT.NONE);
		compo.setLayout(new GridLayout(2, false));
		
		Label buggyLabel = new Label(compo, SWT.NONE);
		buggyLabel.setText("Buggy Path: ");
		buggyProjectPathText = new Text(compo, SWT.NONE);
		buggyProjectPathText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		buggyProjectPathText.setText(this.defaultBuggyProjectPath);
		
		Label correctLabel = new Label(compo, SWT.NONE);
		correctLabel.setText("Correct Path: ");
		correctProjectPathText = new Text(compo, SWT.NONE);
		correctProjectPathText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		correctProjectPathText.setText(this.defaultCorrectProjectPath);
		
		return compo;
	}

	@Override
	public boolean performOk(){
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("tregression.preference");
		preferences.put(BUGGY_PATH, this.buggyProjectPathText.getText());
		preferences.put(CORRECT_PATH, this.correctProjectPathText.getText());
		
		Activator.getDefault().getPreferenceStore().putValue(BUGGY_PATH, this.buggyProjectPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(CORRECT_PATH, this.correctProjectPathText.getText());
		
		return true;
	}
}
