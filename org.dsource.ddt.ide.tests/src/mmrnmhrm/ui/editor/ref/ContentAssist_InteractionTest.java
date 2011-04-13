package mmrnmhrm.ui.editor.ref;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;
import mmrnmhrm.tests.SampleMainProject;
import mmrnmhrm.tests.ui.SWTTestUtils;

import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.junit.Test;

public class ContentAssist_InteractionTest extends ContentAssistUI_CommonTest {
	
	public ContentAssist_InteractionTest() {
		super(SampleMainProject.getFile("src-ca/testCodeCompletion.d"));
	}
	
	@Test
	public void testMoveCursorBeforeStartOffset() throws Exception { testMoveCursorBeforeStartOffset$(); }
	@SuppressWarnings("unused")
	public void testMoveCursorBeforeStartOffset$() throws Exception {
		if(true) 
			return; // This test is disable because this functionality was removed, not even JDT has it
		
		ISourceViewer viewer = editor.getViewer();
		
		int ccOffset = getMarkerStartPos("/+@CC.I+/");
		viewer.setSelectedRange(ccOffset, 0);
		assertTrue(viewer.getSelectedRange().x == ccOffset);
		
		ContentAssistant ca = getContentAssistant(editor);
		CompletionWatcher caWatcher = new CompletionWatcher();
		ca.addCompletionListener(caWatcher);
		
		viewer.revealRange(ccOffset, 10);
		invokeContentAssist();
		assertTrue(caWatcher.active == true);
		
		simulateCursorLeft(); // at start of defunit
		SWTTestUtils.________________flushUIEventQueue________________();
		assertTrue(viewer.getSelectedRange().x == ccOffset - 1);
		assertTrue(caWatcher.active == true);
		
		
		simulateCursorLeft(); // before defunit
		SWTTestUtils.________________flushUIEventQueue________________();
		assertTrue(viewer.getSelectedRange().x == ccOffset - 2);
		
		assertTrue(caWatcher.active == false); // Assert content Assist closed
	}
	
	private void simulateCursorLeft() {
		// TODO: we should use SWTbot instead
		Event event = new Event();
		event.character = 0;
		event.keyCode = SWT.ARROW_LEFT;
		event.type = SWT.KeyDown;
		editor.getViewer().getTextWidget().notifyListeners(SWT.KeyDown, event);
		event.type = SWT.KeyUp;
		editor.getViewer().getTextWidget().notifyListeners(SWT.KeyUp, event);
	}
	
	protected class CompletionWatcher implements ICompletionListener {
		protected boolean active = false;
		protected ContentAssistEvent lastEvent;
		@Override
		public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
		}
		
		@Override
		public void assistSessionStarted(ContentAssistEvent event) {
			active = true;
			lastEvent = event;
		}
		
		@Override
		public void assistSessionEnded(ContentAssistEvent event) {
			active = false;
		}
	}
	
}