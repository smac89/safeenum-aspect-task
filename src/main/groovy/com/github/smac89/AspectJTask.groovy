package com.github.smac89

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

class AspectJTask extends DefaultTask {
    SourceSet sourceSet
    Map<String, String> aspectjOpts

    def aspectj = project.extensions.create('aspectj', AspectJExtension, project)

    AspectJTask() {
        if (!project.configurations.findByName('ajc')) {
            project.configurations.create('ajc').extendsFrom(project.configurations.getByName('compileOnly'))
        }

        if (!project.configurations.findByName('aspects')) {
            project.configurations.create('aspects')
        }

        project.afterEvaluate { p ->
            if (!p.plugins.hasPlugin(JavaPlugin)) {
                p.plugins.apply JavaPlugin
            }

            p.dependencies {
                ajc "org.aspectj:aspectjtools:${aspectj.version}"
                compile "org.aspectj:aspectjrt:${aspectj.version}"
            }

            if (sourceSet.name == "main") {
                p.tasks.getByName('classes', {
                    it.dependsOn this
                    it.dependsOn.remove 'compileJava'
                    this.dependsOn 'compileJava'
                })
            } else if (sourceSet.name == "test") {
                p.tasks.getByName('testClasses', {
                    it.dependsOn this
                    it.dependsOn.remove 'compileTestJava'
                    this.dependsOn 'compileTestJava'
                })
            }
        }
    }

    //https://github.com/sedovalx/gradle-aspectj-binary/blob/master/plugin/src/main/kotlin/com/github/sedovalx/gradle/aspectj/AjcTask.kt
    @TaskAction
    def compileAspect() {
        def iAspectjOpts = [
                maxmem       : '1024m',
                fork         : 'true',
                Xlint        : 'ignore',
                proc         : 'none',
                sourceRoots  : sourceSet.java.sourceDirectories.asPath,
                destDir      : sourceSet.output.classesDir.absolutePath,
                aspectPath   : project.configurations.aspects.asPath,
                inpath       : sourceSet.output.classesDir.absolutePath,
                classpath    : (sourceSet.compileClasspath + sourceSet.runtimeClasspath).filter { it.exists() }.asPath,
                source       : project.sourceCompatibility,
                target       : project.targetCompatibility,
                showWeaveInfo: 'true',
        ]

        if (aspectjOpts) {
            iAspectjOpts += aspectjOpts
        }

        ant.taskdef(resource: "org/aspectj/tools/ant/taskdefs/aspectjTaskdefs.properties",
                classpath: project.configurations.ajc.asPath)
        ant.iajc(iAspectjOpts)
    }
}
