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

import com.github.xemiru.luamesh.LuaMesh;
import com.github.xemiru.luamesh.LuaType;
import com.github.xemiru.luamesh.LuaType.MetaEntry;

@LuaType
public class TestA {

    public static void println(String msg) {
        System.out.println(msg);
    }

    // =========================================
    // method test

    public void fuck(Object obj) {
        LuaMesh.lua(this, "fuck", () -> {
            System.out.println(obj);
        }, obj);
    }

    @LuaType
    public int intMethod() {
        println("Integer method returns 0.");
        return 0;
    }

    @LuaType
    public void voidMethod() {
        println("Void method does nothing.");
    }

    @LuaType
    public Object objMethod(Object obj) {
        return obj;
    }

    // =========================================
    // type annot tests

    @LuaType(abstractt = true)
    public boolean abstractMethod() {
        println("This shouldn't be printed out.");
        return true;
    }

    @LuaType(entry = MetaEntry.ADD)
    public TestA add(TestA b) {
        println("Addition method returns the second object.");
        return b;
    }

    public void notInLua() {
        println("This shouldn't be printed out from Lua.");
    }

}
