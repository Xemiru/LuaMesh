/**
 * MIT License
 *
 * Copyright (c) 2016 Tellerva, Marc Lawrence G.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.xemiru.luamesh;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles bytecode transformation of meshed classes
 * annotated with {@link LuaType}.
 */
public class MeshTransformer extends ClassVisitor {

    /**
     * Hand-picks specific values from an annotation
     * declaration. For use with {@link LuaType}
     * annotations.
     */
    public static class AnnotationHarvester extends AnnotationVisitor {

        public boolean abstractt;

        public AnnotationHarvester(AnnotationVisitor av) {
            super(ASM5, av);
            this.abstractt = false;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            if (name.equals("abstractt")) {
                this.abstractt = (boolean) value;
            }
        }
    }

    /**
     * Transforms methods annotated with {@link LuaType} to
     * be able to interface with a Lua environment.
     */
    @SuppressWarnings("deprecation")
    public static class MethodTransformer extends MethodVisitor {

        // Method descriptor of the supplier lua method in LuaMesh.
        private static final String LSUP_DESC =
            "(Ljava/lang/Object;Ljava/lang/String;Ljava/util/function/Supplier;[Ljava/lang/Object;)Ljava/lang/Object;";
        // Method descriptor of the runnable lua method in LuaMesh.
        private static final String LRUN_DESC =
            "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Runnable;[Ljava/lang/Object;)V";
        private static final String LAMBDA_PREFIX = "lambda$luam_";
        // Single reference handle to the lambda bootstrap method; we don't need to remake it over and over.
        private static final Handle LAMBDA_BS = new Handle(H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;");

        private ClassVisitor cv;

        private AnnotationHarvester av;
        private String cname, name, desc, signature;
        private String[] exceptions;
        private boolean abstractt;
        private boolean lua;

        public MethodTransformer(ClassVisitor cv, String cname, int access, String name,
            String desc, String signature, String[] exceptions) {
            // let stuff before the main code get passed to the delegate classvisitor
            super(ASM5, cv.visitMethod(access, name, desc, signature, exceptions));
            this.av = null;
            this.cv = cv;

            // record stuff about our target method
            this.cname = cname;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.exceptions = exceptions;
            this.abstractt = false;
            this.lua = false;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (desc.equals(Type.getDescriptor(LuaType.class))) {
                this.lua = true;
                this.av = new AnnotationHarvester(av);
                return this.av;
            }

            return av;
        }

        @Override
        public void visitCode() {
            // if we had a LuaType annot
            if (this.lua) {
                // gather stuff for generating our lambda
                this.abstractt = (this.av != null && this.av.abstractt);
                Matcher m = PARAM_MATCHER.matcher(desc);
                m.find();

                String mparams = m.group(1);
                String lam = LAMBDA_PREFIX + name;
                Type retType = Type.getReturnType(desc);
                int pCount = Type.getArgumentTypes(desc).length;
                boolean run = retType == null || retType.getDescriptor().equals("V"); // void method means runnable, otherwise supplier

                // replaces the original method
                mv.visitCode();

                // generate our lambda func
                if (this.abstractt) {
                    // pass null
                    mv.visitInsn(ACONST_NULL);
                } else {
                    // invokedynamic params
                    for (int i = 0; i <= pCount; i++) {
                        mv.visitVarInsn(ALOAD, i);
                    }

                    // get our sup by
                    String lparams = "(L" + cname + ";" + mparams.substring(1);
                    if (run) { // generating a runnable
                        mv.visitInvokeDynamicInsn("run", lparams + "Ljava/lang/Runnable;",
                            LAMBDA_BS, Type.getType("()V"),
                            new Handle(H_INVOKESPECIAL, cname, lam, desc, false),
                            Type.getType("()V"));
                    } else { // generating a supplier
                        mv.visitInvokeDynamicInsn("get", lparams + "Ljava/util/function/Supplier;",
                            LAMBDA_BS, Type.getType("()Ljava/lang/Object;"),
                            new Handle(H_INVOKESPECIAL, cname, lam, desc, false),
                            Type.getType("()Ljava/lang/Object;"));
                    }
                }

                mv.visitVarInsn(ASTORE, pCount + 1);

                // prep to call LuaMesh's lua
                mv.visitVarInsn(ALOAD, 0); // obj
                mv.visitLdcInsn(name); // method name
                mv.visitVarInsn(ALOAD, pCount + 1); // func obj

                // add our method's parameters
                mv.visitLdcInsn(pCount);
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int i = 0; i < pCount; i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    mv.visitVarInsn(ALOAD, i + 1);
                    mv.visitInsn(AASTORE);
                }

                // invoke LuaMesh's lua
                mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(LuaMesh.class), "lua",
                    run ? LRUN_DESC : LSUP_DESC, false);

                // return
                if (run) {
                    mv.visitInsn(RETURN);
                } else {
                    String desc = retType.getDescriptor();
                    char ch = desc.charAt(0);
                    String iname = null;

                    // decide the correct opcode to invoke a return statement
                    int opcode = IRETURN;
                    if (ch == 'F') {
                        opcode = FRETURN;
                    } else if (ch == 'J') {
                        opcode = LRETURN;
                    } else if (ch == 'D') {
                        opcode = DRETURN;
                    }

                    // correctly cast into the right object type
                    if (ch == 'L') {
                        iname = retType.getInternalName();
                        opcode = ARETURN;
                    } else {
                        iname = primap.get(String.valueOf(ch));
                    }

                    mv.visitTypeInsn(CHECKCAST, iname);

                    if (ch != 'L') {
                        // make sure primitive return methods get their primitive return value
                        String prim = iname.split(Pattern.quote("/"))[2].toLowerCase();
                        if (prim.equals("integer")) {
                            prim = "int";
                        }

                        mv.visitMethodInsn(INVOKEVIRTUAL, iname, prim + "Value", "()" + ch, false);
                    }

                    // return
                    mv.visitInsn(opcode);
                }

                // 7 possible things on operand stack at a time
                // match method param count plus instance and lambda func temp store
                mv.visitMaxs(7, pCount + 2);
                mv.visitEnd();

                // let the original method get turned into our lambda method
                this.mv = cv.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, lam, desc,
                    signature, exceptions);
            }

