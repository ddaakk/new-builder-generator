# Java Builder Generator Plugin

Generate builders for your Java classes effortlessly with this plugin! It automates the creation of builder classes for your existing classes or records in Java, enhancing your productivity and code maintainability.

## Features

- **Builder Generation for Classes and Records**
  - Supports both standard Java classes and records.
  - Generates builder classes with customizable names and package destinations.

- **Inner and Separate Builder Classes**
  - Option to create builders as inner classes or as separate files.
  - Handles naming conflicts and notifies if a builder already exists.

- **Customizable Method Prefix**
  - Define your own method prefix for builder methods (e.g., `with`, `set`, `add`).

- **'But' Method Inclusion**
  - Optionally include a `'but'` method for cloning and modifying existing builders.

- **Use Existing Constructors or Setters**
  - Choose to utilize existing constructors or setter methods in your class.
  - Provides selection dialogs to pick specific constructors or setters.

- **User-Friendly Dialog Interface**
  - Intuitive dialog to configure all settings before generating the builder.
  - Remembers recent package destinations for convenience.

## Benefits

- **Saves Time and Reduces Boilerplate**
  - Automates the repetitive task of writing builder classes manually.

- **Improves Code Readability**
  - Generates clean and consistent builder code, enhancing maintainability.

- **Increases Flexibility**
  - Customize builder generation to fit your coding standards and project needs.

## Usage

1. **Select the Target Class**
   - Place your cursor inside the class or record for which you want to generate a builder.

2. **Invoke Builder Generation**
   - Right-click to open the context menu and select **"Generate > Builder"**.
   - Or use the keyboard shortcut if one is assigned.

3. **Configure Builder Settings**
   - A dialog will appear allowing you to:
     - Set the **Builder Class Name**.
     - Choose the **Method Prefix** for builder methods.
     - Select the **Destination Package**.
     - Decide whether to create an **Inner Builder** or a separate class.
     - Opt to include a **'but' method**.
     - Choose to **Use Existing Constructors** or **Setters**.

4. **Select Constructors or Setters (if applicable)**
   - If you choose to use existing constructors or setters, selection dialogs will help you pick the ones you want.

5. **Generate the Builder**
   - Click **OK** to generate the builder class.
   - The plugin will create the builder in the specified location and notify you upon completion.

**Note:** The plugin ensures that existing builders are not overwritten and alerts you if a builder with the same name already exists.

## Installation

1. **Download the Plugin**
   - Get the latest version of the plugin from the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/25961-new-builder-generator) page.

2. **Install via IDE**
   - Open your JetBrains IDE (e.g., IntelliJ IDEA).
   - Go to **Settings** > **Plugins**.
   - Click on the **Gear Icon** and select **"Install Plugin from Disk..."**.
   - Choose the downloaded plugin file and follow the prompts.

3. **Restart the IDE**
   - After installation, restart your IDE to activate the plugin.

## Contributing

Contributions are welcome! If you have suggestions for new features or improvements, feel free to submit an issue or a pull request on the [GitHub repository](#).

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

*Enhance your development workflow by automating builder class generation with the Java Builder Generator Plugin!*
