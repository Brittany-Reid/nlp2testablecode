package nlp2code;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;

import nlp2code.fixer.UnresolvedElementFixes;

/**
 * Class to listen for changes to Java Project classpath.
 * If the user adds new packages, we want to be able to suggest imports.
 * This is really going far but I might as well implement it.
 */
public class PackagesListener implements IElementChangedListener{
	
	@Override
    public void elementChanged(ElementChangedEvent event) {
        visit(event.getDelta());
    }

    private void visit(IJavaElementDelta delta) {
        IJavaElement el = delta.getElement();
        switch (el.getElementType()) {
        case IJavaElement.JAVA_MODEL:
            visitChildren(delta);
            break;
        //if we changed the classpath
        case IJavaElement.JAVA_PROJECT:
            if (isClasspathChanged(delta.getFlags())) {
            	System.out.println("Classpath changed.");
                UnresolvedElementFixes.clearCache();
            }
            break;
        default:
            break;
        }
    }

    private boolean isClasspathChanged(int flags) {
        return 0!= (flags & (
                IJavaElementDelta.F_CLASSPATH_CHANGED | 
                IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED
        ));
    }

    public void visitChildren(IJavaElementDelta delta) {
        for (IJavaElementDelta c : delta.getAffectedChildren()) {
            visit(c);
        }
    }
}