            super.visitCode();
        }
    }

    private static final Pattern PARAM_MATCHER = Pattern.compile("(\\(.*?\\))");
    private static final Method cl_define;
    private static final Map<String, String> primap;

    static {
        // make the system class loader's define method accessible
        Method m = null;
        try {
            m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class,
                int.class, int.class);
            m.setAccessible(true);
        } catch (Throwable e) {
            // this isn't good; we can't perform our injection
            throw new RuntimeException(e);
        }

        cl_define = m;

        // for some reason asm doesnt have something to let me convert primitive descriptors
        // into their java type counterparts
        // so i'm using this
        // fuck you
        primap = new HashMap<>();
        primap.put("Z", "java/lang/Boolean");
        primap.put("C", "java/lang/Character");
        primap.put("B", "java/lang/Byte");
        primap.put("I", "java/lang/Integer");
        primap.put("S", "java/lang/Short");
        primap.put("F", "java/lang/Float");
        primap.put("J", "java/lang/Long");
        primap.put("D", "java/lang/Double");
    }

    /**
     * Creates a new class of the given qualified name, with
     * the instructions of the provided bytecode.
     * 
     * @param name the qualified Java name of the class to
     *        create (e.g. java.lang.Integer)
     * @param code the bytecode to use
     */
    static void transform(String name, byte[] code) {
        try {
            cl_define.invoke(ClassLoader.getSystemClassLoader(), name, code, 0, code.length);
        } catch (InvocationTargetException e) {
            if (e.getCause() != null && e.getCause() instanceof LinkageError) {
                IllegalStateException ise = new IllegalStateException(
                    "Cannot inject into a class that's already been loaded (avoid referencing the classes in any form before calling LuaMesh.init())");
                ise.initCause(e);
                throw ise;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private String cname;
    private boolean lua;

    public MeshTransformer(ClassVisitor cv) {
        super(ASM5, cv);
        this.cname = null;
        this.lua = false;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
        String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        // harvest the name
        this.cname = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(Type.getDescriptor(LuaType.class))) {
            // make sure we know if we're lua-annotated
            this.lua = true;
        }

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
        // transform our methods
        return new MethodTransformer(this.cv, cname, access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (!this.lua) {
            // not lua annotated?
            //   _____  _____ _____  ______          __  __ 
            //  / ____|/ ____|  __ \|  ____|   /\   |  \/  |
            // | (___ | |    | |__) | |__     /  \  | \  / |
            //  \___ \| |    |  _  /|  __|   / /\ \ | |\/| |
            //  ____) | |____| | \ \| |____ / ____ \| |  | |
            // |_____/ \_____|_|  \_\______/_/    \_\_|  |_|

            throw new InvalidCoercionTargetException(
                "class " + cname + " cannot be transformed: not annotated with LuaType");
        }
    }
}
