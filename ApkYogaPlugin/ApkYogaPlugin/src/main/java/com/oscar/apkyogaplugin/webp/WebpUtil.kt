package com.oscar.apkyogaplugin.webp

import com.android.SdkConstants
import com.oscar.apkyogaplugin.YogaExtension
import com.oscar.apkyogaplugin.utils.CmdUtil
import com.oscar.apkyogaplugin.utils.ProjectInfo
import com.oscar.apkyogaplugin.utils.containsIn
import com.oscar.apkyogaplugin.utils.log
import org.apache.commons.imaging.Imaging
import org.gradle.api.Project
import java.io.File

class WebpUtil {
    companion object {
        fun convert(project: Project, lossy: Boolean, quality: Int, src: File, dst: File) {
            val paramsSb = StringBuilder()
            if (!lossy) {
                paramsSb.append("-lossless ")
            }
            paramsSb.append("-q $quality -m 6 -quiet ${src.absolutePath} -o ${dst.absolutePath}")
            CmdUtil.cmd(project, "cwebp", paramsSb.toString())

            if (dst.length() >= src.length() && lossy) {
                convert(project, false, quality, src, dst)
            }
        }

        fun convertGif(project: Project, lossy: Boolean, quality: Int, src: File, dst: File) {
            val paramsSb = StringBuilder()
            if (lossy) {
                paramsSb.append("-lossy ")
            }
            paramsSb.append("-q $quality -m 6 -quiet ${src.absolutePath} -o ${dst.absolutePath}")
            CmdUtil.cmd(project, "gif2webp", paramsSb.toString())

            if (dst.length() >= src.length() && lossy) {
                convertGif(project, false, quality, src, dst)
            }
        }

        fun executeConvert(file: File, info: ProjectInfo): Long {
            val config = info.extension.webpConvertConfig

            val whiteList = info.extension.webpConvertConfig.whiteList

            if (whiteList.containsIn(file.absolutePath)) {
                log(" >> skip white list file ${file.absolutePath}")
                return 0
            }

            if (file.name.endsWith(SdkConstants.DOT_PNG) && !info.extension.webpConvertConfig.supportAlpha
                && Imaging.getImageInfo(file).isTransparent
            ) {
                log(" >> skip alpha png ${file.absolutePath}")
                return 0
            }

            val fileName = file.name.substring(0, file.name.lastIndexOf(".")) + SdkConstants.DOT_WEBP
            val webpFile = File(file.parentFile.absolutePath, fileName)

            if (file.name.endsWith(SdkConstants.DOT_GIF)) {
                convertGif(
                    info.project,
                    YogaExtension.COMPRESS_TYPE_LOSSY == config.type,
                    config.quality,
                    file,
                    webpFile
                )
            } else {
                convert(
                    info.project,
                    YogaExtension.COMPRESS_TYPE_LOSSY == config.type,
                    config.quality,
                    file,
                    webpFile
                )
            }

            var savedSize = 0L

            if (file.length() > webpFile.length()) {
                log(" >> converted ${file.absolutePath}")
                savedSize = file.length() - webpFile.length()
                file.delete()
            } else {
                log(" >> skip ${file.absolutePath} because webp file size is larger than source file.")
                webpFile.delete()
            }
            return savedSize
        }
    }
}