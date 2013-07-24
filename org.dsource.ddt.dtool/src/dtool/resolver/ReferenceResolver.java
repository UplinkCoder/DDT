package dtool.resolver;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import melnorme.utilbox.core.ExceptionAdapter;
import melnorme.utilbox.misc.IteratorUtil;
import dtool.ast.ASTNode;
import dtool.ast.ASTNodeFinder;
import dtool.ast.IASTNode;
import dtool.ast.declarations.DeclarationImport;
import dtool.ast.declarations.DeclarationImport.IImportFragment;
import dtool.ast.declarations.ImportContent;
import dtool.ast.declarations.ImportSelective;
import dtool.ast.declarations.PartialPackageDefUnitOfPackage;
import dtool.ast.definitions.DefUnit;
import dtool.ast.definitions.DefinitionFunction;
import dtool.ast.definitions.Module;
import dtool.ast.definitions.Module.DeclarationModule;
import dtool.ast.references.CommonRefQualified;
import dtool.ast.references.RefIdentifier;
import dtool.ast.references.RefImportSelection;
import dtool.ast.references.Reference;
import dtool.parser.DeeParserResult;
import dtool.resolver.api.IModuleResolver;

/**
 * Class with static methods encoding D entity lookup rules.
 * Uses an {@link DefUnitSearch} during lookups.
 * A scope plus it's outer scopes is called an extended scope.
 */
public class ReferenceResolver {
	
	private static final String[] EMPTY_PACKAGE = new String[0];
	
	public static Module findModuleUnchecked(IModuleResolver modResolver, String[] packages, String module) {
		try {
			return modResolver.findModule(packages, module);
		} catch (Exception e) {
			throw ExceptionAdapter.unchecked(e);
		}
	}
	
	/* ====================  reference lookup  ==================== */
	
	
	/** Searches for the given CommonDefUnitSearch search, in the scope's 
	 * immediate namespace, secondary namespace (imports), and super scopes.
	 * 
	 * The set of matched {@link DefUnit}s must all be visible in the same
	 * non-extended scope, (altough due to imports, they may originate from 
	 * different scopes XXX: fix this behavior? This is an ambiguity error in D).
	 */
	public static void findDefUnitInExtendedScope(IScopeNode scope, CommonDefUnitSearch search) {
		assertNotNull(scope);

		do {
			findDefUnitInScope(scope, search);
			if(search.isFinished())
				return;

			IScopeNode outerscope = ScopeUtil.getOuterScope(scope);
			if(outerscope == null) {
				Module module = (Module) scope;
				findDefUnitInModuleDec(module, search);
				findDefUnitInObjectIntrinsic(search);
				return;
			}

			// retry in outer scope
			scope = outerscope; 
		} while (true);
		
	}
	
	private static void findDefUnitInObjectIntrinsic(CommonDefUnitSearch search) {
		Module targetModule = search.resolveModule(EMPTY_PACKAGE, "object");
		if (targetModule != null) {
			findDefUnitInScope(targetModule, search);
		}
	}
	
	private static void findDefUnitInModuleDec(Module module, CommonDefUnitSearch search) {
		DeclarationModule decMod = module.md;
		if(decMod != null) {
			DefUnit defUnit;
			
			if(decMod.packages.length == 0 || decMod.packages[0] == "") {
				defUnit = module;
			} else {
				String[] packNames = decMod.packages;
				
				defUnit = PartialPackageDefUnitOfPackage.createPartialDefUnits(packNames, null, module);
			}
			
			if(search.matches(defUnit))
				search.addMatch(defUnit);
		} else {
			if(search.matches(module)) {
				search.addMatch(module);
			}
		}
	}

	/** Searches for the given CommonDefUnitSearch search, in the scope's 
	 * immediate namespace, secondary namespace (imports), and super scopes.
	 *  
	 * Does not search, if the scope has alread been searched in this search.
	 * The set of matched {@link DefUnit}s must all be visible in the same
	 * non-extended scope, (altough due to imports, they may originate from 
	 * different scopes XXX: fix this behavior? This is an ambiguity error in D).
	 */
	public static void findDefUnitInScope(IScope scope, CommonDefUnitSearch search) {
		assertNotNull(scope);
		if(search.hasSearched(scope))
			return;
		
		search.enterNewScope(scope);
		
		findDefUnitInImmediateScope(scope, search);
		if(search.isFinished())
			return;
		
		findDefUnitInSecondaryScope(scope, search);
		if(search.isFinished())
			return;

		// Search super scope 
		if(scope.getSuperScopes(search.modResolver) != null) {
			for(IScope superscope : scope.getSuperScopes(search.modResolver)) {
				if(superscope != null)
					findDefUnitInScope(superscope, search); 
				if(search.isFinished())
					return;
			}
		}
		
	}
	

	private static void findDefUnitInImmediateScope(IScope scope, CommonDefUnitSearch search) {
		Iterator<IASTNode> iter = IteratorUtil.recast(scope.getMembersIterator(search.modResolver));
		
		findDefUnits(search, iter, scope.hasSequentialLookup(), false);
	}
	
	private static void findDefUnitInSecondaryScope(IScope scope, CommonDefUnitSearch search) {
		Iterator<IASTNode> iter = IteratorUtil.recast(scope.getMembersIterator(search.modResolver));
		
		findDefUnits(search, iter, scope.hasSequentialLookup(), true);
	}
	
