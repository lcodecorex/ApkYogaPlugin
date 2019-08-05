package com.oscar.apkyogaplugin.webp

import com.oscar.apkyogaplugin.utils.isWebpConvertableImage
import com.oscar.apkyogaplugin.utils.log
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InvasiveWebpConvertTask : WebpConvertTask() {

    @TaskAction
    override fun webpConvert() {
        val config = info.extension.webpConvertConfig

        val imageList = ArrayList<File>()
        variant.sourceSets.forEach { provider ->
            provider.resDirectories.forEach {
                val fileTree = it.walk()
                fileTree.filter { file -> file.isFile && isWebpConvertableImage(file, config.convertGif) }
                    .iterator()
                    .forEach { file ->
                        imageList.add(file)
                    }
            }
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
