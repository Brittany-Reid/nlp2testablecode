package nlp2testablecode.tests.unittests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.After;
import org.junit.Test;

import nlp2testablecode.Activator;
import nlp2testablecode.DataHandler;
import nlp2testablecode.DocHandler;
import nlp2testablecode.tests.TestEnvironment;

public class ActivatorTest {
	String before = TestEnvironment.before;
	String after = TestEnvironment.after;
	IProject project = null;
	IFile file = null;
	
	/**
	 * Create a clean dummy project for testing before each test.
	 */
	public void setUp() throws Exception {
		
		TestEnvironment.setupWorkspace();
		
		Thread.sleep(1000);
		
	}
	
	/**
	 * Delete previous project after each test.
	 * @throws Exception
	 */
	public void tearDown() throws Exception {
		
		TestEnvironment.cleanWorkspace();
		IProject project = null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject("Test");
		
		if (project.exists()) {
			// Clean up any old project information.
			project.delete(true, true, null);
		}
	}
	
	@After
	public void after() {
		DataHandler.clear();
	}
	
	/**
	 * Test the setup function.
	 */
	@Test
	public void testSetup() throws Exception{
		
		setUp();
		
		//successful load
		DataHandler.limit = 0L;
		Activator.setup();
		
		//sleep for a second to wait
		Thread.sleep(1000);
		
		assertTrue(DataHandler.loaded);
		
		tearDown();
		
	}
	
	/**
	 * Test the setup function.
	 */
	@Test
	public void dontCacheEditor() throws Exception{
		
		setUp();
		
		IEditorPart activeEditor = DocHandler.getEditor();
		if(activeEditor == null) fail();
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
				
			//get document from text editor
			IDocumentProvider provider = textEditor.getDocumentProvider();
			
			if(provider == null) fail();
		}
		
		tearDown();
		
	}
	
	
	/**
	 * What happens if there's no active document.
	 */
	@Test
	public void noDocument() throws Exception{
		
		Activator.setup();
		
		Thread.sleep(1000);
		
		assertFalse(DataHandler.loaded);
		
		tearDown();
	}
	
	/**
	 * Test that the get default function returns the plugin.
	 */
	@Test
	public void getPlugin() throws Exception{
		AbstractUIPlugin plugin = Activator.getDefault();
		assertNotNull(plugin);
		
		tearDown();
	}

}
