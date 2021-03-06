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
package com.github.xemiru.luamesh.test.objects;

import com.github.xemiru.luamesh.LuaType;

/**
 * Set up to test fields.
 */
@LuaType
public class ObjectFields {

    // test all primitives and generic object
    @LuaType public boolean bool = false;
    @LuaType public byte bytee = 0x00;
    @LuaType public char charr = '1';
    @LuaType public short shortt = (short) 4;
    @LuaType public int intt = 12;
    @LuaType public long longg = 69;
    @LuaType public double doublee = 12.4;
    @LuaType public float floatt = 12.6F;
    @LuaType public Object obj = new Object();

    // field will disappear; method takes priority
    @LuaType public int poopy = 124;
    @LuaType public void poopy() {}

}
