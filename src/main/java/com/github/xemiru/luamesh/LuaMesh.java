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

import java.util.HashMap;
import java.util.Map;

/**
 * Primary class for LuaMesh.
 * 
 * <p>Public static variables within this class configure
 * behaviors of name enforcement with Lua-coercible types,
 * when translated into a Lua environment. These must be set
 * before registration of any Lua-coercible classes.</p>
 * 
 * <p>LuaMesh provides an extra function for receiving
 * typenames of Java objects within Lua. See
 * {@link FunctionCType}.</p>
 */
public class LuaMesh {

    /**
     * Name enforcement option.
     * 
     * <ul> <li>1 - Class names only.</li> <li>2 - Method
     * names only.</li> <li>3 - Class and method names.</li>
     * </ul>
     */
    public static short enforcementOption = 1;
    /**
     * Denotes whether or not name overrides are also
     * enforced.
     */
    public static boolean enforceOverrides = false;
    /**
     * Denotes enforcement of the first character in a name
     * being lowercased.
     */
    public static boolean enforceFirstLower = true;
    /**
     * Denotes enforcement of underscoring versus
     * camelcasing.
     */
    public static boolean enforceUnderscore = false;
    /** Denotes enforcement of lowercasing. */
    public static boolean enforceLowercase = false;
    /**
     * Denotes whether or not Lua objects will store their
     * typename in their `_type` metakey.
     */
    public static boolean useTypeMetakey = true;

    private static Map<String, String> names;
    private static Map<Class<?>, LuaMeta> metas;

    static {
        metas = new HashMap<>();
        names = new HashMap<>();
    }

    /**
     * Registers a class as Lua-coercible, and returns the
     * registered {@link LuaMeta}.
     * 
     * @param clazz the class to register
     * 
     * @return the LuaMeta returned
     * 
     * @throws IllegalArgumentException if clazz is not
     *         annotated with LuaType
     */
    public static LuaMeta registerMeta(Class<?> clazz) {
        if (metas.containsKey(clazz)) {
            return metas.get(clazz);
        }

        LuaType typeAnnot = clazz.getDeclaredAnnotation(LuaType.class);
        if (typeAnnot != null) {
            LuaMeta meta = new LuaMeta(typeAnnot, clazz);
            metas.put(clazz, meta);
            names.put(clazz.getName(), meta.getName());

            return meta;
        }

        throw new IllegalArgumentException(
            "Lua-coercible classes and members must be annotated with LuaType");
    }

    /**
     * Returns the Lua name of the given class.
     * 
     * @param clazz the class to get the name for
     * 
     * @return the Lua name of clazz, or "nil" if null
     */
    public static String getLuaName(Class<?> clazz) {
        return getLuaName(clazz == null ? null : clazz.getName());
    }

    /**
     * Returns the Lua name of the given class name, in the
     * format returned by {@link Class#getName()}.
     * 
     * <p>If the given class has no associated name, this
     * instead returns {@code "<unknown type>"}.</p>
     * 
     * @param name the class name to get the name for
     * 
     * @return the Lua name of the given class, or "nil" if
     *         passed null
     */
    public static String getLuaName(String name) {
        if (name == null) {
            return "nil";
        } else if (name.startsWith("java.lang.")) {
            if (name.equals("java.lang.Integer") || name.equals("java.lang.Short")
                || name.equals("java.lang.Long")) {
                return "integer";
            }

            if (name.equals("java.lang.Float") || name.equals("java.lang.Double")) {
                return "number";
            }

            return name.split("\\.")[2].toLowerCase();
        }

        String n = names.get(name);
        return n == null ? "<unknown type>" : n;
    }

    /**
     * Retrieves the {@link LuaMeta} of a class, or null if
     * not registered as Lua-coercible.
     * 
     * @param clazz the class to retrieve LuaMeta for
     * 
     * @return clazz's LuaMeta, or null if not registered
     */
    public static LuaMeta getMeta(Class<?> clazz) {
        if (metas.containsKey(clazz)) {
            return metas.get(clazz);
        }

        return null;
    }

}
