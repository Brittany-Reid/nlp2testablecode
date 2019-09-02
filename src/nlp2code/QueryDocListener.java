package nlp2code;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * class QueryDocListener
 *   Implements the required functionality to conduct code snippet queries by listening
 *    to document changes for a user to type a query in the format: ?{query}?
 */
public class QueryDocListener implements IDocumentListener {	
	public static IEditorPart editorPart;
	private static String whitespaceBefore;
	static Logger logger = Activator.getLogger();
	static List<String> testInput;
		
	/**
	 * Function that activates every time the current edited document is changed.  
	 * Simply put, this document listener listens for ?{query}? format queries in the document,
	 * and conducts a query whenever this format is identified in the document.
	 * This allows for easy query-making without using any external buttons or widgets.
	 */
	@Override
    public void documentChanged(DocumentEvent event) 
    {
		//get the event text
		String insertion = event.getText();
		if(insertion == "") return;
		
		//was the document change an undo. make sure we don't make a query again.
		String check_undo = getLine();
		check_undo = check_undo.trim();
		if (check_undo.startsWith("?")) check_undo = check_undo.substring(1);
		if (check_undo.endsWith("?")) check_undo = check_undo.substring(0, check_undo.length()-1);
		if (InputHandler.previous_queries.contains(check_undo)) {
			InputHandler.previous_queries.remove(InputHandler.previous_queries.indexOf(check_undo));
			return;
		}
		
		//otherwise, lets check if we have a correctly formatted query
		if (insertion.length() >= 1) {
			String line = getLine();
			String newline = line.trim();
			if (!(newline.endsWith("?"))) return;
			//if formatted correctly, preform the query
			doQuery(event,line);
		}
    }
		
