package tregression.editors;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import microbat.model.trace.TraceNode;
import tregression.model.PairList;
import tregression.model.TraceNodePair;
import tregression.separatesnapshots.DiffMatcher;
import tregression.separatesnapshots.diff.DiffChunk;
import tregression.separatesnapshots.diff.FileDiff;
import tregression.separatesnapshots.diff.LineChange;

public class CompareEditor extends EditorPart {

	public static final String ID = "tregression.editor.compare";
	
	private CompareTextEditorInput input;
	
	private StyledText sourceText;
	private StyledText targetText;
	
	public CompareEditor() {
	}

	@Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof CompareTextEditorInput)) {
            throw new RuntimeException("Wrong input");
        }

        this.input = (CompareTextEditorInput) input;
        
        
//		TextFileDocumentProvider provider = new TextFileDocumentProvider();
//		setDocumentProvider(provider);
		
//      super.init(site, input);
        
        setSite(site);
        setInput(input);
        setPartName("Compare");
    }
	
	

	@Override
	public void createPartControl(Composite parent) {
		GridLayout parentLayout = new GridLayout();
		parentLayout.numColumns = 1;
		parent.setLayout(parentLayout);
		
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(data);
		
//		GridLayout sashLayout = new GridLayout(2, true);
//		sashForm.setLayoutData(sashLayout);
		
		sourceText = generateText(sashForm, input.getSourceFilePath(), input.getMatcher(), true);
		targetText = generateText(sashForm, input.getTargetFilePath(), input.getMatcher(), false);
		
		sashForm.setWeights(new int[]{50, 50});
	}

	public void highLight(TraceNode node) {
		input.setSelectedNode(node);
		
		
		highlightStyles(true, sourceText, input.getMatcher(), input.getSourceFilePath());
		highlightStyles(false, targetText, input.getMatcher(), input.getTargetFilePath());
		
		sourceText.redraw();
		targetText.redraw();
	}
	
	
	private void highlightStyles(boolean isSource, StyledText text, DiffMatcher matcher, String path){
		List<StyleRange> ranges = new ArrayList<>();
		
		PairList list = input.getPairList();
		TraceNode node = input.getSelectedNode();
		if(isSource){
			ranges = highlightSourceDiff(text, matcher, path);
			int topLine = node.getLineNumber()-15;
			topLine = (topLine<1) ? 1 : topLine;
			text.setTopIndex(topLine);
			
			StyleRange selectedRange = selectedLineStyle(text, node.getLineNumber()); 
			ranges.add(selectedRange);
		}
		else{
			ranges = highlightTargetDiff(text, matcher, path);		
			TraceNodePair pair = list.findByMutatedNode(node);
			if(pair != null){
				TraceNode correctNode = pair.getOriginalNode();
				int topLine = correctNode.getLineNumber()-15;
				topLine = (topLine<1) ? 1 : topLine;
				text.setTopIndex(topLine);
				
				StyleRange selectedRange = selectedLineStyle(text, correctNode.getLineNumber()); 
				ranges.add(selectedRange);
			}
			else{
				
			}
		}
		
		StyleRange[] rangeArray = sortList(ranges);
		appendLinStyle(text, rangeArray);
	}
	
	public StyledText generateText(SashForm sashForm, String path, DiffMatcher matcher, boolean isSource){
		final StyledText text = new StyledText(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);
		
		File file = new File(path);
		if(!file.exists()){
			path = path.replace(matcher.getSourceFolderName(), matcher.getTestFolderName());
			file = new File(path);
		}
		
		String content = parseSourceContent(file);
		text.setText(content);
		
		highlightStyles(isSource, text, matcher, path);
		
		return text;
	}
	
	/**
	 * Bubble sort
	 * @return
	 */
	private StyleRange[] sortList(List<StyleRange> ranges){
		StyleRange[] rangeArray = ranges.toArray(new StyleRange[0]);
		for(int i=0; i<rangeArray.length; i++){
			for(int j=1; j<rangeArray.length-i; j++){
				int prev = rangeArray[j-1].start;
				int post = rangeArray[j].start;
				if(prev > post){
					StyleRange temp = rangeArray[j];
					rangeArray[j] = rangeArray[j-1];
					rangeArray[j-1] = temp;
				}
			}
		}
		
		System.currentTimeMillis();
		
		return rangeArray;
	}
	
	private StyleRange selectedLineStyle(StyledText text, int line){
		StyleRange range = new StyleRange();
		range.start = text.getOffsetAtLine(line-1);
		range.length = text.getOffsetAtLine(line)-range.start;
		range.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
		range.background = new Color(Display.getCurrent(), 230, 255, 255);
		
		return range;
	}

	private List<StyleRange> highlightSourceDiff(StyledText text, DiffMatcher matcher, String path) {
		
		List<StyleRange> ranges = new ArrayList<>();
		
		FileDiff diff = matcher.findDiffBySourceFile(path);
		
		if(diff==null){
			return ranges;
		}
		
		for(DiffChunk chunk: diff.getChunks()){
			int currentLine = chunk.getStartLineInSource();
			for(LineChange line: chunk.getChangeList()){
				if(line.getLineContent().startsWith("-")){
					StyleRange range = new StyleRange();
					range.start = text.getOffsetAtLine(currentLine-1);
					String content = line.getLineContent();
					range.length = content.length();
					range.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
					range.background = new Color(Display.getCurrent(), 255, 247, 248);
					
					ranges.add(range);
				}
				
				if(!line.getLineContent().startsWith("+")){
					currentLine++;					
				}
			}
		}
		
		return ranges;
		
	}
	
	private List<StyleRange> highlightTargetDiff(StyledText text, DiffMatcher matcher, String path) {
		List<StyleRange> ranges = new ArrayList<>();
		FileDiff diff = matcher.findDiffByTargetFile(path);
		
		if(diff==null){
			return ranges;
		}
		
		for(DiffChunk chunk: diff.getChunks()){
			int currentLine = chunk.getStartLineInSource();
			for(LineChange line: chunk.getChangeList()){
				if(line.getLineContent().startsWith("+")){
					StyleRange range = new StyleRange();
					range.start = text.getOffsetAtLine(currentLine-1);
					String content = line.getLineContent();
					range.length = content.length();
					range.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
					range.background = new Color(Display.getCurrent(), 255, 247, 248);
					
					ranges.add(range);
				}
				
				if(!line.getLineContent().startsWith("-")){
					currentLine++;					
				}
			}
		}
		
		return ranges;
		
	}

	class CusLineStyleListener implements LineStyleListener{

		private StyledText text;
		private StyleRange[] ranges;
		
		
		public CusLineStyleListener(StyledText text, StyleRange[] ranges) {
			super();
			this.text = text;
			this.ranges = ranges;
		}

		@Override
		public void lineGetStyle(LineStyleEvent e) {
			e.bulletIndex = text.getLineAtOffset(e.lineOffset);
	        e.styles = ranges;
	        
	        //Set the style, 12 pixles wide for each digit
	        StyleRange style = new StyleRange();
	        style.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
	        style.metrics = new GlyphMetrics(0, 0, Integer.toString(text.getLineCount()+1).length()*12);

	        //Create and set the bullet
	        e.bullet = new Bullet(ST.BULLET_NUMBER, style);
			
		}
		
	}
	
	private CusLineStyleListener styleListener;
	
	private void appendLinStyle(StyledText text, StyleRange[] ranges) {
		
		if(this.styleListener != null){
			text.removeLineStyleListener(this.styleListener);	
		}
		
		this.styleListener = new CusLineStyleListener(text, ranges);
		text.addLineStyleListener(this.styleListener);
	}

	@SuppressWarnings("resource")
	private String parseSourceContent(File file) {
		String content = "";
		if(file.exists()){
			InputStream stdin;
			try {
				stdin = new FileInputStream(file);
				InputStreamReader isr = new InputStreamReader(stdin);
				BufferedReader br = new BufferedReader(isr);
				
				StringBuffer buffer = new StringBuffer();
				String line = null;
				while ( (line = br.readLine()) != null){
					buffer.append(line);
					buffer.append('\n');
				}
				
				content = buffer.toString();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return content;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void setFocus() {
		
	}

}
