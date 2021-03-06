package dtool.ast.definitions;

import descent.core.ddoc.Ddoc;
import dtool.ast.ILanguageNode;
import dtool.resolver.CommonDefUnitSearch;

/**
 * A handle to a defined, named language element. 
 * May exists in source or outside source, it can be implicitly or explicitly defined.
 * Implementation may be an AST node such as {@link DefUnit} (that is the more common case).
 */
public interface INamedElement extends ILanguageNode {
	
	/** The name of the element that is referred to. */
	String getName();
	
	/** @return the extended name of the element referred to. 
	 * The extended name is the name of the element/defunit plus additional adornments(can contain spaces) that
	 * allow to disambiguate this defUnit from homonym defUnits in the same scope 
	 * (for example the adornment can be function parameters for function elements).
	 */
	String getExtendedName();
	
	/** Gets the archetype (the kind) of this DefElement. */
	EArcheType getArcheType();
	
	/** @return true if this is a pre-defined/native language element. 
	 * (example: primitives such as int, void, or native types like arrays, pointer types) 
	 */
	boolean isLanguageIntrinsic();
	
	/** @return The fully qualified name of this element. Not null. */
	String getFullyQualifiedName();
	
	/** @return the fully qualified name of the module this element belongs to. 
	 * Can be null if element is not contained in a module. */
	String getModuleFullyQualifiedName();
	
	/** @return the nearest enclosing {@link INamedElement}.
	 * For modules and packages, that is null. */
	INamedElement getParentElement();
	
	/** @return the DefUnit this named element represents. In most cases this is the same as the receiver, 
	 * but this method allows proxy {@link INamedElement} classes to resolve to their proxied {@link DefUnit}. 
	 * It may still return null since the underlying defunit may not exist at all (implicitly defined named elements).
	 */
	DefUnit resolveDefUnit();
	
	/** Resolve the underlying element and return its DDoc. See {@link #resolveDefUnit()}.
	 * Can be null. */
	Ddoc resolveDDoc();
	
	/**
	 * Resolve given search in the members scope of this defunit.
	 * Note that the members can be different from the lexical scope that a defunit may provide.
	 */
	void resolveSearchInMembersScope(CommonDefUnitSearch search);
	
}