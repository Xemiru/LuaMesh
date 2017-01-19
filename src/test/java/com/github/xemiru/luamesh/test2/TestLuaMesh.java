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
package com.github.xemiru.luamesh.test2;

import static com.github.xemiru.luamesh.LuaObjectValue.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.xemiru.luamesh.FunctionCType;
import com.github.xemiru.luamesh.LuaMesh;
import com.github.xemiru.luamesh.test.LibraryA;
import com.github.xemiru.luamesh.test.LibraryB;
import com.github.xemiru.luamesh.test.TestA;
import com.github.xemiru.luamesh.test.TestB;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
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
    private LuaValue ctype;

    @Before
    public void prepare() {
        if (g == null) { // this only needs to happen once
            // in order of inheritance
            LuaMesh.register("com.github.xemiru.luamesh.test.TestA");
            LuaMesh.register("com.github.xemiru.luamesh.test.TestB");
            LuaMesh.register(LibraryA.class);
            LuaMesh.register(LibraryB.class);

            try {
                LuaMesh.init();
            } catch (Throwable e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }

            g = JsePlatform.debugGlobals();
            g.set("ctype", new FunctionCType());
            ctype = (FunctionCType) g.get("ctype");

            g.set("a", of(new TestA()));
            g.set("b", of(new TestB()));
            g.load(new LibraryA());
            g.load(new LibraryB());
        }
    }

    @Test
    public void names() {
        LuaValue a = g.get("a");
        LuaValue b = g.get("b");

        // Test names.
        assertEquals("TestA", ctype.call(a).tojstring()); // annot didnt override, should be original class name
        assertEquals("TestBB", ctype.call(b).tojstring()); // annot specifies TestBB

        // Test method name change.
        assertEquals(LuaValue.NIL, b.get("intMethod")); // should be gone
        assertNotEquals(LuaValue.NIL, b.get("luaintMethod")); // annot specifies luaintMethod
    }

    @Test
    public void methods() {
        LuaValue a = g.get("a");
        LuaValue b = g.get("b");

        try {
            // TestA methods
            a.get("voidMethod").call(a);
            assertNotEquals(null, a.get("objMethod").call(a, of(new Object())));

            // intMethod and override
            assertEquals(0, a.get("intMethod").call(a).checkint());
            assertEquals(LuaValue.NIL, b.get("intMethod")); // name override erases this
            assertEquals(1, b.get("luaintMethod").call(b).checkint()); // override in TestB

            // add metamethod
            assertEquals(b, a.add(b));
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            a.get("abstractMethod").call(a);
        } catch (LuaError e) {
            // expected
            a.set("abstractMethod", toFunc(() -> {
                System.out.println("Abstract method implemented by Lua.");
            }));
        }

        // should't fail this time
        a.get("abstractMethod").call(a);
    }

    @Test
    public void libraries() {
        LuaValue a = g.get("LibraryA");
        LuaValue b = g.get("libB"); // b was name overriden

        assertNotEquals(LuaValue.NIL, a);
        assertNotEquals(LuaValue.NIL, b);

        a.get("voidMethod").call();
        assertEquals(LuaValue.NIL, b.get("voidMethod")); // should have been removed by name override
        b.get("voidmethod").call();

        a.get("intMethod").call();
        b.get("intMethod").call();
    }
}
