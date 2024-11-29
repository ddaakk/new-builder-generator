package new.builder.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class BuilderGenerationHandler : CodeInsightActionHandler {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val targetClass = findTargetClass(editor, file) ?: return

        val dialog = BuilderDialog(
            defaultClassName = targetClass.name ?: "",
            defaultPackage = (file as? PsiJavaFile)?.packageName ?: "",
            project = project,
            targetClass = targetClass
        )

        if (!dialog.showAndGet()) {
            return
        }

        val settings = dialog.settings

        if (settings.isInnerBuilder) {
            val existingInnerBuilder = targetClass.innerClasses.find { it.name == settings.className }
            if (existingInnerBuilder != null) {
                Messages.showErrorDialog(
                    project,
                    "An inner builder class named '${existingInnerBuilder.name}' already exists in ${targetClass.name}.",
                    "Cannot Create Builder"
                )
                return
            }
        } else {
            val directory = targetClass.containingFile.containingDirectory
            val fileName = "${settings.className}.java"
            if (directory.findFile(fileName) != null) {
                Messages.showErrorDialog(
                    project,
                    "File '$fileName' already exists in ${directory.virtualFile.path}",
                    "Cannot Create Builder"
                )
                return
            }
        }

        ApplicationManager.getApplication().runWriteAction {
            generateBuilder(targetClass, settings)
        }
    }

    private fun findTargetClass(editor: Editor, file: PsiFile): PsiClass? {
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    }

    override fun startInWriteAction(): Boolean = false

    private fun generateBuilder(targetClass: PsiClass, settings: BuilderSettings) {
        val builderContent = if (targetClass.isRecord) {
            generateRecordBuilderClassText(targetClass, settings)
        } else {
            generateClassBuilderClassText(targetClass, settings)
        }

        val factory = JavaPsiFacade.getInstance(targetClass.project).elementFactory
        val builderClass = factory.createClassFromText(builderContent, targetClass).innerClasses.firstOrNull()
            ?: return

        if (settings.isInnerBuilder) {
            targetClass.add(builderClass)
        } else {
            val directory = targetClass.containingFile.containingDirectory
            val fileName = "${settings.className}.java"
            val content = buildString {
                if (settings.packageName.isNotEmpty()) {
                    append("package ${settings.packageName};\n\n")
                    append("import ${targetClass.qualifiedName};\n\n")
                }
                append(builderClass.text)
            }

            val builderFile = PsiFileFactory.getInstance(targetClass.project)
                .createFileFromText(
                    fileName,
                    targetClass.containingFile.fileType,
                    content
                ) as PsiJavaFile
            directory.add(builderFile)
        }
    }

    private fun generateClassBuilderClassText(clazz: PsiClass, settings: BuilderSettings): String {
        val className = clazz.name ?: return ""
        val instanceName = className.decapitalize()
        val fields = clazz.fields.filter { !it.hasModifierProperty(PsiModifier.STATIC) }
        val builderClass = if (settings.isInnerBuilder) "static class" else "class"

        return buildString {
            append("public $builderClass ${settings.className} {\n")

            // Fields
            fields.forEach { field ->
                append("    private ${field.type.presentableText} ${field.name};\n")
            }
            append("\n")

            // Constructor
            append("    private ${settings.className}() {}\n\n")

            // Static builder method
            append("    public static ${settings.className} builder() {\n")
            append("        return new ${settings.className}();\n")
            append("    }\n\n")

            // Builder methods
            fields.forEach { field ->
                append("    public ${settings.className} ${settings.methodPrefix}${field.name.capitalize()}(${field.type.presentableText} ${field.name}) {\n")
                append("        this.${field.name} = ${field.name};\n")
                append("        return this;\n")
                append("    }\n")
            }

            // But method
            if (settings.hasButMethod) {
                append("\n    public ${settings.className} but() {\n")
                append("        return builder()\n")
                fields.forEach { field ->
                    append("            .${settings.methodPrefix}${field.name.capitalize()}(${field.name})\n")
                }
                append("        ;\n")
                append("    }\n")
            }

            // Build method
            append("\n    public $className build() {\n")
            if (settings.useExistConstructor && settings.selectedConstructor != null) {
                val constructor = clazz.constructors.find { ctor ->
                    val params = ctor.parameterList.parameters.joinToString(", ") { it.type.presentableText }
                    "${ctor.name}($params)" == settings.selectedConstructor
                }
                if (constructor != null) {
                    append("        return new $className(\n")
                    val paramNames = constructor.parameterList.parameters.map { it.name }
                    paramNames.forEachIndexed { index, paramName ->
                        append("            ${paramName}")
                        if (index < paramNames.size -1) append(",\n")
                    }
                    append("\n        );\n")
                } else {
                    append("        // Selected constructor not found.\n")
                }
            } else if (settings.useSetter && settings.selectedSetters.isNotEmpty()) {
                append("        $className $instanceName = new $className();\n")
                settings.selectedSetters.forEach { setterName ->
                    val fieldName = setterName.removePrefix("set").decapitalize()
                    append("        $instanceName.${setterName}(this.${fieldName});\n")
                }
                append("        return $instanceName;\n")
            } else {
                append("        $className $instanceName = new $className();\n")
                fields.forEach { field ->
                    append("        $instanceName.${field.name} = this.${field.name};\n")
                }
                append("        return $instanceName;\n")
            }
            append("    }\n")
            append("}")
        }
    }

    private fun generateRecordBuilderClassText(clazz: PsiClass, settings: BuilderSettings): String {
        val recordName = clazz.name ?: return ""
        val components = clazz.recordComponents ?: return ""
        val builderClass = if (settings.isInnerBuilder) "static class" else "class"

        return buildString {
            append("public $builderClass ${settings.className} {\n")

            // Fields
            components.forEach { component ->
                append("    private ${component.type.presentableText} ${component.name};\n")
            }
            append("\n")

            // Constructor
            append("    private ${settings.className}() {}\n\n")

            // Static builder method
            append("    public static ${settings.className} builder() {\n")
            append("        return new ${settings.className}();\n")
            append("    }\n\n")

            // Builder methods
            components.forEach { component ->
                append("    public ${settings.className} ${settings.methodPrefix}${component.name.capitalize()}(${component.type.presentableText} ${component.name}) {\n")
                append("        this.${component.name} = ${component.name};\n")
                append("        return this;\n")
                append("    }\n")
            }

            // But method
            if (settings.hasButMethod) {
                append("\n    public ${settings.className} but() {\n")
                append("        return builder()\n")
                components.forEach { component ->
                    append("            .${settings.methodPrefix}${component.name.capitalize()}(${component.name})\n")
                }
                append("        ;\n")
                append("    }\n")
            }

            // Build method
            append("\n    public $recordName build() {\n")
            append("        return new $recordName(\n")
            components.forEachIndexed { index, component ->
                append("            ${component.name}")
                if (index < components.size -1) append(",\n")
            }
            append("\n        );\n")
            append("    }\n")
            append("}")
        }
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    private fun String.decapitalize() = replaceFirstChar { it.lowercase() }
}
