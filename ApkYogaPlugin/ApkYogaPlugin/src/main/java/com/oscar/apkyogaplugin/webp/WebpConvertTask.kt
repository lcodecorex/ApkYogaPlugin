package com.oscar.apkyogaplugin.webp

import com.android.build.gradle.internal.api.BaseVariantImpl
import com.oscar.apkyogaplugin.utils.ProjectInfo
import com.oscar.apkyogaplugin.utils.isWebpConvertableImage
import com.oscar.apkyogaplugin.utils.log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class WebpConvertTask : DefaultTask() {

    @Input
    lateinit var info: ProjectInfo

    @Input
    lateinit var variant: BaseVariantImpl

    var oldSize: Long = 0
    var newSize: Long = 0

    @TaskAction
    open fun webpConvert() {
        val resFolder = variant.mergeResources.outputDir.absolutePath ?: return

        val config = info.extension.webpConvertConfig
        val imageList = ArrayList<File>()

        val fileTree = File(resFolder).walk()
        fileTree.filter { file -> file.isFile && isWebpConvertableImage(file, config.convertGif) }
            .iterator()
            .forEach { file ->
                imageList.add(file)
            }

        if (imageList.isEmpty()) {
            return
        }

        log("$name >> ------------task start------------")
        for (file in imageList) {
            val size = file.length()
            oldSize += size
            val savedSize = WebpUtil.executeConvert(file, info)
            newSize += (size - savedSize)
        }

        log(
            "$name >> ------------------------------\n" +
                    "before webp convert: " + oldSize / 1024 + "KB\n" +
                    "after webp convert: " + newSize / 1024 + "KB\n" +
                    "saved size: " + (oldSize - newSize) / 1024 + "KB\n" +
                    "$name >> ------------------------------"
        )
        log("$name >> ------------task end------------")
    }
}