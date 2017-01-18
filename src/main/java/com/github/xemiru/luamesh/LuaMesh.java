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

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Primary class for LuaMesh.
 * 
 * <p>Public static variables within this class configure
 * behaviors of name enforcement with Lua-coercible types,
 * when translated into a Lua environment. These must be set
 * before registration of any Lua-coercible classes.</p>
 * 
 * <p>LuaMesh provides an extra function for receiving
 * typenames of Java objects within Lua. See
 * {@link FunctionCType}.</p>
 */
public class LuaMesh {

    /**
     * Name enforcement option.
     * 
     * <ul> <li>1 - Class names only.</li> <li>2 - Method
     * names only.</li> <li>3 - Class and method names.</li>
     * </ul>
     */
    public static short enforcementOption = 1;
    /**
     * Denotes whether or not name overrides are also
     * enforced.
     */
    public static boolean enforceOverrides = false;
    /**
     * Denotes enforcement of the first character in a name
     * being lowercased.
     */
    public static boolean enforceFirstLower = false;
    /**
     * Denotes enforcement of underscoring versus
     * camelcasing.
     */
    public static boolean enforceUnderscore = false;
    /**
     * Denotes enforcement of lowercasing.
     */
    public static boolean enforceLowercase = false;
    /**
     * Denotes whether or not Lua objects will store their
     * typename in their `_type` metakey.
     */
    public static boolean useTypeMetakey = true;

    private static Map<String, String> names;
    private static Map<Class<?>, LuaMeta> metas;
    private static List<String> classes;
    private static List<Class<? extends LuaLibrary>> lclasses;

    static {
        metas = new HashMap<>();
        names = new HashMap<>();
        classes = new ArrayList<>();
        lclasses = new ArrayList<>();
    }

    /**
     * Internal method. Registers the metadata for
     * Lua-coercible classes.
     * 
     * @param clazz the class to register
     * 
     * @return the LuaMeta returned, or null if not found
     */
    static LuaMeta registerMeta(Class<?> clazz) {
        if (metas.containsKey(clazz)) {
            return metas.get(clazz);
        }

        LuaType typeAnnot = clazz.getAnnotation(LuaType.class);
        if (typeAnnot != null) {
            LuaMeta meta = new LuaMeta(typeAnnot, clazz);
            metas.put(clazz, meta);
            names.put(clazz.getName(), meta.getName());

            return meta;
        }

        return null;
    }

    /**
     * Registers a class, referred to by its fully-qualified
     * name, as Lua-coercible.
     * 
     * @param clazz the class to register, by its qualified
     *        name (e.g. java.lang.Integer)
     */
    public static void register(String clazz) {
        if (classes != null) {
            classes.add(clazz);
        }
    }

    /**
     * Registers a {@link LuaLibrary} for usage.
     * 
     * @param libClass the class of the LuaLibrary to
     *        register
     */
    public static void registerLib(Class<? extends LuaLibrary> libClass) {
        if (lclasses != null) {
            lclasses.add(libClass);
        }
    }

    /**
     * Initializes the classes registered to be loaded by
     * LuaMesh.
     * 
     * <p>This method can only be called once, future calls
     * result in a no-op.</p>
     * 
     * @throws Throwable if something goes wrong :^)
     */
    public static void init() throws Throwable {
        if (classes != null) {
            try {
                for (String str : classes) {
                    String qname = str.replaceAll("\\.", "/");
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                    MeshTransformer mt = new MeshTransformer(cw);
                    ClassReader cr = new ClassReader(qname);
                    cr.accept(mt, 0);

                    MeshTransformer.transform(str, cw.toByteArray());
                }

                for (String str : classes) {
                    registerMeta(Class.forName(str, true, ClassLoader.getSystemClassLoader()));
                }

                for(Class<? extends LuaLibrary> lclass : lclasses) {
                    LuaLibrary.register(lclass);
                }
            } finally {
                classes = null;
                lclasses = null;
            }
        }
    }

