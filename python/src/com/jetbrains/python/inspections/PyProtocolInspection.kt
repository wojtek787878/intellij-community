// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.jetbrains.python.codeInsight.typing.InspectingProtocolSubclassCallback
import com.jetbrains.python.codeInsight.typing.inspectProtocolSubclass
import com.jetbrains.python.codeInsight.typing.isProtocol
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyTypeChecker

class PyProtocolInspection : PyInspection() {

  override fun buildVisitor(holder: ProblemsHolder,
                            isOnTheFly: Boolean,
                            session: LocalInspectionToolSession): PsiElementVisitor = Visitor(holder, session)

  private class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) : PyInspectionVisitor(holder, session) {

    override fun visitPyClass(node: PyClass?) {
      super.visitPyClass(node)

      val type = node?.let { myTypeEvalContext.getType(it) }
      if (type is PyClassType) {
        type
          .getSuperClassTypes(myTypeEvalContext)
          .asSequence()
          .filterIsInstance<PyClassType>()
          .filter { isProtocol(it, myTypeEvalContext) }
          .forEach { protocol ->
            inspectProtocolSubclass(
              protocol,
              type,
              myTypeEvalContext,
              object : InspectingProtocolSubclassCallback {
                override fun onUnresolved(protocolElement: PyTypedElement): Boolean {
                  return true
                }

                override fun onResolved(protocolElement: PyTypedElement, subclassElements: List<RatedResolveResult>): Boolean {
                  val expectedMemberType = myTypeEvalContext.getType(protocolElement)

                  subclassElements
                    .asSequence()
                    .map { it.element }
                    .filterIsInstance<PyTypedElement>()
                    .filter { it.containingFile == node.containingFile }
                    .filterNot { PyTypeChecker.match(expectedMemberType, myTypeEvalContext.getType(it), myTypeEvalContext) }
                    .forEach {
                      val place = if (it is PsiNameIdentifierOwner) it.nameIdentifier else it
                      registerProblem(place, "Type of '${it.name}' is incompatible with '${protocol.name}'")
                    }

                  return true
                }
              }
            )
          }
      }
    }
  }
}