import com.intellij.codeInsight.generation.actions.BaseGenerateAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import new.builder.generator.BuilderGenerationHandler

class BuilderGenerationAction : BaseGenerateAction(BuilderGenerationHandler()) {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file !is PsiJavaFile || file is PsiCompiledElement) return false
        val targetClass = getTargetClass(editor, file)
        return targetClass != null && isValidForClass(targetClass)
    }

    override fun isValidForClass(targetClass: PsiClass): Boolean {
        return !targetClass.isInterface
    }
}