package com.oscar.apkyogaplugin

import com.android.build.gradle.internal.api.BaseVariantImpl
import com.oscar.apkyogaplugin.utils.*
import com.oscar.apkyogaplugin.webp.WebpConvertConfig
import org.gradle.api.GradleException
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

        // Do proxy WorkerActionServiceRegistry to hook Aapt2-compile-process's CompileResourceRequest
        if (mYogaExtension.enable && (mYogaExtension.webpConvertConfig.enable || mYogaExtension.imageCompressConfig.enable)) {
            project.afterEvaluate {
                if (info.android.defaultConfig.minSdkVersion.apiLevel < 14) {
                    throw GradleException(
                        "webpConvertConfig >> You Android minSdkVersion is under 14, which is not support webp!" +
                                "You can set webpConvertConfig.enable=false or update your minSdkVersion to fix this Exception."
                    )
                }

                if (info.extension.webpConvertConfig.supportAlpha && info.android.defaultConfig.minSdkVersion.apiLevel < 18) {
                    throw GradleException(
                        "webpConvertConfig >> You Android minSdkVersion is under 18, which is not support transparent webp!" +
                                "You can set webpConvertConfig.supportAlpha=false or update your minSdkVersion to fix this Exception."
                    )
                }
            }
            // 代理 WorkerActionServiceRegistry
            project.afterEvaluate {
                var mVariant: BaseVariantImpl? = null
                info.variants.all { variant ->
                    mVariant = variant as BaseVariantImpl
                }
                if (mVariant != null && aapt2Enabled(mVariant!!, project)) {
                    proxyServiceRegistry(mVariant!!, info)
                } else {
                    WebpConvertConfig.applyBuildingTask(info)
                }
            }
        }

        // make webpConvert${variantName}Resources tasks visible
        WebpConvertConfig().apply(project)

        if (!mYogaExtension.enable) {
            log("disabled!")
            return
        }

        if (info.isDebugTask && !info.extension.enableOnDebug) {
            log("disabled on debug mode!")
            return
        }
    }
}
