package com.oscar.apkyogaplugin.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;

public class MyClassLoader extends ClassLoader {
    public MyClassLoader(){
        super(null);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
//        try {
//            ClassReader reader = new ClassReader(name);
//            ClassWriter writer = new ClassWriter(reader, 0);
//            RemoveFinalFlagClassVisitor visitor = new RemoveFinalFlagClassVisitor(writer);
//            reader.accept(visitor, ClassReader.SKIP_CODE);
//            byte[] bytes = writer.toByteArray();
//            return defineClass(name, bytes, 0, bytes.length);
//        } catch (IOException e) {
//            throw new ClassNotFoundException(name, e);
//        }
        return null;
    }
}