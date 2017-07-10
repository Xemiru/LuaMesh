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

import com.github.xemiru.luamesh.test.objects.ObjectFields;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import static com.github.xemiru.luamesh.LuaObjectValue.of;
import static com.github.xemiru.luamesh.test.Utility.init;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.luaj.vm2.LuaValue.valueOf;

public class TestFields {

    private Globals g;

    @Before
    public void before() {
        g = init();
    }

    @Test
    public void fields() {
        ObjectFields jobj = new ObjectFields();
        LuaValue obj = of(jobj);
        g.set("obj", obj);
        obj = g.get("obj");

        assertEquals(false, obj.get("bool").checkboolean());
        obj.set("bool", valueOf(true));
        assertEquals(true, jobj.bool);

        assertEquals(0x00, obj.get("bytee").checkint());
        obj.set("bytee", 15);
        assertEquals(0x0F, jobj.bytee);

        assertEquals('1', obj.get("charr").checkint());
        obj.set("charr", 'a');
        assertEquals('a', jobj.charr);

        assertEquals(4, obj.get("shortt").checkint());
        obj.set("shortt", 12);
        assertEquals(12, jobj.shortt);

        assertEquals(12, obj.get("intt").checkint());
        obj.set("intt", 64);
        assertEquals(64, jobj.intt);

        assertEquals(69, obj.get("longg").checklong());
        obj.set("longg", 495);
        assertEquals(495, jobj.longg);

        assertEquals(12.4, obj.get("doublee").checkdouble(), 0F);
        obj.set("doublee", 44.9);
        assertEquals(44.9, jobj.doublee, 0F);

        assertEquals(12.6F, obj.get("floatt").checknumber().tofloat(), 0F);
        obj.set("floatt", 99.4);
        assertEquals(99.4F, jobj.floatt, 0F);

        Object objj = new Object();
        assertEquals(of(jobj.obj), obj.get("obj"));
        obj.set("obj", of(objj));
        assertEquals(objj, jobj.obj);

        assertTrue(obj.get("poopy").isfunction());
    }

}
