package com.oscar.apkyogaplugin.webp

import com.android.SdkConstants
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.android.build.gradle.internal.res.getAapt2FromMaven
import com.android.build.gradle.internal.res.namespaced.*
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry
import com.android.build.gradle.options.BooleanOption
import com.android.builder.internal.aapt.v2.Aapt2DaemonImpl
import com.android.builder.internal.aapt.v2.Aapt2DaemonManager
import com.android.builder.internal.aapt.v2.Aapt2DaemonTimeouts
import com.android.sdklib.BuildToolInfo
import com.android.utils.ILogger
import com.oscar.apkyogaplugin.YogaExtension.Companion.COMPRESS_TYPE_LOSSY
import com.oscar.apkyogaplugin.loader.MyClassLoader
import com.oscar.apkyogaplugin.loader.RemoveFinalFlagClassVisitor
import com.oscar.apkyogaplugin.utils.ProjectInfo
import com.oscar.apkyogaplugin.utils.TASK_GROUP_NAME
import com.oscar.apkyogaplugin.utils.getProjectInfo
import com.oscar.apkyogaplugin.utils.log
import javassist.ClassPool
import javassist.CtNewMethod
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyFactory
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javassist.util.proxy.ProxyObject
import java.lang.reflect.Method
import javassist.compiler.MemberResolver.getModifiers
import java.lang.reflect.Modifier
import javassist.compiler.MemberResolver.getModifiers
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.lang.reflect.Array.setInt


class WebpConvertConfig : Plugin<Project> {

    var enable = true  // 是否允许图片转webp功能
    var type = COMPRESS_TYPE_LOSSY // webp压缩类型
    var convertGif = false // 是否允许把gif转为webp
    var supportAlpha = true // 是否支持透明
    var quality = 75 // webp画质
    var whiteList = arrayOf<String>()

    override fun apply(project: Project) {
        this.apply(project, null)
    }

    /**
     * invasive: if true, newly generated webp images will overlay image source files;
     * otherwise images will be converted after resources merged.
     *
     * translation:
     * 执行gradle build时，webp转换是无侵入式的，webp转换会在mergeResource时进行；执行gradlew webpConvert，
     * webp转换是侵入式的，将会覆盖源文件
     *
     * gradlew build:        invasive = false
     * gradlew webpConvert:  invasive = true
     */
    fun apply(project: Project, projectInfo: ProjectInfo?) {
        val info = projectInfo ?: getProjectInfo(project)

        project.afterEvaluate {
            if (info.android.defaultConfig.minSdkVersion.apiLevel < 14) {
                throw GradleException(
                    "webpConvertConfig >> You Android minSdkVersion is under 14, which is not support webp!" +
                            "You can set webpConvertConfig.enable=false or update your minSdkVersion to fix this Exception."
                )
            }

            if (info.extension.webpConvertConfig.supportAlpha && info.android.defaultConfig.minSdkVersion.apiLevel < 18) {
                throw GradleException(
                    "webpConvertConfig >> You Android minSdkVersion is under 18, which is not support transparent webp!" +
                            "You can set webpConvertConfig.supportAlpha=false or update your minSdkVersion to fix this Exception."
                )
            }

            applyInvasiveTask(info)
            if (info.extension.webpConvertConfig.enable) {
                applyBuildingTask(info)
            }
        }
    }

