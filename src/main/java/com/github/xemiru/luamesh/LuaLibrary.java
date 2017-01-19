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

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Lua library implemented by Java methods.
 */
public class LuaLibrary extends TwoArgFunction {

    /**
     * Small wrapper used so we don't need to store
     * duplicate instances of method bindings.
     */
    private class MethodBindDelegate extends VarArgFunction {

        private Object inst;
        private LuaMethodBind bind;

        public MethodBindDelegate(LuaLibrary inst, LuaMethodBind bind) {
            this.inst = inst;
            this.bind = bind;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return this.bind.invoke(inst, bind);
        }
    }

    private static Map<Class<?>, LibraryMeta> libs;

    static {
        libs = new HashMap<>();
    }

    /**
     * Registers a {@link LuaLibrary} for usage.
     * 
     * @param libClass the class of the LuaLibrary to
     *        register
     * 
     * @throws Throwable if something goes wrong
     */
    static void register(Class<? extends LuaLibrary> libClass) throws Throwable {
        if (libs.containsKey(libClass)) {
            return; // do nothing if we already have it
        }

        LibraryMeta meta = new LibraryMeta();
        Class<?> superr = libClass.getSuperclass();
        List<Class<?>> supers = new ArrayList<>();
        while (superr != Object.class) {
            if (superr.getDeclaredAnnotation(LuaType.class) != null) {
                supers.add(superr);
            }

            superr = superr.getSuperclass();
        }

        while (!supers.isEmpty()) {
            superr = supers.get(supers.size() - 1);
            LibraryMeta smeta = libs.get(superr);
            if (smeta == null) {
                throw new InvalidCoercionTargetException(String.format(
                    "Parent library %s of library %s has not been registered; could not inherit",
                    superr.getName(), libClass.getName()));
            }

            meta.getBinds().putAll(smeta.getBinds());
            meta.getNames().putAll(smeta.getNames());
            supers.remove(superr);
        }

        for (Method m : libClass.getDeclaredMethods()) {
            LuaType annot = m.getDeclaredAnnotation(LuaType.class);

            if (annot != null) {
                String mName = m.getName();
                String cName = LuaMeta.convertMethodName(m, annot.name().trim());
                if (meta.getNames().containsKey(mName)) {
                    meta.getBinds().remove(meta.getNames().get(mName));
                }

                meta.getNames().put(mName, cName);
                meta.getBinds().put(cName, new LuaMethodBind(m));
            }
        }

        LuaType cannot = libClass.getDeclaredAnnotation(LuaType.class);
        if (cannot == null) {
            throw new InvalidCoercionTargetException(String
                .format("Library %s is not annotated with LuaType", libClass.getClass().getName()));
        }

        meta.setName(LuaMeta.convertClassName(libClass, cannot.name()));
        libs.put(libClass, meta);
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        Class<?> me = this.getClass();
        LibraryMeta meta = libs.get(me);
        if (meta == null) {
            throw new InvalidCoercionTargetException(
                String.format("Library %s not registered, cannot generate function table",
                    this.getClass().getName()));
        }

        String name = meta.getName();
        LuaTable tab = new LuaTable();
        if (name == null) {
            name = LuaMeta.convertClassName(me, null);
        }

        env.set(name, tab);
        Map<String, LuaMethodBind> funcs = meta.getBinds();
        funcs.keySet().forEach(key -> {
            tab.set(key, new MethodBindDelegate(this, funcs.get(key)));
        });

        return tab;
    }

}
