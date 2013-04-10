package dtool.ast.definitions;

import melnorme.utilbox.tree.TreeVisitor;
import dtool.ast.ASTCodePrinter;
import dtool.ast.ASTNodeTypes;
import dtool.ast.IASTVisitor;
import dtool.ast.references.Reference;
import dtool.ast.statements.IStatement;
import dtool.refmodel.IScopeNode;
import dtool.refmodel.pluginadapters.IModuleResolver;

public class NamedMixinDeclaration extends DefUnit implements IStatement {
	
	public final Reference templateInstance;
	
	public NamedMixinDeclaration(Reference templateInstance, DefUnitTuple dudt) {
		super(dudt);
		this.templateInstance = parentize(templateInstance);
	}
	
	@Override
	public ASTNodeTypes getNodeType() {
		return ASTNodeTypes.NAMED_MIXIN;
	}
	
	@Override
	public void accept0(IASTVisitor visitor) {
		boolean children = visitor.visit(this);
		if (children) {
			TreeVisitor.acceptChildren(visitor, templateInstance);
			TreeVisitor.acceptChildren(visitor, defname);
		}
		visitor.endVisit(this);
	}
	
	@Override
	public void toStringAsCode(ASTCodePrinter cp) {
		cp.append("mixin ");
		cp.appendNode(templateInstance, " ");
		cp.appendNode(defname);
		cp.append(";");
	}
	
	@Override
	public EArcheType getArcheType() {
		return EArcheType.Mixin;
	}
	
	@Override
	public IScopeNode getMembersScope(IModuleResolver moduleResolver) {
		return templateInstance.getTargetScope(moduleResolver);
	}
	
}