    private fun applyBuildingTask(info: ProjectInfo) {
        info.variants.all { variant ->
            variant as BaseVariantImpl

            val variantName = variant.name.capitalize()
            val processResTask =
                info.project.tasks.getByName("process${variantName}Resources") ?: return@all
//            val mergeResTask = info.project.tasks.findByName("merge${variantName}Resources") ?: return@all

            val task = info.project.tasks.create("webpConvert${variantName}MergeResources", WebpConvertTask::class.java)
            task.group = TASK_GROUP_NAME
            task.description = "webp convert merge resource task"
            task.info = info
            task.variant = variant

            var aapt2Enabled = false
            try {
                //判断aapt2是否开启，低版本不存在这个方法，因此需要捕获异常
                processResTask as LinkApplicationAndroidResourcesTask
//                aapt2Enabled = processResTask.isAapt2Enabled
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
                info.project.logger.error(e.message)
            }

            // insert webpConvertResources before processResources
            if (aapt2Enabled) {
                // TODO 代理 WorkerActionServiceRegistry.INSTANCE
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

                val aaptMmClazz =
                    Class.forName("com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerMaintainer")
                val aaptMmCons = aaptMmClazz.getConstructor()
                aaptMmCons.isAccessible = true

                val rsClazz = Class.forName("com.android.build.gradle.internal.res.namespaced.RegisteredAaptService")
                val rsCons = rsClazz.getDeclaredConstructor(Aapt2DaemonManager::class.java)
                rsCons.isAccessible = true

//                val ctClass = ClassPool.getDefault()
//                    .get("com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry")
//                val mName = "registerService"
//                val rsCtMethod = ctClass.getDeclaredMethod(mName)
//                val newName = "registerService\$impl"
//                rsCtMethod.name = newName
//                val mnew = CtNewMethod.copy(rsCtMethod, mName, ctClass, null)


                // fixme final class不能被代理，此方法无效，需要从字节码层面操作
//                val reader = ClassReader(WorkerActionServiceRegistry::class.java.name)
//                val writer = ClassWriter(reader, 0)
//                val visitor = RemoveFinalFlagClassVisitor(writer)
//                reader.accept(visitor, ClassReader.SKIP_CODE)
//                val bytes = writer.toByteArray()
//
//                val loader = ClassReader::class.java.classLoader
//                val defineClassMethod = loader.javaClass.getDeclaredMethod(
//                    "defineClass",
//                    String::class.java,
//                    ByteArray::class.java,
//                    Int::class.java,
//                    Int::class.java
//                )
//                defineClassMethod.isAccessible = true
//                val clazz = defineClassMethod.invoke(loader, WorkerActionServiceRegistry::class.java.name, bytes, 0, bytes.size) as Class<*>

                val proxyFactory = ProxyFactory()
//                val classLoader = MyClassLoader()
//                val clazz =
//                    classLoader.loadClass("com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry")
//                val clazz = Class.forName(
//                    "com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry",
//                    true,
//                    classLoader
//                )
                proxyFactory.superclass = WorkerActionServiceRegistry::class.java
                val proxyClass = proxyFactory.createClass()
                val registry = proxyClass.newInstance()

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
                                val service =
                                    rsCons.newInstance(manager) as WorkerActionServiceRegistry.RegisteredService<*>
                                return proceed.invoke(self, serviceKey, service)
                            }
                        }
                        return proceed.invoke(self, args)
                    }
                }

                val dependencies = processResTask.taskDependencies.getDependencies(processResTask)
                task.dependsOn(dependencies)
                processResTask.dependsOn(task)
            } else {
                val dependencies = processResTask.taskDependencies.getDependencies(processResTask)
                task.dependsOn(dependencies)
                processResTask.dependsOn(task)
            }
        }
    }

    private fun applyInvasiveTask(info: ProjectInfo) {
        info.variants.all { variant ->
            variant as BaseVariantImpl

            val variantName = variant.name.capitalize()
//            val processResTask = info.project.tasks.findByName("process${variantName}Resources") ?: return@all
//            val resFolder =
//                if (variant.mergeResources != null) variant.mergeResources.outputDir.absolutePath else null

            val task =
                info.project.tasks.create("webpConvert${variantName}Resources", InvasiveWebpConvertTask::class.java)
            task.group = TASK_GROUP_NAME
            task.description = "webp convert task"
            task.info = info
            task.variant = variant

            // insert webpConvertResources before processResources
//            val dependencies = processResTask.taskDependencies.getDependencies(processResTask)
//            task.dependsOn(dependencies)
//            processResTask.dependsOn(task)
        }
    }

    fun enable(enable: Boolean) {
        this.enable = enable
    }

    fun type(type: String) {
        this.type = type
    }

    fun convertGif(enable: Boolean) {
        this.convertGif = enable
    }

    fun supportAlpha(supportAlpha: Boolean) {
        this.supportAlpha = supportAlpha
    }

    fun quality(quality: Int) {
        this.quality = when {
            quality in 75..100 -> quality
            quality < 75 -> {
                log("webpConvertConfig >> webp quality can be set in [75,100], now changed to 75")
                75
            }
            else -> {
                log("webpConvertConfig >> webp quality can be set in [75,100], now changed to 100")
                100
            }
        }
    }

    fun whiteList(whiteList: Array<String>) {
        this.whiteList = whiteList
    }
}