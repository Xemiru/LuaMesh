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

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines class and class members able to be pushed into a
 * Lua environment.
 * 
 * <p>This annotation is <strong>ALWAYS</strong> processed
 * without considering inheritance, that is, if a class
 * member was annotated in a superclass, it will not be
 * considered annotated in its subclasses unless explicitly
 * redefined.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LuaType {

    /**
     * Denotes a list of targets within a Lua metatable
     * where a Java member reference might be placed.
     * 
     * <p>See http://lua-users.org/wiki/MetatableEvents for
     * information as to how each metatable entry
     * function.</p>
     */
    public static enum MetaEntry {
        // generic
        INDEX(LuaValue.INDEX), CALL(LuaValue.CALL), TOSTRING(LuaValue.TOSTRING), LEN(
            LuaValue.LEN), CONCAT(LuaValue.CONCAT), EQ(LuaValue.EQ),

        // mathematical
        UNM(LuaValue.UNM), ADD(LuaValue.ADD), SUB(LuaValue.SUB), MUL(LuaValue.MUL), DIV(
            LuaValue.DIV), MOD(LuaValue.MOD), POW(LuaValue.POW), LT(LuaValue.LT), LE(LuaValue.LE);

        private LuaString key;

        MetaEntry(LuaString key) {
            this.key = key;
        }

        public LuaString getKey() {
            return this.key;
        }
    }

    /**
     * Overrides the name of this object when its passed
     * into a Lua object's metatable.
     * 
     * <p>Names are completely ignored if this member is not
     * placed in {@link MetaEntry#INDEX}.</p>
     */
    String name() default "";

    /**
     * Sets where the member is placed in the object's
     * metatable entry. This is ignored when the target
     * element is a class/type.
     * 
     * <p>By default, members are placed in
     * {@link MetaEntry#INDEX}.</p>
     */
    MetaEntry entry() default MetaEntry.INDEX;

}
