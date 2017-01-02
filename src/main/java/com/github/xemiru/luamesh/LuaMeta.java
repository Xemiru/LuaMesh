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

import com.github.xemiru.luamesh.LuaType.MetaEntry;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains metadata for a Lua-coercible Java type.
 */
public class LuaMeta {

    /**
     * Performs name enforcement based on options set in
     * {@link LuaMesh} on a given name.
     */
    static String convertName(String name) {
        String n = name;
        if (LuaMesh.enforceFirstLower) {
            n = Character.toLowerCase(n.charAt(0)) + n.substring(1);
        }

        if (LuaMesh.enforceUnderscore) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n.length(); i++) {
                char ch = n.charAt(i);
                if (i == 0) {
                    sb.append(ch);
                    continue;
                }

                if (Character.isUpperCase(ch)) {
                    sb.append('_');
                    sb.append(Character.toLowerCase(ch));
                } else {
                    sb.append(ch);
                }
            }

            n = sb.toString();
        }

        if (LuaMesh.enforceLowercase) {
            n = n.toLowerCase();
        }

        return n;
    }

    /**
     * Quick method to perform class name enforcement.
     * 
     * @param type the target class
     * @param override a name override, if any
     * 
     * @return enforced name
     */
    static String convertClassName(Class<?> type, String override) {
        boolean noOverride = override == null || override.trim().isEmpty();
        String name = noOverride ? type.getSimpleName() : override;
        if (LuaMesh.enforcementOption == 1 || LuaMesh.enforcementOption == 3) {
            if (noOverride || LuaMesh.enforceOverrides) {
                name = convertName(name);
            }
        }

        return name;
    }

    /**
     * Quick method to perform method name enforcement.
     * 
     * @param method the target method
     * @param override a name override, if any
     * 
     * @return enforced name
     */
    static String convertMethodName(Method method, String override) {
        boolean noOverride = override == null || override.trim().isEmpty();
        String name = noOverride ? method.getName() : override;
        if (LuaMesh.enforcementOption == 2 || LuaMesh.enforcementOption == 3) {
            if (noOverride || LuaMesh.enforceOverrides) {
                name = convertName(name);
            }
        }

        return name;
    }

    private Class<?> type;
    private LuaTable metatable;
    private String name;
    private Map<String, String> names;

    public LuaMeta(LuaType rannot, Class<?> type) {
        this.names = new HashMap<>();

        LuaType annot = rannot;
        if (type.getDeclaredAnnotation(LuaType.class) != null) {
            annot = type.getDeclaredAnnotation(LuaType.class);
        }

        this.type = type;

        // perform name enforcement
        this.name = convertClassName(type, annot.name());

        // generate the metatable
        this.metatable = new LuaTable();
        if (LuaMesh.useTypeMetakey) {
            this.metatable.set("__type", LuaValue.valueOf(name));
        }

        // first, get the parents' stuff
        List<Class<?>> parents = new ArrayList<>();
        Class<?> parent = type.getSuperclass();
        while (parent != null && parent != Object.class) {
            parents.add(parent);
            parent = type.getSuperclass();
        }

        // reverse the order so we dont fuck up overrides
        // clone parents' metatables
        while (!parents.isEmpty()) {
            LuaMeta meta = LuaMesh._registerMeta(parents.get(parents.size() - 1));
            if (meta != null) {
                LuaUtil.clone(this.metatable, meta.metatable, true);
                this.names.putAll(meta.names);
            }
        }

        // apply the target class's stuff
        LuaValue __index = this.metatable.get(LuaValue.INDEX);
        if (__index.isnil()) {
            __index = new LuaTable();
            this.metatable.set(LuaValue.INDEX, __index);
        }

        for (Method method : type.getDeclaredMethods()) {
            LuaType typeAnnot = method.getAnnotation(LuaType.class);
            if (method.getDeclaredAnnotation(LuaType.class) != null) {
                typeAnnot = method.getDeclaredAnnotation(LuaType.class);
            }

            if (typeAnnot != null) {

                // verify that the method is compatible
                /*
                 * for (Class<?> paramType :
                 * method.getParameterTypes()) { if
                 * (!LuaUtil.isConvertable(paramType)) {
                 * throw new InvalidCoercionTargetException(
                 * String.
                 * format("Method %s of class %s has uncoercible parameter type %s"
                 * , method.getName(), type.getName(),
                 * paramType.getName())); } }
                 * 
                 * if (!LuaUtil.isConvertable(method.
                 * getReturnType())) { throw new
                 * InvalidCoercionTargetException( String.
                 * format("Method %s of class %s has uncoercible return type %s"
                 * , method.getName(), type.getName(),
                 * method.getReturnType().getName())); }
                 */

                String mName = method.getName();

                // in case of override
                if (this.names.containsKey(mName)) {
                    this.metatable.set(this.names.get(mName), LuaValue.NIL);
                }

                // apply the name override if its there
                // perform name enforcement
                String aName = typeAnnot.name().trim();

                // check if we need to replace, in case of override
                if(!aName.isEmpty() || !this.names.containsKey(mName)) {
                    String name = convertMethodName(method, aName);
                    this.names.put(mName, name);
                }

                try {
                    // register
                    LuaMethodBind lfunc = new LuaMethodBind(method);
                    if (typeAnnot.entry() != MetaEntry.INDEX) {
                        this.metatable.set(typeAnnot.entry().getKey(), lfunc);
                    } else {
                        __index.set(mName, lfunc);
                    }
                } catch (IllegalAccessException e) {
                    // let it cause a crash, this isn't good
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Returns the name associated with the owning LuaType
     * class.
     * 
     * @return the Lua name of the associated class
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the object type targetted by this
     * {@link LuaMeta}.
     * 
     * @return the type targetted by this LuaMeta
     */
    public Class<?> getTargetType() {
        return this.type;
    }

    /**
     * Returns the full Lua metatable for this
     * {@link LuaMeta} and its associated object type.
     * 
     * <p>This generally should not be modified by anything
     * outside of the LuaMeta itself.</p>
     * 
     * @return this LuaMeta's metatable
     */
    public LuaTable getMetatable() {
        return this.metatable;
    }

    /**
     * Returns the Lua name of the provided member's Java
     * name.
     * 
     * @param memberName the name of the Java member
     * 
     * @return the name of the Lua member
     */
    public String getLuaName(String memberName) {
        return this.names.get(memberName);
    }

}
