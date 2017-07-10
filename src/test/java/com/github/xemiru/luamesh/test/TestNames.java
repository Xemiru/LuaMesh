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

import com.github.xemiru.luamesh.LuaObjectValue;
import com.github.xemiru.luamesh.LuaUtil;
import com.github.xemiru.luamesh.test.objects.ObjectNames;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import static com.github.xemiru.luamesh.LuaObjectValue.of;
import static com.github.xemiru.luamesh.test.Utility.init;
import static org.junit.Assert.*;
import static org.luaj.vm2.LuaValue.NIL;

public class TestNames {

    private Globals g;

    @Before
    public void before() {
        this.g = init();

        this.g.set("obj", of(new ObjectNames()));
    }

    @Test
    public void names() {
        LuaValue obj = g.get("obj");

        assertEquals("luanames", g.get("ctype").call(obj).checkjstring());
        assertEquals(NIL, obj.get("a"));
        assertNotEquals(NIL, obj.get("fielda"));

        assertEquals(NIL, obj.get("b"));
        assertNotEquals(NIL, obj.get("fieldb"));

        assertTrue(obj.get("fieldb").isfunction());
        assertTrue(obj.get("methoda").isfunction());
    }

}
