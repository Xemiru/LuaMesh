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

import com.github.xemiru.luamesh.FunctionCType;
import com.github.xemiru.luamesh.LuaMesh;
import com.github.xemiru.luamesh.LuaObjectValue;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

public class TestLuaMesh {

    private Globals g;
    private LuaValue obj;
    private FunctionCType ctype;

    @Before
    public void prepare() {
        LuaMesh.registerMeta(LuaObjectThing.class);

        g = JsePlatform.debugGlobals();
        g.set("ctype", new FunctionCType());
        g.set("obj", LuaObjectValue.of(new LuaObjectThing()));

        ctype = (FunctionCType) g.get("ctype");
        obj = g.get("obj");
    }

    @Test
    public void test() {
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

}
