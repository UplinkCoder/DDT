/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package org.dsource.ddt.ide.core.model.engine;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import org.dsource.ddt.ide.core.model.DeeModelConstants;
import org.dsource.ddt.ide.core.model.DeeModuleDeclaration;
import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.compiler.IElementRequestor.FieldInfo;
import org.eclipse.dltk.compiler.IElementRequestor.TypeInfo;
import org.eclipse.dltk.compiler.ISourceElementRequestor;

import descent.internal.compiler.parser.STC;
import dtool.ast.definitions.ArrayView;
import dtool.ast.definitions.BaseClass;
import dtool.ast.definitions.DefUnit;
import dtool.ast.definitions.Definition;
import dtool.ast.definitions.DefinitionAlias;
import dtool.ast.definitions.DefinitionClass;
import dtool.ast.definitions.DefinitionCtor;
import dtool.ast.definitions.DefinitionEnum;
import dtool.ast.definitions.DefinitionFunction;
import dtool.ast.definitions.DefinitionInterface;
import dtool.ast.definitions.DefinitionStruct;
import dtool.ast.definitions.DefinitionTemplate;
import dtool.ast.definitions.DefinitionTypedef;
import dtool.ast.definitions.DefinitionUnion;
import dtool.ast.definitions.DefinitionVariable;
import dtool.ast.definitions.ICallableElement;
import dtool.ast.definitions.IFunctionParameter;
import dtool.ast.definitions.Module;
import dtool.ast.references.NamedReference;

public final class DeeSourceElementProvider extends DeeSourceElementProvider_BaseVisitor {
	
	protected static final String[] EMPTY_STRING = new String[0];
	protected static final String OBJECT = "Object"; //$NON-NLS-1$
	protected static final String[] OBJECT_SUPER_CLASS_LIST = new String[] { OBJECT };
	
	protected ISourceElementRequestor requestor;
	
	public DeeSourceElementProvider(ISourceElementRequestor requestor) {
		this.requestor = requestor;
	}
	
	public void provide(DeeModuleDeclaration moduleDecl) {
		requestor.enterModule();
		
		Module neoModule = moduleDecl.neoModule;
		if(neoModule != null) {
			neoModule.accept(this);
		}
		
		requestor.exitModule(moduleDecl.dmdModule.getEndPos());
	}
	
	@Override
	public boolean visit(Module node) {
		requestor.enterType(createTypeInfoForModule(node));
//		DeclarationModule md = node.md;
//		String pkgName = "";
//		if(md != null) {
//			for (int i = 0; i < md.packages.length; i++) {
//				String id = md.packages[i];
//				if(i == 0) {
//					pkgName = pkgName + id.toString();
//				} else {
//					pkgName = pkgName + "." + id.toString();
//				}
//			}
//			requestor.acceptPackage(md.getStartPos(), md.getEndPos()-1, pkgName.toCharArray());
//		} else {
//			//requestor.acceptPackage(0, 0-1, "".toCharArray());
//		}
		return true;
	}
	