	private static void findDefUnits(CommonDefUnitSearch search, Iterator<? extends IASTNode> iter,
			boolean isStatementScope, boolean importsOnly) {
		
		int refOffset = search.refOffset;
		
		while(iter.hasNext()) {
			IASTNode elem = iter.next();
			
			if (elem instanceof INonScopedBlock) {
				INonScopedBlock container = ((INonScopedBlock) elem);
				findDefUnits(search, container.getMembersIterator(), isStatementScope, importsOnly);
				if(search.isFinished() && search.findOnlyOne)
					return; // Return if we only want one match in the scope
			}
			
			// Check if the reference is before the point of definition
			if(isStatementScope && refOffset < elem.getEndPos()) {
				/* XXX: Technically we could return right away, since
				 * no further nodes should match, but keep going in case 
				 * there are source range errors. */ 
				continue;
			}
			
			if(!importsOnly && elem instanceof DefUnit) {
				DefUnit defunit = (DefUnit) elem;
				if(search.matches(defunit)) {
					search.addMatch(defunit);
					if(search.isFinished() && search.findOnlyOne)
						return; // Return if we only want one match in the scope
				}
			} else if(importsOnly && elem instanceof DeclarationImport) {
				DeclarationImport declImport = (DeclarationImport) elem;
				
				Module searchOriginModule = search.getSearchReferenceModule();
				if(!declImport.isTransitive && !privateNodeIsVisible(declImport, searchOriginModule))
					continue; // Don't consider private imports
				
				for (IImportFragment impFrag : declImport.imports) {
					impFrag.searchInSecondaryScope(search);
					// continue regardless of search.findOnlyOne because of partial packages
				}
			} 
		}
	}
	
	public static boolean privateNodeIsVisible(ASTNode node, Module searchOriginModule) {
		Module nodeModule = node.getModuleNode();
		// only visible if in node in same module as search origin ref.
		return searchOriginModule.getFullyQualifiedName().equals(nodeModule.getFullyQualifiedName());
	}
	
	/* ====================  import lookup  ==================== */

	public static void findDefUnitInStaticImport(ImportContent importStatic, CommonDefUnitSearch search) {
		DefUnit defunit = importStatic.getPartialDefUnit(search.modResolver);
		if(defunit != null && search.matches(defunit))
			search.addMatch(defunit);
	}
	
	public static void findDefUnitInContentImport(ImportContent impContent, CommonDefUnitSearch search) {
		findDefUnitInStaticImport(impContent, search);
		//if(search.isScopeFinished()) return;
		
		Module targetModule = findImporTargetModule(search.modResolver, impContent);
		if (targetModule != null)
			findDefUnitInScope(targetModule, search);
	}
	
	private static Module findImporTargetModule(IModuleResolver modResolver, IImportFragment impSelective) {
		String[] packages = impSelective.getModuleRef().packages.getInternalArray();
		String modules = impSelective.getModuleRef().module;
		Module targetModule;
		targetModule =  findModuleUnchecked(modResolver, packages, modules);
		return targetModule;
	}
	
	public static void findDefUnitInSelectiveImport(
			ImportSelective impSelective, CommonDefUnitSearch search) {

		Module targetModule = findImporTargetModule(search.modResolver, impSelective);
		if (targetModule == null)
			return;
			
		for(ASTNode impSelFrag: impSelective.impSelFrags) {
			if(impSelFrag instanceof RefImportSelection) {
				RefImportSelection refImportSelection = (RefImportSelection) impSelFrag;
				String name = refImportSelection.getIdString();
				// Do pre-emptive matching
				if(!search.matchesName(name)) {
					continue;
				}
				DefUnit defUnit = refImportSelection.findTargetDefUnit(search.modResolver);
				if(defUnit != null) { 
					search.addMatch(defUnit);
				}
			} // Aliases are matched in the primary namespace 
			/*
				else if(impSelFrag instanceof ImportSelectiveAlias) {
				ImportSelectiveAlias selFrag = (ImportSelectiveAlias) impSelFrag;
				if(search.matches(selFrag))
					search.addMatch(selFrag);
			} */
		}
	}

	public static IScopeNode getStartingScope(RefIdentifier refSingle) {
		IScopeNode scope = ScopeUtil.getOuterScope(refSingle);
		if(scope instanceof DefinitionFunction) {
			// Skip it as this scope can't look into itself
			scope = ScopeUtil.getOuterScope(scope);
		}
		return scope;
	}
	
	public static class DirectDefUnitResolve {
		
		protected ASTNode pickedNode;
		protected Reference pickedRef;
		public Collection<DefUnit> resolvedDefUnits;
		public boolean invalidPickRef = false;
		
		public void pickLocation(Module module, int offset) {
			ASTNodeFinder nodeFinder = new ASTNodeFinder(module, offset, true);
			
			if(nodeFinder.matchOnLeft instanceof Reference) {
				this.pickedNode = nodeFinder.matchOnLeft;
				this.pickedRef = (Reference) pickedNode;
			} else if(nodeFinder.match instanceof Reference) {
				this.pickedRef = (Reference) nodeFinder.match;
			}
			this.pickedNode = nodeFinder.match;
			
			if(pickedRef instanceof CommonRefQualified) {
				invalidPickRef = true;
			}
		}
		
		public boolean isValidPickRef() {
			return pickedRef != null && invalidPickRef == false;
		}
		
		public Collection<DefUnit> getResolvedDefUnits() {
			assertTrue(isValidPickRef()); // a valid ref must have picked from offset
			return resolvedDefUnits;
		}
		
	}
	
	public static DirectDefUnitResolve resolveAtOffset(DeeParserResult parseResult, int offset, IModuleResolver mr) {
		DirectDefUnitResolve refResolve = new DirectDefUnitResolve();
		
		refResolve.pickLocation(parseResult.module, offset);
		
		if(refResolve.isValidPickRef()) {
			refResolve.resolvedDefUnits = refResolve.pickedRef.findTargetDefUnits(mr, false);
		}
		return refResolve;
	}
	
}