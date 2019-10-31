package nlp3code;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import nlp3code.code.Snippet;
import nlp3code.compiler.IMCompiler;
import nlp3code.fixer.Deleter;
import nlp3code.listeners.PackagesListener;
import nlp3code.recommenders.TaskRecommender;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.TypeSuggestions;

/**
 * The Activator class controls the plug-in life cycle
 * The Activator class is the first class to be instantiated whenever the plug-in is invoked.
 */
public class Activator extends AbstractUIPlugin {
	//the global logger
	private static final Logger parentLogger = LogManager.getLogger();
	public static Logger logger = parentLogger;
	//for testing, is this the first run?
    public static Boolean first = true;
	// The plug-in ID
	public static final String PLUGIN_ID = "nlp3code"; //$NON-NLS-1$
	// The shared instance
	private static Activator plugin;
	//language version
	public static String version = "11";
	//setup complete
	public static boolean loaded = false;
	//our random number generator
	public static Random random;
	public static int level = AST.JLS11;
	
	/**
	 * The constructor
	 */
	public Activator() {
		DataHandler.limit = 10000L;
		setupListeners();
		random = new Random();
	}
	
	/**
	 * Sets up document listeners.
	 */
	private static void setupListeners() {
		IDocument document = DocHandler.getDocument();
		InputHandler.documents.add(document);
		document.addDocumentListener(InputHandler.queryDocListener);
		JavaCore.addElementChangedListener(new PackagesListener(), ElementChangedEvent.POST_CHANGE );
	}
	
	
	//tests that need to be run with the ui :(
	
	public static void deletionTests() {
		Map<String, Integer> errorIDs = new HashMap<>();
		Map<String, String> errorMessages = new HashMap<>();
		String before = "class Main {\npublic static void main(String[] args){\n";
		String after = "\nreturn; }\n}\n";
		long start;
		
		for(int i=6; i<8; i++) {
			//set deletion options
			if(i==0){
				logger.debug("SETTING: DESC, STRICT, SINGLE\n");
				Deleter.setOptions(false, false, false);
			}
			if(i==1){
				logger.debug("SETTING: ASC, STRICT, SINGLE\n");
				Deleter.setOptions(true, false, false);
			}
			if(i==2){
				logger.debug("SETTING: DESC, NONSTRICT, SINGLE\n");
				Deleter.setOptions(false, true, false);
			}
			if(i==3){
				logger.debug("SETTING: ASC, NONSTRICT, SINGLE\n");
				Deleter.setOptions(true, true, false);
			}
			if(i==4){
				logger.debug("SETTING: DESC, STRICT, LOOP\n");
				Deleter.setOptions(false, false, true);
			}
			if(i==5){
				logger.debug("SETTING: ASC, NONSTRICT, LOOP\n");
				Deleter.setOptions(true, true, true);
			}
			if(i==6){
				logger.debug("SETTING: ASC, STRICT, LOOP\n");
				Deleter.setOptions(true, false, true);
			}
			if(i==7){
				logger.debug("SETTING: DESC, NONSTRICT, LOOP\n");
				Deleter.setOptions(false, true, true);
			}
			
			//get results for each task
			start = System.currentTimeMillis();
			int errors;
			int totalErrors = 0;
			int test;
			int compiled;
			int totalCompiled = 0;
			for(String task : DataHandler.queries) {
				errors = 0;
				compiled = 0;
				
				//find the snippets
				List<Snippet> snippets;
				snippets = Searcher.getSnippets(task);
				if(snippets == null) continue;
				
				//call evaluator
				snippets = Evaluator.evaluate(null, snippets, before, after);
			 
				//with the results of evaluator, we compile to count errors
				IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, null, new OutputStreamWriter(new NullOutputStream()));
				for(Snippet snippet : snippets) {
					compiler.clearSaved();
					String code = Snippet.insert(snippet, before+after, before.length());
					compiler.addSource("Main", code);
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
					if(snippet.getPassedTests() > 0) {
						System.out.println(snippet.getCode());
						logger.debug("Return: " + snippet.getReturn() + ", Argument Types: ");
						for(String arg : snippet.getArgumentTypes()) {
							logger.debug(arg + " ");
						}
						logger.debug("\n");
					}
					
				}
				logger.debug("TASK: " + task + ", " + compiled + "\n");
				totalCompiled += compiled;
				
			}
			
			logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
			logger.debug("TOTAL: " + totalCompiled + "\n");
		}
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
		for(String task : DataHandler.queries) {
			logger.debug("TASK: " + task + "\n");
			errors = 0;
			compiled = 0;
			
			//find the snippets
			List<Snippet> snippets;
			snippets = Searcher.getSnippets(task);
			if(snippets == null) continue;
			
			//call evaluator
			snippets = Evaluator.evaluate(null, snippets, before, after);
		 
			//with the results of evaluator, we compile to count errors
			IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, null, new OutputStreamWriter(new NullOutputStream()));
			for(Snippet snippet : snippets) {
				compiler.clearSaved();
				String code = Snippet.insert(snippet, before+after, before.length());
				compiler.addSource("Main", code);
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
				if(snippet.getPassedTests() > 0) {
					System.out.println(snippet.getCode());
					logger.debug("Return: " + snippet.getReturn() + ", Argument Types: ");
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
	 * Test the type suggestions for 47 tasks.
	 */
	public static void suggestionTests() {
		long start;
		
		logger.debug("COMPILING FOR 47 TASKS\n");
		start = System.currentTimeMillis();
		String before = "class Main {\npublic static void main(String[] args){\n";
		String after = "\nreturn;\n }\n}\n";
		InputHandler.before = before;
		InputHandler.after = after;
		
		//for each task
		for(String task : DataHandler.queries) {
			logger.debug("TASK: " + task + "\n");

			
			//find the snippets
			List<Snippet> snippets;
			snippets = Searcher.getSnippets(task);
			if(snippets == null) continue;
			
			//evaluate
			snippets = Evaluator.evaluate(null, snippets, before, after);
			
			//get type suggestions
			snippets = TypeSuggestions.getTypeSuggestions(snippets, before, after, null);
			
			
			List<String> types = TypeRecommender.sortIOTypes(snippets);
			for(String type : types) {
				logger.debug("TYPE: " + type + "\n");
			}
			
		}		
		logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
	}
	
	/**
	 * Loads data base and task recommendations.
	 */
	private static void loadData(IProgressMonitor monitor) {
		//record start time
		long start = System.currentTimeMillis();
		
		//get submonitor
		SubMonitor sub = SubMonitor.convert(monitor, 100);
		
		//load stop words
		sub.split(5);
		DataHandler.loadStopWords();
		
		//load questions
		DataHandler.loadQuestions(sub.split(40));
		monitor.worked(1);
		
		//load answers
		DataHandler.loadAnswers(sub.split(50));
		
		//load tasks
		sub.split(5);
		DataHandler.loadTasks(null);
		
		//set loaded state
    	loaded = true;
		
		//record end time
		long end = System.currentTimeMillis();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		//load data job
		Job loadData = new Job("Loading Data") {
	        @Override
	        protected IStatus run(IProgressMonitor monitor) {
	        	monitor.beginTask("Loading Data", 100);
	        	loadData(monitor);
	            return Status.OK_STATUS;
	        }

	    };
	    
	    loadData.setPriority(Job.BUILD);
	    loadData.schedule();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
