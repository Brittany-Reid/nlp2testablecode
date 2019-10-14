package nlp2code.tester;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Node.TreeTraversal;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import nlp2code.Evaluator;
import nlp2code.Snippet;
import nlp2code.fixer.UnresolvedElementFixes;

/**
 * This class generates type recommendations for the content assist menu.
 */
public class TypeRecommendations {
	public static JavaParser parser = null;
	public static BlockStmt block;
	public static String returnType = null;
	public static List<String> argTypes = null;
	private static Node returnNode = null;
	private static List<String> arguments = null;
	
	/**
	 * Generates type recommendations for test input/output, and returns a snippet with info added.
	 */
	public static Snippet generate(Snippet snippet, String before, String after){
		//initialize parser
		if(parser == null) {
			initializeParser();
		}
		
		block = getSnippetAST(snippet, before, after);
		if(block == null) return null;
		
		findTestIO();
		if(returnNode == null) return null;
		
		//return 1 if we found arguments
		if(argTypes.size() > 0) {
			snippet.setPassed(1);
			snippet.setArguments(argTypes);
			snippet.setReturn(returnType);
			return snippet;
		}
		
		snippet.setPassed(0);
		return snippet;
	}
	
	/**
	 * Given a snippet, parses and returns the snippet's AST as a block statement.
	 */
	public static BlockStmt getSnippetAST(Snippet snippet, String before, String after) {
		BlockStmt blockStmt = null;
		String flag = "NLP3Code_comment"; //it would be a good idea to add some random key to this
		
		//add import statements to before
		String proposedBefore = before;
		if(snippet.getImportList().size() > 0) {
			proposedBefore = Snippet.addImportToBefore(snippet, before);
		}
		
		//parse with added flag and block
		ParseResult result = parser.parse(proposedBefore + "//" + flag +"\n{\n" + snippet.getCode() + "}\n" + after);
		if(result.getResult().isEmpty()) return null;
		CompilationUnit cu = (CompilationUnit) result.getResult().get();
		
		//get list of comments
		for(Comment c : cu.getComments()) {
			if(c.getContent().equals(flag)) {
				if(c.getCommentedNode().isPresent()) {
					Node node = c.getCommentedNode().get();
					if(node.getClass() == BlockStmt.class) {
						blockStmt = (BlockStmt) c.getCommentedNode().get();
					}
				}
			}
		}
		
		return blockStmt;
	}
	
