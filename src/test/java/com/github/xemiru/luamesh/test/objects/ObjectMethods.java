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

import static com.github.xemiru.luamesh.test.Utility.println;

import com.github.xemiru.luamesh.LuaType;

import static com.github.xemiru.luamesh.LuaType.MetaEntry.*;

/**
 * Set up to test methods.
 */
@LuaType
public class ObjectMethods {

    @LuaType
    public int intMethod(int a, int b) {
        println("Int method adds two ints together.");
        return a + b;
    }

    @LuaType
    public double doubleMethod(double a, double b) {
        println("Double method adds two doubles together.");
        return a + b;
    }

    /*@LuaType // soon
    public int[] arrayMethod(int... array) {
        println("Array method returns a given int array back.");
        return array;
    }*/

    @LuaType
    public Object objectMethod(Object obj) {
        println("Object method returns the given object back.");
        return obj;
    }

    @LuaType
    public void voidMethod() {
        println("Void method does nothing.");
    }

    @LuaType(abstractt = true)
    public void abstractMethod() {
        println("This method body is ignored. An error is thrown if Java or Lua attempt to call it without Lua implementing it.");
    }

    public void invisibleMethod() {
        println("This doesn't appear in the Lua object.");
    }

    // metamethods

    @LuaType(entry = CALL)
    public void l_call() {
        println("The object itself was invoked in Lua.");
    }

    @LuaType(entry = TOSTRING)
    public String l_tostring() {
        println("The object was tostring'd in Lua.");
        return "poopy";
    }

    @LuaType(entry = LEN)
    public int l_length() {
        println("The object was length-checked in Lua.");
        return 495;
    }

    @LuaType(entry = CONCAT)
    public ObjectMethods l_concat(ObjectMethods other) {
        println("The object was concatenated in Lua.");
        return other;
    }

    @LuaType(entry = EQ)
    public boolean l_equals(ObjectMethods other) {
        println("The object was similarity-checked by Lua.");
        return true;
    }

    // arithmetic metamethods

    @LuaType(entry = UNM)
    public ObjectMethods l_neg() {
        println("The object was negated in Lua.");
        return this;
    }

    @LuaType(entry = ADD)
    public Object l_add(Object other) {
        println("The object was added in Lua.");
        return other;
    }

    @LuaType(entry = SUB)
    public Object l_sub(Object other) {
        println("The object was subtracted in Lua.");
        return other;
    }

    @LuaType(entry = MUL)
    public Object l_mul(Object other) {
        println("The object was multiplied in Lua.");
        return other;
    }

    @LuaType(entry = DIV)
    public Object l_div(Object other) {
        println("The object was divided in Lua.");
        return other;
    }

    @LuaType(entry = MOD)
    public Object l_mod(Object other) {
        println("The object was used with modulo in Lua.");
        return other;
    }

    @LuaType(entry = POW)
    public Object l_pow(Object other) {
        println("The object was raised to a power in Lua.");
        return other;
    }

    @LuaType(entry = LT)
    public Object l_lessthan(Object other) {
        println("The object was less in Lua.");
        return other;
    }

    @LuaType(entry = LE)
    public Object l_lessthanorequals(Object other) {
        println("The object was added in Lua.");
        return other;
    }
}
