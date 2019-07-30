package org.sonatype.maven.polyglot.kotlin

import org.apache.maven.model.Model
import org.apache.maven.model.io.ModelReader
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.component.annotations.Component
import org.codehaus.plexus.component.annotations.Requirement
import org.sonatype.maven.polyglot.execute.ExecuteManager
import org.sonatype.maven.polyglot.kotlin.dsl.Project
import org.sonatype.maven.polyglot.kotlin.engine.ScriptHost
import java.io.File
import java.io.InputStream
import java.io.Reader

@Component(role = ModelReader::class, hint = "kotlin")
class KotlinModelReader : ModelReader {

    @Requirement
    private lateinit var executeManager: ExecuteManager

    @Requirement(optional = true)
    private var project: MavenProject? = null

    override fun read(input: File, options: Map<String, *>): Model {
        val model = Project(input)
        ScriptHost.eval(input, project?.basedir ?: input.parentFile, model)
        val tasks = ArrayList(model.tasks)
        executeManager.register(model, tasks)
        executeManager.install(model, options)
        model.tasks.clear() // Must be cleared or Maven goes into an infinitely repeatable introspection
        return model
    }

    override fun read(input: Reader, options: MutableMap<String, *>): Model {
        val temp = File.createTempFile("pom", ".kts")
        temp.deleteOnExit()
        temp.writer().use { input.copyTo(it) }
        return read(temp, options)
    }

    override fun read(input: InputStream, options: MutableMap<String, *>): Model {
        val temp = File.createTempFile("pom", ".kts")
        temp.deleteOnExit()
        temp.outputStream().use { input.copyTo(it) }
        return read(temp, options)
    }
}
