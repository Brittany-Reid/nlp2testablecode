package nlp3code;

import java.util.Random;

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
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import nlp3code.listeners.PackagesListener;

/**
 * The Activator class controls the plug-in life cycle
 * The Activator class is the first class to be instantiated whenever the plug-in is invoked.
 * JUnit plug-in tests trigger the activator and start functions.
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
	//initialized database?
	public static boolean initialized = false;
	//our random number generator
	public static Random random;
	public static int level = AST.JLS11;
	
	/**
	 * Constructor.
	 */
	public Activator() {
		//initialize random generator
		random = new Random();
	}
	
	/**
	 * Begin database setup. 
	 * This function is called on first use of plugin in InputHander and TaskRecommender.
	 * To load different databases during testing and prevent immediate db loading, this is no longer called within start.
	 */
	public static void setup() {
		//setup listeners 
		int e = setupListeners();
		
		//any non 0 return value, failed
		if(e != 0) return;
		
		//load data job
		Job loadData = new Job("Loading Data") {
	        @Override
	        protected IStatus run(IProgressMonitor monitor) {
	        	monitor.beginTask("Loading Data", 100);
	        	DataHandler.loadData(monitor);
	        	initialized = true;
	            return Status.OK_STATUS;
	        }

	    };
	    
	    //schedule job, this will run in the background
	    loadData.setPriority(Job.BUILD);
	    loadData.schedule();
	}
	
	/**
	 * Sets up document listeners for written queries and any changes to project classpath.
	 * @return 0 on success, 1 if no document open to add listeners
	 */
	private static int setupListeners() {
		//get open document
		IDocument document = DocHandler.getDocument();
		
		//no document open?
		if(document == null) return 1;
		
		//add listeners
		InputHandler.documents.add(document);
		document.addDocumentListener(InputHandler.queryDocListener);
		JavaCore.addElementChangedListener(new PackagesListener(), ElementChangedEvent.POST_CHANGE );
		return 0;
	}
	

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
//	/*
//	 * These tests can't be done without plug-in resources.
//	 *	... I figured out how to do plug-in tests without crashing so these can be likely be moved out.
//	 */
//	
//	public static void deletionTests() {
//		Map<String, Integer> errorIDs = new HashMap<>();
//		Map<String, String> errorMessages = new HashMap<>();
//		String before = "class Main {\npublic static void main(String[] args){\n";
//		String after = "\nreturn; }\n}\n";
//		long start;
//		
//		for(int i=4; i<8; i++) {
//			//set deletion options
//			if(i==0){
//				logger.debug("SETTING: DESC, STRICT, SINGLE\n");
//				Deleter.setOptions(false, false, false);
//			}
//			if(i==1){
//				logger.debug("SETTING: ASC, STRICT, SINGLE\n");
//				Deleter.setOptions(true, false, false);
//			}
//			if(i==2){
//				logger.debug("SETTING: DESC, NONSTRICT, SINGLE\n");
//				Deleter.setOptions(false, true, false);
//			}
//			if(i==3){
//				logger.debug("SETTING: ASC, NONSTRICT, SINGLE\n");
//				Deleter.setOptions(true, true, false);
//			}
//			if(i==4){
//				logger.debug("SETTING: DESC, STRICT, LOOP\n");
//				Deleter.setOptions(false, false, true);
//			}
//			if(i==5){
//				logger.debug("SETTING: ASC, NONSTRICT, LOOP\n");
//				Deleter.setOptions(true, true, true);
//			}
//			if(i==6){
//				logger.debug("SETTING: ASC, STRICT, LOOP\n");
//				Deleter.setOptions(true, false, true);
//			}
//			if(i==7){
//				logger.debug("SETTING: DESC, NONSTRICT, LOOP\n");
//				Deleter.setOptions(false, true, true);
//			}
//			
//			//get results for each task
//			start = System.currentTimeMillis();
//			int errors;
//			int totalErrors = 0;
//			int test;
//			int compiled;
//			int totalCompiled = 0;
//			for(String task : DataHandler.queries) {
//				errors = 0;
//				compiled = 0;
//				
//				//find the snippets
//				List<Snippet> snippets;
//				snippets = Searcher.getSnippets(task);
//				if(snippets == null) continue;
//				
//				//call evaluator
//				snippets = Evaluator.evaluate(null, snippets, before, after);
//			 
//				//with the results of evaluator, we compile to count errors
//				IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, null, new OutputStreamWriter(new NullOutputStream()));
//				for(Snippet snippet : snippets) {
//					compiler.clearSaved();
//					String code = Snippet.insert(snippet, before+after, before.length());
//					compiler.addSource("Main", code);
//					compiler.compileAll();
//					test = compiler.getErrors();
//					errors += test;
//					
//					//add to error map
//					for(Diagnostic<? extends JavaFileObject> d : compiler.getDiagnostics().getDiagnostics()) {
//						String id = d.getCode();
//						String message = d.getMessage(null);
//						
//						
//						if(!errorIDs.containsKey(id)) {
//							errorIDs.put(id, 1);
//							errorMessages.put(id, message);
//						}
//						else {
//							int num = errorIDs.get(id);
//							num++;
//							errorIDs.put(id, num);
//						}
//					}
//					
//					if(test == 0) {
//						compiled++;
//					}
//					
//					
//					//test in/out
//					if(snippet.getPassedTests() > 0) {
//						System.out.println(snippet.getCode());
//						logger.debug("Return: " + snippet.getReturn() + ", Argument Types: ");
//						for(String arg : snippet.getArgumentTypes()) {
//							logger.debug(arg + " ");
//						}
//						logger.debug("\n");
//					}
//					
//				}
//				logger.debug("TASK: " + task + ", " + compiled + "\n");
//				totalCompiled += compiled;
//				
//			}
//			
//			logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
//			logger.debug("TOTAL: " + totalCompiled + "\n");
//		}
//	}
//	
//	public static void queryTests() {
//		Map<String, Integer> errorIDs = new HashMap<>();
//		Map<String, String> errorMessages = new HashMap<>();
//		long start;
//		
//		logger.debug("COMPILING FOR 47 TASKS\n");
//		start = System.currentTimeMillis();
//		String before = "class Main {\npublic static void main(String[] args){\n";
//		String after = "\nreturn; }\n}\n";
//		
//		//for each task
//		int errors;
//		int totalErrors = 0;
//		int test;
//		int compiled;
//		int totalCompiled = 0;
//		for(String task : DataHandler.queries) {
//			logger.debug("TASK: " + task + "\n");
//			errors = 0;
//			compiled = 0;
//			
//			//find the snippets
//			List<Snippet> snippets;
//			snippets = Searcher.getSnippets(task);
//			if(snippets == null) continue;
//			
//			//call evaluator
//			snippets = Evaluator.evaluate(null, snippets, before, after);
//		 
//			//with the results of evaluator, we compile to count errors
//			IMCompiler compiler = new IMCompiler(Evaluator.javaCompiler, null, new OutputStreamWriter(new NullOutputStream()));
//			for(Snippet snippet : snippets) {
//				compiler.clearSaved();
//				String code = Snippet.insert(snippet, before+after, before.length());
//				compiler.addSource("Main", code);
//				compiler.compileAll();
//				test = compiler.getErrors();
//				errors += test;
//				
//				//add to error map
//				for(Diagnostic<? extends JavaFileObject> d : compiler.getDiagnostics().getDiagnostics()) {
//					String id = d.getCode();
//					String message = d.getMessage(null);
//					
//					
//					if(!errorIDs.containsKey(id)) {
//						errorIDs.put(id, 1);
//						errorMessages.put(id, message);
//					}
//					else {
//						int num = errorIDs.get(id);
//						num++;
//						errorIDs.put(id, num);
//					}
//				}
//				
//				if(test == 0) {
//					compiled++;
//				}
//				
//				
//				//test in/out
//				if(snippet.getPassedTests() > 0) {
//					System.out.println(snippet.getCode());
//					logger.debug("Return: " + snippet.getReturn() + ", Argument Types: ");
//					for(String arg : snippet.getArgumentTypes()) {
//						logger.debug(arg + " ");
//					}
//					logger.debug("\n");
//				}
//				
//			}
//			logger.debug("ERRORS: " + errors + "\n");
//			logger.debug("COMPILED: " + compiled + "\n");
//			totalErrors += errors;
//			totalCompiled += compiled;
//			
//		}
//		
//		logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
//		logger.debug("TOTAL ERRORS: " + totalErrors + "\n");
//		logger.debug("TOTAL COMPILED: " + totalCompiled + "\n");
//		
//		//sort list
//		List<Entry<String, Integer>> list = new ArrayList<>(errorIDs.entrySet());
//        list.sort(Entry.<String, Integer>comparingByValue().reversed());
//
//        Map<String, Integer> result = new LinkedHashMap<>();
//        for (Entry<String, Integer> entry : list) {
//        	logger.debug(entry.getValue() + ", " + errorMessages.get(entry.getKey()) + ", " + entry.getKey() + "\n");
//            //result.put(entry.getKey(), entry.getValue());
//        }
//		
//	}
//	
//	/**
//	 * Test the type suggestions for 47 tasks.
//	 */
//	public static void suggestionTests() {
//		long start;
//		
//		logger.debug("COMPILING FOR 47 TASKS\n");
//		start = System.currentTimeMillis();
//		String before = "class Main {\npublic static void main(String[] args){\n";
//		String after = "\nreturn;\n }\n}\n";
//		InputHandler.before = before;
//		InputHandler.after = after;
//		
//		//for each task
//		for(String task : DataHandler.queries) {
//			logger.debug("TASK: " + task + "\n");
//
//			
//			//find the snippets
//			List<Snippet> snippets;
//			snippets = Searcher.getSnippets(task);
//			if(snippets == null) continue;
//			
//			//evaluate
//			snippets = Evaluator.evaluate(null, snippets, before, after);
//			
//			//get type suggestions
//			snippets = TypeSuggestions.getTypeSuggestions(snippets, before, after, null);
//			
//			
//			List<String> types = TypeRecommender.sortIOTypes(snippets);
//			for(String type : types) {
//				logger.debug("TYPE: " + type + "\n");
//			}
//			
//		}		
//		logger.debug("TIME: " + (System.currentTimeMillis() - start) + "ms\n");
//	}

}
