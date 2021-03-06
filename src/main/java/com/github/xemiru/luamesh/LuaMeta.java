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

import static com.github.xemiru.luamesh.LuaMesh.debug;

import com.github.xemiru.luamesh.LuaType.MetaEntry;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.omg.CORBA.DynAnyPackage.Invalid;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
     * Quick member to perform member name enforcement.
     *
     * @param member the target member
     * @param override a name override, if any
     *
     * @return enforced name
     */
    static String convertMemberName(Member member, String override) {
        boolean noOverride = override == null || override.trim().isEmpty();
        String name = noOverride ? member.getName() : override;
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
    protected Map<String, Field> fields;
    private Set<String> meta;

    LuaMeta(Class<?> type, String name) {
        this.fields = new HashMap<>();
        this.names = new HashMap<>();
        this.meta = new HashSet<>();

        this.type = type;
        this.name = name;

        // generate metatable
        this.metatable = new LuaTable();

        // first, get the parents' stuff
        List<Class<?>> parents = new ArrayList<>();
        Class<?> parent = type.getSuperclass();
        while (parent != null && parent != Object.class) {
            parents.add(parent);
            parent = parent.getSuperclass();
        }

        // reverse the order so we dont fuck up overrides
        // clone parents' metatables
        while (!parents.isEmpty()) {
            Class<?> p = parents.get(parents.size() - 1);
            if (p.getDeclaredAnnotation(LuaType.class) != null) {
                LuaMeta meta = LuaMesh.getMeta(p);
                if (meta == null) {
                    throw new InvalidCoercionTargetException(String.format(
                            "Parent class %s of class %s has not been registered; could not inherit",
                            p.getName(), type.getName()));
                }

                LuaUtil.clone(this.metatable, meta.metatable, true);
                this.fields.putAll(meta.fields);
                this.names.putAll(meta.names);
                this.meta.addAll(meta.meta); // wew, meta meta
            }

            parents.remove(p);
        }

        // apply the target class's stuff
        if (LuaMesh.useTypeMetakey) {
            this.metatable.set("__type", LuaValue.valueOf(name));
        }

        LuaValue __index = this.metatable.get(LuaValue.INDEX);
        if (__index.isnil()) {
            __index = new LuaTable();
            this.metatable.set(LuaValue.INDEX, __index);
        }
    }

    /**
     * Constructor generating metadata based on
     * {@link LuaType} annotations found within the given
     * type's class.
     *
     * @param rannot the annotation of the class
     * @param delegate the class representing `type`, if
     *        unidirectional
     * @param type the class to create metadata with
     */
    LuaMeta(LuaType rannot, Class<?> delegate, Class<?> type) {
        this(type, convertClassName(type, (
                delegate.getDeclaredAnnotation(LuaType.class) != null
                        ? delegate.getDeclaredAnnotation(LuaType.class)
                        : rannot).name()));

        // safecheck the delegate
        Object dinstance = null;
        if (delegate != type) {
            try {
                dinstance = delegate.getDeclaredConstructor().newInstance();
            } catch(Exception e) {
                InvalidCoercionTargetException thrown =
                        new InvalidCoercionTargetException(String.format("delegate class %s does not have empty constructor", delegate.getName()));
                thrown.initCause(e);
                throw thrown;
            }
        }

        // register annotated methods and fields
        LuaValue __index = this.metatable.get(LuaValue.INDEX);

        for (Method dmethod : delegate.getDeclaredMethods()) {
            LuaType typeAnnot = dmethod.getDeclaredAnnotation(LuaType.class);
            boolean delMethod = false;
            Method method;

            try {
                method = type.getDeclaredMethod(dmethod.getName(), dmethod.getParameterTypes());
            } catch (NoSuchMethodException whoCares) {
                method = dmethod;
                delMethod = true;

                if(dmethod.getParameterTypes().length < 1 || dmethod.getParameterTypes()[0] != type) {
                    throw new InvalidCoercionTargetException("Delegate methods must have a self-reference as the first parameter");
                }

                debug(String.format("delegate class %s is registering its own method %s for target class %s", delegate.getName(), dmethod.getName(), type.getName()));
            }

            if (typeAnnot != null) {
                if (method.getReturnType().isArray()) {
                    // arrays not allowed
                    throw new InvalidCoercionTargetException("Cannot bind method returning array type");
                }

                String mName = method.getName();

                // apply the name override if its there
                // perform name enforcement
                String aName = typeAnnot.name().trim();

                // check if we need to replace, in case of override
                if (!aName.isEmpty() || !this.names.containsKey(mName)) {
                    aName = convertMemberName(method, aName);
                }

                // in case of override
                if (this.names.containsValue(aName)) {
                    if (typeAnnot.entry() != MetaEntry.INDEX) {
                        this.metatable.set(typeAnnot.entry().getKey(), LuaValue.NIL);
                        this.meta.remove(getKey(aName));
                        this.names.remove(getKey(aName)); // last, otherwise getKey doesn't work
                    } else {
                        __index.set(aName, LuaValue.NIL);
                    }
                }

                try {
                    // register
                    method.setAccessible(true);
                    LuaMethodBind lfunc = new LuaMethodBind(method);
                    if(delMethod) lfunc.dinstance = dinstance;

                    if (typeAnnot.entry() != MetaEntry.INDEX) {
                        this.metatable.set(typeAnnot.entry().getKey(), lfunc);
                        this.names.put(mName, typeAnnot.entry().getKey().tojstring());
                        this.meta.add(mName);
                    } else {
                        __index.set(aName, lfunc);
                        this.names.put(mName, aName);
                    }
                } catch (IllegalAccessException e) {
                    // let it cause a crash, this isn't good
                    throw new RuntimeException(e);
                }
            }
        }

        for (Field dfield : delegate.getDeclaredFields()) {
            LuaType typeAnnot = dfield.getDeclaredAnnotation(LuaType.class);
            Field field;

            try {
                field = type.getDeclaredField(dfield.getName());
            } catch (NoSuchFieldException whoCares) {
                throw new InvalidCoercionTargetException("Delegate classes cannot bind fields");
            }

            if (typeAnnot != null) {
                if (field.getType().isArray()) {
                    // arrays not allowed
                    throw new InvalidCoercionTargetException("Cannot bind field holding array type");
                }

                field.setAccessible(true); // for later
                String fName = field.getName();

                // perform name enforcement
                String aName = typeAnnot.name().trim();

                // check for override
                if (!aName.isEmpty() || !this.names.containsKey(fName)) {
                    aName = convertMemberName(field, aName);
                }

                // in case of override
                if (this.names.containsValue(aName)) {
                    if (!this.fields.containsKey(aName)) {
                        debug(String.format("field %s in class %s was not linked in favor of existing method of the same Lua name",
                                aName, type.getName()));
                        continue; // don't replace a method
                    }

                    if (typeAnnot.entry() != MetaEntry.INDEX) {
                        this.metatable.set(typeAnnot.entry().getKey(), LuaValue.NIL);
                        this.meta.remove(getKey(aName));
                        this.names.remove(getKey(aName));
                    } else {
                        __index.set(aName, LuaValue.NIL);
                    }
                }

                this.fields.put(aName, field);
                this.names.put(fName, aName);
                this.meta.add(fName);

                debug(String.format("field %s in class %s linked with Lua name %s", fName, type.getName(), aName));
            }
        }
    }

    /**
     * Constructor generating one-way metadata for the
     * methods contained within the given class.
     *
     * <p>The filter receives the names of candidate
     * methods within the given class to be written to
     * the Lua instance. It can return the same name, or
     * a different name to rename the method in the Lua
     * environment. It can also return null to signify
     * that the candidate method should not be written to
     * to the class's Lua counterpart.</p>
     *
     * <p>If the filter is null, all methods will be
     * registered with their default names.</p>
     *
     * @param type the class to generate metadata with
     * @param filter a filter determining which methods
     *        should be accessible from the Lua instance
     */
    LuaMeta(Class<?> type, Function<String, String> filter) {
        this(type, convertClassName(type, null));

        // register methods
        LuaValue __index = this.metatable.get(LuaValue.INDEX);
        for (Method method : type.getDeclaredMethods()) {
            String mname = convertMemberName(method, null);
            String name = filter == null ? mname : filter.apply(mname);
            if (name == null) {
                continue;
            }

            try {
                method.setAccessible(true);
                __index.set(name, new LuaMethodBind(method));
            } catch (IllegalAccessException e) {
                // let it cause a crash, this isn't good
                throw new RuntimeException(e);
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
     * Returns whether or not the provided member's Java
     * name was registered within the Lua objects' main
     * metatable (the one holding __index).
     *
     * @param memberName the name of the Java member
     *
     * @return if the member resides in the main metatable
     */
    public boolean isMeta(String memberName) {
        return meta.contains(memberName);
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

    //
    // private utility
    //

    private String getKey(String value) {
        for (String key : this.names.keySet()) {
            if (this.names.get(key).equals(value)) return key;
        }

        return null;
    }

    /**
     * Checks whether or not two methods are similar to
     * each other.
     *
     * <p>Similar to {@link Method#equals(Object)}, without
     * checking if both have the same declaring class.</p>
     *
     * @param a a method
     * @param b another method
     *
     * @return whether `a` and `b` are similar
     */
    private boolean methodsEqual(Method a, Method b) {
        if (a.getName().equals(b.getName())
                && a.getReturnType().equals(b.getReturnType())) {
            Class<?>[] params1 = a.getParameterTypes();
            Class<?>[] params2 = a.getParameterTypes();

            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i]) return false;
                }

                return true;
            }
        }

        return false;
    }

}
