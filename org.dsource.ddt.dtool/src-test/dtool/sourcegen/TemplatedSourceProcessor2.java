/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package dtool.sourcegen;

import static dtool.util.NewUtils.assertNotNull_;
import static dtool.util.NewUtils.emptyToNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import melnorme.utilbox.misc.ArrayUtil;
import melnorme.utilbox.misc.StringUtil;
import melnorme.utilbox.misc2.ChainedIterator2;
import melnorme.utilbox.misc2.CopyableListIterator;
import melnorme.utilbox.misc2.ICopyableIterator;
import dtool.sourcegen.AnnotatedSource.MetadataEntry;
import dtool.tests.SimpleParser;

/**
 * Generates multiple source cases from a templated source, using an embedded markup language. 
 */
public class TemplatedSourceProcessor2 {
	
	@SuppressWarnings("serial")
	public class TemplatedSourceException extends Exception {
		public int errorOffset;
		public TemplatedSourceException(int errorOffset) {
			this.errorOffset = errorOffset;
		}
	}
	
	public static AnnotatedSource[] processTemplatedSource(String marker, String source) 
		throws TemplatedSourceException {
		TemplatedSourceProcessor2 tsp = new TemplatedSourceProcessor2();
		return tsp.processSource(marker, source);
	}
	
	protected String kMARKER;
	protected final ArrayList<AnnotatedSource> genCases = new ArrayList<AnnotatedSource>();
	protected final Map<String, TspExpansionElement> globalExpansions = new HashMap<String, TspExpansionElement>();
	protected Map<String, TspExpansionElement> localExpansions = new HashMap<String, TspExpansionElement>();
	
	public TemplatedSourceProcessor2() { }
	
	public ArrayList<AnnotatedSource> getGenCases() {
		return genCases;
	}
	
	public AnnotatedSource[] processSource_unchecked(String keyMARKER, String unprocessedSource) {
		try {
			return processSource(keyMARKER, unprocessedSource);
		} catch(TemplatedSourceException e) {
			throw melnorme.utilbox.core.ExceptionAdapter.unchecked(e);
		}
	}
	
	public AnnotatedSource[] processSource(String keyMARKER, String fileSource) 
		throws TemplatedSourceException {
		this.kMARKER = keyMARKER;
		
		SimpleParser parser = new SimpleParser(fileSource);
		
		final String[] splitKeywords = { "#:HEADER", "#:SPLIT"};
		
		do {
			boolean isHeader = false;
			
			do {
				if(parser.tryConsume(splitKeywords[0])) {
					isHeader = true;
				} else if(parser.tryConsume(splitKeywords[1])) {
				} else {
					assertTrue(parser.getSourcePosition() == 0);
					break;
				}
				if(!parser.seekToNewLine()) {
					reportError(parser.getSourcePosition());
				}
			} while(false);
			
			parser.consumeUntilAny(splitKeywords);
			String unprocessedCaseSource = parser.getLastConsumedString();
			
			processSplitCaseSource(unprocessedCaseSource, isHeader);
		} while(!parser.lookaheadIsEOF());
		
		return ArrayUtil.createFrom(getGenCases(), AnnotatedSource.class);
	}
	
	protected ArrayList<TspElement> parseSource(String unprocessedSource) throws TemplatedSourceException {
		ArrayList<TspElement> unprocessedSourceElements = new ArrayList<TspElement>(); 
		
		SimpleParser parser = new SimpleParser(unprocessedSource);
		while(true) {
			TspElement tspElem = readElement(parser);
			if(tspElem == null) {
				break;
			}
			unprocessedSourceElements.add(tspElem);
		}
		return unprocessedSourceElements;
	}
	
	protected static abstract class TspElement {
		public String getElementType() { return null; };
	}
	
	protected class TspStringElement extends TspElement {
		public static final String RAW_TEXT = "TextElement";
		protected final String producedText;
		protected final String elemType;
		
		protected TspStringElement(String source) {
			this(source, RAW_TEXT);
		}
		protected TspStringElement(String producedText, String elemType) {
			this.producedText = assertNotNull_(producedText);
			this.elemType = elemType;
		}
		@Override
		public String getElementType() {
			return elemType;
		}
		@Override
		public String toString() {
			return producedText;
		}
	}
	
