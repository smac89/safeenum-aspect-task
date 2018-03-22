package com.github.smac89

import org.gradle.api.Project

class AspectJExtension {
    final String version

    AspectJExtension(Project project) {
        version = project.findProperty('aspectjVersion') ?: '1.8.13'
    }
}
