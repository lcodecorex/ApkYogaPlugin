package com.oscar.apkyogaplugin.utils

import com.android.build.gradle.tasks.MergeResources
import java.io.File
import com.android.ide.common.resources.CompileResourceRequest

fun MergeResources.computeResourceSetList0(): List<File>? {
    val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
        .firstOrNull { it.name == "computeResourceSetList" && it.parameterCount == 0 }
        ?: return null

    val oldIsAccessible = computeResourceSetListMethod.isAccessible
    try {
        computeResourceSetListMethod.isAccessible = true

        val resourceSets = computeResourceSetListMethod.invoke(this) as? Iterable<*>

        return resourceSets
            ?.mapNotNull { resourceSet ->
                val getSourceFiles =
                    resourceSet?.javaClass?.methods?.find { it.name == "getSourceFiles" && it.parameterCount == 0 }
                val files = getSourceFiles?.invoke(resourceSet)
                @Suppress("UNCHECKED_CAST")
                files as? Iterable<File>
            }
            ?.flatten()

    } finally {
        computeResourceSetListMethod.isAccessible = oldIsAccessible
    }
}

fun Array<String>.containsIn(words: String): Boolean {
    if (isEmpty()) return false
    forEach {
        if (words.contains(it)) {
            return true
        }
    }
    return false
}

//var CompileResourceRequest.converted: File?
//
//public val CompileResourceRequest.inputFile: File
//    get() {
//        return if (this.converted != null) {
//            inputFile
//        } else {
//            //TODO
//            log("CompileResourceRequest.inputFile convert")
//            File("")
//        }
//    }