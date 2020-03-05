package nlp3code;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
		//DataHandler.limit = 100000L;
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

}
