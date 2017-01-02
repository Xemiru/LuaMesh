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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.function.Function;

public class TestLuaMesh {

    static LuaFunction toFunc(Function<Varargs, Varargs> func) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return func.apply(args);
            }
        };
    }

    static LuaFunction toFunc(Runnable func) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                func.run();
                return LuaValue.NIL;
            }
        };
    }

    private Globals g;
    private FunctionCType ctype;

    @Before
    public void prepare() {
        LuaMesh.registerMeta(LuaObjectThing.class);
        LuaMesh.registerMeta(LuaImplementableThing.class);

        g = JsePlatform.debugGlobals();
        g.set("ctype", new FunctionCType());

        ctype = (FunctionCType) g.get("ctype");
    }

    @Test
    public void test() {
        g.set("obj", LuaObjectValue.of(new LuaObjectThing()));
        LuaValue obj = g.get("obj");

        // Make sure we're getting the right typename and function entries.
        assertEquals("luaObjectThing", ctype.call(obj).checkjstring());
        assertEquals(true, obj.get("voidMethod").isfunction());
        assertEquals(true, obj.get("intMethod").isfunction());
        assertEquals(true, obj.get("objMethod").isfunction());
        assertEquals(false, obj.get("notAMethod").isfunction());

        // something's wrong if these throw exceptions
        obj.get("voidMethod").call(obj);
        obj.get("intMethod").call(obj);
        obj.get("objMethod").call(obj);
        obj.get("objMethod").call(obj, obj);
        obj.get("objMethod").call(obj, obj, obj);
    }

    @Test
    public void testImplementable() {
        g.set("impl", LuaObjectValue.of(new LuaImplementableThing()));

        long time;
        LuaValue impl = g.get("impl");
        LuaImplementableThing obj = (LuaImplementableThing) ((LuaObjectValue<?>) impl).getObject();

        // test calling java from lua
        time = System.nanoTime();
        impl.get("implementable").call(impl);
        impl.get("implementablee").call(impl);
        time = System.nanoTime() - time;
        System.out.println("unimpl call time (probably inaccurate) (from lua) (ns): " + time);

        // test calling default java from java
        time = System.nanoTime();
        obj.implementable();
        obj.implementablee(null, null);
        time = System.nanoTime() - time;
        System.out.println("unimpl call time (from java) (ns): " + time);

        // replace it
        impl.set("implementable", toFunc(vargs -> {
            System.out.println("bbeeee");
            return LuaValue.valueOf(0);
        }));

        impl.set("implementablee", toFunc(vargs -> {
            System.out.println("bbcccc");
            return LuaValue.valueOf(0);
        }));

        // should have replaced it
        time = System.nanoTime();
        obj.implementable();
        obj.implementablee(null, null);
        time = System.nanoTime() - time;
        System.out.println("impl call time (ns): " + time);
    }

}
