package com.oscar.apkyogaplugin

import com.android.SdkConstants
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry
import com.android.builder.internal.aapt.v2.Aapt2DaemonManager
import com.android.sdklib.BuildToolInfo
import com.android.utils.ILogger
import com.oscar.apkyogaplugin.utils.*
import com.oscar.apkyogaplugin.webp.InvasiveWebpConvertTask
import com.oscar.apkyogaplugin.webp.WebpConvertConfig
import javassist.util.proxy.ProxyFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.lang.reflect.Modifier
import java.nio.file.Paths

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

        if (mYogaExtension.enable && (mYogaExtension.webpConvertConfig.enable || mYogaExtension.imageCompressConfig.enable)) {
            // 代理 WorkerActionServiceRegistry
            project.afterEvaluate {
                var mVariant: BaseVariantImpl? = null
                info.variants.all { variant ->
                    mVariant = variant as BaseVariantImpl
                }
                if (mVariant != null && aapt2Enabled(mVariant!!, project)) {
                    proxyServiceRegistry(mVariant!!, info)
                }
            }
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