	protected class TspCommand extends TspElement {
		protected final String command;
		
		protected TspCommand(String command) {
			this.command = assertNotNull_(command);
		}
	}
	
	protected TspElement readElement(SimpleParser parser) throws TemplatedSourceException {
		return readElementWithExtraTokens(parser, null, null);
	}
	
	protected TspElement readElementWithExtraTokens(SimpleParser parser, String extraToken1, String extraToken2)
		throws TemplatedSourceException {
		if(parser.lookaheadIsEOF()) {
			return null;
		}
		
		boolean hasExtraTokens = extraToken1 != null; 
		
		if(hasExtraTokens) {
			//Hopefully Java is smart enough to inline this call
			parser.consumeUntilAny(new String[]{ kMARKER, extraToken1, extraToken2 }); 
		} else {
			parser.consumeUntil(kMARKER);
		}
		
		final String string = parser.getLastConsumedString();
		
		if(!string.isEmpty()) {
			return new TspStringElement(string);
		}
		
		if(hasExtraTokens) {
			if(parser.tryConsume(extraToken1)) {
				return new TspStringElement(parser.getLastConsumedString(), extraToken1);
			} else if(parser.tryConsume(extraToken2)) {
				return new TspStringElement(parser.getLastConsumedString(), extraToken2);
			}
		}
		
		parser.consume(kMARKER);
		
		if(parser.tryConsume(kMARKER)) {
			return new TspStringElement(kMARKER);
		} else if(extraToken1 != null && parser.tryConsume(extraToken1)) {
			return new TspStringElement(extraToken1);
		} else if(extraToken2 != null && parser.tryConsume(extraToken2)) {
			return new TspStringElement(extraToken2);
		} else if(parser.lookAhead() == '{' || parser.lookAhead() == '@') {
			return readExpansionCommand(parser); 
		} else if(Character.isJavaIdentifierStart(parser.lookAhead())) {
			return readMetadataElement(parser);
		}
		
		reportError(parser.getSourcePosition());
		return null;
	}
	
	protected void reportError(final int offset) throws TemplatedSourceException {
		throw new TemplatedSourceException(offset);
	}
	
	protected void consumeExpected(SimpleParser parser, String string) throws TemplatedSourceException {
		if(parser.tryConsume(string) == false) {
			reportError(parser.getSourcePosition());
		}
	}
	
	protected class TspExpansionElement extends TspElement {
		String expansionId; String pairedExpansionId; ArrayList<Argument> arguments; boolean defineOnly;
		public TspExpansionElement(String expansionId, String pairedExpansionId, ArrayList<Argument> arguments, 
			boolean defineOnly) {
			this.expansionId = expansionId;
			this.pairedExpansionId = pairedExpansionId;
			this.arguments = arguments;
			this.defineOnly = defineOnly;
		}
		
		@Override
		public String toString() {
			return "EXPANSION["+(defineOnly?"!":"")+
				StringUtil.nullAsEmpty(expansionId)+
				(pairedExpansionId == null ? "" : "("+pairedExpansionId+")")+
				(arguments == null ? "" : "{"+StringUtil.collToString(arguments, "#,#")+"}")+
				"]";
		}
	}
	
	@SuppressWarnings("serial")
	protected class Argument extends ArrayList<TspElement> { 
		@Override
		public String toString() {
			return "ARGUMENT["+StringUtil.collToString(this, "")+"]";
		}
	}
	
	protected TspElement readExpansionCommand(SimpleParser parser) throws TemplatedSourceException {
		assertTrue(parser.lookAhead() == '{' || parser.lookAhead() == '@');
		
		String expansionId = null;
		boolean defineOnly = false; 
		if(parser.tryConsume("@")) {
			expansionId = emptyToNull(parser.consumeAlphaNumericUS(false));
			if(parser.tryConsume("!")) {
				defineOnly = true;
			}
		}
		
		ArrayList<Argument> arguments = null;
		if(parser.tryConsume("{")) {
			arguments = readArgumentList(parser);
		}
		
		String pairedExpansionId = null;
		if(parser.tryConsume(":")) {
			pairedExpansionId = consumeDelimitedId(parser, ":");
		} else if(parser.tryConsume("(")) {
			pairedExpansionId = consumeDelimitedId(parser, ")");
		}
		
		if(pairedExpansionId == null && arguments == null) {
			reportError(parser.getSourcePosition());
		}
		return new TspExpansionElement(expansionId, pairedExpansionId, arguments, defineOnly);
	}
	