	/**
	 * This function tries to find input and output for a JUnit test from the snippet.
	 */
	public static void findTestIO() {
		//use atomic references so we can do things inside the lambda walk
		AtomicReference<List<Node>> noInits = new AtomicReference<>();
		AtomicReference<List<Node>> argumentNodes = new AtomicReference<>();
		AtomicReference<List<Node>> argumentTypes = new AtomicReference<>();
		AtomicReference<Node> lastStatement = new AtomicReference<>();
		AtomicReference<Node> lastVar = new AtomicReference<>();
		argumentNodes.set(new ArrayList<>());
		argumentTypes.set(new ArrayList<>());
		noInits.set(new ArrayList<>());
		
		//init values
		returnType = null;
		returnNode = null;
		arguments = new ArrayList<>();
		argTypes = new ArrayList<>();
		
		
		//walk the ast tree
		block.walk(TreeTraversal.PREORDER, node -> {
			
			//nodes within the block that are the contents of an expression statement
			if(node.getParentNode().isPresent() && node.getParentNode().get().getClass() == ExpressionStmt.class) {
				List<Node> currentArgs = argumentNodes.get();
				List<Node> currentTypes = argumentTypes.get();
				
				//variable declarations
				if(node.getClass() == VariableDeclarationExpr.class) {
					//initialization must be in the highest scope
					if(node.findAncestor(BlockStmt.class).isPresent() && node.findAncestor(BlockStmt.class).get() == block) {
						List<Node> currentNoInits = noInits.get();
						//check all vars
						for(VariableDeclarator v : ((VariableDeclarationExpr)node).getVariables()) {
							lastVar.set(v.getName());
							
							//has no initialization
							if(v.getInitializer().isEmpty()) {
//								//add to check later
//								currentNoInits.add(v.getName());
//								currentArgs.add(node);
								continue;
							}
							//otherwise get init value
							Expression init = v.getInitializer().get();
							if(isIndependent(init)) {
								currentArgs.add(v.getName());
								currentTypes.add(v.getType());
							}
						}
						noInits.set(currentNoInits);
					}
				}
				
				//variable assignment
				if(node.getClass() == AssignExpr.class) {
					lastVar.set(((AssignExpr)node).getTarget());
				}
				
				//store last statement
				lastStatement.set(node);
				argumentNodes.set(currentArgs);
				argumentTypes.set(currentTypes);
			}
			
		});
		
		//process return
		String returnString = null;
		returnNode = lastStatement.get();
		if(returnNode == null) return;
		
		//variable declaration
		if(returnNode.getClass() == VariableDeclarationExpr.class) {
			VariableDeclarator var = ((VariableDeclarationExpr)returnNode).getVariable(0);
			returnString = var.getTypeAsString();
			returnNode = var.getName();
		}
		//variable assignment
		else if(returnNode.getClass() == AssignExpr.class) {
			Expression target = ((AssignExpr)returnNode).getTarget();
			try {
				returnString = processResolvedType(target.calculateResolvedType());
				returnNode = target;
			}catch(Exception e) {
				returnString = null;
				returnNode = null;
			}
		}
		//method call
		else if(returnNode.getClass() == MethodCallExpr.class) {
			MethodCallExpr methodCall = ((MethodCallExpr)returnNode);
			//if we have a system out we can accept a really common case
			if(methodCall.getScope().isPresent() && methodCall.getScope().get().toString().equals("System.out")) {
				if(methodCall.getNameAsString().equals("print") || methodCall.getNameAsString().equals("println")) {
					if(methodCall.getArguments().size() > 0) {
						ResolvedType type  = methodCall.getArgument(0).calculateResolvedType();
						returnString = processResolvedType(type);
					}
				}
			}
			//otherwise, try to get return type
			else {
				try {
					//resolve method
					ResolvedMethodDeclaration methodDeclaration = methodCall.resolve();
					methodDeclaration.getQualifiedSignature();
					ResolvedType type = methodDeclaration.getReturnType();
					returnString = processResolvedType(type);
				}catch(Exception e) {
					returnString = null;
				}
			}
			returnNode = null;
		}
		else {
			returnNode = null;
		}
		
		if(returnString == null) returnString = "void";
		
		
		List<Node> args = argumentNodes.get();
		List<Node> types = argumentTypes.get();
		for(int i=0; i<args.size(); i++) {
			Node n = args.get(i);
			boolean accept = true;
			if(returnNode != null) {
				if(returnNode.toString().equals(n.toString())) {
					accept = false;
				}
			}
			if(accept == true) {
				//System.out.println(types.get(i).toString());
				argTypes.add(types.get(i).toString());
				arguments.add(n.toString());
			}
		}
		
		//System.out.println("Return type: " + returnString);
		
		//set fields
		returnType = returnString;
	}
	
	/**
	 * Checks if a given node is independent. A node is independent if it doesn't rely on
	 * some other variable. JavaParser considers functions to be SimpleNames so we can't simply
	 * check if a node contains SimpleNames.
	 */
	private static boolean isIndependent(Node node) {
		//is the node a name expression
		if(node.getClass() == NameExpr.class) {
			return false;
		}
		
		//otherwise, traverse tree
		AtomicReference<Boolean> accept = new AtomicReference<Boolean>();
		accept.set(true);
		node.walk(node2 ->{
			//anything that can have arguments, check if they are not names
			if(NodeWithArguments.class.isAssignableFrom(node2.getClass())) {
				NodeList<Node> arguments = ((NodeWithArguments)node2).getArguments();
				for(Node arg : arguments){
					if(arg.getClass() == NameExpr.class) {
						accept.set(false);
					}
				}
			}
			//check if binary expression sides are names
			if(node2.getClass() == BinaryExpr.class) {
				if(((BinaryExpr) node2).getRight().isNameExpr()) {
					accept.set(false);
				}
				else if(((BinaryExpr) node2).getLeft().isNameExpr()) {
					accept.set(false);
				}
			}
			//check if a unary expr is a name
			if(node2.getClass() == UnaryExpr.class) {
				if(((UnaryExpr)node2).getExpression().isNameExpr()) {
					accept.set(false);
				}
			}
		});
		
		return accept.get();
	}
	
	/**
	 * Returns the String type of a ResolvedType Object.
	 */
	private static String processResolvedType(ResolvedType resolvedType) {
		return UnresolvedElementFixes.processResolvedType(resolvedType);
	}
	
	
	/**
	 * Initialize the parser on first use.
	 */
	public static void initializeParser() {
		//initialize the global parser if not already
		if(Evaluator.parser == null) {
			Evaluator.initializeParser();
		}
		//use global parser
		parser = Evaluator.parser;
	}
	
}