	/**
	 *   Extracts a query from the current line the cursor is on, and performs that query to retrieve
	 *   top code snippets for that query.
	 *   @param event The last document event that was identified to create a legitimate query.
	 *   @param line The text in the current line that contains the query.
	 */
	private static int doQuery(DocumentEvent event, String line) {
		
		//on first run, preform our tests - move this to junit soon
		if(Activator.first == true) {
			
			//Activator.checkArgs();
			Activator.first = false;
			if(logger.isDebugEnabled()) {
				//Activator.tests(1);
			}
		}
		
		//get whitespace from line
		whitespaceBefore = line.substring(0, line.indexOf(line.trim()));
		
		//extract query from current line, if empty do nothing
		String query = getQuery(line);
		if (query.length() == 0) return -1;
		
		//get snippets
		List<Snippet> snippets = Searcher.getSnippets(query);
	    if (snippets.equals(null)) {
	    	System.out.println("Error! Snippet list is null!");
	    	return 9;
	    }
	    if (snippets.size() == 0) {
	    	System.out.println("Could not find snippets for task.");
	    	return -1;
	    }
	    
		// Get the current document (for isolating substring of text in document using line number from selection).
		IDocument document = getDocument();
		if(document == null) return -1;
		
	    // Store some previous search and query data so we can have undo functionality.
	    InputHandler.previous_search.clear();
	    InputHandler.previous_search = snippets;
	    InputHandler.previous_query = query;
	    InputHandler.previous_queries.add("?" + query+ "?");
	    
	    try {
	    	//get insert location information 
	    	int lineNum = document.getLineOfOffset(event.getOffset());
	    	int lineOffset = document.getLineOffset(lineNum);
	    	int lineLength = document.getLineLength(lineNum);
	    	if (lineOffset < 0 || lineOffset > document.getLength()) return -1;
      		if (lineLength > document.getLength() || lineOffset + lineLength > document.getLength()) return 11;
      		
      		//get surrounding code
	    	String before = document.get(0,lineOffset);
	    	String after = document.get(lineOffset + lineLength, document.getLength()-(lineOffset+lineLength));
	    	
		    //evaluate our snippets
		    snippets = Evaluator.evaluate(snippets, before, after);
	    	
	    	//overwrite fixed code
//			code = new Vector<>();
//			for(Snippet s : snippets) {
//				code.add(s.getFormattedCode());
//			}
			InputHandler.previous_search = snippets;
			
			//get info comment
			String queryComment = whitespaceBefore + "//Query: " + query + "\n";
      		String infoComment = whitespaceBefore + "//Retrieved: " + Evaluator.retrieved + ", Compiled: " + Evaluator.compiled + ", Passed: " + Evaluator.passed + "\n";
      		String replacementText = queryComment + infoComment + snippets.get(0).getFormattedCode();
      		InputHandler.previousInfo = queryComment + infoComment;
      		
      		//add code to document
      		addToDocument(replacementText, lineOffset, lineLength);
      		
      		// Add the CycleDocListener for cycling through snippets.
      		document.addDocumentListener(InputHandler.doclistener);
      		
      		// Store the previous length so we can have both the previous offset and previous length to know where the inserted snippet is in the document.
      		InputHandler.previous_length = replacementText.length();
      		ITextEditor editor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
      		editor.selectAndReveal(lineOffset+replacementText.length(), 0);
      		
      		addImports(snippets.get(0), replacementText);
      		
      		//reset changed
      		CycleAnswersHandler.changed_doc = false;
	    } catch (BadLocationException e) {
			System.err.println("Error with inserting code after Autocomplete");
			e.printStackTrace();
		} catch (IllegalStateException e) {
			System.err.println("ILLEGAL STATE EXCEPTION");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("ILLEGAL ARGUMENT EXCEPTION");
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.err.println("NULL POINTER EXCEPTION"); 
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Function that extracts query from line.
	 * @param line The line to extract query from.
	 * @return A string query.
	 */
	private static String getQuery(String line) {
		String query;
		
		//trim whitespace
		query = line.trim();
		query = query.toLowerCase();
		
		//extract
		if (query.endsWith("?")) query = query.substring(0, query.length()-1);
		if (query.startsWith("?")) query = query.substring(1);
		
		//trim any whitespace between question mark
		query = query.trim();
		
		//if there are any invalid characters, return empty
		if (!query.matches("[abcdefghijklmnopqrstuvwxyz ]*")) return "";
		
		return query;
	}
		
	/**
	 * Returns the IDocument. Returns Null if error occurs.
	 */
	private static IDocument getDocument() {
		// Need to retreive the offset of the query, so we know where to insert retreived code snippets into.
		// We need the current ITextEditor and document to do this.
		editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ISelectionProvider selectionProvider = ((ITextEditor)editorPart).getSelectionProvider();
		if (selectionProvider.equals(null)) return null;
		ISelection selection = selectionProvider.getSelection();
		if (selection.equals(null)) return null;
		ITextEditor ite = (ITextEditor)editorPart;
		if (ite.equals(null)) return null;
		// Get the current document (for isolating substring of text in document using line number from selection).
		
		return ite.getDocumentProvider().getDocument(ite.getEditorInput());
	}
	
	/**
	 * Adds imports to the document if there are any new. We leave these on cycle for the user to clean up.
	 */
	public static void addImports(Snippet snippet, String text) {
		//by default, add to start
		int offset = 0;
		String importBlock = "";
		
		List<String> imports = new ArrayList<>(snippet.getImportList());
		if(imports == null || imports.size() < 1) return;
		
		//parse the document
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setSource(getDocument().get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        AST ast = cu.getAST();
        
        //get import statements
        ImportDeclarationVisitor idv = new ImportDeclarationVisitor();
        cu.getRoot().accept(idv);
        List<ImportDeclaration> importNodes = idv.imports;
        if(importNodes != null && importNodes.size() > 0) {
        	//insert will be before first import
        	offset = importNodes.get(0).getStartPosition();
        	//remove duplicates
        	for(ImportDeclaration i : importNodes) {
        		if(imports.contains(i.toString().trim())) {
        			imports.remove(i.toString().trim());
        		}
        	}
        }else {
        	//find package node
        	PackageDeclarationVisitor pdv = new PackageDeclarationVisitor();
        	cu.getRoot().accept(pdv);
        	PackageDeclaration pk = pdv.pk;
        	if(pk !=  null) offset = pk.getStartPosition() + pk.getLength() + 1;
        }
        
        //construct import block
        for(String i : imports) {
        	importBlock += i + "\n";
        }
        
        final int fOffset = offset;
        final String finalImportB = importBlock;
        
        
        //based on offset, insert
        // To ensure the Document doesnt COMPLETELY BREAK when inserting a code snippet, queue the insertion for when the document is inactive.
  		Display.getDefault().asyncExec(new Runnable() 
  	    {
  	      public void run()
  	      {
  	    	try {
  	    		ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
  				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				doc.replace(fOffset, 0, finalImportB);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
  	      }
  	    });
  		
  		// Get the offset for the inserted code snippet, so we can use it to identify the start and end positions of the inserted code snippet in the document.
  		Display.getDefault().asyncExec(new Runnable()
  		{
  			public void run()
  			{
  				ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
  				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				String document = doc.get();
				InputHandler.previous_offset = document.indexOf(text);
  			}
  		});
	}
	
	/**
	 * Adds given text to the document, returns the offset.
	 * @param text The text to insert.
	 */
	private static Integer addToDocument(String text, int lineOffset, int lineLength) {
		
  		// To ensure the Document doesnt COMPLETELY BREAK when inserting a code snippet, queue the insertion for when the document is inactive.
  		Display.getDefault().asyncExec(new Runnable() 
  	    {
  	      public void run()
  	      {
  	    	try {
  	    		ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
  				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				doc.replace(lineOffset, lineLength, text);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
  	      }
  	    });
  		
  		// Get the offset for the inserted code snippet, so we can use it to identify the start and end positions of the inserted code snippet in the document.
  		Display.getDefault().asyncExec(new Runnable()
  		{
  			public void run()
  			{
  				ITextEditor editor = (ITextEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
  				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				String document = doc.get();
				InputHandler.previous_offset = document.indexOf(text);
  			}
  		});
  		
  		return InputHandler.previous_offset;
	}
	
	public static String getWhitespaceBefore() {
		return whitespaceBefore;
	}
	
	/*
		 * Function getUrls
		 * 	 Given a String array of Stack Overflow forum thread IDs, return a vector of Stack Overflow question URLS.
		 *  
		 *   Input: String[] urls - an array of Stack Overflow forum thread IDs.
		 *   Returns: Vector<String> - A vector of Stack Overflow forum URLs.
		 */
		private static Vector<String> getUrls(String[] urls) {
			Vector<String> google_urls = new Vector<String>();
			for (int i=0; i<urls.length; i++) {
				google_urls.add("http://stackoverflow.com/questions/" + urls[i]);
			}		
			return google_urls;
		}
		

		
		/*
		 * Function getLine
		 *   Retrieves the text on the current line the text edit cursor is on.
		 *   
		 *   Returns: String - The line of text for the line the cursor is on.
		 */
		private static String getLine() {
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			IDocument doc;
			int offset;
			String line = "";
			// If we are dealing with a text editor. 
			if(editor instanceof ITextEditor) {
				// Get the current selection of the document (for determining what line a query is on).
				ISelectionProvider selectionProvider = ((ITextEditor)editor).getSelectionProvider();
			    if (selectionProvider.equals(null)) return "";
			    ISelection selection = selectionProvider.getSelection();
			    if (selection.equals(null)) return "";
			    ITextEditor ite = (ITextEditor)editor;
			    if (ite.equals(null)) return "";
			    // Get the current document (for isolating substring of text in document using line number from selection).
			    doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
			    if (doc.equals(null)) return "";
			    
			    if (selection instanceof ITextSelection) {
			        ITextSelection textSelection = (ITextSelection)selection;
			        try {
			        	// Get the line number we are currently on.
			        	if (textSelection.equals(null)) return "";
			        	offset = textSelection.getOffset();
			        	// Get the string on the current line and use that as the query line to be auto-completed.
			        	if (offset > doc.getLength() || offset < 0) return "";
						line = doc.get(doc.getLineOffset(doc.getLineOfOffset(offset)), doc.getLineLength(doc.getLineOfOffset(offset)));
					} catch (BadLocationException e) {
						System.out.println("Error with getting input query.");
						e.printStackTrace();
						return "";
					}
			    }
			} else {
				// If we are not dealing with a text editor.
				return "";
			}
			return line;
		}
		
		// Unused.
		@Override
        public void documentAboutToBeChanged(DocumentEvent event) { }
		
		
		public static int getImportOffset() {
			return -1;
		}
}
