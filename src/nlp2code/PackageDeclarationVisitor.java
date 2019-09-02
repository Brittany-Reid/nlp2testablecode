package nlp2code;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class PackageDeclarationVisitor extends ASTVisitor {
	public PackageDeclaration pk;
	@Override
	public boolean visit(PackageDeclaration node) {
		pk = node;
		return super.visit(node);
	}
}
