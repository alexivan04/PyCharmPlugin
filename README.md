# PyCharm Plugin - Variable Type Widget

This project is a **PyCharm Plugin** developed to enhance the development experience by providing additional tools within the PyCharm IDE. The plugin introduces a **Variable Type Widget** for better visualization and management of variable types during Python development.

## Features

- **Variable Type Widget**: A widget that shows the data type of variables at a glance, helping developers quickly understand the types of variables in their code without needing to check manually.
- **Compatibility**: This plugin is built specifically for **PyCharm** and is compatible with the **PyCharm 2023.3** version.

## Requirements

Before using this plugin, ensure that you have the following installed:

- **PyCharm** (2023.3 or compatible version)
- **Python Plugin**: The plugin depends on the Python plugin in PyCharm, which should be installed and enabled.

## How to Use

Once installed, the plugin will automatically show a widget on the **status bar** in PyCharm displaying the current data type of the selected variable in your code.

### Widget Behavior
**Hover**: Hover over any variable in the code, and the widget will show its current type.

## Development

This plugin is built using **Kotlin** and **Java** for the backend logic, with PyCharm’s SDK for integration into the PyCharm IDE.

### Building the Plugin

To build the plugin, run the following Gradle tasks:

```bash
./gradlew clean build
```

This will create the plugin artifact, which can then be installed in PyCharm as described in the installation section.

### Running PyCharm with the Plugin

To run PyCharm with the plugin installed for testing and development, execute:

```bash
./gradlew runIde
```

This will launch a new PyCharm instance with your plugin activated.

## Dependencies

- **Kotlin**: For writing the plugin logic in Kotlin.
- **PyCharm Plugin SDK**: The core SDK to build plugins for PyCharm.
- **Python Plugin**: Required to use the Python features within the PyCharm IDE.