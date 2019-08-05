package com.oscar.apkyogaplugin;

import com.android.build.gradle.internal.workeractions.WorkerActionServiceRegistry;
import com.android.builder.internal.aapt.v2.Aapt2Daemon;
import com.android.builder.internal.aapt.v2.Aapt2DaemonManager;
import com.android.builder.internal.aapt.v2.Aapt2DaemonTimeouts;
import com.android.utils.ILogger;
import com.google.common.base.Ticker;
import com.oscar.apkyogaplugin.utils.ProjectInfo;
import com.oscar.apkyogaplugin.webp.Aapt2ExtDaemonImpl;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class WorkerActionServiceRegistryProxy extends WorkerActionServiceRegistry {
    private final ILogger iLogger;
    private final Path aaptExecutablePath;
    private final ProjectInfo info;

    public WorkerActionServiceRegistryProxy(@NotNull ILogger iLogger, @NotNull Path aaptExecutablePath, @NotNull ProjectInfo info) {
        this.iLogger = iLogger;
        this.aaptExecutablePath = aaptExecutablePath;
        this.info = info;
    }

    public void registerService(@NotNull ServiceKey key, @NotNull Function0 serviceFactory) {
        try {
            if (Objects.equals(key.getType(), Aapt2DaemonManager.class)) {
                Class aaptMmClazz = Class.forName("com.android.build.gradle.internal.res.namespaced.Aapt2DaemonManagerMaintainer");
                Constructor aaptMmCons = aaptMmClazz.getConstructor();
                aaptMmCons.setAccessible(true);

                Function1<Integer, Aapt2Daemon> function1 = new Function1<Integer, Aapt2Daemon>() {

                    @Override
                    public Aapt2Daemon invoke(Integer displayId) {
                        return (Aapt2Daemon) (new Aapt2ExtDaemonImpl("" + '#' + displayId, aaptExecutablePath, new Aapt2DaemonTimeouts(), iLogger, info));
                    }
                };

                Aapt2DaemonManager manager = new Aapt2DaemonManager(iLogger, function1, TimeUnit.MINUTES.toSeconds(3L), TimeUnit.SECONDS, (Aapt2DaemonManager.Listener) aaptMmCons.newInstance(), Ticker.systemTicker());

                Class rsClazz = Class.forName("com.android.build.gradle.internal.res.namespaced.RegisteredAaptService");
                Constructor rsCons = rsClazz.getDeclaredConstructor(Aapt2DaemonManager.class);
                rsCons.setAccessible(true);
                Object var9 = rsCons.newInstance(manager);

                RegisteredService service = (RegisteredService) var9;
                Map var10 = this.getServices();
                Intrinsics.checkExpressionValueIsNotNull(var10, "services");
                var10.put(key, service);
            } else {
                super.registerService(key, serviceFactory);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