	protected String consumeDelimitedId(SimpleParser parser, String closeDelim) throws TemplatedSourceException {
		String pairedExpansionId;
		pairedExpansionId = emptyToNull(parser.consumeAlphaNumericUS(false));
		if(pairedExpansionId == null) {
			reportError(parser.getSourcePosition());
		}
		consumeExpected(parser, closeDelim);
		return pairedExpansionId;
	}
	
	protected ArrayList<Argument> readArgumentList(SimpleParser parser) throws TemplatedSourceException {
		ArrayList<Argument> arguments = new ArrayList<Argument>();
		String argumentSeparator = ",";
		String listEnd = "}";
		
		Argument argument = new Argument();
		while(true) {
			TspElement element = readElementWithExtraTokens(parser, argumentSeparator, listEnd);
			
			if(element == null) {
				reportError(parser.getSourcePosition());
			} else if(element.getElementType() == argumentSeparator) {
				arguments.add(argument);
				argument = new Argument();
			} else if(element.getElementType() == listEnd) {
				arguments.add(argument);
				argument = new Argument();
				break;
			} else {
				argument.add(element);
			}
		}
		return arguments;
	}
	
	protected class TspMetadataElement extends TspElement {
		String tag; String value; Argument associatedElements; boolean outputSource;
		
		public TspMetadataElement(String tag, String value, Argument associatedElements, boolean outputSource) {
			this.tag = tag;
			this.value = value;
			this.associatedElements = associatedElements;
			this.outputSource = outputSource;
		}
	}
	
	protected TspMetadataElement readMetadataElement(SimpleParser parser) throws TemplatedSourceException {
		String name = parser.consumeAlphaNumericUS(false);
		assertTrue(!name.isEmpty());
		
		String value = parser.tryConsume("(") ? consumeDelimitedString(parser, ")", false) : null;
		
		boolean outputSource = true;
		Argument sourceValue = null;
		
		sourceValue = parser.tryConsume("{") ? readArgument(parser, "}", false) : null;
		if(sourceValue == null && parser.tryConsume(":")) {
			sourceValue = readArgument(parser, "#:END", true);
			if(!parser.lookaheadIsEOF()) {
				parser.seekToNewLine();
			}
			outputSource = false;
		}
		
		return new TspMetadataElement(name, value, sourceValue, outputSource);
	}
	
	protected Argument readArgument(SimpleParser parser, String listEnd, boolean eofTerminates)
		throws TemplatedSourceException {
		Argument argument = new Argument();
		while(true) {
			TspElement element = readElementWithExtraTokens(parser, listEnd, listEnd);
			
			if(element == null) {
				if(!eofTerminates) {
					reportError(parser.getSourcePosition());
				}
				break;
			} else if(element.getElementType() == listEnd) {
				break;
			} else {
				argument.add(element);
			}
		}
		return argument;
	}
	
	protected String consumeDelimitedString(SimpleParser parser, String closeSep, boolean eofTerminates) 
		throws TemplatedSourceException {
		StringBuilder value = new StringBuilder();
		
		final String[] alts = new String[]{closeSep, kMARKER};
		while(true) {
			int alt = parser.consumeUntilAny(alts);
			value.append(parser.getLastConsumedString());
			
			if(parser.lookaheadIsEOF()) {
				if(eofTerminates) 
					break;
				reportError(parser.getSourcePosition()); // Unterminated
			} else if(alt == 0) {
				parser.consume(closeSep);
				break;
			} else if(alt == 1) {
				parser.consume(kMARKER);
				if(parser.tryConsume("#")) {
					value.append("#");
				} else if(parser.tryConsume(closeSep)) {
					value.append(closeSep);
				} else {
					reportError(parser.getSourcePosition()); // Invalid Escape
				}
			} else {
				assertFail();
			}
		}
		return value.toString();
	}
	
	// --------------------- Generation phase ---------------------
	
