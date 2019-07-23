package com.oscar.apkyogaplugin.loader;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class RemoveFinalFlagClassVisitor extends ClassVisitor {

    public RemoveFinalFlagClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // we have the final flag
        if ((access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL) {
            //remove the final flag
            access = access ^ Opcodes.ACC_FINAL;
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }
}