# 🐘 Morphe Patches Gradle plugin

A Gradle plugin for Morphe Patches projects.

## ❓ About

Morphe Patches Gradle plugin configures a project to develop Morphe Patches.

For that, the plugin provides:

- The [settings plugin](plugin/src/main/kotlin/app/morphe/patches/gradle/SettingsPlugin.kt):
Applied to the `settings.gradle.kts` file, configures the project repositories and subprojects
- The [patches plugin](plugin/src/main/kotlin/app/morphe/patches/gradle/PatchesPlugin.kt):
Applied to the patches subproject by the settings plugin
- The [extension plugin](plugin/src/main/kotlin/app/morphe/patches/gradle/ExtensionPlugin.kt):
Applied to extension subprojects by the settings plugin

> [!CAUTION]
> This plugin is a hard fork and was not originally created by Morphe.
> This plugin functionality may dramatically change at any time.

## 🚀 How to get started

> [!TIP]
> The [Morphe Patches template](https://github.com/MorpheApp/morphe-patches-template) repository
> uses this plugin and is a good starting point to create a new Morphe Patches project.

Add the following to the `settings.gradle.kts` file:

```kotlin
pluginManagement {
    repositories {
        `mavenLocal()`
        gradlePluginPortal()
        google()
        maven {
           name = "GitHubPackages"
           url = uri("https://maven.pkg.github.com/MorpheApp/registry")
           credentials {
              username = providers.gradleProperty("gpr.user")
              password = providers.gradleProperty("gpr.key")
           }
        }
    }
}

plugins {
   id("app.morphe.patches") version "<version>"
}
```

> [!NOTE]
> The plugin is published to the GitHub Package Registry, so you need to authenticate with GitHub.  
> More information
> [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

Create the patches project and configure the `build.gradle.kts` file:

```kotlin
patches {
    about {
        name = "Morphe custom patches" // Use your own name if desired.
        description = "Custom patches for Morphe"
        // ...   
    }
}
```

> [!NOTE]
> By default, the plugin expects the patches project to be in the `patches` directory.

Create the extension project and configure the `build.gradle.kts` file:

```kotlin
extension {
   name = "extensions/extension.mpe"
}

android {
   namespace = "app.morphe.extension"
}
```

> [!NOTE]
> By default, the plugin expects extension projects to be under the `extensions` directory.

## 📚 Everything else

### 🛠️ Building

To build Morphe Patches Gradle plugin, follow these steps:

1. Clone the repository and navigate to the project directory.
2. Authenticate with GitHub. More information
   [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).
3. Run `./gradlew build` to build the plugin.
4. Optionally, run `./gradlew publishToMavenLocal` to publish the plugin to your local Maven repository for development.

## 📜 Licence

Morphe Patches are licensed under the [GNU GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.html), with additional conditions under Section 7:

- **Name Restriction (7c):** The name **"Morphe"** may not be used for derivative works.  
  Derivatives must adopt a distinct identity unrelated to "Morphe."

See the [LICENSE](./LICENSE) file for full terms.
