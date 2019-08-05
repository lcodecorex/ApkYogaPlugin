package com.oscar.apkyogaplugin.utils

import com.android.SdkConstants
import com.android.build.gradle.*
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry
import com.android.build.gradle.options.BooleanOption
import com.android.builder.internal.aapt.v2.Aapt2DaemonManager
import com.android.builder.internal.aapt.v2.Aapt2DaemonTimeouts
import com.android.sdklib.BuildToolInfo
import com.android.utils.ILogger
import com.oscar.apkyogaplugin.WorkerActionServiceRegistryProxy
import com.oscar.apkyogaplugin.YogaExtension
import com.oscar.apkyogaplugin.webp.Aapt2ExtDaemonImpl
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyFactory
import javassist.util.proxy.ProxyObject
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

const val PLUGIN_EXTENSION_NAME = "ApkYoga"
const val TASK_GROUP_NAME = "ApkYoga"

data class ProjectInfo(
    val project: Project,
    val variants: DomainObjectSet<out Any>,
    val android: BaseExtension,
    val extension: YogaExtension,
    val isDebugTask: Boolean,
    val containsAssembleTask: Boolean
)

fun getProjectInfo(project: Project): ProjectInfo {
    val appliedOnApp = project.plugins.hasPlugin("com.android.application")
    val variants = if (appliedOnApp) {
        (project.property("android") as AppExtension).applicationVariants
    } else {
        (project.property("android") as LibraryExtension).libraryVariants
    }

    val android = project.property("android") as BaseExtension

    val mYogaExtension = project.property(PLUGIN_EXTENSION_NAME) as YogaExtension

    val taskNames = project.gradle.startParameter.taskNames
    var isDebugTask = false
    var containsAssembleTask = false
    for (index in 0 until taskNames.size) {
        val taskName = taskNames[index]
        if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains("bundle")) {
            containsAssembleTask = true
            if (taskName.toLowerCase().endsWith("debug") && taskName.toLowerCase().contains("debug")) {
                isDebugTask = true
            }
            break
        }
    }

    return ProjectInfo(project, variants, android, mYogaExtension, isDebugTask, containsAssembleTask)
}

