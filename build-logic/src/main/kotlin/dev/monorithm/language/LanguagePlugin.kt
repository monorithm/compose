package dev.monorithm.language

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.language.base.plugins.LifecycleBasePlugin

/** The settings of the plugin dev.monorithm.language. */
public interface LanguageExtension {
    /** The folder of the language source: the submodule design/. */
    public val source: DirectoryProperty

    /** The generated Kotlin file of the tokens. */
    public val tokens: RegularFileProperty

    /** The Kotlin package of the generated file. */
    public val packageName: Property<String>
}

/** Writes the Kotlin file of the tokens from the IR. */
public abstract class EmitTokens : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val source: DirectoryProperty

    @get:Input
    public abstract val packageName: Property<String>

    @get:OutputFile
    public abstract val tokens: RegularFileProperty

    @TaskAction
    public fun emit() {
        val file = tokens.get().asFile
        file.parentFile.mkdirs()
        file.writeText(Emitter.emit(source.get().asFile, packageName.get()))
        logger.lifecycle("emitTokens: wrote ${file.path}")
    }
}

/**
 * Stops with an error if a file of the IR does not match its checksum in the manifest, or if the committed file of
 * the tokens differs from the output of the emitter (the drift check of CI).
 */
@UntrackedTask(because = "The check has no output. It runs each time.")
public abstract class CheckLanguage : DefaultTask() {
    @get:Internal
    public abstract val source: DirectoryProperty

    @get:Input
    public abstract val packageName: Property<String>

    @get:InputFile
    public abstract val tokens: RegularFileProperty

    @TaskAction
    public fun check() {
        val dir = source.get().asFile
        Emitter.verify(dir)
        val file = tokens.get().asFile
        if (!file.exists() || file.readText() != Emitter.emit(dir, packageName.get())) {
            throw GradleException("checkLanguage: ${file.name} differs from the output of the emitter. Run: ./gradlew emitTokens")
        }
        logger.lifecycle(
            "checkLanguage: the IR of language ${Emitter.version(dir)} matches its manifest, and ${file.name} matches the IR.",
        )
    }
}

/** Adds the tasks emitTokens and checkLanguage. The task check runs checkLanguage. */
public class LanguagePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val language = project.extensions.create("language", LanguageExtension::class.java)
        project.tasks.register("emitTokens", EmitTokens::class.java) {
            group = "language"
            description = "Writes the Kotlin file of the tokens from the IR of the language."
            source.set(language.source)
            tokens.set(language.tokens)
            packageName.set(language.packageName)
        }
        val checkLanguage = project.tasks.register("checkLanguage", CheckLanguage::class.java) {
            group = "language"
            description = "Examines the checksums of the IR, and compares the file of the tokens with the emitter."
            source.set(language.source)
            tokens.set(language.tokens)
            packageName.set(language.packageName)
        }
        project.plugins.withType(LifecycleBasePlugin::class.java) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(checkLanguage) }
        }
    }
}
