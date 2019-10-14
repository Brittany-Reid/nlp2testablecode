package nlp2code;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.stanford.nlp.pipeline.CoreNLPProtos.Document;

/** 
 * The DocumentHandler class handles inserting, replacing and removing text from the user's IDocument.
 */
public class DocumentHandler {
	
	/**
	 * Return the document being modified.
	 * @return The IDocument, or null.
	 */
	public static IDocument getDocument() {
		if(QueryDocListener.editorPart ==  null) {
			return null;
		}
		
		//does the current editor have a selection?
		ISelectionProvider selectionProvider = ((ITextEditor)QueryDocListener.editorPart).getSelectionProvider();
		if (selectionProvider.equals(null)) return null;
		ISelection selection = selectionProvider.getSelection();
		if (selection.equals(null)) return null;
		
		//get the editor
		ITextEditor ite = (ITextEditor)QueryDocListener.editorPart;
		if (ite.equals(null)) return null;
		
		//get the document
		return ite.getDocumentProvider().getDocument(ite.getEditorInput());
	}
	
	/**
	 * Removes content at the given position within the current document.
	 * @param offset The offset to begin at.
	 * @param length The length to remove beginning from the offset.
	 * @return Returns 0 on success.
	 */
	public static int removeAt(int offset, int length) {
		//get the document
		IDocument document = getDocument();
		
		//if null, return null
		if(document == null) return -1;
		
		//if the range is beyond the document length, return null
		if(offset+length > document.getLength()) return -2;
		
		//try to delete
		try {
			document.replace(offset, length, "");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	/**
	 * Replaces the given offset and length with the given String contents.
	 * @return Returns 0 on success.
	 */
	public static int replaceAt(int offset, int length, String contents) {
		//get the document
		IDocument document = getDocument();
		
		//if null, return null
		if(document == null) return -1;
		
		//if the range is beyond the document length, return null
		if(offset+length > document.getLength()) return -2;
		
		//try to delete
		try {
			document.replace(offset, length, contents);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	/**
	 * Using the Eclipse AST parser, we can add a String function after other functions.
	 */
	public static int addFunction(String code) {
		//get the document
		IDocument document = getDocument();
		
		//if null, return null
		if(document == null) return -1;
		
		//parse document
		ASTParser parser = ASTParser.newParser(Activator.level);
		parser.setSource(document.get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        AST ast = cu.getAST();
        
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
        
        try {
			document.replace(pos, 0, code);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
				
		return 0;
	}
	
	/**
	 * Adds imports to the document if no duplicates.
	 */
	public static int addImportStatements(List<String> imports) {
		//parse the document
		ASTParser parser = ASTParser.newParser(Activator.level);
		parser.setSource(DocumentHandler.getDocument().get().toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        AST ast = cu.getAST();
        
        //lets get the offset to insert at, by default the top of the document
        int offset = 0;
        
        //get list of import statements
        ImportDeclarationVisitor idv = new ImportDeclarationVisitor();
        cu.getRoot().accept(idv);
        List<ImportDeclaration> importNodes = idv.imports;
        
        //if there already exist import statements
        if(importNodes != null && importNodes.size() > 0) {
        	//insert will be before first import
        	offset = importNodes.get(0).getStartPosition();
        	//remove duplicates from our list
        	for(ImportDeclaration i : importNodes) {
        		if(imports.contains(i.toString().trim())) {
        			imports.remove(i.toString().trim());
        		}
        	}
        }
        //if there don't exist import statements, we want to place under the package declaration
        else {
	       //find package node
        	PackageDeclarationVisitor pdv = new PackageDeclarationVisitor();
	        cu.getRoot().accept(pdv);
	        PackageDeclaration pk = pdv.pk;
	        
	        //if exists, update offset
	        if(pk !=  null) offset = pk.getStartPosition() + pk.getLength() + 2;
        }
        
        //construct a string block of imports to add
        String importBlock = "";
        for(String i : imports) {
        	importBlock += i + "\r\n";
        }
        
        //if our import block ends up empty, do nothing
        if(importBlock.equals("")) return 0;
        
        //add to document
        DocumentHandler.replaceAt(offset, 0, importBlock);
        
        //update imports
        InputHandler.previousImports = importBlock;
        
        return 0;
	}
	
	/**
	 * Function to get the open JavaProject
	 */
	public static IJavaProject getProject() {
		//get the java project from open editor
		IEditorInput input2 = QueryDocListener.editorPart.getEditorInput();
		IResource file2 = ((IFileEditorInput)input2).getFile();
		IProject pp = file2.getProject();
		IJavaProject jp = (IJavaProject) JavaCore.create(pp);
		
		return jp;
	}
	
	/**
	 * Get open IJavaProject's classpath of external libraries.
	 * @return
	 */
	public static String getClassPath() {
		
		IJavaProject project = getProject();
		IClasspathEntry[] classPathEntries = null;
		try {
			classPathEntries = project.getRawClasspath();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//if we have no classpath entries return null
		if(classPathEntries == null || classPathEntries.length == 0) return "";
		
		String classPath = "";
		
		//otherwise, for each class path entry
		for(IClasspathEntry c : classPathEntries) {
			//if a library, add to classPath
			if(c.getEntryKind() == IClasspathEntry.CPE_LIBRARY)
				classPath += c.getPath().toString() + ";";
		}
		
		return classPath;
	}
	
	public static void refresh(int offset, int length) {
		ITextEditor editor = (ITextEditor) QueryDocListener.editorPart;
  		editor.selectAndReveal(offset, length);
	}
	
	public static void replaceSnippet(Snippet snippet) {
		int offset = InputHandler.previous_offset;
        int length = InputHandler.previous_length;
        String replacement = InputHandler.previousInfo + snippet.getFormattedCode();
        
        replaceAt(offset, length, replacement);
        InputHandler.previous_length = replacement.length();
        CycleAnswersHandler.previous_index = 0;
        QueryDocListener.addImports(snippet, replacement);
	}
	
}
