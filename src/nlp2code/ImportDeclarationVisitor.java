package nlp2code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;

/**
 * This class is used to find import statements in the open document.
 */
public class ImportDeclarationVisitor extends ASTVisitor {
	public List<ImportDeclaration> imports = new ArrayList<>();;
	@Override
	public boolean visit(ImportDeclaration node) {
		imports.add(node);
		return super.visit(node);
	}

}