	protected void processSplitCaseSource(String unprocessedCaseSource, boolean isHeader) throws TemplatedSourceException {
		ArrayList<TspElement> sourceElements = parseSource(unprocessedCaseSource);
		localExpansions.clear();
		ProcessingState processingState = new ProcessingState(isHeader);
		processCaseContents(processingState, new CopyableListIterator<TspElement>(sourceElements));
	}
	
	protected class ProcessingState {
		protected final boolean isHeaderCase;
		protected final StringBuilder sourceSB = new StringBuilder();
		protected final ArrayList<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
		protected final Map<String, Integer> activeExpansions = new HashMap<String, Integer>();
		
		public ProcessingState(boolean isHeaderCase) {
			this.isHeaderCase = isHeaderCase;
		}
		
		public ProcessingState(boolean isHeaderCase, String source, List<MetadataEntry> metadata,
			Map<String, Integer> activeExpansions) {
			this.isHeaderCase = isHeaderCase;
			this.sourceSB.append(source);
			this.metadata.addAll(metadata);
			this.activeExpansions.putAll(activeExpansions);
		}
		
		@Override
		public ProcessingState clone() {
			return new ProcessingState(isHeaderCase, sourceSB.toString(), metadata, activeExpansions);
		}
		
		public TspExpansionElement getExpansion(String expansionId) {
			if(!isHeaderCase) {
				TspExpansionElement expansionElement = localExpansions.get(expansionId);
				if(expansionElement != null) {
					return expansionElement;
				}
			}
			return globalExpansions.get(expansionId);
		}
		
		public void putExpansion(String expansionId, TspExpansionElement expansionElement) {
			if(!isHeaderCase) {
				localExpansions.put(expansionId, expansionElement);
			} else {
				globalExpansions.put(expansionId, expansionElement);
			}
		}
	}
	
	protected void processCaseContents(ProcessingState sourceCase, ICopyableIterator<TspElement> elementStream) 
		throws TemplatedSourceException {
		assertNotNull(sourceCase);
		
		while(elementStream.hasNext()) {
			TspElement tspElem = elementStream.next();
			elementStream = elementStream.optimizedSelf();
			
			if(tspElem instanceof TspExpansionElement) {
				TspExpansionElement expansionElem = (TspExpansionElement) tspElem;
				
				boolean endProcessing = processExpansionElement(sourceCase, elementStream, expansionElem);
				if(endProcessing) {
					return;
				}
			} else if(tspElem instanceof TspStringElement) {
				TspStringElement stringElem = (TspStringElement) tspElem;
				sourceCase.sourceSB.append(stringElem.producedText);
			} else if(tspElem instanceof TspMetadataElement) {
				final TspMetadataElement mdElem = (TspMetadataElement) tspElem;
				
				int offset = sourceCase.sourceSB.length();
				int metadataIx = sourceCase.metadata.size();
				sourceCase.metadata.add(null); // To be filled later
				final TspMetadataEndElement mdEndElem = 
					new TspMetadataEndElement(mdElem, offset, metadataIx);
				
				if(mdElem.associatedElements != null) {
					Argument sourceArgument = mdElem.associatedElements;
					
					ICopyableIterator<TspElement> mdArgIter = ChainedIterator2.create(
						CopyableListIterator.create(sourceArgument),
						CopyableListIterator.create(Collections.<TspElement>singletonList(mdEndElem))
						);
					elementStream = ChainedIterator2.create(mdArgIter, elementStream);
				} else {
					processMetadataEndElem(sourceCase, mdEndElem);
				}
			} else if(tspElem instanceof TspMetadataEndElement) {
				processMetadataEndElem(sourceCase, (TspMetadataEndElement) tspElem);
			} else {
				assertFail();
			}
		}
		
		addFullyProcessedSourceCase(sourceCase);
	}
	
	public void processMetadataEndElem(ProcessingState sourceCase, TspMetadataEndElement mdEndElem) {
		int offset = mdEndElem.offset;
		String associatedSource = null;
		
		TspMetadataElement mdElem = mdEndElem.mdElem;
		if(mdElem.associatedElements != null) {
			int endOffset = sourceCase.sourceSB.length();
			associatedSource = sourceCase.sourceSB.substring(offset, endOffset);
			
			if(mdElem.outputSource) {
				// already done
			} else {
				sourceCase.sourceSB.setLength(offset); // Reset sourceBuilder
			}
		}
		
		MetadataEntry mde = new MetadataEntry(mdElem.tag, mdElem.value, associatedSource, offset);
		assertTrue(sourceCase.metadata.get(mdEndElem.metadataIx) == null);
		sourceCase.metadata.set(mdEndElem.metadataIx, mde);
	}
	
