package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiSuperMethodUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;

import java.text.MessageFormat;

public class MethodReturnFix implements IntentionAction {
  private final PsiMethod myMethod;
  private final PsiType myReturnType;
  private final boolean myFixWholeHierarchy;

  public MethodReturnFix(PsiMethod method, PsiType toReturn, boolean fixWholeHierarchy) {
    this.myMethod = method;
    this.myReturnType = toReturn;
    myFixWholeHierarchy = fixWholeHierarchy;
  }

  public String getText() {
    final String text = MessageFormat.format("Make ''{0}'' return ''{1}''",
        new Object[]{
          myMethod.getName(),
          myReturnType.getCanonicalText(),
        });
    return text;
  }

  public String getFamilyName() {
    return "Fix Return Type";
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    return myMethod != null
        && myMethod.isValid()
        && myMethod.getManager().isInProject(myMethod)
        && myReturnType != null
        && myReturnType.isValid()
        && !TypeConversionUtil.isNullType(myReturnType)
        && myMethod.getReturnType() != null
        && !Comparing.equal(myReturnType, myMethod.getReturnType());
  }

  public void invoke(Project project, Editor editor, PsiFile file) {
    if (!CodeInsightUtil.prepareFileForWrite(myMethod.getContainingFile())) return;
    PsiMethod method = myFixWholeHierarchy ? PsiSuperMethodUtil.findDeepestSuperMethod(myMethod) : myMethod;
    if (method == null) method = myMethod;
    ChangeSignatureProcessor processor = new ChangeSignatureProcessor(myMethod.getProject(),
                                                                      method,
        false, null,
        method.getName(),
        myReturnType,
        RemoveUnusedParameterFix.getNewParametersInfo(method, null));
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      processor.testRun();
    }
    else {
      processor.run();
    }
    if (method.getContainingFile() != file) {
      QuickFixAction.markDocumentForUndo(file);
    }
  }

  public boolean startInWriteAction() {
    return true;
  }

}