    /**
     * Call the equivalent Lua function of the named method
     * on the given object's Lua wrapper if it exists,
     * otherwise execute and provide the result of the given
     * supplier.
     * 
     * @param <T> the return type of the method
     * @param obj the object performing the method
     * @param methodName the name of the Java method
     * @param sup the supplier to default to
     * @param args the parameters passed to the method, to
     *        give to the Lua function
     * 
     * @return the return value of the Lua or Java function
     */
    @SuppressWarnings("unchecked")
    public static <T> T lua(Object obj, String methodName, Supplier<T> sup, Object... args) {
        Object returned = lua(obj, methodName, sup, null, args);
        try {
            return (T) returned;
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new LuaError("bad return value: got " + getLuaName(returned.getClass()));
        }
    }


    /**
     * Call the equivalent Lua function of the named method
     * on the given object's Lua wrapper if it exists,
     * otherwise execute and provide the result of the given
     * runnable.
     * 
     * @param obj the object performing the method
     * @param methodName the name of the Java method
     * @param sup the runnable to default to
     * @param args the parameters passed to the method, to
     *        give to the Lua function
     */
    public static void lua(Object obj, String methodName, Runnable sup, Object... args) {
        lua(obj, methodName, sup, null, args);
    }

    // extra null parameter for a unique method signature
    // because screw trying to cast that
    static Object lua(Object obj, String methodName, Object sup, Object _null, Object[] args) {
        // when java calls a lua function
        LuaObjectValue<?> lobj = LuaObjectValue.of(obj);
        LuaMeta meta = getMeta(obj.getClass());
        String luaName = meta.getLuaName(methodName);
        LuaValue func = lobj.get(luaName);
        if (meta.isMeta(methodName)) {
            func = lobj.getmetatable().get(luaName);
        }

        if (!func.isfunction()) {
            throw new LuaError("bad value: " + luaName + " is expected to be a function");
        }

        if (!(func instanceof LuaMethodBind)) {
            // call lua func
            return LuaUtil.toJava(func.call(lobj, LuaUtil.toLua(args)), false);
        }

        // the function didn't exist in lua
        if (sup == null) {
            System.out.println("method " + methodName + " is abstract");
            // our java method didnt exist either
            // scream
            throw new LuaError("bad value: " + luaName + " is expected to be a function");
        } else {
            if (sup instanceof Runnable) {
                ((Runnable) sup).run();
                return null;
            }

            return ((Supplier<?>) sup).get();
        }
    }

    /**
     * Returns the Lua name of the given class.
     * 
     * @param clazz the class to get the name for
     * 
     * @return the Lua name of clazz, or "nil" if null
     */
    public static String getLuaName(Class<?> clazz) {
        return getLuaName(clazz == null ? null : clazz.getName());
    }

    /**
     * Returns the Lua name of the given class name, in the
     * format returned by {@link Class#getName()}.
     * 
     * <p>If the given class has no associated name, this
     * instead returns {@code "<unknown type>"}.</p>
     * 
     * @param name the class name to get the name for
     * 
     * @return the Lua name of the given class, or "nil" if
     *         passed null
     */
    public static String getLuaName(String name) {
        if (name == null) {
            return "nil";
        } else if (name.startsWith("java.lang.")) {
            if (name.equals("java.lang.Integer") || name.equals("java.lang.Short")
                || name.equals("java.lang.Long")) {
                return "integer";
            }

            if (name.equals("java.lang.Float") || name.equals("java.lang.Double")) {
                return "number";
            }

            return name.split("\\.")[2].toLowerCase();
        }

        String n = names.get(name);
        return n == null ? "<unknown type>" : n;
    }

    /**
     * Retrieves the {@link LuaMeta} of a class, or null if
     * not registered as Lua-coercible.
     * 
     * @param clazz the class to retrieve LuaMeta for
     * 
     * @return clazz's LuaMeta, or null if not registered
     */
    public static LuaMeta getMeta(Class<?> clazz) {
        if (metas.containsKey(clazz)) {
            return metas.get(clazz);
        }

        return null;
    }

}