	protected class TspMetadataEndElement extends TspElement {
		public final TspMetadataElement mdElem;
		public final int offset;
		public final int metadataIx;
		
		public TspMetadataEndElement(TspMetadataElement mdElem, int offset, int metadataIx) {
			this.mdElem = mdElem;
			this.offset = offset;
			this.metadataIx = metadataIx;
		}
		
		@Override
		public String toString() {
			return "<MD-END:"+offset+">";
		}
	}
	
	protected void addFullyProcessedSourceCase(ProcessingState caseState ) {
		if(caseState.isHeaderCase == false) {
			genCases.add(new AnnotatedSource(caseState.sourceSB.toString(), caseState.metadata));
		}
	}
	
	protected boolean processExpansionElement(ProcessingState sourceCase, ICopyableIterator<TspElement> elementStream,
		TspExpansionElement expansionElem) throws TemplatedSourceException {
		
		String expansionId = expansionElem.expansionId;
		checkError(sourceCase.isHeaderCase && expansionId == null, sourceCase);
		ArrayList<Argument> arguments = expansionElem.arguments;
		
		Integer pairedExpansionIx = null;
		if(expansionElem.pairedExpansionId != null) {
			 // Definition-only must not have a paired expansion
			checkError(expansionElem.defineOnly && expansionId != null, sourceCase);
			
			TspExpansionElement referredExpansion = sourceCase.getExpansion(expansionElem.pairedExpansionId);
			checkError(referredExpansion == null, sourceCase); // If referred, then it must be defined
			
			pairedExpansionIx = sourceCase.activeExpansions.get(expansionElem.pairedExpansionId);
			
			if(pairedExpansionIx == null) {
				// Paired expansion is not active
				checkError(arguments != null, sourceCase); // Must be active if expansion has its own arguments
			}
			
			if(arguments == null) {
				arguments = referredExpansion.arguments;
			} else {
				checkError(referredExpansion.arguments.size() != arguments.size(), sourceCase);
			}
		}
		
		if(expansionId != null) {
			checkError(arguments == null, sourceCase); // No arguments or pairing present 
			TspExpansionElement definedExpansionElem = sourceCase.getExpansion(expansionId);
			if(definedExpansionElem != null && definedExpansionElem != expansionElem) {
				reportError(sourceCase);
			}
			sourceCase.putExpansion(expansionId, expansionElem);
		}
		
		if((expansionElem.defineOnly && expansionId != null) || sourceCase.isHeaderCase) {
			return false;
		}
		
		if(pairedExpansionIx != null) {
			int ix = pairedExpansionIx;
			processArgument(sourceCase, elementStream, expansionId, arguments.get(ix), ix);
		} else {
			if(expansionId == null) {
				expansionId = expansionElem.pairedExpansionId;
			}
			
			boolean activateOnly = expansionElem.defineOnly;
			
			for (int ix = 0; ix < arguments.size(); ix++) {
				Argument argument = activateOnly ? null : arguments.get(ix);
				processArgument(sourceCase, elementStream, expansionId, argument, ix);
			}
		}
		return true;
	}
	
	protected void checkError(boolean errorCondition, ProcessingState sourceCase) throws TemplatedSourceException {
		if(errorCondition) {
			reportError(sourceCase); 
		}
	}
	
	protected void reportError(ProcessingState sourceCase) throws TemplatedSourceException {
		reportError(sourceCase.sourceSB.length());
	}
	
	protected void processArgument(ProcessingState sourceCase, ICopyableIterator<TspElement> elementStream,
		String expansionId, Argument argument, int index) throws TemplatedSourceException {
		ProcessingState newState = sourceCase.clone();
		if(expansionId != null) {
			newState.activeExpansions.put(expansionId, index);
		}
		
		ICopyableIterator<TspElement> newElements = (argument == null) ? 
			elementStream.copyState() : 
			ChainedIterator2.create(CopyableListIterator.create(argument), elementStream.copyState())
			;
		processCaseContents(newState, newElements);
	}
	
}