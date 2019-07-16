package com.oscar.apkyogaplugin.webp

import com.oscar.apkyogaplugin.utils.isWebpConvertableImage
import com.oscar.apkyogaplugin.utils.log
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import com.oscar.apkyogaplugin.utils.containsIn
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
            executeConvert(file)
        }

//        val resDir = File(resFolder)
//        val regex = Regex("/(drawable|mipmap)[a-z0-9-]*/")
//        val fileTree = resDir.walk()
//        fileTree.filter { file -> file.isFile && regex.containsMatchIn(file.absolutePath) && isWebpConvertableImage(file) }
//            .forEach { file ->
//                executeConvert(file)
//            }

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
