package com.laytonsmith.core.compiler.keywords;


import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.ParseTree;
import com.laytonsmith.core.compiler.FileOptions;
import com.laytonsmith.core.compiler.Keyword;
import com.laytonsmith.core.constructs.CBareString;
import com.laytonsmith.core.constructs.CFunction;
import com.laytonsmith.core.constructs.CKeyword;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.functions.DataHandling;
import java.util.List;

/**
 *
 */
@Keyword.keyword("proc")
public class ProcKeyword extends Keyword {

	@Override
	public int process(List<ParseTree> list, int keywordPosition) throws ConfigCompileException {
		if(list.get(keywordPosition).getData() instanceof CKeyword) {
			// It's a lone keyword, so we expect some function to follow, which is the proc name + variables
			FileOptions options = list.get(keywordPosition).getFileOptions();
			if(list.size() <= keywordPosition + 1) {
				throw new ConfigCompileException("Unexpected keyword", list.get(keywordPosition).getTarget());
			}
			if(list.get(keywordPosition + 1).getData() instanceof CFunction) {
				ParseTree procNode = new ParseTree(new CFunction(
						DataHandling.proc.NAME, list.get(keywordPosition).getTarget()), options);
				procNode.getNodeModifiers().merge(list.get(keywordPosition).getNodeModifiers());
				procNode.addChild(new ParseTree(new CString(list.get(keywordPosition + 1).getData().val(),
						list.get(keywordPosition + 1).getTarget()), options));
				// Grab the functions children, and put them on the stack
				for(ParseTree child : list.get(keywordPosition + 1).getChildren()) {
					procNode.addChild(child);
				}
				boolean forwardDeclaration = false;
				if(list.size() > keywordPosition + 2) {
					if(list.get(keywordPosition + 2).getData() instanceof CFunction cf
							&& com.laytonsmith.core.functions.Compiler.__cbrace__.NAME.equals(cf.val())) {
						validateCodeBlock(list.get(keywordPosition + 2), "Expected braces to follow proc definition");
						procNode.addChild(getArgumentOrNull(list.get(keywordPosition + 2)));
					} else {
						// Forward declaration, add a null "implementation"
						forwardDeclaration = true;
						procNode.addChild(new ParseTree(CNull.NULL, list.get(keywordPosition + 1).getFileOptions(), true));
					}
				} else {
					throw new ConfigCompileException("Expected braces to follow proc definition", list.get(keywordPosition + 1).getTarget());
				}
				list.remove(keywordPosition); // Remove the keyword
				list.remove(keywordPosition); // Remove the function definition
				if(!forwardDeclaration) {
					list.remove(keywordPosition); // Remove the cbrace
				}
				list.add(keywordPosition, procNode); // Add in the new proc definition
			} else if(list.get(keywordPosition + 1).getData() instanceof CBareString name) {
				// get_proc rewrite
				list.remove(keywordPosition);
				list.remove(keywordPosition);
				ParseTree getProc = new ParseTree(new CFunction(DataHandling.get_proc.NAME, Target.UNKNOWN), options, true);
				getProc.addChild(new ParseTree(new CString(name.val(), name.getTarget()), options));
				list.add(keywordPosition, getProc);
			} else {
				throw new ConfigCompileException("Unexpected use of \"proc\" keyword", list.get(keywordPosition).getTarget());
			}

		} else if(nodeIsProcFunction(list.get(keywordPosition))) {
			// It's the functional usage, possibly followed by a cbrace. If so, pull the cbrace in, and that's it
			if(list.size() > keywordPosition + 1) {
				if(isValidCodeBlock(list.get(keywordPosition + 1))) {
					list.get(keywordPosition).addChild(getArgumentOrNull(list.get(keywordPosition + 1)));
					list.remove(keywordPosition + 1);
				}
			}
		} else {
			// Random keyword in the middle of nowhere
			throw new ConfigCompileException("Unexpected use of \"proc\" keyword", list.get(keywordPosition).getTarget());
		}
		return keywordPosition;
	}

	private boolean nodeIsProcFunction(ParseTree node) {
		return node.getData() instanceof CFunction && node.getData().val().equals(DataHandling.proc.NAME);
	}

	@Override
	public String docs() {
		return "Defines a procedure, which can be called from elsewhere in code.";
	}

	@Override
	public Version since() {
		return MSVersion.V3_3_1;
	}

}
