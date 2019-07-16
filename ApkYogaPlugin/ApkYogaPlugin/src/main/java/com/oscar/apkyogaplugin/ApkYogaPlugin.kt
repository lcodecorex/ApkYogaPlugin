package com.oscar.apkyogaplugin

import com.android.build.gradle.internal.api.BaseVariantImpl
import com.oscar.apkyogaplugin.utils.PLUGIN_EXTENSION_NAME
import com.oscar.apkyogaplugin.utils.TASK_GROUP_NAME
import com.oscar.apkyogaplugin.utils.getProjectInfo
import com.oscar.apkyogaplugin.utils.log
import com.oscar.apkyogaplugin.webp.InvasiveWebpConvertTask
import com.oscar.apkyogaplugin.webp.WebpConvertConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class ApkYogaPlugin : Plugin<Project> {

    private lateinit var mYogaProject: Project
    private lateinit var mYogaExtension: YogaExtension

    override fun apply(project: Project) {
        mYogaProject = project

        //Add the 'ApkYoga' extension object
        project.extensions.create(PLUGIN_EXTENSION_NAME, YogaExtension::class.java)

        val info = getProjectInfo(project)
        val mYogaExtension = info.extension

        // Add a task that uses configuration from the extension object
        val task = project.task(mapOf("group" to TASK_GROUP_NAME, "description" to "hello test"), "hello")
        task.doLast {
            log("Hello gradle! Yoga enabled = ${mYogaExtension.enable}")
        }

        WebpConvertConfig().apply(project, info)

        if (!mYogaExtension.enable) {
            log("disabled!")
            return
        }

        if (info.isDebugTask && !info.extension.enableOnDebug) {
            log("disabled on debug mode!")
            return
        }

//        if (!info.containsAssembleTask) {
//            return
//        }

        project.afterEvaluate {
            info.variants.all {
                it as BaseVariantImpl

                if (!mYogaExtension.enable) {
                    log("disabled!")
                    return@all
                }

                if (info.isDebugTask && !mYogaExtension.enable) {
                    log("disabled on debug mode!")
                    return@all
                }
            }
        }
    }
}
