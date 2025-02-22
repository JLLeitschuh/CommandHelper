package com.laytonsmith.core.functions;

import com.laytonsmith.core.Documentation;
import com.laytonsmith.core.LogLevel;
import com.laytonsmith.core.ParseTree;
import com.laytonsmith.core.Script;
import com.laytonsmith.core.compiler.SelfStatement;
import com.laytonsmith.core.compiler.analysis.Scope;
import com.laytonsmith.core.compiler.analysis.StaticAnalysis;
import com.laytonsmith.core.compiler.signature.FunctionSignatures;
import com.laytonsmith.core.compiler.signature.SignatureBuilder;
import com.laytonsmith.core.constructs.CClassType;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.CancelCommandException;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.natives.interfaces.Mixed;
import java.util.List;
import java.util.Set;

/**
 * Note that to "activate" this class as a function, you must prefix the '@api' annotation to it.
 */
//This will be re-added at some point, so get ready!
//@MustUseOverride
public interface Function extends FunctionBase, Documentation, Comparable<Function> {

	/**
	 * Returns the types of catchable exceptions this function can throw. (Uncatchable exceptions need not be listed)
	 *
	 * @return An array of the exception enums, or null, if the function throws no catchable exceptions.
	 */
	public Class<? extends CREThrowable>[] thrown();

	/**
	 * Whether or not a function needs to be checked against the permissions file, if there are possible security
	 * concerns with a user running this function. If this function returns true, the permissions file will be checked
	 * for commandhelper.func.use.&lt;function name&gt; upon usage in game.
	 *
	 * @return
	 */
	public boolean isRestricted();

	/**
	 * If a function doesn't want to have to deal with a variable as a variable, but instead wants to recieve it as an
	 * atomic, resolved value, the function can return true from this function. This will signal the interpreter to go
	 * ahead and resolve the variable into one of the atomic Constructs. If it returns false, it is possible the exec
	 * function will receive an IVariable Construct.
	 *
	 * @return
	 */
	public boolean preResolveVariables();

	/**
	 * Whether or not to run this function asynchronously from the main server thread. If you return true, you may NOT
	 * have any interaction with the bukkit api, other than bukkit thread safe methods. Returning true WILL run this
	 * function in the CH thread, returning false WILL run this function in the main server thread, and returning null
	 * will run this function in whatever context the script is currently running in.
	 *
	 * @return
	 */
	public Boolean runAsync();

	/**
	 * This function is invoked when the script is run. The line number is provided so that if there is an error, the
	 * function can provide a more specific error message for the user. If the function was canceled due to a fatal
	 * error in the syntax of the user input or some similar situation, it should throw a ConfigRuntimeException. All
	 * parameters sent to the function have already been resolved into an atomic value, so functions do not have to
	 * worry about resolving parameters. There is an explicit check made before calling exec to ensure that Construct
	 * ... args will only be one of the atomic Constructs. If a code tree is needed instead of a resolved construct, the
	 * function should indicate so, and {@code execs} will be called instead. If exec is needed, execs should return
	 * CVoid.
	 *
	 * @param t The location of this function call in the code, used for correct error messages
	 * @param environment The current code environment
	 * @param args An array of evaluated objects
	 * @return
	 * @throws CancelCommandException
	 */
	public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException;

	/**
	 * Gets the function's signatures. {@link SignatureBuilder} offers a convenient way to create these signatures.
	 * @return This function's signatures.
	 */
	public FunctionSignatures getSignatures();

	/**
	 * Gets the return type of this function, based on the types of the passed arguments.
	 * @param t - The code target, used for setting the code target in thrown exceptions.
	 * @param argTypes - The types of the passed arguments.
	 * @param argTargets - The {@link Target}s belonging to the argTypes (in the same order).
	 * @param env - The {@link Environment}, used for instanceof checks on types.
	 * @param exceptions - A set to which all type errors will be added.
	 * @return The return type of this function when invoked with the given argument types. If the return type
	 * is unknown, {@link CClassType#AUTO} is returned, indicating that this value should be handled as dynamic
	 * during static type checking.
	 */
	public CClassType getReturnType(Target t, List<CClassType> argTypes,
			List<Target> argTargets, Environment env, Set<ConfigCompileException> exceptions);

	/**
	 * Typechecks this function, generating compile errors when types or signatures don't match.
	 * This method is responsible for type checking the function arguments (child nodes of the passed AST node),
	 * which can be done by calling this method on those child nodes.
	 * @param analysis - The {@link StaticAnalysis}, used to resolve variable/proc/... references.
	 * @param ast - The parse tree.
	 * @param env - The {@link Environment}, used for instanceof checks on types.
	 * @param exceptions - Any compile exceptions will be added to this set.
	 * @return The return type of this function.
	 */
	public CClassType typecheck(StaticAnalysis analysis,
			ParseTree ast, Environment env, Set<ConfigCompileException> exceptions);

