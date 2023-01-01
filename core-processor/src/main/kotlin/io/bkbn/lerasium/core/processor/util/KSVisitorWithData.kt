package io.bkbn.lerasium.core.processor.util

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSDynamicReference
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSParenthesizedReference
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor

abstract class KSVisitorWithData<T> : KSVisitor<T, Unit> {
  override fun visitAnnotated(annotated: KSAnnotated, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitAnnotation(annotation: KSAnnotation, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitCallableReference(reference: KSCallableReference, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitClassifierReference(reference: KSClassifierReference, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitDeclaration(declaration: KSDeclaration, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitDeclarationContainer(declarationContainer: KSDeclarationContainer, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitDynamicReference(reference: KSDynamicReference, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitFile(file: KSFile, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitNode(node: KSNode, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitPropertyGetter(getter: KSPropertyGetter, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitPropertySetter(setter: KSPropertySetter, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitReferenceElement(element: KSReferenceElement, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitTypeAlias(typeAlias: KSTypeAlias, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitTypeArgument(typeArgument: KSTypeArgument, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitTypeParameter(typeParameter: KSTypeParameter, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitValueArgument(valueArgument: KSValueArgument, data: T) {
    TODO("Not yet implemented")
  }

  override fun visitValueParameter(valueParameter: KSValueParameter, data: T) {
    TODO("Not yet implemented")
  }
}
