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

import static com.github.xemiru.luamesh.test.Utility.func;
import static org.junit.Assert.*;
import static com.github.xemiru.luamesh.LuaObjectValue.of;
import static com.github.xemiru.luamesh.test.Utility.init;
import static org.luaj.vm2.LuaValue.NIL;
import static org.luaj.vm2.LuaValue.valueOf;

import com.github.xemiru.luamesh.LuaObjectValue;
import com.github.xemiru.luamesh.test.objects.UnidirectionalTarget;
import com.github.xemiru.luamesh.test.objects.UnidirectionalTargetB;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

public class TestUnidirectional {

    private Globals g;

    @Before
    public void before() {
        this.g = init();

        this.g.set("obj", of(new UnidirectionalTarget()));
        this.g.set("objb", of(new UnidirectionalTargetB()));
    }

    @Test
    public void unidirectional() {
        LuaValue obj = this.g.get("obj");
        UnidirectionalTarget ut = ((LuaObjectValue<UnidirectionalTarget>) obj).getObject();

        obj.get("doStuff").call(obj);
        assertEquals(5, obj.get("add").call(obj, valueOf(2), valueOf(3)).checkint());
        assertEquals(obj, obj.get("giveStuff").call(obj));

        obj.set("add", func(args -> valueOf(args.checkint(1) * args.checkint(2))));
        obj.set("giveStuff", func(args -> NIL));

        // make sure it doesn't care about the lua function changes
        assertEquals(5, ut.add(2, 3));
        assertEquals(ut, ut.giveStuff());
    }

    @Test
    public void unidirectionalB() {
        LuaValue obj = this.g.get("objb");
        UnidirectionalTargetB ut = ((LuaObjectValue<UnidirectionalTargetB>) obj).getObject();

        obj.get("doStuff").call(obj);
        assertEquals(5, obj.get("add").call(obj, valueOf(2), valueOf(3)).checkint());
        assertEquals(obj, obj.get("giveStuff").call(obj));

        obj.set("add", func(args -> valueOf(args.checkint(1) * args.checkint(2))));
        obj.set("giveStuff", func(args -> NIL));

        // make sure it doesn't care about the lua function changes
        assertEquals(5, ut.add(2, 3));
        assertEquals(ut, ut.giveStuff());

        // check for new metaentry added by delegate
        assertEquals(obj, obj.add(obj));
    }
}
