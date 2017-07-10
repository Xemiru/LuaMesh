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

import com.github.xemiru.luamesh.LuaUtil;
import com.github.xemiru.luamesh.test.objects.ObjectMethods;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import static com.github.xemiru.luamesh.LuaObjectValue.*;
import static com.github.xemiru.luamesh.test.Utility.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMethods {

    private Globals g;

    @Before
    public void before() {
        this.g = init();
        this.g.set("obj", of(new ObjectMethods()));
    }

    @Test
    public void methods() {
        LuaValue obj = this.g.get("obj");
        // LuaValue intArray = LuaUtil.toLua(new int[]{12, 16, 29, 44}); // soon

        assertEquals(25, obj.get("intMethod").call(obj, valueOf(12), valueOf(13)).checkint());
        assertEquals(15.5D, obj.get("doubleMethod").call(obj, valueOf(10.2), valueOf(5.3)).checkdouble(), 0F);
        assertEquals(obj, obj.get("objectMethod").call(obj, obj));
        obj.get("voidMethod").call(obj);

        try {
            obj.get("abstractMethod").call(obj);
            fail("Abstract call didn't fail.");
        } catch (LuaError e) {
            obj.set("abstractMethod", func(args -> {
                println("Abstract method implemented by Lua.");
                return NIL;
            }));
        }

        obj.get("abstractMethod").call(obj);
        assertEquals(NIL, obj.get("invisibleMethod"));

        // metamethods
        LuaValue other = of(new ObjectMethods());
        obj.call();

        assertEquals("poopy", g.get("tostring").call(obj).checkjstring());
        assertEquals(495, obj.length());
        assertEquals(other, obj.concat(other));
        assertEquals(true, obj.eq(other).checkboolean());

        // arithmetic metamethods
        assertEquals(obj, obj.neg());
        assertEquals(other, obj.add(other));
        assertEquals(other, obj.sub(other));
        assertEquals(other, obj.mul(other));
        assertEquals(other, obj.div(other));
        assertEquals(other, obj.mod(other));
        assertEquals(other, obj.pow(other));
        assertEquals(other, obj.lt(other));
        assertEquals(other, obj.lteq(other));
    }
}
