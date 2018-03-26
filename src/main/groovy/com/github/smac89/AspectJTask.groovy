package com.github.smac89

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

class AspectJTask extends DefaultTask {
    SourceSet sourceSet = project.sourceSets.main

    Map<String, String> aspectjOpts = [:]

    String aspectjVersion = '1.8.13'

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
                ajc "org.aspectj:aspectjtools:${aspectjVersion}"
            }

            if (sourceSet.name == "main" || sourceSet.name.isEmpty()) {
                p.tasks.getByName('classes', {
                    it.dependsOn this
                    if (it.dependsOn.remove('compileJava')) {
                        this.dependsOn 'compileJava'
                    }
                })
            } else {
                p.tasks.getByName(String.format("%sClasses", sourceSet.name), {
                    it.dependsOn this
                    if (it.dependsOn.remove(String.format("compile%sJava", sourceSet.name.capitalize()))) {
                        this.dependsOn String.format("compile%sJava", sourceSet.name.capitalize())
                    }
                })
            }

            sourceSet.java.sourceDirectories.each { inputs.dir it }
        }
    }

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
                inpath       : sourceSet.compileClasspath.asPath,
                classpath    : (sourceSet.compileClasspath + sourceSet.runtimeClasspath).filter { it.exists() }.asPath,
                source       : project.sourceCompatibility,
                target       : project.targetCompatibility,
                showWeaveInfo: 'true',
        ] + aspectjOpts

        ant.taskdef(resource: "org/aspectj/tools/ant/taskdefs/aspectjTaskdefs.properties",
                classpath: project.configurations.ajc.asPath)
        ant.iajc(iAspectjOpts)
    }
}
