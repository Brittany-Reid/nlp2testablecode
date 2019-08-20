package nlp2code.test;

import static org.junit.Assert.*;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import nlp2code.*;
import nlp2code.compiler.*;

import org.junit.Test;

public class FixerTest {

	@Test
	public void testFileSystem() {		
		//upgraded ecj_fix.jar to fix bug on this (only occurred with testing):
		new EclipseCompiler().getStandardFileManager(null, null, null);
		return;
	}
	
	@Test
	public void testDeletion() {
	}

}
