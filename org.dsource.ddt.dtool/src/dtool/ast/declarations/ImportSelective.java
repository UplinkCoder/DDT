package dtool.ast.declarations;

import java.util.Arrays;
import java.util.Iterator;

import melnorme.utilbox.tree.TreeVisitor;
import descent.internal.compiler.parser.ast.IASTNode;
import dtool.ast.ASTNeoNode;
import dtool.ast.IASTNeoVisitor;
import dtool.ast.SourceRange;
import dtool.ast.declarations.DeclarationImport.ImportFragment;
import dtool.ast.definitions.DefUnit;
import dtool.ast.definitions.EArcheType;
import dtool.ast.references.RefImportSelection;
import dtool.ast.references.RefModule;
import dtool.refmodel.CommonDefUnitSearch;
import dtool.refmodel.INonScopedBlock;
import dtool.refmodel.IScopeNode;
import dtool.refmodel.ReferenceResolver;

public class ImportSelective extends ImportFragment implements INonScopedBlock {
	public static interface IImportSelectiveSelection extends IASTNode {
		//String getTargetName();
	}
	
	public static class ImportSelectiveAlias extends DefUnit 
		implements IImportSelectiveSelection {

		public final RefImportSelection target;
		
		public ImportSelectiveAlias(DefUnitDataTuple dudt, RefImportSelection impSelection, SourceRange sourceRange) {
			super(dudt);
			initSourceRange(sourceRange);
			this.target = impSelection; parentize(this.target);
		}

		@Override
		public void accept0(IASTNeoVisitor visitor) {
			boolean children = visitor.visit(this);
			if (children) {
				TreeVisitor.acceptChildren(visitor, defname);
				TreeVisitor.acceptChildren(visitor, target);
			}
			visitor.endVisit(this);		
		}


		@Override
		public EArcheType getArcheType() {
			return EArcheType.Alias;
		}

		@Override
		public IScopeNode getMembersScope() {
			return target.getTargetScope();
		}
	}

	
	public ASTNeoNode impSelFrags[];
	
	public ImportSelective(RefModule refModule, ASTNeoNode[] frags, SourceRange sourceRange) {
		super(refModule, sourceRange);
		this.impSelFrags = frags; parentizeFrags(this.impSelFrags);
	}
	
	public void parentizeFrags(ASTNeoNode[] frags) {
		if (frags != null) {
			for (ASTNeoNode n : frags) {
				n.setParent(this);
				if (n instanceof ImportSelectiveAlias) {
					((ImportSelectiveAlias) n).target.impSel = this;
				} else if (n instanceof RefImportSelection) {
					((RefImportSelection) n).impSel = this;
				}
			}
		}
	}
	
	@Override
	public void accept0(IASTNeoVisitor visitor) {
		boolean children = visitor.visit(this);
		if (children) {
			TreeVisitor.acceptChildren(visitor, moduleRef);
			TreeVisitor.acceptChildren(visitor, impSelFrags);
		}
		visitor.endVisit(this);
	
	}
	@Override
	public Iterator<? extends ASTNeoNode> getMembersIterator() {
		return Arrays.asList(impSelFrags).iterator();
	}

	@Override
	public void searchInSecondaryScope(CommonDefUnitSearch search) {
		ReferenceResolver.findDefUnitInSelectiveImport(this, search);
	}
	
	@Override
	public String toStringAsElement() {
		String str = "";
		for (int i = 0; i < impSelFrags.length; i++) {
			ASTNeoNode fragment = impSelFrags[i];
			if(i > 0)
				str = str + ", ";
			str = str + fragment.toStringAsElement();
		}
		return moduleRef.toStringAsElement() + " : " + str;
	}

}