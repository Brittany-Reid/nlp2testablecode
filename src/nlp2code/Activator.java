package nlp2code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.BundleContext;

import nlp2code.compiler.IMCompiler;

/**
 * The activator class controls the plug-in life cycle.
 *   The Activator class is the first class to be instantiated whenever the plugin is invoked.
 */
public class Activator extends AbstractUIPlugin {
	// The plug-in ID
	public static final String PLUGIN_ID = "nlp2code";
	// The shared instance
	private static Activator plugin;
	// global langauge level for parsing
	public static int level = AST.JLS11;
	public static String version = "11";
	
	//Logger
    private static final Logger parentLogger = LogManager.getLogger();
    private static Logger logger = parentLogger;
    public static Boolean first = true;


	/**
	 *  Constructor.
	 *  Initializes defaults for document listeners, loads data.
	 */
	public Activator() {
		long start;
		
		ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		doc.addDocumentListener(InputHandler.qdl);
		doc.addDocumentListener(InputHandler.tl);
		JavaCore.addElementChangedListener(new PackagesListener(), ElementChangedEvent.POST_CHANGE );
		InputHandler.documents.add(doc);
		InputHandler.previous_search = new ArrayList();
		InputHandler.previous_offset = 0;
		InputHandler.previous_length = 0;
		InputHandler.previous_queries = new Vector<String>();
		InputHandler.doclistener = new CycleDocListener();
		
		start = System.currentTimeMillis();
		DataHandler.loadQuestions();
		DataHandler.loadAnswers();
		TaskRecommender.loadTasks();
		logger.debug("Initialization took: " + (System.currentTimeMillis() - start) + "ms\n");
		
		//saveState();
		//loadState();
		
	}

	public static void queryTests() {
		Map<String, Integer> errorIDs = new HashMap<>();
		Map<String, String> errorMessages = new HashMap<>();
		long start;
		
		logger.debug("COMPILING FOR 47 TASKS\n");
		start = System.currentTimeMillis();
		String before = "class Main {\npublic static void main(String[] args){\n";
		String after = "\nreturn; }\n}\n";
		
		//for each task
		int errors;
		int totalErrors = 0;
		int test;
		int compiled;
		int totalCompiled = 0;
		for(String task : TaskRecommender.queries) {
			logger.debug("TASK: " + task + "\n");
			errors = 0;
			compiled = 0;
			
			//find the snippets
			List<Snippet> snippets;
			snippets = Searcher.getSnippets(task);
			if(snippets == null) continue;
			
			//call evaluator
			snippets = Evaluator.evaluate(snippets, before, after);
		 
			//with the results of evaluator, we compile to count errors
			IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, null);
			for(Snippet snippet : snippets) {
				compiler.clearSaved();
				String proposedBefore = before;
				if(snippet.getImportList() != null || snippet.getImportList().size() > 0) {
					proposedBefore = Snippet.addImportToBefore(snippet, before);
				}
				compiler.addSource("Main", proposedBefore+snippet.getCode()+after);
				compiler.compileAll();
				test = compiler.getErrors();
				errors += test;
				
				//add to error map
				for(Diagnostic<? extends JavaFileObject> d : compiler.getDiagnostics().getDiagnostics()) {
					String id = d.getCode();
					String message = d.getMessage(null);
					
					
					if(!errorIDs.containsKey(id)) {
						errorIDs.put(id, 1);
						errorMessages.put(id, message);
					}
					else {
						int num = errorIDs.get(id);
						num++;
						errorIDs.put(id, num);
					}
				}
				
				if(test == 0) {
					compiled++;
				}
				
				
				//test in/out
				if(snippet.getPassed() > 0) {
					System.out.println(snippet.getCode());
					logger.debug("Return: " + snippet.getReturnType() + ", Argument Types: ");
					for(String arg : snippet.getArgumentTypes()) {
						logger.debug(arg + " ");
					}
					logger.debug("\n");
				}
				
			}
			logger.debug("ERRORS: " + errors + "\n");
			logger.debug("COMPILED: " + compiled + "\n");
			totalErrors += errors;
			totalCompiled += compiled;
			
		}
		
		logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
		logger.debug("TOTAL ERRORS: " + totalErrors + "\n");
		logger.debug("TOTAL COMPILED: " + totalCompiled + "\n");
		
		//sort list
		List<Entry<String, Integer>> list = new ArrayList<>(errorIDs.entrySet());
        list.sort(Entry.<String, Integer>comparingByValue().reversed());

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Entry<String, Integer> entry : list) {
        	logger.debug(entry.getValue() + ", " + errorMessages.get(entry.getKey()) + ", " + entry.getKey() + "\n");
            //result.put(entry.getKey(), entry.getValue());
        }
		
	}
	
	/**
	 * This function tests and logs the results of search on our 47 queries.
	 */
	public static List<Snippet> searchTest() {
		long start;
		List<Snippet> totalSnippets = new ArrayList<>();
		
		logger.debug("SEARCHING 47 TASKS\n");
		start = System.currentTimeMillis();
		for(String task : TaskRecommender.queries) {
			List<Snippet> snippets;
			snippets = Searcher.getSnippets(task);
			if(snippets != null) {
				totalSnippets.addAll(snippets);
			}
		}
		logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
		logger.debug("TOTAL: " + totalSnippets.size() + " snippets from search.");
		return totalSnippets;
	}


	/*
	 * Function start
	 *   Called when the plugin starts.
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * Function stop
	 *   Called when the plugin stops.
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/*
	 * Function getDefault
	 *   Returns the shared instance (Activator object).
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Loads default information from preferences.txt file.
	 */
	public static void loadState() {
		// Open up defaults from preferences.txt file
		try (BufferedReader br = new BufferedReader(new FileReader("plugins/nlp2code/preferences.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	//read info from file
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	//returns logger
	public static Logger getLogger() {
        return logger;
    }
	
	/** 
	 * Saves default API key and custom search ID information to the preferences.txt file.
	 */
	private static void saveState() {
		String text = "APIKEY=AIzaSyClq5H_Nd7RdVSIMaRPQhwpG5m_-68fWRU\nCUSTOM_SEARCH_ID=011454571462803403544:zvy2e2weyy8\nFEEDBACK=true";
		File dir = new File("plugins");
		if (!dir.exists()) makeDir(dir);
		File dir1 = new File("plugins/nlp2code");
		// if the directory does not exist, create it
		if (!dir1.exists()) makeDir(dir1);
		String file_path = dir1.getAbsolutePath() + "/preferences.txt";
		File dir2 = new File(file_path);
		dir2.getParentFile().mkdirs();
		try {
			if (!dir2.exists());
				dir2.createNewFile();
		} catch (IOException e1) {
			System.out.println("ERROR MAKING FILE");
			e1.printStackTrace();
			return;
		}
		
		// Load file to see if anything is in there.
		String result = "";
		try (BufferedReader br = new BufferedReader(new FileReader("plugins/nlp2code/preferences.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	result += line;
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
		// If file has stuff in it, don't overwrite it.
		if (!result.equals("")) return;
		
		// Save default preferences.
		try {
			Files.write(Paths.get(file_path), text.getBytes() , StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Function makeDir
	 * 	 Given a relative directory filepath, create the directory.
	 * 
	 * 	 Input: File theDir - Relative directory filepath.
	 */
	private static void makeDir (File theDir) {
		System.out.println("Creating DIR");
	    boolean result = false;
	    try{
	        theDir.mkdir();
	        result = true;
	    } 
	    catch(SecurityException se){
	    	se.printStackTrace();
	    }        
	    if(result) {    
	        System.out.println("DIR created");  
	    }
	}
}