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
package com.github.xemiru.luamesh.test;

import com.github.xemiru.luamesh.FunctionCType;
import com.github.xemiru.luamesh.LuaMesh;
import com.github.xemiru.luamesh.test.objects.UnidirectionalTarget;
import org.junit.Assert;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.function.Function;

public class Utility {

    static Globals g = null;

    public static void println(Object msg) {
        System.out.println(msg.toString());
    }

    public static Globals init() {
        if(g == null) {
            LuaMesh.debug = System.out::println;
            register("ObjectFields");
            register("ObjectLibraries");
            register("ObjectMethods");
            register("ObjectNames");
            register("UnidirectionalDelegate");
            LuaMesh.register(UnidirectionalTarget.class, name -> {
                switch(name) {
                    case "doThings": return "doStuff";
                    default: return name;
                }
            });

            try {
                LuaMesh.init();
            } catch(Throwable any) {
                any.printStackTrace();
                Assert.fail("Initialization failed.");
            }

            g = JsePlatform.debugGlobals();
            g.set("ctype", new FunctionCType());
        }

        return g;
    }

    public static LuaFunction func(Function<Varargs, Varargs> func) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return func.apply(args);
            }
        };
    }

    static void register(String name) {
        LuaMesh.register("com.github.xemiru.luamesh.test.objects." + name);
    }
}
