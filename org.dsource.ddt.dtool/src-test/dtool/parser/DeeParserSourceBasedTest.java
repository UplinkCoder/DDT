/*******************************************************************************
 * Copyright (c) 2012, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package dtool.parser;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dtool.tests.AnnotatedSource;
import dtool.tests.AnnotatedSource.MetadataEntry;
import dtool.tests.DToolTestResources;

@RunWith(Parameterized.class)
public class DeeParserSourceBasedTest extends DeeSourceBasedTest {
	
	protected static final String TESTFILESDIR = "dtool.parser/parser-tests";
	
	private static final int PARSER_SOURCE_BASED_TESTS_COUNT = 5;
	private static int splitTestCount = 0;
	
	@Parameters
	public static Collection<Object[]> filesToParse() throws IOException {
		return getTestFilesFromFolderAsParameterList(DToolTestResources.getTestResource(TESTFILESDIR));
	}
	
	protected final File file;
	
	public DeeParserSourceBasedTest(File file) {
		this.file = file;
	}
	
	@Test
	public void runSourceBasedTests() throws IOException {
		for (AnnotatedSource testString : getSourceBasedTests(file)) {
			splitTestCount++;
			runSourceBasedTest(testString);
		}
	}
	
	@AfterClass
	public static void checkTestCount() {
		assertTrue(splitTestCount == PARSER_SOURCE_BASED_TESTS_COUNT);
	}
	
	public void runSourceBasedTest(AnnotatedSource testSource) {
		String parseSource = testSource.source;
		String expectedGenSource = null;
		
		ArrayList<String> expectedErrors = new ArrayList<String>();
		
		for (MetadataEntry mde : testSource.metadata) {
			if(mde.name.equals("AST_EXPECTED")) {
				assertTrue(expectedGenSource == null);
				expectedGenSource = mde.associatedSource;
			} else if(mde.name.equals("error")){
				// TODO
				expectedErrors.add(mde.extraValue);
			} else {
				assertFail("Unknown metadata");
			}
		}
		
		DeeParserTest.runParserTest(parseSource, expectedGenSource);
	}
	
}