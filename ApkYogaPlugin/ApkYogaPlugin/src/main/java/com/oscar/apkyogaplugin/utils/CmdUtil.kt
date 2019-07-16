package com.oscar.apkyogaplugin.utils

import org.gradle.api.Project
import java.io.File
import java.io.InputStream

class CmdUtil {
    companion object {
        private const val CACHE_FOLDER_NAME = "ApkYoga"

        fun cmd(project: Project, cmd: String, params: String) {
            val toolPath = getToolDir(project)
            val cmdStr = "$toolPath/$cmd $params"

            val process = Runtime.getRuntime().exec(cmdStr)
            process.waitFor()
        }

        private fun ioCopy(input: InputStream, output: File) {
            val os = output.outputStream()
            input.copyTo(os)
            input.close()
            os.close()
        }

        private fun getToolDir(project: Project): String {
            val gradleHome = project.gradle.gradleUserHomeDir
            val toolDir = File(gradleHome, "$CACHE_FOLDER_NAME/tools")
            if (!toolDir.exists()) {
                toolDir.mkdirs()

                val prefix: String
                var suffix = ""
                var isWindows = false
                when (System.getProperty("os.name")) {
                    "Mac OS X" -> {
                        prefix = "mac"
                    }
                    "Linux" -> {
                        prefix = "linux"
                    }
                    "Windows" -> {
                        prefix = "windows"
                        suffix = ".exe"
                        isWindows = true
                    }
                    else -> {
                        prefix = ""
                    }
                }
                ioCopy(CmdUtil::class.java.getResourceAsStream("/tools/$prefix/cwebp"), File(toolDir, "cwebp$suffix"))
                ioCopy(
                    CmdUtil::class.java.getResourceAsStream("/tools/$prefix/gif2webp$suffix"),
                    File(toolDir, "gif2webp$suffix")
                )
                if (!isWindows) {
                    Runtime.getRuntime().exec("chmod -R +x ${toolDir.absolutePath}").waitFor()
                }
            }
            return toolDir.absolutePath
        }
    }
}