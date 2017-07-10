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

import static org.luaj.vm2.LuaValue.*;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.function.BiConsumer;

/**
 * Utility class for interfacing with Lua objects.
 */
public class LuaUtil {

    /**
     * Shallowly clones a given {@link LuaTable} into a new
     * table, and returns it.
     * 
     * @param target the LuaTable to clone
     * 
     * @return the table clone
     */
    public static LuaTable clone(LuaTable target) {
        return clone(null, target, false);
    }

    /**
     * Shallowly clones a given {@link LuaTable} into the
     * provided table, and returns it.
     * 
     * @param into the table to clone into, or null to use a
     *        new table
     * @param target the table to clone
     * 
     * @return into, with the contents of the target table
     */
    public static LuaTable clone(LuaTable into, LuaTable target) {
        return clone(into, target, false);
    }

    /**
     * Clones a given {@link LuaTable} into the provided
     * table, and returns it.
     * 
     * @param into the table to clone into, or null to use a
     *        new table
     * @param target the table to clone
     * @param deep whether or not to deep-clone
     * 
     * @return into, with the contents of the target table
     */
    public static LuaTable clone(LuaTable into, LuaTable target, boolean deep) {
        LuaTable table = into == null ? new LuaTable() : into;
        iterate(target, (k, v) -> {
            // deep-cloning tables?
            if (v.istable() && deep) {
                if (table.get(k).istable()) {
                    // if its already there, clone into it
                    clone(table.get(k).checktable(), v.checktable(), true);
                } else {
                    // if its not a table or its not even there, make a new one
                    table.set(k, clone(null, v.checktable(), true));
                }
            } else {
                // just set the value
                table.set(k, v);
            }
        });

        return table;
    }

    public static void iterate(LuaTable tab, BiConsumer<LuaValue, LuaValue> consumer) {
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = tab.next(k);
            k = n.arg1();

            if (k.isnil()) {
                break;
            }

            consumer.accept(n.arg1(), n.arg(2));
        }
    }

    /**
     * Turns a Lua object into its corresponding Java
     * object.
     * 
     * <p>If the object cannot be converted, it is returned
     * as it was given, as a {@link LuaValue}.</p>
     * 
     * @param obj the LuaValue to convert
     * @param flt whether to convert decimal numbers into
     *        floats, otherwise doubles
     *
     * @return the corresponding Java object, or
     *         <code>obj</code> if unable to convert
     */
    public static Object toJava(LuaValue obj, boolean flt) {
        if (obj.isnil()) {
            return null;
        } else if (obj instanceof LuaObjectValue) {
            return ((LuaObjectValue<?>) obj).getObject();
        } else if (obj.isint()) {
            return obj.checkint();
        } else if (obj.isnumber()) {
            return flt ? (float) obj.checkdouble() : obj.checkdouble();
        } else if (obj.isboolean()) {
            return obj.checkboolean();
        } else if (obj.isstring()) {
            return obj.checkjstring();
        } else {
            return obj;
        }
    }

    public static Object toJava(LuaValue obj, Class<?> targetClass) {
        switch(targetClass.getSimpleName()) {
            case "Long": return (long) obj.checkint();
            case "Integer": return obj.checkint();
            case "Short": return (short) obj.checkint();
            case "Double": return obj.checkdouble();
            case "Float": return obj.checknumber().tofloat();
            case "Boolean": return obj.checkboolean();
            case "Byte": return (byte) obj.checkint();
            case "Character": return (char) obj.checkint();
            default: return toJava(obj, targetClass.getSimpleName().equals("Float"));
        }
    }

    /**
     * Converts the provided Java object into its
     * corresponding Lua object.
     * 
     * <p>A non-primitive object is turned into a
     * {@link LuaObjectValue}.</p>
     * 
     * <p>Arrays are turned into Lua arrays, that is, a
     * table with numerical keys.</p>
     * 
     * @param obj the object to convert
     * 
     * @return the converted object
     */
    public static LuaValue toLua(Object obj) {
        if (obj == null) {
            return LuaValue.NIL;
        } else if (obj instanceof Object[]) {
            LuaTable tab = new LuaTable();
            Object[] array = (Object[]) obj;
            for (int i = 0; i < array.length; i++) {
                tab.set(i + 1, toLua(array[i]));
            }

            return tab;
        } else {
            if (obj instanceof Integer) {
                return LuaValue.valueOf((int) obj);
            } else if (obj instanceof Double) {
                return LuaValue.valueOf((double) obj);
            } else if (obj instanceof Float) {
                return LuaValue.valueOf((float) obj);
            } else if (obj instanceof Boolean) {
                return LuaValue.valueOf((boolean) obj);
            } else if (obj instanceof String) {
                return LuaValue.valueOf((String) obj);
            } else if(obj instanceof Byte) {
                return LuaValue.valueOf((byte) obj);
            } else if(obj instanceof Character) {
                return LuaValue.valueOf((char) obj);
            } else if(obj instanceof Long) {
                return LuaValue.valueOf((long) obj);
            } else if(obj instanceof Short) {
                return LuaValue.valueOf((short) obj);
            } else {
                return LuaObjectValue.of(obj);
            }
        }
    }

    /**
     * Queries whether or not a given type has been
     * registered as a valid Lua-coercible type.
     * 
     * @param type the type to test
     * 
     * @return if the type is considered a valid
     *         Lua-coercible type
     */
    public static boolean isConvertable(Class<?> type) {
        if (type.isPrimitive() && type != Byte.class) {
            return true;
        }

        return type.getDeclaredAnnotation(LuaType.class) != null;
    }

    /**
     * Prints the contents of a Lua table in <code>key:
     * value</code> form.
     * 
     * <p>Keys and values are printed after being passed
     * through Lua's <code>tostring</code> function.</p>
     * 
     * @param tab the table to print
     */
    public static void printTable(LuaTable tab) {
        iterate(tab, (k, v) -> {
            System.out.println(k.tojstring() + ": " + v.tojstring());
        });
    }

    public static Object box(Object obj) {
        if(obj.getClass().isPrimitive()) {
            System.out.println("get box'd");
            switch(obj.getClass().getName()) {
                case "byte": return Byte.valueOf((byte) obj);
                case "float": return Float.valueOf((float) obj);
                case "double": return Double.valueOf((double) obj);
                case "short": return Short.valueOf((short) obj);
                case "int": return Integer.valueOf((int) obj);
                case "long": return Long.valueOf((long) obj);
                case "boolean": return Boolean.valueOf((boolean) obj);
                case "char": return Character.valueOf((char) obj);
            }
        }

        return obj;
    }
}
