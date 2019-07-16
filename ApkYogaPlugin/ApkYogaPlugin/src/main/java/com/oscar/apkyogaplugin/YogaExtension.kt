package com.oscar.apkyogaplugin

import com.oscar.apkyogaplugin.YogaExtension.Companion.COMPRESS_TYPE_LOSSY
import com.oscar.apkyogaplugin.webp.WebpConvertConfig
import org.gradle.api.Action

open class YogaExtension {
    companion object {
        val COMPRESS_TYPE_LOSSY = "lossy" // 有损压缩
        val COMPRESS_TYPE_LOSSLESS = "lossless" // 无损压缩
    }

    var enable = true // 是否开启ApkYoga功能
    var enableOnDebug = true // 是否在调试时开启
    var webpConvertConfig = WebpConvertConfig() // 图片转webp的配置
    var imageCompressConfig = ImageCompressConfig() // 图片压缩配置
    var removeResourceConfig = RemoveResourceConfig() // 删除资源的配置
    var webCompressConfig = WebCompressConfig() // web文件资源压缩配置
    var removeApkResourceConfig = RemoveApkResourceConfig() // 删除apk zip包下文件的配置

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun enableOnDebug(enableOnDebug: Boolean) {
        this.enableOnDebug = enableOnDebug
    }

    fun webpConvertConfig(action: Action<WebpConvertConfig>) {
        action.execute(webpConvertConfig)
    }

    fun imageCompressConfig(action: Action<ImageCompressConfig>) {
        action.execute(imageCompressConfig)
    }

    fun removeResourceConfig(action: Action<RemoveResourceConfig>) {
        action.execute(removeResourceConfig)
    }

    fun webCompressConfig(action: Action<WebCompressConfig>) {
        action.execute(webCompressConfig)
    }

    fun removeApkResourceConfig(action: Action<RemoveApkResourceConfig>) {
        action.execute(removeApkResourceConfig)
    }
}

class ImageCompressConfig {
    var enable = true // 是否开启图片资源压缩功能
    var auto = true // 是否在gradle build时启用此功能
    var type = COMPRESS_TYPE_LOSSY // 压缩类型
    var jpegQuality = 84 // jpeg压缩画质
    var pngQuality = 80 // png压缩画质
    var whiteList = arrayOf<String>()
    var blackList = arrayOf<String>()

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun auto(auto: Boolean) {
        this.auto = auto
    }

    fun type(type: String) {
        this.type = type
    }

    fun jpegQuality(jpegQuality: Int) {
        this.jpegQuality = jpegQuality
    }

    fun pngQuality(pngQuality: Int) {
        this.pngQuality = pngQuality
    }

    fun whiteList(whiteList: Array<String>) {
        this.whiteList = whiteList
    }

    fun blackList(blackList: Array<String>) {
        this.blackList = blackList
    }
}

class RemoveResourceConfig {
    var enable = true
    var removeDuplicate = true // 移除重复资源
    var blackList = arrayOf<String>() // 删除不想要的资源

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun removeDuplicate(removeDuplicate: Boolean) {
        this.removeDuplicate = removeDuplicate
    }

    fun blackList(blackList: Array<String>) {
        this.blackList = blackList
    }
}

class WebCompressConfig {
    var enable = true
    var html = true // 压缩html
    var js = true // 压缩js
    var css = true // 压缩css

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun html(html: Boolean) {
        this.html = html
    }

    fun js(js: Boolean) {
        this.js = js
    }

    fun css(css: Boolean) {
        this.css = css
    }
}

class RemoveApkResourceConfig {
    var enable = true
    var whiteList = arrayOf<String>()
    var blackList = arrayOf<String>()

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun whiteList(whiteList: Array<String>) {
        this.whiteList = whiteList
    }

    fun blackList(blackList: Array<String>) {
        this.blackList = blackList
    }
}