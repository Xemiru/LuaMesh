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

import com.github.xemiru.luamesh.LuaType;
import com.github.xemiru.luamesh.LuaType.MetaEntry;

@LuaType
class LuaObjectThing {

    @LuaType
    public void voidMethod() {
        // callable in Lua as :voidMethod()
        System.out.println("void method");
    }

    @LuaType
    int intMethod() {
        return 0; // callable in Lua as :intMethod(), returns lua integer
    }

    @LuaType
    public LuaObjectThing objMethod(LuaObjectThing a, LuaObjectThing b) {
        return new LuaObjectThing();
    }

    @LuaType(entry = MetaEntry.ADD)
    public LuaObjectThing add(LuaObjectThing a) {
        System.out.println("added and did nothing woo");
        return null;
    }

    @LuaType(abstractt = true)
    public void blankMethod() {}

    public void notAMethod() {}
}
