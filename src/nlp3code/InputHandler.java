package nlp3code;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;

import nlp3code.code.Snippet;
import nlp3code.cycler.CycleAnswersHandler;
import nlp3code.fixer.Integrator;
import nlp3code.listeners.BeginTestingListener;
import nlp3code.listeners.CycleDocListener;
import nlp3code.listeners.QueryDocListener;
import nlp3code.listeners.TypeDocListener;
import nlp3code.recommenders.TaskRecommender;
import nlp3code.recommenders.TypeRecommender;

public class InputHandler extends AbstractHandler{
	private static JavaParser javaParser = null;
	//listeners
	public static QueryDocListener queryDocListener = new QueryDocListener();
	public static CycleDocListener cycleDocListener = new CycleDocListener();
	//documents with a query listener
	public static List<IDocument> documents = new ArrayList<>();
	//previous search query
	public static List<String> previousQueries = new ArrayList<>();
	public static List<Snippet> previousSnippets = null;
	public static String previousInfo = null;
	public static int previousIndex = 0;
	public static String previousQuery = null;
	public static int previousLength = 0;
	public static int previousOffset = 0;
	public static Snippet previousSnippet = null;
	//whitespace before
	public static String whitespaceBefore = null;
	// the context we are currently inserting at
	public static int insertionContext = -1;
	public static Job evalJob = null;
	public static BeginTestingListener beginTestingListener = new BeginTestingListener();
	static public String before;
	static public String after;
	public static TypeDocListener typeDocListener = new TypeDocListener();
	//editor for testing, for some reason getting this at the time returns null
	public static IEditorPart editor = null;
	
	//insertion contexts depending on user's cursor
	//some features would need to be tackled differently depending on context
	//we mostly just handle inserting inside a function for now
	
	//we are inside a function
	public static final int FUNCTION = 0;
	
	//we are inside a function and it is main
	public static final int MAIN = 1;
	
	//we are inside a class but not a function
	public static final int CLASS = 2;
	
	//we are not within a class
	public static final int OUTSIDE = 3;
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if(Activator.loaded == false) return null;
		
		//get the document
		IDocument document = DocHandler.getDocument();
		DocHandler.documentChanged();
		if(document == null) return null;
		
		//if this is a new document
		if (!InputHandler.documents.contains(document)) {
			document.addDocumentListener(InputHandler.queryDocListener);
			QueryDocListener.currentDocument = document;
		}
		
		int offset = DocHandler.getCurrentOffset();
		if(offset == -1) return null;
		
		String line = null;
		try {
			line = document.get(document.getLineOffset(document.getLineOfOffset(offset)), document.getLineLength(document.getLineOfOffset(offset)));
		} catch (BadLocationException e) {
			line = null;
		}
		if(line == null || line == "") return null;
	
		
		String trimmed = line.trim();
		
