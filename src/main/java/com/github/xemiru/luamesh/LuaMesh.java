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
import java.util.function.Consumer;
import java.util.function.Function;
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
     * <ul> <li>1 - Class names only.</li> <li>2 - Member
     * names only.</li> <li>3 - Class and member names.</li>
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

    public static Consumer<String> debug = null;
    private static Map<String, String> names;
    private static Map<Class<?>, LuaMeta> metas;
    private static List<String> classes;
    private static Map<Class<?>, Function<String, String>> uclasses;

    static {
        metas = new HashMap<>();
        names = new HashMap<>();
        classes = new ArrayList<>();
        uclasses = new HashMap<>();
    }

    /**
     * Internal method. Registers the metadata for
     * Lua-coercible classes.
     *
     * @param clazz the class to register
     * @param filter an optional filter
     *
     * @return the LuaMeta returned, or null if not found
     */
    static LuaMeta registerMeta(Class<?> clazz, Function<String, String> filter) {
        if (metas.containsKey(clazz)) {
            return metas.get(clazz);
        }

        LuaType typeAnnot = clazz.getAnnotation(LuaType.class);
        Class<?> target = clazz;
        LuaMeta meta;

        if(typeAnnot != null) {
            target = typeAnnot.target() == Object.class ? target : typeAnnot.target();
            meta = new LuaMeta(typeAnnot, clazz, target);
        } else {
            meta = new LuaMeta(clazz, filter);
        }

        metas.put(target, meta);
        names.put(target.getName(), meta.getName());
        return meta;
    }

    public static void debug(String message) {
        if(LuaMesh.debug != null) debug.accept(message);
    }

    /**
     * Registers a class, referred to by its fully-qualified
     * name, as Lua-coercible.
     *
     * <p>This should be used for classes that have a
     * {@link LuaType} annotation and are therefore
     * bi-directional (Java method calls can call Lua
     * functions and vice-versa).</p>
     *
     * <p>Classes with a LuaType annotation cannot be loaded
     * before the init() method is called; one should
     * manually type the qualified name passed to this
     * method.</p>
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
     * Registers a class, referred to by its fully-qualified
     * name, as Lua-coercible.
     *
     * <p>This should be used for classes that do not have a
     * {@link LuaType} annotation and are therefore unidirectional
     * (Lua can call Java methods, but Java can't call Lua's).</p>
     *
     * <p>Classes without a LuaType annotation can safely be loaded
     * prior to the init() method, as no transformations occur for
     * these classes. One can simply use Class.getName() to pass to
     * this method.</p>
     *
     * <p>The filter receives the names of candidate
     * methods within the given class to be written to
     * the Lua instance. It can return the same name, or
     * a different name to rename the method in the Lua
     * environment. It can also return null to signify
     * that the candidate method should not be written to
     * to the class's Lua counterpart.</p>
     *
     * <p>If the filter is null, all methods will be
     * registered with their default names.</p>
     *
     * @param clazz the class to register, by its qualified
     *        name (e.g. java.lang.Integer)
     * @param filter a filter determining which methods
     *        should be accessible from the Lua instance
     */
    public static void register(Class<?> clazz, Function<String, String> filter) {
        if (uclasses != null) {
            uclasses.put(clazz, filter);
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
                    debug("applying transformations to class " + str);

                    String qname = str.replaceAll("\\.", "/");
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                    MeshTransformer mt = new MeshTransformer(cw);
                    ClassReader cr = new ClassReader(qname);
                    cr.accept(mt, 0);

                    MeshTransformer.transform(str, cw.toByteArray());
                }

                for (String str : classes) {
                    debug("registering meta for class " + str);
                    registerMeta(Class.forName(str, true, ClassLoader.getSystemClassLoader()), null);
                }

                for (Class<?> clazz : uclasses.keySet()) {
                    debug("registering meta for class " + clazz.getName());
                    registerMeta(clazz, uclasses.get(clazz));
                }
            } finally {
                classes = null;
                uclasses = null;
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
     * @return the Lua name of clazz, or "nil" if passed null
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
        return n == null ? "<uncoercible Java type " + name + ">" : n;
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
