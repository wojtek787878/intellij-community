// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleSettings;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OverrideImplementsAnnotationsHandlerImpl implements OverrideImplementsAnnotationsHandler {
  @Override
  public String[] getAnnotations(Project project) {
    List<String> annotations = getCoreAnnotations(project);

    CodeStyleSettings settings = CodeStyle.getSettings(project);
    annotations.addAll(settings.getCustomSettings(JavaCodeStyleSettings.class).getRepeatAnnotations());

    return ArrayUtilRt.toStringArray(annotations);
  }

  @Override
  public String[] getAnnotations(@NotNull PsiFile file) {
    List<String> annotations = getCoreAnnotations(file.getProject());
    annotations.addAll(JavaCodeStyleSettings.getInstance(file).getRepeatAnnotations());
    return ArrayUtilRt.toStringArray(annotations);
  }

  @NotNull
  private static List<String> getCoreAnnotations(Project project) {
    List<String> annotations = new ArrayList<>();

    NullableNotNullManager manager = NullableNotNullManager.getInstance(project);
    annotations.addAll(manager.getNotNulls());
    annotations.addAll(manager.getNullables());

    annotations.add(AnnotationUtil.NLS);
    return annotations;
  }
}