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

public class LuaLibrary extends TwoArgFunction {

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

    private static Map<Class<?>, Map<String, LuaMethodBind>> libs;
    private static Map<Class<?>, String> cnames;

    /**
     * Registers a {@link LuaLibrary} for usage.
     * 
     * @param libClass the class of the LuaLibrary to register
     * 
     * @throws Throwable if something goes wrong
     */
    static void register(Class<? extends LuaLibrary> libClass) throws Throwable {
        if (!libs.containsKey(libClass)) {
            return; // do nothing if we already have it
        }

        Map<String, LuaMethodBind> funcs = new HashMap<>();
        Class<?> superr = libClass.getSuperclass();
        List<Class<?>> supers = new ArrayList<>();
        while (superr != Object.class) {
            supers.add(superr);
            superr = superr.getSuperclass();
        }

        

        for (Method m : libClass.getDeclaredMethods()) {
            LuaType annot = m.getAnnotation(LuaType.class);
            if (m.getDeclaredAnnotation(LuaType.class) != null) {
                annot = m.getDeclaredAnnotation(LuaType.class);
            }

            if (annot != null) {
                funcs.put(LuaMeta.convertMethodName(m, annot.name().trim()), new LuaMethodBind(m));
            }
        }

        LuaType cannot = libClass.getDeclaredAnnotation(LuaType.class);
        if (cannot != null) {
            cnames.put(libClass, LuaMeta.convertClassName(libClass, cannot.name()));
        }

        libs.put(libClass, funcs);
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        Class<?> me = this.getClass();
        Map<String, LuaMethodBind> funcs = libs.get(me);
        if (funcs == null) {
            throw new InvalidCoercionTargetException(
                String.format("Library %s not registered, cannot generate function table",
                    this.getClass().getName()));
        }

        String name = cnames.get(me);
        LuaTable tab = new LuaTable();
        if (name == null) {
            name = LuaMeta.convertClassName(me, null);
        }

        env.set(name, tab);
        funcs.keySet().forEach(key -> {
            tab.set(key, new MethodBindDelegate(this, funcs.get(key)));
        });

        return tab;
    }

}
