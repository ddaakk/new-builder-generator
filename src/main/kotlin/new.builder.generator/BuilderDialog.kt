package new.builder.generator

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class BuilderDialog(
    private val defaultClassName: String,
    private val defaultPackage: String,
    private val project: Project,
    private val targetClass: PsiClass
) : DialogWrapper(true) {

    private val EMPTY_PACKAGE_DISPLAY = "<default>"
    private val classNameField = JBTextField(defaultClassName + "Builder")
    private val methodPrefixField = JBTextField("with")
    private val packageComboBox = ComboBox<String>()
    private val packageChooserButton = JButton("...")
    private val innerBuilderCheckBox = JCheckBox("Inner builder", false)
    private val butMethodCheckBox = JCheckBox("'but' method", false)
    private val useSetterCheckBox = JCheckBox("Use setter", false)
    private val selectSettersButton = JButton("Select Setters")
    private val useExistConstructorCheckBox = JCheckBox("Use exist constructor", false)
    private val selectConstructorButton = JButton("Select Constructor")

    private var selectedSetters: List<String> = emptyList()
    private var selectedConstructor: String? = null

    private val propertiesComponent = PropertiesComponent.getInstance()
    private val RECENT_PACKAGES_KEY = "builder.recent.packages"
    private val MAX_RECENT_PACKAGES = 5

    init {
        title = "Generate Builder"
        init()
        setSize(450, 350)

        packageChooserButton.addActionListener { choosePackage() }

        packageComboBox.isEditable = true
        packageComboBox.selectedItem = if (defaultPackage.isEmpty()) EMPTY_PACKAGE_DISPLAY else defaultPackage
        loadRecentPackages()

        innerBuilderCheckBox.addItemListener {
            val enabled = !innerBuilderCheckBox.isSelected
            packageComboBox.isEnabled = enabled
            packageChooserButton.isEnabled = enabled
        }

        useSetterCheckBox.addItemListener {
            if (useSetterCheckBox.isSelected) {
                useExistConstructorCheckBox.isEnabled = false
                selectSettersButton.isEnabled = true
            } else {
                useExistConstructorCheckBox.isEnabled = true
                selectSettersButton.isEnabled = false
            }
        }

        useExistConstructorCheckBox.addItemListener {
            if (useExistConstructorCheckBox.isSelected) {
                useSetterCheckBox.isEnabled = false
                selectConstructorButton.isEnabled = true
            } else {
                useSetterCheckBox.isEnabled = true
                selectConstructorButton.isEnabled = false
            }
        }

        selectSettersButton.isEnabled = false
        selectSettersButton.addActionListener {
            showSetterSelectionDialog()
        }

        selectConstructorButton.isEnabled = false
        selectConstructorButton.addActionListener {
            showConstructorSelectionDialog()
        }
    }

    private fun choosePackage() {
        val chooser = PackageChooserDialog("Choose Destination Package", project)
        if (chooser.showAndGet()) {
            val psiPackage = chooser.selectedPackage
            val packageName = psiPackage?.qualifiedName ?: ""
            packageComboBox.selectedItem = if (packageName.isEmpty()) EMPTY_PACKAGE_DISPLAY else packageName
        }
    }

    private fun showSetterSelectionDialog(): Boolean {
        val setterMethods = targetClass.methods.filter { method ->
            method.name.startsWith("set") && method.parameterList.parametersCount == 1
        }

        if (setterMethods.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No setter methods found in the class.",
                "No Setters"
            )
            return false
        }

        val methodNames = setterMethods.map { it.name }.toTypedArray()

        val list = JList(methodNames)
        list.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        val result = JOptionPane.showConfirmDialog(
            null,
            JScrollPane(list),
            "Select setters to use:",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        return if (result == JOptionPane.OK_OPTION && list.selectedIndices.isNotEmpty()) {
            selectedSetters = list.selectedValuesList
            true
        } else {
            selectedSetters = emptyList()
            false
        }
    }

    private fun showConstructorSelectionDialog(): Boolean {
        val constructors = targetClass.constructors

        if (constructors.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No constructors found in the class.",
                "No Constructors"
            )
            return false
        }

        val constructorSignatures = constructors.map { constructor ->
            val params = constructor.parameterList.parameters.joinToString(", ") { it.type.presentableText }
            "${constructor.name}($params)"
        }.toTypedArray()

        val list = JList(constructorSignatures)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val result = JOptionPane.showConfirmDialog(
            null,
            JScrollPane(list),
            "Select constructor to use:",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        return if (result == JOptionPane.OK_OPTION && list.selectedIndex >= 0) {
            selectedConstructor = constructorSignatures[list.selectedIndex]
            true
        } else {
            selectedConstructor = null
            false
        }
    }

    private fun loadRecentPackages() {
        val recentPackages = propertiesComponent.getValue(RECENT_PACKAGES_KEY, "")
            .split(',')
            .filter { it.isNotEmpty() }

        packageComboBox.removeAllItems()
        val initialPackage = if (defaultPackage.isEmpty()) EMPTY_PACKAGE_DISPLAY else defaultPackage
        packageComboBox.addItem(initialPackage)
        recentPackages
            .filter { it != defaultPackage }
            .take(MAX_RECENT_PACKAGES - 1)
            .forEach { packageComboBox.addItem(it) }
    }

    private fun saveRecentPackage(packageName: String) {
        if (packageName == EMPTY_PACKAGE_DISPLAY || packageName.isEmpty()) return

        val recentPackages = propertiesComponent.getValue(RECENT_PACKAGES_KEY, "")
            .split(',')
            .filter { it.isNotEmpty() }
            .toMutableList()

        recentPackages.remove(packageName)
        recentPackages.add(0, packageName)

        propertiesComponent.setValue(
            RECENT_PACKAGES_KEY,
            recentPackages.take(MAX_RECENT_PACKAGES).joinToString(",")
        )
    }

    override fun doOKAction() {
        if (useSetterCheckBox.isSelected) {
            if (selectedSetters.isEmpty()) {
                val selectionMade = showSetterSelectionDialog()
                if (!selectionMade) {
                    Messages.showErrorDialog(
                        project,
                        "No setters selected.",
                        "Cannot Proceed"
                    )
                    return
                }
            }
        }

        if (useExistConstructorCheckBox.isSelected) {
            if (selectedConstructor == null) {
                val selectionMade = showConstructorSelectionDialog()
                if (!selectionMade) {
                    Messages.showErrorDialog(
                        project,
                        "No constructor selected.",
                        "Cannot Proceed"
                    )
                    return
                }
            }
        }

        if (!innerBuilderCheckBox.isSelected) {
            saveRecentPackage(packageComboBox.selectedItem as? String ?: "")
        }
        super.doOKAction()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(5)
        }

        // Class name
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JLabel("Class name:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0
        panel.add(classNameField, gbc)

        // Method prefix
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JLabel("Method prefix:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0
        panel.add(methodPrefixField, gbc)

        // Package
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(JLabel("Destination package:"), gbc)
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0
        panel.add(packageComboBox, gbc)
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0.0
        panel.add(packageChooserButton, gbc)

        // Checkboxes and Buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.insets = JBUI.insets(10, 5, 5, 5)
        panel.add(innerBuilderCheckBox, gbc)
        gbc.gridy = 4; gbc.insets = JBUI.insets(5)
        panel.add(butMethodCheckBox, gbc)

        gbc.gridy = 5; gbc.gridwidth = 1
        panel.add(useSetterCheckBox, gbc)
        gbc.gridx = 1
        panel.add(selectSettersButton, gbc)

        gbc.gridx = 0; gbc.gridy = 6
        panel.add(useExistConstructorCheckBox, gbc)
        gbc.gridx = 1
        panel.add(selectConstructorButton, gbc)

        return panel
    }

    val settings: BuilderSettings
        get() = BuilderSettings(
            className = classNameField.text,
            methodPrefix = methodPrefixField.text,
            packageName = when (val pkg = packageComboBox.selectedItem as? String ?: "") {
                EMPTY_PACKAGE_DISPLAY -> ""
                else -> pkg
            },
            isInnerBuilder = innerBuilderCheckBox.isSelected,
            hasButMethod = butMethodCheckBox.isSelected,
            useSetter = useSetterCheckBox.isSelected,
            useExistConstructor = useExistConstructorCheckBox.isSelected,
            selectedSetters = selectedSetters,
            selectedConstructor = selectedConstructor
        )
}

data class BuilderSettings(
    val className: String,
    val methodPrefix: String,
    val packageName: String,
    val isInnerBuilder: Boolean,
    val hasButMethod: Boolean,
    val useSetter: Boolean,
    val useExistConstructor: Boolean,
    val selectedSetters: List<String> = emptyList(),
    val selectedConstructor: String? = null
)