package mmrnmhrm.ui.editor.codeassist;

import org.dsource.ddt.ide.core.DeeNature;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProcessor;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.IEditorPart;

public class DeeCompletionProcessor extends ScriptCompletionProcessor {
	
	public DeeCompletionProcessor(IEditorPart editor, ContentAssistant assistant, String partition) {
		super(editor, assistant, partition);
		setCompletionProposalAutoActivationCharacters(new char[] { '.' });
	}
	
	@Override
	protected String getNatureId() {
		return DeeNature.NATURE_ID;
	}
	
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] { '(' };
	}
	
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return super.getContextInformationValidator();
	}
	
//	protected CompletionProposalLabelProvider getProposalLabelProvider() {
//		return new CompletionProposalLabelProvider();
//	}
	
//	protected IPreferenceStore getPreferenceStore() {
//		return PythonCorePlugin.getDefault().getPreferenceStore();
//	}
	
}