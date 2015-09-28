package com.kms.katalon.groovy.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.refactoring.DelegateUIHelper;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardPage;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import com.kms.katalon.groovy.constant.GroovyConstants;

@SuppressWarnings("restriction")
public class GroovyCompilationHelper {
	
	private static String constructSimpleTypeStub(String typeName) {
		StringBuffer buf= new StringBuffer("public class "); //$NON-NLS-1$
		buf.append(typeName);
		buf.append("{ }"); //$NON-NLS-1$
		return buf.toString();
	}
	
	private static String constructCUContent(ICompilationUnit cu, String typeContent, String lineDelimiter) throws Exception {
		IPackageFragment pack= (IPackageFragment) cu.getParent();
		String content= CodeGeneration.getCompilationUnitContent(cu, null, null, typeContent, lineDelimiter);
		if (content != null) {
			ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
			parser.setProject(cu.getJavaProject());
			parser.setSource(content.toCharArray());
			CompilationUnit unit= (CompilationUnit) parser.createAST(null);
			if ((pack.isDefaultPackage() || unit.getPackage() != null) && !unit.types().isEmpty()) {
				return content;
			}
		}
		StringBuffer buf= new StringBuffer();
		if (!pack.isDefaultPackage()) {
			buf.append("package ").append(pack.getElementName()).append(';'); //$NON-NLS-1$
		}
		buf.append(lineDelimiter).append(lineDelimiter);
		buf.append(typeContent);
		return buf.toString();
	}
	
	private static CompilationUnit createASTForImports(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setFocalPosition(0);
		return (CompilationUnit) parser.createAST(null);
	}

	@SuppressWarnings("unchecked")
	private static Set<String> getExistingImports(CompilationUnit root) {
		List<ImportDeclaration> imports= root.imports();
		Set<String> res= new HashSet<String>(imports.size());
		for (int i= 0; i < imports.size(); i++) {
			res.add(ASTNodes.asString(imports.get(i)));
		}
		return res;
	}
	
