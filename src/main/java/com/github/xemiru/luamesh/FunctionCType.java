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

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

/**
 * ctype() function, meant to be injected into a Lua
 * environment while using LuaMesh. Or at least, its
 * recommended to.
 * 
 * <p>ctype() will allow users of the program utilizing
 * LuaMesh to query for the internal typename of a
 * LuaObjectValue. The return value of this function is
 * dictated by the following instructions:</p>
 * 
 * <ul>
 * <li>If the Lua object's metatable contains a __type metakey, its value is returned.</li>
 * <li>If the Lua object is a LuaObjectValue, its meta is queried for its typename.</li>
 * <li>If the Lua object is a LuaObjectValue, but does not have a meta, its held Java object's class name is name-enforced and returned.</li>
 * <li>Otherwise, works similarly to Lua's built-in type() function.</li>
 * </ul>
 * 
 * <p>Injection of this function into a Lua environment
 * typically is done as seen below.</p>
 * 
 * <pre>
 * Globals g = ...;
 * 
 * // ...
 * 
 * g.set("ctype", new FunctionCType());
 * </pre>
 */
public class FunctionCType extends OneArgFunction {

    static final LuaValue TYPE_JOBJ = LuaValue.valueOf("jobj");

    @Override
    public LuaValue call(LuaValue arg) {
        LuaValue typename = null;
        if (!arg.getmetatable().isnil()) {
            typename = arg.getmetatable().get("__type");
        }

        if (typename.isnil()) {
            if (arg instanceof LuaObjectValue) {
                typename = LuaValue.valueOf(((LuaObjectValue<?>) arg).getTypename());
            }
        }

        if (typename.isnil()) {
            typename = LuaValue.valueOf(arg.typename());
        }

        return typename;
    }

}
