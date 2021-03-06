package nlp2testablecode.visitors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

/**
 * Custom visitor to find a package declaration with the eclipse parser.
 */
public class PackageDeclarationVisitor extends ASTVisitor {
	public PackageDeclaration pk;
	
	/**
	 * This function determines what happens when the visitor finds a package declaration.
	 */
	@Override
	public boolean visit(PackageDeclaration node) {
		pk = node;
		return super.visit(node);
	}
}