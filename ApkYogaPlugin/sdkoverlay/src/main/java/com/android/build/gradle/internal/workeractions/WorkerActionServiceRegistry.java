package com.android.build.gradle.internal.workeractions;

import kotlin.jvm.JvmField;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class WorkerActionServiceRegistry {
    @NotNull
    private Map<ServiceKey<?>, RegisteredService<?>> services;
    @JvmField
    @NotNull
    public static WorkerActionServiceRegistry INSTANCE = new WorkerActionServiceRegistry();

    public interface ServiceKey<T> extends Serializable {
        @NotNull
        Class<T> getType();
    }

    public interface RegisteredService<T> {
        @NotNull
        T getService();

        void shutdown();
    }

    public WorkerActionServiceRegistry() {
        this.services = new LinkedHashMap<>();
    }

    @NotNull
    public Map getServices() {
        return this.services;
    }

    public synchronized <T> void registerService(@NotNull ServiceKey<T> key, @NotNull Function0<RegisteredService<T>> serviceFactory) {
        System.out.println("ApkYogaPlugin: registerService");
        if (services.get(key) == null) {
            services.put(key, serviceFactory.invoke());
        }
    }

    @NotNull
    public synchronized <T> RegisteredService getService(@NotNull ServiceKey<T> key) {
        RegisteredService service = this.services.get(key);
        if (service != null) {
            return service;
        } else {
            if (this.services.isEmpty()) {
                throw new IllegalStateException("No services are registered. Ensure the worker actions use IsolationMode.NONE.");
            } else {
                throw new IllegalStateException("Service " + key + " not registered.");
            }
        }
    }

    @Nullable
    public synchronized <T> RegisteredService removeService(@NotNull ServiceKey<T> key) {
        return this.services.remove(key);
    }

    private synchronized Collection<RegisteredService<?>> removeAllServices() {
        ArrayList<RegisteredService<?>> toBeShutdown = new ArrayList<>(this.services.values());
        this.services.clear();
        return toBeShutdown;
    }

    public void shutdownAllRegisteredServices(@NotNull Executor executor) {
        Collection<RegisteredService<?>> toBeShutdown = this.removeAllServices();

        for (RegisteredService service : toBeShutdown) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    service.shutdown();
                }
            });
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}