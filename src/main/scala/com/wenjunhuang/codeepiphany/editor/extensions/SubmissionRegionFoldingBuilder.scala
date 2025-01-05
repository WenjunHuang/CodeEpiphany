package com.wenjunhuang.codeepiphany.editor.extensions

import com.intellij.lang.folding.{FoldingBuilderEx, FoldingDescriptor}
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.{Document, FoldingGroup}
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiComment, PsiElement}
import com.intellij.psi.util.PsiTreeUtil
import com.wenjunhuang.codeepiphany.model.Constants

import scala.jdk.CollectionConverters.*

class SubmissionRegionFoldingBuilder extends FoldingBuilderEx with DumbAware {
  override def buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array[FoldingDescriptor] = {
    val group = FoldingGroup.newGroup("CodeEpiphany Submission Region")
    PsiTreeUtil
      .findChildrenOfType(root, classOf[PsiComment])
      .asScala
      .collect {
        case comment
            if comment.getText.contains(Constants.SUBMIT_CODE_REGION_BEGIN) ||
              comment.getText.contains(Constants.SUBMIT_CODE_REGION_END) =>
          new FoldingDescriptor(comment.getNode, comment.getTextRange, group)
      }
      .toArray
  }

  override def isCollapsedByDefault(node: ASTNode): Boolean = true

  override def getPlaceholderText(node: ASTNode): String = {
    node.getPsi match {
      case comment: PsiComment =>
        if (comment.getText.contains(Constants.SUBMIT_CODE_REGION_BEGIN)) {
          "🚀 Begin 🚀"
        } else {
          "🔚 End 🔚"
        }
      case _ => ""
    }
  }
}
