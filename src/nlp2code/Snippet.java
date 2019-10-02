package nlp2code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/**
 * A snippet is an object that contains a code snippet and related information.
 * Code is represented by a List of lines, containing deletion state.
 * As well as a String, containing code. The goal is to prioritize
 * quick string access for compiling, and construct lines on demand.
 */
public class Snippet implements Comparable<Snippet>{
	//the comment attached to deleted lines for all snippets
	private static String deletionMessage = " //removed by NLP3Code";
	
	private Boolean showDeletions = true;
	private List<Pair<String, Boolean>> code;
	private List<String> importStatements = new ArrayList<>();
	private String codeString;
	private String formattedCodeString;
	private int id;
	private List<Diagnostic<? extends JavaFileObject>> diagnostics;
	private boolean compiled = false;
	private int errors = -1;
	private int passed = -1;
	private int LOC = -1;
	//testing information for data gathering
	private List<String> argumentTypes = null;
	private String returnType = null;
	
	
	/**
	 * Constructs a snippet object from a String of code;
	 * @param code The code to store in this snippet.
	 */
	public Snippet(String code, int id){
		changed();
		codeString = code;
		this.id = id;
		extractImports();
		constructLines(codeString);
	}
	
	/**
	 * Copy constructor to facilitate deep copies.
	 * @param that The snippet to make a copy of.
	 */
	public Snippet(Snippet that) {
		if(that.code == null) code = null;
		else {
			code = new ArrayList<>();
			for(Pair<String, Boolean> line : that.code) {
				code.add(new MutablePair<String, Boolean>(line.getLeft(), line.getRight()));
			}
		}
		codeString = that.codeString;
		formattedCodeString = that.formattedCodeString;
		id = that.id;
		diagnostics = that.diagnostics;
		compiled = that.compiled;
		errors = that.errors;
		LOC = that.LOC;
		importStatements = that.importStatements;
		argumentTypes = that.argumentTypes;
		returnType = that.returnType;
		passed = that.passed;
	}
	
	@Override
	public int compareTo(Snippet b) {
		//empty snippets vs non-empty compiling
		if(b.getCode() == "" && getCode() != "") return -1;
		if(getCode() == "" && b.getCode() != "") return 1;
		
		//handle negative error value
		if(b.getErrors() == -1 && errors != -1) return -1;
		if(b.getErrors() != -1 && errors == -1) return 1;
		
		//if error value is 0, look at passed tests
		if(b.getErrors() == 0 && errors == 0) {
			//handle any negatives
			if(b.getPassed() == -1 && passed != -1) return -1;
			if(b.getPassed() != -1 && passed == -1) return 1;
			
			//otherwise, compare passed
			return Integer.compare(passed, b.getPassed());
		}
		
		//compare error value
		return Integer.compare(errors, b.getErrors());
	}
	
	/**
	 * Stores the given code, overwriting previous code.
	 * @param code The code to store.
	 */
	public void setCode(String code) {
		changed();
		codeString = code;
	}
	
	public void setPassed(int passed) {
		this.passed = passed;
	}
	
	/**
	 * Update error information.
	 * @param errors The error value to use.
	 */
	public void updateErrors(Integer errors, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		this.errors = errors;
		this.diagnostics = diagnostics;
		compiled = true;
	}
	
	/**
	 * Add import statements to a list. This list can later be used to
	 * insert imports in the users code.
	 * @param importStatement The statement to add.
	 */
	public void addImportStatement(String importStatement) {
		if(!importStatements.contains(importStatement)) importStatements.add(importStatement);
	}
	
	public void setArguments(List<String> arguments) {
		argumentTypes = new ArrayList<>(arguments);
	}
	
	public void setReturn(String returnType) {
		this.returnType = returnType;
	}
	