	/**
	 * If functions contain declarations or references, then these functions should create a new scope,
	 * link it to the parent scope if it is allowed to perform lookups
	 * in there, put the new declaration or reference in the new scope and return this new scope.
	 * Functions are also responsible for calling this method on their children to further generate the scope graph.
	 * @param analysis - The {@link StaticAnalysis}.
	 * @param parentScope - The current scope.
	 * @param ast - The abstract syntax tree representing this function.
	 * @param env - The environment.
	 * @param exceptions - A set to put compile errors in.
	 * @return The new (linked) scope, or the parent scope if this function does not require a new scope.
	 */
	public Scope linkScope(StaticAnalysis analysis, Scope parentScope,
			ParseTree ast, Environment env, Set<ConfigCompileException> exceptions);

	/**
	 * Allows functions to perform a rewrite step to rewrite the AST as received from the parser to a valid
	 * executable AST. Optimizations should not yet be performed in this rewrite step.
	 * Traversal is pre-order depth-first.
	 * @param ast - The abstract syntax tree representing this function.
	 * @param env - The environment.
	 * @param envs - The set of expected environment classes at runtime.
	 * @param exceptions - A set to put compile errors in.
	 * @return The rewritten AST node that should completely replace the AST node representing this function, or
	 * {@code null} to not replace this AST node.
	 */
	public ParseTree postParseRewrite(ParseTree ast, Environment env,
			Set<Class<? extends Environment.EnvironmentImpl>> envs, Set<ConfigCompileException> exceptions);

	/**
	 * If a function needs a code tree instead of a resolved construct, it should return true here. Most functions will
	 * return false for this value.
	 *
	 * @return
	 */
	public boolean useSpecialExec();

	/**
	 * If useSpecialExec indicates it needs the code tree instead of the resolved constructs, this gets called instead
	 * of exec. If execs is needed, exec should return CVoid.
	 *
	 * @param t
	 * @param env
	 * @param nodes
	 * @return
	 */
	public Mixed execs(Target t, Environment env, Script parent, ParseTree... nodes);

	/**
	 * Returns an array of example scripts, which are used for documentation purposes.
	 * <p>
	 * If there are no examples, null or empty array should be returned.
	 *
	 * @return
	 * @throws com.laytonsmith.core.exceptions.ConfigCompileException If the script could not be compiled
	 */
	public ExampleScript[] examples() throws ConfigCompileException;

	/**
	 * Returns true if this function should be profilable. Only a very select few functions should avoid profiling.
	 * AbstractFunction handles this by checking if the noprofile annotation is present.
	 *
	 * @return
	 */
	public boolean shouldProfile();

	/**
	 * Returns the minimum level at which this function should be profiled at.
	 *
	 * @return
	 */
	public LogLevel profileAt();

	/**
	 * Returns the message to use when this function gets profiled, if useSpecialExec returns false.
	 *
	 * @param args
	 * @return
	 */
	public String profileMessage(Mixed... args);

	/**
	 * Returns the message to use when this function gets profiled, if useSpecialExec returns true.
	 *
	 * @param args
	 * @return
	 */
	public String profileMessageS(List<ParseTree> args);

	/**
	 * Returns true if this function is a self statement. The arguments are passed to the function in the case
	 * of dynamic self-statements, but this generally not desirable, and instead should always be a self
	 * statement, or always not be. For functions that are self statements and extend AbstractFunction, it
	 * can simply be tagged with the {@link SelfStatement} annotation.
	 * @param t The code target.
	 * @param env The environment.
	 * @param nodes The nodes being sent to this function. They may or may not contain types, depending on if typechecking
	 * is enabled. If not enabled, the general rule is to look for statements.
	 * @param envs The environments in this compilation.
	 * @return
	 */
	public boolean isSelfStatement(Target t, Environment env, List<ParseTree> nodes,
			Set<Class<? extends Environment.EnvironmentImpl>> envs) throws ConfigCompileException;

	/**
	 * In addition to being a function, an object may also be a code branch, that is, it conditionally will execute some
	 * of its arguments. For optimization and static code analysis purposes, it it important for these functions to be
	 * able to provide more information about their branches; if the branch conditions are static, they should be
	 * reduceable to a single branch anyways, but some optimizations require knowledge about code branches.
	 */
	public interface CodeBranch {

		/**
		 * Returns a list of all the child nodes that are considered separate code branches. Likely this is all of them,
		 * but not necessarily, especially if the optimization routine could eliminate some of the branches, due to
		 * const conditions. The current "self" ParseTree is passed in, which is the function's ParseTree wrapper, and
		 * from there, the children can be selected. The list of children returned should reference equal (==, not just
		 * .equals()) the children passed in.
		 *
		 * @return
		 */
		public List<ParseTree> getBranches(ParseTree self);
	}
}
