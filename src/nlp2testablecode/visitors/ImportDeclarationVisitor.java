package nlp2testablecode.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * Custom visitor to find import declarations with the eclipse parser.
 */
public class ImportDeclarationVisitor extends ASTVisitor {
	public List<ImportDeclaration> imports = new ArrayList<>();
	
	/**
	 * This function determines what happens when the visitor finds an import declaration.
	 */
	@Override
	public boolean visit(ImportDeclaration node) {
		imports.add(node);
		return super.visit(node);
	}

}