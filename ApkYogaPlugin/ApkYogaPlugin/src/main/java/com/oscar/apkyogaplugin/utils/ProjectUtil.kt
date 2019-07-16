package com.oscar.apkyogaplugin.utils

import com.android.build.gradle.*
import com.oscar.apkyogaplugin.YogaExtension
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project

const val PLUGIN_EXTENSION_NAME = "ApkYoga"
const val TASK_GROUP_NAME = "ApkYoga"

data class ProjectInfo(
    val project: Project,
    val variants: DomainObjectSet<out Any>,
    val android: BaseExtension,
    val extension: YogaExtension,
    val isDebugTask: Boolean,
    val containsAssembleTask: Boolean
)

fun getProjectInfo(project: Project): ProjectInfo {
    val appliedOnApp = project.plugins.hasPlugin("com.android.application")
    val variants = if (appliedOnApp) {
        (project.property("android") as AppExtension).applicationVariants
    } else {
        (project.property("android") as LibraryExtension).libraryVariants
    }

    val android = project.property("android") as BaseExtension

    val mYogaExtension = project.property(PLUGIN_EXTENSION_NAME) as YogaExtension

    val taskNames = project.gradle.startParameter.taskNames
    var isDebugTask = false
    var containsAssembleTask = false
    for (index in 0 until taskNames.size) {
        val taskName = taskNames[index]
        if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains("bundle")) {
            containsAssembleTask = true
            if (taskName.toLowerCase().endsWith("debug") && taskName.toLowerCase().contains("debug")) {
                isDebugTask = true
            }
            break
        }
    }

    return ProjectInfo(project, variants, android, mYogaExtension, isDebugTask, containsAssembleTask)
}