	private static String constructTypeStub(String typeName, ICompilationUnit parentCU, ImportsManager imports, String lineDelimiter) throws Exception {
		StringBuffer buf= new StringBuffer();

		int modifiers= Flags.AccPublic;
		buf.append(Flags.toString(modifiers));
		if (modifiers != 0) {
			buf.append(' ');
		}
		String type= "class ";  //$NON-NLS-1$
		String templateID= CodeGeneration.CLASS_BODY_TEMPLATE_ID;
		
		buf.append(type);
		buf.append(typeName);

		buf.append(" {").append(lineDelimiter); //$NON-NLS-1$
		String typeBody= CodeGeneration.getTypeBody(templateID, parentCU, typeName, lineDelimiter);
		if (typeBody != null) {
			buf.append(typeBody);
		} else {
			buf.append(lineDelimiter);
		}
		buf.append('}').append(lineDelimiter);
		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	private static void removeUnusedImports(ICompilationUnit cu, Set<String> existingImports, boolean needsSave) throws Exception {
		ASTParser parser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);
		parser.setSource(cu);
		parser.setResolveBindings(true);

		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		if (root.getProblems().length == 0) {
			return;
		}

		List<ImportDeclaration> importsDecls= root.imports();
		if (importsDecls.isEmpty()) {
			return;
		}
		ImportsManager imports= new ImportsManager(root);

		int importsEnd= ASTNodes.getExclusiveEnd(importsDecls.get(importsDecls.size() - 1));
		IProblem[] problems= root.getProblems();
		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			if (curr.getSourceEnd() < importsEnd) {
				int id= curr.getID();
				if (id == IProblem.UnusedImport || id == IProblem.NotVisibleType) { // not visible problems hide unused -> remove both
					int pos= curr.getSourceStart();
					for (int k= 0; k < importsDecls.size(); k++) {
						ImportDeclaration decl= importsDecls.get(k);
						if (decl.getStartPosition() <= pos && pos < decl.getStartPosition() + decl.getLength()) {
							if (existingImports.isEmpty() || !existingImports.contains(ASTNodes.asString(decl))) {
								String name= decl.getName().getFullyQualifiedName();
								if (decl.isOnDemand()) {
									name += ".*"; //$NON-NLS-1$
								}
								if (decl.isStatic()) {
									imports.removeStaticImport(name);
								} else {
									imports.removeImport(name);
								}
							}
							break;
						}
					}
				}
			}
		}
		imports.create(needsSave, null);
	}
	
	public static String getCompilationUnitName(String typeName) {
		return typeName + GroovyConstants.GROOVY_FILE_EXTENSION;
	}
	
	private static ICompilationUnit createType(IPackageFragment parentPackage, String typeName) throws Exception {
	    return createType(parentPackage, typeName, true);
	}
	
	private static ICompilationUnit createType(IPackageFragment parentPackage, String typeName, boolean needDefaultImport) throws Exception {
		boolean needsSave;
		ICompilationUnit connectedCU= null;
		try {
			IType createdType;
			ImportsManager imports;
			int indent= 0;

			Set<String> existingImports;

			String lineDelimiter= null;
			lineDelimiter= StubUtility.getLineDelimiterUsed(parentPackage.getJavaProject());

			String cuName= typeName + GroovyConstants.GROOVY_FILE_EXTENSION;
			ICompilationUnit parentCU= parentPackage.createCompilationUnit(cuName, "", false, null); //$NON-NLS-1$

			needsSave= true;
			parentCU.becomeWorkingCopy(null);
			connectedCU= parentCU;

			IBuffer buffer= parentCU.getBuffer();

			String simpleTypeStub= constructSimpleTypeStub(typeName);
			
			String cuContent= constructCUContent(parentCU, simpleTypeStub, lineDelimiter);
			buffer.setContents(cuContent);

			CompilationUnit astRoot= createASTForImports(parentCU);
			existingImports= getExistingImports(astRoot);

            imports = needDefaultImport ? addImports(parentPackage, typeName, astRoot) : new ImportsManager(astRoot);

			String typeContent= constructTypeStub(typeName, parentCU, imports, lineDelimiter);

			int index= cuContent.lastIndexOf(simpleTypeStub);
			if (index == -1) {
				AbstractTypeDeclaration typeNode= (AbstractTypeDeclaration) astRoot.types().get(0);
				int start= ((ASTNode) typeNode.modifiers().get(0)).getStartPosition();
				int end= typeNode.getStartPosition() + typeNode.getLength();
				buffer.replace(start, end - start, typeContent);
			} else {
				buffer.replace(index, simpleTypeStub.length(), typeContent);
			}

			createdType= parentCU.getType(typeName);
			
			// add imports for superclass/interfaces, so types can be resolved correctly

			ICompilationUnit cu= createdType.getCompilationUnit();

			imports.create(false, null);

			JavaModelUtil.reconcile(cu);

			// set up again
			astRoot= createASTForImports(imports.getCompilationUnit());
			imports= new ImportsManager(astRoot);

			// add imports
			imports.create(false, null);

			removeUnusedImports(cu, existingImports, false);

			JavaModelUtil.reconcile(cu);

			ISourceRange range= createdType.getSourceRange();

			IBuffer buf= cu.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());

			String formattedContent= CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, originalContent, indent, lineDelimiter, parentPackage.getJavaProject());
			formattedContent= Strings.trimLeadingTabsAndSpaces(formattedContent);
			buf.replace(range.getOffset(), range.getLength(), formattedContent);

			if (needsSave) {
				cu.commitWorkingCopy(true, null);
			}

		} finally {
			if (connectedCU != null) {
				connectedCU.discardWorkingCopy();
			}
		}
		return connectedCU;
	}

	protected static ImportsManager addImports(IPackageFragment parentPackage, String typeName, CompilationUnit astRoot) {
		ImportsManager imports;
		imports= new ImportsManager(astRoot);
		// add an import that will be removed again. Having this import solves 14661
		imports.addImport(JavaModelUtil.concatenateName(parentPackage.getElementName(), typeName));
		for (String className : GroovyConstants.getStartingClassesName()) {
		    imports.addImport(className);
		}
		return imports;
	}

	public static ICompilationUnit createGroovyType(IPackageFragment parentPackage, String typeName)
            throws Exception {
	    return createGroovyType(parentPackage, typeName, true, true);
	}
	
    public static ICompilationUnit createGroovyType(IPackageFragment parentPackage, String typeName, boolean noClassDeclaration, boolean needDefaultImport)
            throws Exception {
        if (noClassDeclaration) {
            createType(parentPackage, typeName);
        } else {
            createType(parentPackage, typeName, needDefaultImport);
        }
		GroovyCompilationUnit unit = (GroovyCompilationUnit) parentPackage
				.getCompilationUnit(getCompilationUnitName(typeName));
		try {
			unit.becomeWorkingCopy(null);

			// remove ';' on package declaration
			IPackageDeclaration[] packs = unit.getPackageDeclarations();
			char[] contents = unit.getContents();
			MultiTextEdit multi = new MultiTextEdit();
			if (packs.length > 0) {
				ISourceRange range = packs[0].getSourceRange();
				int position = range.getOffset() + range.getLength();
				if (contents[position] == ';') {
					multi.addChild(new ReplaceEdit(position, 1, ""));
				}
			}

			// remove ';' on import declaration
			IImportDeclaration[] imports = unit.getImports();
			if (imports != null && imports.length > 0) {
				ISourceRange range = imports[0].getSourceRange();
				int position = range.getOffset() + range.getLength() - 1;
				if (contents[position] == ';') {
					multi.addChild(new ReplaceEdit(position, 1, ""));
				}
			}

            if (noClassDeclaration) {
             // remove type declaration for scripts
                ISourceRange range = unit.getTypes()[0].getSourceRange();
                multi.addChild(new DeleteEdit(range.getOffset(), range.getLength()));
            }

			if (multi.hasChildren()) {
				unit.applyTextEdit(multi, null);
				unit.commitWorkingCopy(true, null);
			}
		} finally {
			if (unit != null) {
				unit.discardWorkingCopy();
			}
		}
		return unit;
	}
	
	public static RenameJavaElementDescriptor createRenameDescriptor(IJavaElement javaElement, String newName) throws JavaModelException {
		String contributionId;
		// see RefactoringExecutionStarter#createRenameSupport(..):
		int elementType= javaElement.getElementType();
		switch (elementType) {
			case IJavaElement.JAVA_PROJECT:
				contributionId= IJavaRefactorings.RENAME_JAVA_PROJECT;
				break;
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				contributionId= IJavaRefactorings.RENAME_SOURCE_FOLDER;
				break;
			case IJavaElement.PACKAGE_FRAGMENT:
				contributionId= IJavaRefactorings.RENAME_PACKAGE;
				break;
			case IJavaElement.COMPILATION_UNIT:
				contributionId= IJavaRefactorings.RENAME_COMPILATION_UNIT;
				break;
			case IJavaElement.TYPE:
				contributionId= IJavaRefactorings.RENAME_TYPE;
				break;
			case IJavaElement.METHOD:
				final IMethod method= (IMethod) javaElement;
				if (method.isConstructor())
					return createRenameDescriptor(method.getDeclaringType(), newName);
				else
					contributionId= IJavaRefactorings.RENAME_METHOD;
				break;
			case IJavaElement.FIELD:
				IField field= (IField) javaElement;
				if (field.isEnumConstant())
					contributionId= IJavaRefactorings.RENAME_ENUM_CONSTANT;
				else
					contributionId= IJavaRefactorings.RENAME_FIELD;
				break;
			case IJavaElement.TYPE_PARAMETER:
				contributionId= IJavaRefactorings.RENAME_TYPE_PARAMETER;
				break;
			case IJavaElement.LOCAL_VARIABLE:
				contributionId= IJavaRefactorings.RENAME_LOCAL_VARIABLE;
				break;
			default:
				return null;
		}

		RenameJavaElementDescriptor descriptor= (RenameJavaElementDescriptor) RefactoringCore.getRefactoringContribution(contributionId).createDescriptor();
		descriptor.setJavaElement(javaElement);
		descriptor.setNewName(newName);
		if (elementType != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			descriptor.setUpdateReferences(true);

		IDialogSettings javaSettings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings refactoringSettings= javaSettings.getSection(RefactoringWizardPage.REFACTORING_SETTINGS); //TODO: undocumented API
		if (refactoringSettings == null) {
			refactoringSettings= javaSettings.addNewSection(RefactoringWizardPage.REFACTORING_SETTINGS);
		}

		switch (elementType) {
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
				descriptor.setDeprecateDelegate(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_DEPRECATION));
				descriptor.setKeepOriginal(refactoringSettings.getBoolean(DelegateUIHelper.DELEGATE_UPDATING));
		}
		switch (elementType) {
			case IJavaElement.TYPE:
//			case IJavaElement.COMPILATION_UNIT: // TODO
				descriptor.setUpdateSimilarDeclarations(refactoringSettings.getBoolean(RenameRefactoringWizard.TYPE_UPDATE_SIMILAR_ELEMENTS));
				int strategy;
				try {
					strategy= refactoringSettings.getInt(RenameRefactoringWizard.TYPE_SIMILAR_MATCH_STRATEGY);
				} catch (NumberFormatException e) {
					strategy= RenamingNameSuggestor.STRATEGY_EXACT;
				}
				descriptor.setMatchStrategy(strategy);
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
				descriptor.setUpdateHierarchy(refactoringSettings.getBoolean(RenameRefactoringWizard.PACKAGE_RENAME_SUBPACKAGES));
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.TYPE:
				String fileNamePatterns= refactoringSettings.get(RenameRefactoringWizard.QUALIFIED_NAMES_PATTERNS);
				if (fileNamePatterns != null && fileNamePatterns.length() != 0) {
					descriptor.setFileNamePatterns(fileNamePatterns);
					boolean updateQualifiedNames= refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_QUALIFIED_NAMES);
					descriptor.setUpdateQualifiedNames(updateQualifiedNames);
				}
		}
		switch (elementType) {
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
				boolean updateTextualOccurrences= refactoringSettings.getBoolean(RenameRefactoringWizard.UPDATE_TEXTUAL_MATCHES);
				descriptor.setUpdateTextualOccurrences(updateTextualOccurrences);
		}
		switch (elementType) {
			case IJavaElement.FIELD:
				descriptor.setRenameGetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_GETTER));
				descriptor.setRenameSetters(refactoringSettings.getBoolean(RenameRefactoringWizard.FIELD_RENAME_SETTER));
		}
		return descriptor;
	}
}
