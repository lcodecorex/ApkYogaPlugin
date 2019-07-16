package com.oscar.apkyogaplugin.webp

import com.oscar.apkyogaplugin.utils.CmdUtil
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
    }
}