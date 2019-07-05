package com.oscar.apkyogaplugin

import com.oscar.apkyogaplugin.utils.log
import org.gradle.api.Plugin
import org.gradle.api.Project

class ApkYogaPlugin : Plugin<Project> {

    private lateinit var mYogaProject: Project
    private lateinit var mYogaExtension: YogaExtension

    override fun apply(project: Project) {
        //Add the 'ApkYoga' extension object
        project.extensions.create("ApkYoga", YogaExtension::class.java)
        mYogaExtension = project.property("ApkYoga") as YogaExtension

        // Add a task that uses configuration from the extension object
        val task = project.task("hello")
        task.doLast {
            log("Hello gradle! Yoga enabled = ${mYogaExtension.enable}")
        }
    }
}