		//if formatted correctly, preform the query
		doQuery(offset, line);
		
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 *   Extracts a query from the current line the cursor is on, and performs that query to retrieve
	 *   top code snippets for that query.
	 *   @param event The last document event that was identified to create a legitimate query.
	 *   @param line The text in the current line that contains the query.
	 */
	public static int doQuery(int lineOffset, String line) {
		editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		
		if(Activator.first == true) {
			Activator.queryTests();
			Activator.first = false;
		}
		
		TypeRecommender.generated = null;
		
		TypeRecommender.canTest = false;
		TypeRecommender.canRecommend = false;
		TypeRecommender.testing = false;
		
		//get whitespace
		whitespaceBefore = line.substring(0, line.indexOf(line.trim()));
		
		String query = getQuery(line, TaskRecommender.queryChar);
		if (query.length() == 0) return -1;
		
		//get snippets
		List<Snippet> snippets = Searcher.getSnippets(query);
		if (snippets == null) {
	    	System.out.println("Error! Snippet list is null!");
	    	return -1;
	    }
	    if (snippets.size() == 0) {
	    	System.out.println("Could not find snippets for task.");
	    	return -1;
	    }
	    
	    
	    int lineNum = DocHandler.getLineOfOffset(lineOffset);
	    int offset = DocHandler.getLineOffset(lineNum);
	    int length = DocHandler.getLineLength(lineNum);
	    if (offset < 0 || offset > QueryDocListener.currentDocument.getLength()) return -1;
	    if (length > QueryDocListener.currentDocument.getLength() || offset + length > QueryDocListener.currentDocument.getLength()) return -1;
	    
	    before = null;
	    after = null;
    	try {
    	    //get surrounding code
    	    before = QueryDocListener.currentDocument.get(0,offset);
			after = QueryDocListener.currentDocument.get(offset + length, QueryDocListener.currentDocument.getLength()-(offset+length));
			
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
    	
    	if(before == null || after == null) return -1;
    	
		insertionContext = getContext(before, after);
		
	    //evaluate snippets
	    snippets = processSnippets(snippets, before, after);
	    if (snippets == null || snippets.size() == 0) return -1;
	    
	    //store snippets
	    InputHandler.previousSnippets = snippets;
		
		String comment = generateQueryComment(query);
		InputHandler.previousInfo = comment;
		
		DocHandler.addSnippet(comment, snippets.get(0), offset, length);
		
		InputHandler.previousSnippet = snippets.get(0);
		InputHandler.previousQuery = query;
		
		QueryDocListener.currentDocument.addDocumentListener(InputHandler.cycleDocListener);
		//associate a document
		InputHandler.cycleDocListener.currentDocument = QueryDocListener.currentDocument;
		CycleAnswersHandler.changedDoc = false;
		
		//enable testing on the partial list
		Evaluator.canTest();
		
		return 0;
	}
	
	public static String generateQueryComment(String query) {
		String queryComment = whitespaceBefore + "//Query: " + query + "\n";
		String infoComment = whitespaceBefore + "//Retrieved: " + Evaluator.retrieved + ", Compiled: " + Evaluator.compiled + ", Passed: " + Evaluator.passed + "\n";
		return queryComment + infoComment;
	}
	
	/**
	 * Parses the file and determines the insertion context.
	 * If javaParser has trouble finding our line, we'll assume the context is OUTSIDE.
	 */
	public static int getContext(String before, String after) {
		initializeParser();
		
		//construct our flag with a random key so we aren't reserving any words
		int key = Activator.random.nextInt(1000000);
		String flag = "NLP3Code_comment " + key;
		
		//add our flag
		ParseResult result = javaParser.parse(before + "//" + flag +"\n{\n" + "int i=0;\n" + "}\n" + after);
		if(!result.getResult().isPresent()) return OUTSIDE;
		CompilationUnit cu = (CompilationUnit) result.getResult().get();
		
		Node node = null;
		//find comment
		for(Comment c : cu.getComments()) {
			if(c.getContent().equals(flag)) {
				if(c.getCommentedNode().isPresent()) {
					node = c.getCommentedNode().get();
				}
			}
		}
		
		if(node == null) return OUTSIDE;
		
		//is there a method declaration above this block statement?
		Optional<MethodDeclaration> method = node.findAncestor(MethodDeclaration.class);
		if(method.isPresent()) {
			//is it main?
			if(Integrator.isMain(method.get())) return MAIN;
			//no, just a function
			return FUNCTION;
		}
		
		Optional<ClassOrInterfaceDeclaration> classNode = node.findAncestor(ClassOrInterfaceDeclaration.class);
		if(classNode.isPresent()) {
			return CLASS;
		}
		
		//otherwise outside
		return OUTSIDE;
	}
	
	/**
	 * Calls the evaluation process within a UI job with progress reporting.
	 */
	private static List<Snippet> processSnippets(final List<Snippet> snippets, String before, String after) {
		AtomicReference<List<Snippet>> result = new AtomicReference<>();
		
		//progress of evaluation
		IRunnableWithProgress process = new IRunnableWithProgress() {
             @Override
             public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Evaluating Snippets", 100);
					
					//we define a background job that evaluates snippets
					evalJob= new Job("Evaluating Snippets") {
				        @Override
				        protected IStatus run(IProgressMonitor monitor) {
				        	Evaluator.evaluate(monitor, snippets, before, after);
				            return Status.OK_STATUS;
				        }

				    };
				    
				    //set our queue to empty
				    Evaluator.compilingSnippets = new ArrayList<>();
				    
				    //schedule the job
				    evalJob.schedule();
				    
				    //wait for a compiling snippet or job to be done
				    while(Evaluator.compilingSnippets.isEmpty() && evalJob.getState() != Job.NONE && !monitor.isCanceled()) {
				    	Thread.sleep(100);
				    }
				    if(monitor.isCanceled()) {
				    	evalJob.cancel();
				    }
				    
					result.set(Evaluator.compilingSnippets);
			}
		};
		
		//begin
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setActive();
        try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(process);
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalStateException e) {
			//ignore
		}
		
        //return the final list for querydoclistener
		return result.get();
	}

	
	/**
	 * Function that extracts query from line.
	 * @param line The line to extract query from.
	 * @return A string query.
	 */
	public static String getQuery(String line, String queryChar) {
		String query;
		
		//trim whitespace
		query = line.trim();
		query = query.toLowerCase();
		
		//extract
		if (query.endsWith(queryChar)) query = query.substring(0, query.length()-1);
		if (query.startsWith(queryChar)) query = query.substring(1);
		
		//trim any whitespace between question mark
		query = query.trim();
		
		//if there are any invalid characters, return empty
		if (!query.matches("[abcdefghijklmnopqrstuvwxyz, ]*")) return "";
		
		return query;
	}
	
	/**
	 * Initializes parser if first use.
	 */
	private static void initializeParser() {
		if(javaParser == null) {
			if(Evaluator.parser == null) Evaluator.initializeParser();
			javaParser = Evaluator.parser;
		}
	}
	
	/**
	 * Clears the previous query information.
	 */
	public static void clear() {
		System.out.println("reset");
		previousQueries.clear();
		//previousOffset = 0;
		previousInfo = null;
		previousIndex = 0;
		//previousQuery = null;
		//previousSnippet = null;
		//previousLength = 0;
		//previousSnippets = null;
		//make sure theres no running eval when we clear
		if(evalJob != null) evalJob.cancel();
		evalJob = null;
	}
	
	
}
