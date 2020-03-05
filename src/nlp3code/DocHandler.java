package nlp3code;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import nlp3code.code.Fragment;
import nlp3code.code.Snippet;
import nlp3code.cycler.CycleAnswersHandler;
import nlp3code.visitors.ImportDeclarationVisitor;
import nlp3code.visitors.MethodVisitor;
import nlp3code.visitors.PackageDeclarationVisitor;

/**
 * The DocHandler class includes functionality for modifying IDocuments and handling other workspace interactions.
 */
public class DocHandler {
	//the current documents abstract syntax tree
	public static CompilationUnit documentAST = null;
	//the current project
	public static IJavaProject currentProject = null;
	//the global eclipse parser
	public static ASTParser eclipseParser = null;
	//documents current import statements
	public static List<String> imports = null;
	//documents current import offset
	public static int importStart = -1;
	private static String fileName = null;
	private static IEditorPart currentEditor = null;
	
	/**
	 * Clear the cache on document change.
	 */
	public static void documentChanged() {
		imports = null;
		importStart = -1;
		documentAST = null;
		//currentProject = null;
		fileName = null;
		currentEditor = null;
	}
	
	/**
	 * Function to add a Snippet to the current document.
	 */
	public static void addSnippet(String comment, Snippet snippet, int offset, int length) {
		int fragments = snippet.getNumFragments();
		InputHandler.previousLength = 0;
		InputHandler.previousOffset = 0;
		IDocument document = getDocument();
		
		for(int i=fragments-1; i>=0; i--) {
			Fragment fragment = snippet.getFragment(i);
			
			//add imports to top
			if(fragment.getType() == Fragment.IMPORTS) {
				//have to have more than 1 import
				if(fragment.size() >= 1) {
					String replacement = null;
					
					try {
						importStart = getImportOffset(document.get(0, offset) + document.get(offset + length, document.getLength()-(offset+length)));
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
					
					//if the document has imports remove any duplicates
					if(DocHandler.imports != null && DocHandler.imports.size() > 0) {
						for(String im : DocHandler.imports) {
							fragment.deleteLineContaining(im.trim());
						}
						replacement = fragment.getFormattedCode();
					}else {
						//add spacing to fragment
						replacement = fragment.getFormattedCode() + "\n";
					}
					
					//add imports
					replace(replacement, importStart, 0);
					
					//update offset
					InputHandler.previousOffset += replacement.length();
				}
			}
			
			//insert snippet at offset
			if(fragment.getType() == Fragment.SNIPPET) {
				String replacement = comment + fragment.getFormattedCode();
				replace(replacement, offset, length);
				InputHandler.previousLength += replacement.length();
				updateOffset(replacement);
			}
		}
	}
	
	/**
	 * Adds to the current offset, the beginning of given string.
	 */
	private static void updateOffset(String text) {
		Display.getDefault().asyncExec(new Runnable() 
  	    {
  	      public void run()
  	      {
					String contents = getDocument().get();
					InputHandler.previousOffset += contents.indexOf(text);
  	      }
  	    });
	}
	
	/**
	 * Replaces range with given String.
	 */
	public static void replace(String replacement, int offset, int length) {
		Display.getDefault().asyncExec(new Runnable() 
  	    {
  	      public void run()
  	      {
  	    	try {
  	    		CycleAnswersHandler.inserting = true;
  	    		IDocument document = getDocument();
				document.replace(offset, length, replacement);
				CycleAnswersHandler.inserting = false;
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
  	      }
  	    });
	}
	
	/**
	 * Returns the active editor, otherwise null.
	 */
	public static IEditorPart getEditor() {
		//if(currentEditor != null) return currentEditor; //shouldnt cache this so we can keep active up to date
		//get editor from workbench
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench == null) return null;
		IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
		if(activeWindow == null) return null;
		IWorkbenchPage activePage = activeWindow.getActivePage();
		if(activePage == null) return null;
		currentEditor = activePage.getActiveEditor();
		return currentEditor;
	}
	
	/**
	 * Returns the active document, otherwise null.
	 */
	public static IDocument getDocument() {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return null;
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			
			//get document from text editor
			IDocumentProvider provider = textEditor.getDocumentProvider();
			if(provider == null) return null;
			IEditorInput input = activeEditor.getEditorInput();
			if(input == null) return null;
			IDocument document = provider.getDocument(input);
			return document;
		}
		
		//otherwise return null
		return null;
	}
	
	public static int getCurrentOffset() {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int offset = -1;
			
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			
			//get selection
			ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
			if (selectionProvider.equals(null)) return offset;
			ISelection selection = selectionProvider.getSelection();
			if (selection.equals(null)) return offset;
			
			//if selection is text
			if (selection instanceof ITextSelection) {
		        ITextSelection textSelection = (ITextSelection)selection;
		        //int num = textSelection.getStartLine();
		        offset = textSelection.getOffset();
			}
			else {
				return offset;
			}
		}
			