fun aapt2Enabled(variant: BaseVariantImpl, project: Project): Boolean {
    val variantName = variant.name.capitalize()
    val processResTask = project.tasks.getByName("process${variantName}Resources") ?: return false

    var aapt2Enabled = false
    try {
        //判断aapt2是否开启，低版本不存在这个方法，因此需要捕获异常
        processResTask as LinkApplicationAndroidResourcesTask
        // aapt2Enabled = processResTask.isAapt2Enabled
        val field = LinkApplicationAndroidResourcesTask::class.java.getDeclaredField("variantScope")
        field.isAccessible = true
        val scope = field.get(processResTask) as VariantScope

        // 反射获取enum判断属性是否存在
        val objects = BooleanOption::class.java.enumConstants
        val propertyNameField = BooleanOption::class.java.getDeclaredField("propertyName")
        propertyNameField.isAccessible = true
        val defaultValueField = BooleanOption::class.java.getDeclaredField("defaultValue")
        defaultValueField.isAccessible = true
        for (obj in objects) {
            if (propertyNameField.get(obj) == "android.enableAapt2") {
                aapt2Enabled = scope.globalScope.projectOptions.get(obj as BooleanOption)
                break
            } else if (propertyNameField.get(obj) == "android.enableAapt2WorkerActions") {
                aapt2Enabled = true
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return aapt2Enabled
}

fun proxyServiceRegistry(variant: BaseVariantImpl, info: ProjectInfo) {
    val variantName = variant.name.capitalize()
    val processResTask = info.project.tasks.getByName("process${variantName}Resources") ?: return
    processResTask as LinkApplicationAndroidResourcesTask

    val aapt2FromMaven = processResTask.getAapt2FromMaven()

    val getBuildToolsMethod = processResTask::class.java.getMethod("getBuildTools")
    getBuildToolsMethod.isAccessible = true
    val buildTools = getBuildToolsMethod.invoke(processResTask) as BuildToolInfo?

    val getILoggerMethod = processResTask::class.java.getMethod("getILogger")
    getILoggerMethod.isAccessible = true
    val iLogger = getILoggerMethod.invoke(processResTask) as ILogger

    val aaptExecutablePath = when {
        aapt2FromMaven != null -> {
            val dir = aapt2FromMaven.singleFile
            dir.toPath().resolve(SdkConstants.FN_AAPT2)
        }
        buildTools != null -> {
            Paths.get(buildTools.getPath(BuildToolInfo.PathId.AAPT2))
        }
        else -> throw IllegalArgumentException(
            "Must supply one of aapt2 from maven, build tool info or custom location."
        )
    }

    // 反射修改 WorkerActionServiceRegistry.INSTANCE
//    val insField = WorkerActionServiceRegistry::class.java.getDeclaredField("INSTANCE")
//    insField.isAccessible = true
//    val modifiers = insField.javaClass.getDeclaredField("modifiers")
//    modifiers.isAccessible = true
//    modifiers.setInt(insField, insField.modifiers and Modifier.FINAL.inv())
//    insField.set(null, WorkerActionServiceRegistryProxy(iLogger, aaptExecutablePath))
//    modifiers.setInt(insField, insField.modifiers and Modifier.FINAL.inv())

    WorkerActionServiceRegistry.INSTANCE = WorkerActionServiceRegistryProxy(iLogger, aaptExecutablePath, info)

    /*val aaptMmClazz =
        Class.forName("com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerMaintainer")
    val aaptMmCons = aaptMmClazz.getConstructor()
    aaptMmCons.isAccessible = true

    val rsClazz = Class.forName("com.android.build.gradle.internal.res.namespaced.RegisteredAaptService")
    val rsCons = rsClazz.getDeclaredConstructor(Aapt2DaemonManager::class.java)
    rsCons.isAccessible = true

    val proxyFactory = ProxyFactory()

    proxyFactory.superclass = WorkerActionServiceRegistry::class.java
    val proxyClass = proxyFactory.createClass()
    val registry = proxyClass.newInstance() as WorkerActionServiceRegistry

    // 反射修改 WorkerActionServiceRegistry.INSTANCE
    val insField = WorkerActionServiceRegistry::class.java.getDeclaredField("INSTANCE")
    insField.isAccessible = true
    val modifiers = insField.javaClass.getDeclaredField("modifiers")
    modifiers.isAccessible = true
    modifiers.setInt(insField, insField.modifiers and Modifier.FINAL.inv())
    insField.set(null, registry)
    modifiers.setInt(insField, insField.modifiers and Modifier.FINAL.inv())


    (registry as ProxyObject).handler = object : MethodHandler {
        override fun invoke(self: Any?, thisMethod: Method, proceed: Method, args: Array<out Any>?): Any {
            if (thisMethod.name == "registerService" && args != null) {
                val serviceKey = args.first() as WorkerActionServiceRegistry.ServiceKey<*>
                if (serviceKey.type == Aapt2DaemonManager::class.java) {
                    val manager = Aapt2DaemonManager(
                        logger = iLogger,
                        daemonFactory = { displayId ->
                            Aapt2ExtDaemonImpl(
                                displayId = "#$displayId",
                                aaptExecutable = aaptExecutablePath,
                                daemonTimeouts = Aapt2DaemonTimeouts(),
                                logger = iLogger
                            )
                        },
                        expiryTime = TimeUnit.MINUTES.toSeconds(3),
                        expiryTimeUnit = TimeUnit.SECONDS,
                        listener = aaptMmCons.newInstance() as Aapt2DaemonManager.Listener
                    )
                    val service = rsCons.newInstance(manager) as WorkerActionServiceRegistry.RegisteredService<*>
                    return proceed.invoke(self, serviceKey, service)
                }
            }
            return proceed.invoke(self, args)
        }
    }*/
}