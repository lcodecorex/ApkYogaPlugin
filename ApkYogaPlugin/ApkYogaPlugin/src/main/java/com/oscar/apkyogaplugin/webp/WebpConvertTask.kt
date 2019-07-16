package com.oscar.apkyogaplugin.webp

import com.android.SdkConstants
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.oscar.apkyogaplugin.YogaExtension.Companion.COMPRESS_TYPE_LOSSY
import com.oscar.apkyogaplugin.utils.*
import org.apache.commons.imaging.Imaging
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

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

//        val resList =
//            if (variant.mergeResources != null) variant.mergeResources.computeResourceSetList0() else null
//        if (resList == null || resList.isEmpty()) return

        val config = info.extension.webpConvertConfig
        val imageList = ArrayList<File>()

        val fileTree = File(resFolder).walk()
        fileTree.filter { file -> file.isFile && isWebpConvertableImage(file, config.convertGif) }
            .iterator()
            .forEach { file ->
                imageList.add(file)
            }

//        resList.forEach {
//            if (it.isFile) {
//                if (isWebpConvertableImage(it, config.convertGif)) {
//                    imageList.add(it)
//                }
//            } else {
//                val fileTree = it.walk()
//                fileTree.filter { file -> file.isFile && isWebpConvertableImage(file, config.convertGif) }
//                    .iterator()
//                    .forEach { file ->
//                        imageList.add(file)
//                    }
//            }
//        }

        if (imageList.isEmpty()) {
            return
        }

        log("$name >> ------------task start------------")
        for (file in imageList) {
            executeConvert(file)
        }

        /*val coreNum = Runtime.getRuntime().availableProcessors()
        if (imageList.size < coreNum) {
            for (file in imageList) {
                executeConvert(file)
            }
        } else {
            val results = ArrayList<Future<Unit>>()
            val pool = Executors.newFixedThreadPool(coreNum)
            val part = imageList.size / coreNum
            for (i in 0 until coreNum) {
                val from = i * part
                val to = if (i == coreNum - 1) imageList.size - 1 else (i + 1) * part - 1
                results.add(pool.submit(Callable<Unit> {
                    for (index in from..to) {
                        executeConvert(imageList[index])
                    }
                }))
            }
            for (f in results) {
                try {
                    f.get()
                } catch (ignore: Exception) {
                }
            }
        }*/

        log(
            "$name >> ------------------------------\n" +
                    "before webp convert: " + oldSize / 1024 + "KB\n" +
                    "after webp convert: " + newSize / 1024 + "KB\n" +
                    "saved size: " + (oldSize - newSize) / 1024 + "KB\n" +
                    "$name >> ------------------------------"
        )
        log("$name >> ------------task end------------")
    }

    fun executeConvert(file: File) {
        val config = info.extension.webpConvertConfig

        val whiteList = info.extension.webpConvertConfig.whiteList

        if (whiteList.containsIn(file.absolutePath)) {
            log("$name >> skip white list file ${file.absolutePath}")
            return
        }

        if (file.name.endsWith(SdkConstants.DOT_PNG) && !info.extension.webpConvertConfig.supportAlpha
            && Imaging.getImageInfo(file).isTransparent
        ) {
            log("$name >> skip alpha png ${file.absolutePath}")
            return
        }

        oldSize += file.length()

        val fileName = file.name.substring(0, file.name.lastIndexOf(".")) + SdkConstants.DOT_WEBP
        val webpFile = File(file.parentFile.absolutePath + File.separator + fileName)

        if (file.name.endsWith(SdkConstants.DOT_GIF)) {
            WebpUtil.convertGif(
                project,
                COMPRESS_TYPE_LOSSY == config.type,
                config.quality,
                file,
                webpFile
            )
        } else {
            WebpUtil.convert(
                project,
                COMPRESS_TYPE_LOSSY == config.type,
                config.quality,
                file,
                webpFile
            )
        }

        if (file.length() > webpFile.length()) {
            log("$name >> converted ${file.absolutePath}")
            file.delete()
            newSize += webpFile.length()
        } else {
            log("$name >> skip ${file.absolutePath} because webp file size is larger than source file.")
            webpFile.delete()
        }
    }
}