		return offset;
	}
	
	/**
	 * Return the current line.
	 */
	public static String getCurrentLine() {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return null;
		
		String line = null;
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			
			//get selection
			ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
			if (selectionProvider.equals(null)) return line;
			ISelection selection = selectionProvider.getSelection();
			if (selection.equals(null)) return line;
			
			//if selection is text
			if (selection instanceof ITextSelection) {
		        ITextSelection textSelection = (ITextSelection)selection;
		        //int num = textSelection.getStartLine();
		        int offset = textSelection.getOffset();
		        IDocument document = getDocument();
		        if(document == null) return line;
		        try {
					line = document.get(document.getLineOffset(document.getLineOfOffset(offset)), document.getLineLength(document.getLineOfOffset(offset)));
				} catch (BadLocationException e) {
					//keep going
					return null;
				}
			}
			else {
				return line;
			}
		}
		
		return line;
	}
	
	
	public static int getImportOffset(String surrounding) {
		if(importStart != -1) return importStart;
		if(surrounding == null) surrounding = getDocument().get();
		CompilationUnit ast = null;
		importStart = 0;
		
		//get the current document AST
		if(documentAST != null) {
			ast = DocHandler.documentAST;
		}
		else {
			//generate ast
			if(eclipseParser == null) initializeEclipseParser();
			eclipseParser.setSource(surrounding.toCharArray());
			ast = (CompilationUnit) eclipseParser.createAST(null);
			documentAST = ast;
		}
		
		//if import statements exist
		ImportDeclarationVisitor idv = new ImportDeclarationVisitor();
        ast.getRoot().accept(idv);
        List<ImportDeclaration> importNodes = idv.imports;
        if(importNodes != null && importNodes.size() > 0) {
        	//insert will be before first import
        	importStart = importNodes.get(0).getStartPosition();
        	//add to list
        	imports = new ArrayList<String>();
        	for(ImportDeclaration i : importNodes) {
        		String im = i.toString();
        		if(im != null) imports.add(i.toString());
        	}	
        }
        else {
        	//does a package declaration exist
        	PackageDeclarationVisitor pdv = new PackageDeclarationVisitor();
        	ast.getRoot().accept(pdv);
        	PackageDeclaration pk = pdv.pk;
        	//if it exists, the offset is on the line after
        	if(pk !=  null) importStart = pk.getStartPosition() + pk.getLength() + 1;
        }
		
		return importStart;
	}

	/**
	 * Returns the offset for the currently selected line.
	 */
	public static int getCurrentLineOffset() {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int line = -1;
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			
			//get selection
			ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
			if (selectionProvider.equals(null)) return line;
			ISelection selection = selectionProvider.getSelection();
			if (selection.equals(null)) return line;
			
			//if selection is text
			if (selection instanceof ITextSelection) {
		        ITextSelection textSelection = (ITextSelection)selection;
		        int num = textSelection.getStartLine();
		        IDocument document = getDocument();
		        if(document == null) return line;
		        try {
					line = document.getLineOffset(num);
				} catch (BadLocationException e) {
					System.err.println("Error trying to get current line!");
					e.printStackTrace();
				}
			}
			else {
				return line;
			}
		}
		
		return line;
	}
	
	/**
	 * Returns the length for the currently selected line.
	 */
	public static int getCurrentLineLength() {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int line = -1;
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			
			//get selection
			ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
			if (selectionProvider.equals(null)) return line;
			ISelection selection = selectionProvider.getSelection();
			if (selection.equals(null)) return line;
			
			//if selection is text
			if (selection instanceof ITextSelection) {
		        ITextSelection textSelection = (ITextSelection)selection;
		        int num = textSelection.getStartLine();
		        IDocument document = getDocument();
		        if(document == null) return line;
		        try {
					line = document.getLineLength(num);
				} catch (BadLocationException e) {
					System.err.println("Error trying to get current line!");
					e.printStackTrace();
				}
			}
			else {
				return line;
			}
		}
		
		return line;
	}
	
	public static int getLineOfOffset(int offset) {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int line = -1;
		
		IDocument document = getDocument();
		if(document == null) return -1;
		
		try {
			line = document.getLineOfOffset(offset);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return line;
	}
	
	public static int getLineOffset(int line) {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int offset = -1;
		
		IDocument document = getDocument();
		if(document == null) return -1;
		
		try {
			offset = document.getLineOffset(line);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return offset;
	}
	
	public static int getLineLength(int line) {
		//get editor
		IEditorPart activeEditor = getEditor();
		if(activeEditor == null) return -1;
		
		int length = -1;
		
		IDocument document = getDocument();
		if(document == null) return -1;
		
		try {
			length = document.getLineLength(line);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return length;
	}
	
	/**
	 * Return the current filename.
	 * @return
	 */
	public static String getFileName() {
		//cached filename
		if(fileName != null) return fileName;
		
		//otherwise get editor
		IEditorPart activeEditor = getEditor();
		
		//if we can't get editor use cached
		if(activeEditor == null) {
			activeEditor = currentEditor;
		}
		
		//must be a text editor
		if(activeEditor instanceof ITextEditor) {
			//convert
			ITextEditor textEditor = (ITextEditor)activeEditor;
			fileName = textEditor.getTitle().replace(".java", "");
		}
		
		return fileName;
	}
	
	public static void setFileName(String name) {
		fileName = name;
	}
	
	public static IJavaProject getJavaProject() {
		if(currentProject != null) return currentProject;
		IEditorPart editor = getEditor();
		if(editor == null) return null;
		IEditorInput input = editor.getEditorInput();
		if(input == null) return null;
		IResource file2 = ((IFileEditorInput)input).getFile();
		IProject pp = file2.getProject();
		currentProject = (IJavaProject) JavaCore.create(pp);
		return currentProject;
	}
	
	/**
	 * Get open IJavaProject's classpath of external libraries.
	 * @return
	 */
	public static String getClassPath() {
		
		IJavaProject project = getJavaProject();
		if(project == null || !project.exists()) {
			System.out.println("Could not get Java Project.");
			return null;
		}
		IClasspathEntry[] classPathEntries = null;
		try {
			classPathEntries = project.getRawClasspath();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		//if we have no classpath entries return null
		if(classPathEntries == null || classPathEntries.length == 0) {
			System.out.println("No classPath entries.");
			return "";
		}
		
		String classPath = "";
		
		//otherwise, for each class path entry
		for(IClasspathEntry c : classPathEntries) {
			//if a library, add to classPath
			if(c.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				classPath += c.getPath().toString() + ";";
		}
		if(classPath.length()>1) {
			//cut off semi-colon
			classPath = classPath.substring(0, classPath.length()-1);
		}
		return classPath;
	}
	
	public static String getAtOffset(IDocument document, int offset, int length) {
		if(document == null) document = getDocument();
		if(document == null) return null;
		String result = null;
		try {
			result = document.get(offset, length);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Initializes the Eclipse ASTParser.
	 */
	public static void initializeEclipseParser() {
		eclipseParser = ASTParser.newParser(AST.JLS11);
	}

	/**
	 * Adds function to open document.
	 * @param Function as String to add.
	 * @return 0 on success, non-zero value on failure.
	 */
	public static int addFunction(String function) {
		//get the document
		IDocument document = getDocument();
		
		//if null, return null
		if(document == null) return -1;
		
		//parse document
		ASTParser parser = ASTParser.newParser(Activator.level);
		parser.setSource(document.get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        //AST ast = cu.getAST();
        
        //get list of methods
        MethodVisitor mv = new MethodVisitor();
        cu.getRoot().accept(mv);
        List<MethodDeclaration> methodNodes = mv.methods;
        if(methodNodes == null || methodNodes.size() == 0) {
        	//for now return error
        	//the assumption is code will always be inserted in a function
        	return -1;
        }
        MethodDeclaration last = methodNodes.get(methodNodes.size()-1);
        int pos = last.getStartPosition() + last.getLength();
        
        function = indent(function, 1);
        
        //use async replace
		replace("\n\n" + function, pos, 0);
				
		return 0;
		
	}

	public static void addImportStatements(ArrayList<String> importList) {
		IDocument document = getDocument();
		if(document == null) return;
		if(importStart == -1) importStart = getImportOffset(document.get());
		
		if(imports != null && imports.size() > 0) {
			for(String i : DocHandler.imports) {
				if(importList.contains(i.trim())) {
					importList.remove(i.trim());
				}
			}
		}
		
		String importBlock = "";
		for(String i : importList) {
			importBlock += i +"\n";
		}
		
		replace(importBlock, importStart, 0);
	}

	public static String[] getSurrounding(int offset, int length) {
		IDocument document = getDocument();
		String[] surrounding = new String[2];
		
		//if null, return null
		if(document == null) {
			System.err.println("Error: Could not get document!");
			return null;
		}
		String docString = document.get();
		
		//if the range is beyond the document length, return null
		if(offset+length > document.getLength()) {
			System.err.println("Error: Range is beyond document length.");
			return null;
		}
       
        String before = docString.substring(0, offset);
        String after = docString.substring(offset+length);
        surrounding[0] = before;
        surrounding[1] = after;
        
        return surrounding;
	}
	
	public static String indent(String code, int offset) {
		String indented = "";
		String[] array = code.split("\n");
		for(String line : array) {
			for(int i=0; i<offset; i++) {
				indented += "\t";
			}
			indented += line + "\n";
		}
		
		return indented;
	}
}
