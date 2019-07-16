package com.oscar.apkyogaplugin.webp

import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.oscar.apkyogaplugin.YogaExtension.Companion.COMPRESS_TYPE_LOSSY
import com.oscar.apkyogaplugin.utils.ProjectInfo
import com.oscar.apkyogaplugin.utils.TASK_GROUP_NAME
import com.oscar.apkyogaplugin.utils.getProjectInfo
import com.oscar.apkyogaplugin.utils.log
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class WebpConvertConfig : Plugin<Project> {

    var enable = true  // 是否允许图片转webp功能
    var type = COMPRESS_TYPE_LOSSY // webp压缩类型
    var convertGif = false // 是否允许把gif转为webp
    var supportAlpha = true // 是否支持透明
    var quality = 75 // webp画质
    var whiteList = arrayOf<String>()

    override fun apply(project: Project) {
        this.apply(project, null)
    }

    /**
     * invasive: if true, newly generated webp images will overlay image source files;
     * otherwise images will be converted after resources merged.
     *
     * translation:
     * 执行gradle build时，webp转换是无侵入式的，webp转换会在mergeResource时进行；执行gradlew webpConvert，
     * webp转换是侵入式的，将会覆盖源文件
     *
     * gradlew build:        invasive = false
     * gradlew webpConvert:  invasive = true
     */
    fun apply(project: Project, projectInfo: ProjectInfo?) {
        val info = projectInfo ?: getProjectInfo(project)

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

            applyInvasiveTask(info)
            if (info.extension.webpConvertConfig.enable) {
                applyBuildingTask(info)
            }
        }
    }

    private fun applyBuildingTask(info: ProjectInfo) {
        info.variants.all { variant ->
            variant as BaseVariantImpl

            val variantName = variant.name.capitalize()
            val processResTask =
                info.project.tasks.getByName("process${variantName}Resources") ?: return@all
//            val mergeResTask = info.project.tasks.findByName("merge${variantName}Resources") ?: return@all

            val task = info.project.tasks.create("webpConvert${variantName}MergeResources", WebpConvertTask::class.java)
            task.group = TASK_GROUP_NAME
            task.description = "webp convert merge resource task"
            task.info = info
            task.variant = variant

            var aapt2Enabled = false
            try {
                //判断aapt2是否开启，低版本不存在这个方法，因此需要捕获异常
                processResTask as LinkApplicationAndroidResourcesTask
                aapt2Enabled = processResTask.isAapt2Enabled
            } catch (e: Exception) {
                info.project.logger.error(e.message)
            }

            // insert webpConvertResources before processResources
            if (aapt2Enabled) {
                // TODO
            } else {
                val dependencies = processResTask.taskDependencies.getDependencies(processResTask)
                task.dependsOn(dependencies)
                processResTask.dependsOn(task)
            }
        }
    }

    private fun applyInvasiveTask(info: ProjectInfo) {
        info.variants.all { variant ->
            variant as BaseVariantImpl

            val variantName = variant.name.capitalize()
//            val processResTask = info.project.tasks.findByName("process${variantName}Resources") ?: return@all
//            val resFolder =
//                if (variant.mergeResources != null) variant.mergeResources.outputDir.absolutePath else null

            val task =
                info.project.tasks.create("webpConvert${variantName}Resources", InvasiveWebpConvertTask::class.java)
            task.group = TASK_GROUP_NAME
            task.description = "webp convert task"
            task.info = info
            task.variant = variant

            // insert webpConvertResources before processResources
//            val dependencies = processResTask.taskDependencies.getDependencies(processResTask)
//            task.dependsOn(dependencies)
//            processResTask.dependsOn(task)
        }
    }

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun type(type: String) {
        this.type = type
    }

    fun convertGif(enable: Boolean) {
        this.convertGif = enable
    }

    fun supportAlpha(supportAlpha: Boolean) {
        this.supportAlpha = supportAlpha
    }

    fun quality(quality: Int) {
        this.quality = when {
            quality in 75..100 -> quality
            quality < 75 -> {
                log("webpConvertConfig >> webp quality can be set in [75,100], now changed to 75")
                75
            }
            else -> {
                log("webpConvertConfig >> webp quality can be set in [75,100], now changed to 100")
                100
            }
        }
    }

    fun whiteList(whiteList: Array<String>) {
        this.whiteList = whiteList
    }
}