	@Override
	public void endVisit(Module node) {
		requestor.exitType(node.sourceEnd() - 1);
	}
	
	
	@Override
	public boolean visit(DefinitionStruct node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_STRUCT));
		return true;
	}
	@Override
	public void endVisit(DefinitionStruct node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionUnion node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_UNION));
		return true;
	}
	@Override
	public void endVisit(DefinitionUnion node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionInterface node) {
		requestor.enterType(createTypeInfoForInterface(node));
		return true;
	}
	@Override
	public void endVisit(DefinitionInterface node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionClass node) {
		assertTrue(node.getClass() == DefinitionClass.class);
		requestor.enterType(createTypeInfoForClass(node));
		return true;
	}
	@Override
	public void endVisit(DefinitionClass node) {
		endVisitDefinition(node);
	}
	
	public final void endVisitDefinition(Definition node) {
		requestor.exitType(node.sourceEnd() - 1);
	}
	
	@Override
	public boolean visit(DefinitionTemplate node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_TEMPLATE));
		return true;
	}
	@Override
	public void endVisit(DefinitionTemplate node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionEnum node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_ENUM));
		return true;
	}
	@Override
	public void endVisit(DefinitionEnum node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionTypedef node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_TYPEDEF));
		return true;
	}
	@Override
	public void endVisit(DefinitionTypedef node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionAlias node) {
		requestor.enterType(createTypeInfoForDefinition(node, DeeModelConstants.TYPE_ALIAS));
		return true;
	}
	@Override
	public void endVisit(DefinitionAlias node) {
		endVisitDefinition(node);
	}
	
	/* ---------------------------------- */
	
	@Override
	public boolean visit(DefinitionFunction node) {
		requestor.enterMethod(createMethodInfo(node));
		return true;
	}
	@Override
	public void endVisit(DefinitionFunction node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(DefinitionCtor node) {
		requestor.enterMethod(createConstructorInfo(node));
		return true;
	}
	@Override
	public void endVisit(DefinitionCtor node) {
		requestor.exitType(node.sourceEnd() - 1);
	}
	
	/* ---------------------------------- */
	
	@Override
	public boolean visit(DefinitionVariable node) {
		requestor.enterField(createFieldInfo(node));
		return true;
	}
	
	@Override
	public void endVisit(DefinitionVariable node) {
		endVisitDefinition(node);
	}
	
	@Override
	public boolean visit(NamedReference elem) {
		requestor.acceptTypeReference(elem.toStringAsElement(), elem.sourceStart() /*-1*/);
		return true;
	}
	
	/* ================================== */
	
	
	
	protected static void setupDefUnitTypeInfo(DefUnit defAggr, ISourceElementRequestor.ElementInfo elemInfo) {
		elemInfo.name = defAggr.getName();
		elemInfo.declarationStart = defAggr.sourceStart();
		elemInfo.nameSourceStart = defAggr.defname.sourceStart();
		elemInfo.nameSourceEnd = defAggr.defname.sourceEnd() - 1;
	}
	
	protected static void setupDefinitionTypeInfo(Definition elem, ISourceElementRequestor.ElementInfo elemInfo) {
		elemInfo.modifiers = getModifiersFlags(elem);
		elemInfo.modifiers = getProtectionFlags(elem, elemInfo.modifiers);
	}
	
	
	
	protected static int getModifiersFlags(Definition elem) {
		int modifiers = 0;
		
		modifiers = addBitFlag(elem.effectiveModifiers, STC.STCabstract, modifiers, Modifiers.AccAbstract);
		modifiers = addBitFlag(elem.effectiveModifiers, STC.STCconst, modifiers, Modifiers.AccConst);
		modifiers = addBitFlag(elem.effectiveModifiers, STC.STCfinal, modifiers, Modifiers.AccFinal);
		modifiers = addBitFlag(elem.effectiveModifiers, STC.STCstatic, modifiers, Modifiers.AccStatic);
/*		
		for (int i = 0; i < elem.modifiers.length; i++) {
			Modifier mod = elem.modifiers[i];
			if(mod.tok.value.equals(TOK.TOKabstract))
				modifiers |= Modifiers.AccAbstract; 
			if(mod.tok.value.equals(TOK.TOKconst))
				modifiers |= Modifiers.AccConst; 
			if(mod.tok.value.equals(TOK.TOKfinal))
				modifiers |= Modifiers.AccFinal; 
			if(mod.tok.value.equals(TOK.TOKstatic))
				modifiers |= Modifiers.AccStatic; 
//			if(mod.tok.value.equals(TOK.TOKabstract))
//				modifiers |= Modifiers.AccAbstract; 
				
		}*/
		return modifiers;
	}
	
	private static int addBitFlag(int effectiveModifiers, int conditionFlag, int modifiers, int modifierFlag) {
		if((effectiveModifiers & conditionFlag) != 0) {
			modifiers |= modifierFlag;
		}
		return modifiers;
	}
	
	protected static int getProtectionFlags(Definition elem, int modifiers) {
		// default:
		
		switch(elem.getEffectiveProtection()) {
		case PROTprivate: modifiers |= Modifiers.AccPrivate; break;
		case PROTpublic: modifiers |= Modifiers.AccPublic; break;
		case PROTprotected: modifiers |= Modifiers.AccProtected; break;
		case PROTpackage: modifiers |= Modifiers.AccDefault; break;
		default: modifiers |= Modifiers.AccPublic;
		}
		return modifiers;
	}
	
	protected static TypeInfo createTypeInfoForModule(Module elem) {
		ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
		setupDefUnitTypeInfo(elem, typeInfo);
		typeInfo.modifiers |= Modifiers.AccModule;
		//typeInfo.superclasses = DeeSourceElementProvider.EMPTY_STRING;
		return typeInfo;
	}
	
	protected static TypeInfo createTypeInfoForDefinition(Definition elem, int archetypeMask) {
		assertTrue((archetypeMask & DeeModelConstants.MODIFIERS_ARCHETYPE_MASK) == archetypeMask);
		ISourceElementRequestor.TypeInfo typeInfo = new ISourceElementRequestor.TypeInfo();
		setupDefUnitTypeInfo(elem, typeInfo);
		setupDefinitionTypeInfo(elem, typeInfo);
		typeInfo.modifiers |= archetypeMask;
		typeInfo.superclasses = DeeSourceElementProvider.EMPTY_STRING;
		return typeInfo;
	}
	
	
	protected static TypeInfo createTypeInfoForClass(DefinitionClass elem) {
		int archeType = DeeModelConstants.TYPE_CLASS;
		ISourceElementRequestor.TypeInfo typeInfo = createTypeInfoForDefinition(elem, archeType);
		typeInfo.superclasses = DeeSourceElementProvider.processSuperClassNames(elem, false);
		return typeInfo;
	}
	
	protected static TypeInfo createTypeInfoForInterface(DefinitionInterface elem) {
		int archetype = DeeModelConstants.TYPE_INTERFACE;
		ISourceElementRequestor.TypeInfo typeInfo = createTypeInfoForDefinition(elem, archetype);
		typeInfo.modifiers |= Modifiers.AccInterface;
		typeInfo.superclasses = DeeSourceElementProvider.processSuperClassNames(elem, true);
		return typeInfo;
	}
	
	
	protected static String[] processSuperClassNames(DefinitionClass defClass, boolean isInterface) {
		if(defClass.getName().equals("Object"))
			return DeeSourceElementProvider.EMPTY_STRING;
		
		BaseClass[] baseClasses = defClass.baseClasses;
		if(baseClasses == null || baseClasses.length == 0) {
			if(isInterface) {
				return DeeSourceElementProvider.EMPTY_STRING;
			} else {
				return DeeSourceElementProvider.OBJECT_SUPER_CLASS_LIST;
			}
		}
		String[] baseClassesStr = new String[baseClasses.length];
		for (int i = 0; i < baseClasses.length; i++) {
			// There is no way this can work without a FQN, but I don't know what DLTK wants
			baseClassesStr[i] = baseClasses[i].type.toStringAsElement(); 
		}
		return baseClassesStr;
	}
	
	protected static ISourceElementRequestor.MethodInfo createMethodInfo(DefinitionFunction elem) {
		ISourceElementRequestor.MethodInfo methodInfo = new ISourceElementRequestor.MethodInfo();
		setupDefUnitTypeInfo(elem, methodInfo);
		setupDefinitionTypeInfo(elem, methodInfo);
		
		setupParametersInfo(elem, methodInfo);
		return methodInfo;
	}
	
	protected static ISourceElementRequestor.MethodInfo createConstructorInfo(DefinitionCtor elem) {
		ISourceElementRequestor.MethodInfo elemInfo = new ISourceElementRequestor.MethodInfo();
		elemInfo.declarationStart = elem.sourceStart();
		elemInfo.isConstructor = true; // for the purposes of the ModelElement's, any kind is constructor
		elemInfo.name = elem.kind.specialName;
		elemInfo.nameSourceStart = elem.nameStart;
		elemInfo.nameSourceEnd = elem.nameStart + elem.kind.specialName.length() - 1; 
		
		//setupDefinitionTypeInfo(elem, methodInfo);
		setupParametersInfo(elem, elemInfo);
		return elemInfo;
	}
	
	protected static void setupParametersInfo(ICallableElement elem, ISourceElementRequestor.MethodInfo methodInfo) {
		ArrayView<IFunctionParameter> params = elem.getParameters();
		
		methodInfo.parameterNames = new String[params.size()];
		methodInfo.parameterInitializers = new String[params.size()];
		for (int i = 0; i < methodInfo.parameterNames.length; i++) {
			String name = params.get(i).toStringAsFunctionSimpleSignaturePart();
			if(name == null) {
				name = "";
			}
			methodInfo.parameterNames[i] = name;
			String initStr = params.get(i).toStringInitializer();
			methodInfo.parameterInitializers[i] = initStr; 
		}
	}
	
	protected static FieldInfo createFieldInfo(DefinitionVariable elem) {
		ISourceElementRequestor.FieldInfo fieldInfo = new ISourceElementRequestor.FieldInfo();
		setupDefUnitTypeInfo(elem, fieldInfo);
		setupDefinitionTypeInfo(elem, fieldInfo);
		return fieldInfo;
	}
	
}