	/**
	 * Sometimes, code strings contain imports. This will remove them and add
	 * them to the import representation.
	 */
	public void extractImports() {
		if(codeString == null) return;
		String newCodeString = "";
		String line, copy;
		BufferedReader bufReader = new BufferedReader(new StringReader(codeString));
		try {
			while ( (line = bufReader.readLine()) != null) {
				copy = line;
				//if a line starts with import, add to our list
				if(line.trim().startsWith("import ")) {
					addImportStatement(copy);
				}
				//otherwise, add to code string
				else {
					newCodeString += copy+"\n";
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		codeString = newCodeString;
	}
	
	/**
	 * Function to return the stored code string, without any formatting.
	 * @return A String code.
	 */
	public String getCode() {
		if(codeString != null) return codeString;
		return constructCode();
	}
	
	public List<Diagnostic<? extends JavaFileObject>> getDiagnostics(){
		return diagnostics;
	}
	
	/**
	 * Returns the String line at the given point.
	 * @param lineNum The line to return, starting at 1
	 */
	public String getLine(int lineNum) {
		if(code == null) constructLines(codeString);
		return code.get(lineNum-1).getLeft();
	}
	
	/**
	 * Returns the number of errors. Returns -1 if snippet hasn't been compiled.
	 * @return
	 */
	public int getErrors() {
		return errors;
	}
	
	public int getPassed() {
		return passed;
	}
	
	public String getReturnType() {
		return returnType;
	}
	
	public List<String> getArgumentTypes(){
		return argumentTypes;
	}
	
	/**
	 * Returns a string representing the additional import statements.
	 * To be appended to the user's import statements.
	 * @return
	 */
	public String getImportStatements() {
		String importBlock = "";
		for(String importStatement : importStatements) {
			importBlock+=importStatement + "\n";
		}
		return importBlock;
	}
	
	public List<String> getImportList() {
		return importStatements;
	}
	
	/**
	 * Returns true if the current form of the snippet has been compiled.
	 * @return
	 */
	public boolean isCompiled() {
		return compiled;
	}
	
	/**
	 * Returns if the given line number is deleted.
	 * @param line The line to check.
	 * @return A boolean representing deletion state.
	 */
	public boolean getDeleted(int line) {
		if(code == null) constructLines(codeString);
		return code.get(line-1).getRight();
	}
	/**
	 * Deletes a given line, starting at line 1.
	 * @param lineNum
	 */
	public void deleteLine(int lineNum) {
		
		//get a copy of lines
		if(code == null) constructLines(codeString);
		List<Pair<String, Boolean>> modified = code;
		
		//discard all cached information
		changed();
		
		//set this line to deleted
		Pair<String, Boolean> line = modified.get(lineNum-1);
		line.setValue(true);
		
		code = modified;
	}
	
	/**
	 * Returns the total size of the code, including deleted lines.
	 */
	public int size() {
		if(code != null) return code.size();
		
		code = constructLines(codeString);
		return code.size();
	}
	
	/**
	 * Return the number of non-deleted lines.
	 */
	public int getLOC() {
		if(LOC != -1) return LOC;
		
		if(codeString == null) constructCode();
		if(code == null) constructLines(codeString);
		return LOC;
	}
	
	/**
	 * Returns the code string with SO source appended.
	 */
	public String getFormattedCode() {
		return formatCode();
	}
	
	/**
	 * Function to return the question ID this snippet belongs to.
	 * @return The Integer ID.
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Private function to reset values on change.
	 */
	private void changed() {
		code = null;
		codeString = null;
		formattedCodeString = null;
		compiled = false;
		diagnostics = null;
		errors = -1;
		LOC = -1;
		passed = -1;
	}
	
	/**
	 * Using whitespace data from QueryDocListener, adds a fixed offset to each 
	 * line of code in a snippet.Fixes alignment issues.
	 * @return String snippet with a fixed offset.
	 */
	private String formatCode() {
		//if we have a cahced  code string
		if(formattedCodeString != null) return formattedCodeString;

		//otherwise, construct
		String source = "//https://stackoverflow.com/questions/" + id + "\n";
		String line;
		
		//get spacing
		String spacing = QueryDocListener.getWhitespaceBefore();
		if(spacing == null) spacing = "";
		
		formattedCodeString = spacing + source;

		//if we have a list of lines, construct formattedCodeString from this
		if(code != null) {
			for(Pair<String, Boolean> linePair : code) {
				//add lines if not deleted
				if(linePair.getRight() == false) {
					line = linePair.getLeft();
					formattedCodeString += spacing + line + "\n";
				}
				else if(showDeletions){
					line = linePair.getLeft();
					formattedCodeString += spacing + "//" + line + deletionMessage + "\n";
				}
			}
		}
		//otherwise, we must use the codeString
		else {
			BufferedReader bufReader = new BufferedReader(new StringReader(getCode()));
			try {
				while ( (line = bufReader.readLine()) != null) {
					formattedCodeString += spacing + line + "\n";
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
//		
		return formattedCodeString;
	}
	
	/**
	 * Converts a String into a List of Pairs representing lines and their deletion state.
	 */
	private List<Pair<String, Boolean>> constructLines(String code){
		List<Pair<String, Boolean>> lines = new ArrayList<>();
		String line;
		LOC = 0;
		
		BufferedReader bufReader = new BufferedReader(new StringReader(code));
		Boolean deleted = false;
		try {
			while ( (line = bufReader.readLine()) != null) {
				if(deleted == false) {
					LOC++;
				}
				Pair<String, Boolean> linePair = new MutablePair<String, Boolean>(line, deleted);
				lines.add(linePair);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.code = lines;
		return lines;
	}
	
	/**
	 * Converts line List into a String with no formatting.
	 */
	private String constructCode() {
		codeString = "";
		LOC = 0;
		
		for(Pair<String, Boolean> line : code) {
			if(line.getRight() == false) {
				String lineString = line.getLeft();
				
				//skip unneccessary lines 
				if(lineString.equals("")) continue;
				if(lineString.trim().startsWith("//")) continue;
				codeString += line.getLeft() + "\n";
				LOC++;
			}
		}
		
		return codeString;
	}
	
	/**
	 * This static function will accept a snippet and the before segment of the user's code
	 * and add import statements into the before code.
	 * @param snippet The snippet to get imports from.
	 * @param before The before string.
	 * @return
	 */
	public static String addImportToBefore(Snippet snippet, String before) {
		//get imports
		List<String> imports = new ArrayList<>(snippet.getImportList());
		
		//otherwise, parse to find insert offset, default is start
		int offset = 0;
		String modified;
		
		ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setSource(before.toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        
        //get import statements
        ImportDeclarationVisitor idv = new ImportDeclarationVisitor();
        cu.getRoot().accept(idv);
        List<ImportDeclaration> importNodes = idv.imports;
        
        //if imports exist
        if(importNodes != null && importNodes.size() > 0) {
        	//insert will be before first import
        	offset = importNodes.get(0).getStartPosition();
        	//check for and remove duplicates from list to add
        	for(ImportDeclaration i : importNodes) {
        		if(imports.contains(i.toString().trim())) {
        			imports.remove(i.toString().trim());
        		
        		}
        	}
        //if imports do not exist, look for package declaration
        }else {
        	//find package node
        	PackageDeclarationVisitor pdv = new PackageDeclarationVisitor();
        	cu.getRoot().accept(pdv);
        	PackageDeclaration pk = pdv.pk;
        	//if it exists, the offset is on the line after
        	if(pk !=  null) offset = pk.getStartPosition() + pk.getLength() + 1;
        }
        
        //construct import block
		String importBlock = "";
        for(String importStatement : imports) {
        	importBlock += importStatement + "\n";
        }
        
        //insert import block
        modified = before.substring(0, offset);
        modified += importBlock;
        modified += before.substring(offset, before.length());
		
		return modified;
	}
}