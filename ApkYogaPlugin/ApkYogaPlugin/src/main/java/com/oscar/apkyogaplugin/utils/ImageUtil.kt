package com.oscar.apkyogaplugin.utils

import com.android.SdkConstants
import java.io.File

fun isWebpConvertableImage(file: File, convertGif: Boolean): Boolean {
    return (file.name.endsWith(SdkConstants.DOT_PNG) ||
            file.name.endsWith(SdkConstants.DOT_JPEG) ||
            file.name.endsWith(SdkConstants.DOT_JPG) ||
            file.name.endsWith(SdkConstants.DOT_BMP) ||
            (convertGif && file.name.endsWith(SdkConstants.DOT_GIF))) &&
            !file.name.endsWith(SdkConstants.DOT_9PNG)
}