/*
 * Copyright 2011-2013 Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.migration;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.LibraryUtil;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MethodCanBeVariableArityMethodInspection extends BaseInspection {

  @SuppressWarnings({"PublicField"})
  public boolean ignoreByteAndShortArrayParameters = false;

  @SuppressWarnings("PublicField")
  public boolean ignoreOverridingMethods = false;

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return InspectionGadgetsBundle.message("method.can.be.variable.arity.method.display.name");
  }

  @NotNull
  @Override
  protected String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("method.can.be.variable.arity.method.problem.descriptor");
  }

  @Override
  public JComponent createOptionsPanel() {
    final MultipleCheckboxOptionsPanel panel = new MultipleCheckboxOptionsPanel(this);
    panel.addCheckbox(InspectionGadgetsBundle.message("method.can.be.variable.arity.method.ignore.byte.short.option"),
                      "ignoreByteAndShortArrayParameters");
    panel.addCheckbox(InspectionGadgetsBundle.message("ignore.methods.overriding.super.method"),
                      "ignoreOverridingMethods");
    return panel;
  }

  @Override
  protected InspectionGadgetsFix buildFix(Object... infos) {
    return new MethodCanBeVariableArityMethodFix();
  }

  private static class MethodCanBeVariableArityMethodFix extends InspectionGadgetsFix {
       @Override
  @NotNull
  public String getFamilyName() {
    return getName();
  }

    @NotNull
    @Override
    public String getName() {
      return InspectionGadgetsBundle.message("convert.to.variable.arity.method.quickfix");
    }

    @Override
    protected void doFix(Project project, ProblemDescriptor descriptor) {
      final PsiElement element = descriptor.getPsiElement();
      final PsiElement parent = element.getParent();
      if (!(parent instanceof PsiMethod)) {
        return;
      }
      final PsiMethod method = (PsiMethod)parent;
      final PsiParameterList parameterList = method.getParameterList();
      if (parameterList.getParametersCount() == 0) {
        return;
      }
      final PsiParameter[] parameters = parameterList.getParameters();
      final PsiParameter lastParameter = parameters[parameters.length - 1];
      final PsiType type = lastParameter.getType();
      if (!(type instanceof PsiArrayType)) {
        return;
      }
      final PsiArrayType arrayType = (PsiArrayType)type;
      final PsiType componentType = arrayType.getComponentType();
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
      final PsiTypeElement newTypeElement = factory.createTypeElementFromText(componentType.getCanonicalText() + "...", method);
      final PsiTypeElement typeElement = lastParameter.getTypeElement();
      if (typeElement != null) {
        typeElement.replace(newTypeElement);
      }
    }
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new MethodCanBeVariableArityMethodVisitor();
  }

  private class MethodCanBeVariableArityMethodVisitor extends BaseInspectionVisitor {

    @Override
    public void visitMethod(PsiMethod method) {
      if (!PsiUtil.isLanguageLevel5OrHigher(method)) {
        return;
      }
      super.visitMethod(method);
      final PsiParameterList parameterList = method.getParameterList();
      if (parameterList.getParametersCount() == 0) {
        return;
      }
      final PsiParameter[] parameters = parameterList.getParameters();
      final PsiParameter lastParameter = parameters[parameters.length - 1];
      final PsiType type = lastParameter.getType();
      if (!(type instanceof PsiArrayType)) {
        return;
      }
      if (type instanceof PsiEllipsisType) {
        return;
      }
      final PsiArrayType arrayType = (PsiArrayType)type;
      final PsiType componentType = arrayType.getComponentType();
      if (componentType instanceof PsiArrayType) {
        // don't report when it is multidimensional array
        return;
      }
      if (ignoreByteAndShortArrayParameters) {
        if (PsiType.BYTE.equals(componentType) || PsiType.SHORT.equals(componentType)) {
          return;
        }
      }
      if (LibraryUtil.isOverrideOfLibraryMethod(method)) {
        return;
      }
      if (ignoreOverridingMethods && MethodUtils.hasSuper(method)) {
        return;
      }
      registerMethodError(method);
    }
  }
}
