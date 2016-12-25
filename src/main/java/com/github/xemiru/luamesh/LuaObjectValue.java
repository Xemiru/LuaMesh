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

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Generic container for Java objects to be passable as Lua
 * objects.
 *
 * @param <T> the type of the Java object to contain
 */
public class LuaObjectValue<T> extends LuaTable {

    static final Map<Object, WeakReference<LuaObjectValue<?>>> STORED;

    static {
        STORED = new WeakHashMap<>();
    }

    /**
     * Returns a {@link LuaObjectValue} holding the provided
     * object.
     * 
     * <p>If the given object is already held by an existing
     * object value, the existing instance is returned.
     * Otherwise, a new one is generated, registered and
     * returned.</p>
     * 
     * @param object the Object for the value to hold
     * 
     * @return the LuaObjectValue holding the provided
     *         object
     */
    @SuppressWarnings("unchecked")
    public static <T> LuaObjectValue<T> of(T object) {
        if (object == null) {
            return null;
        }

        if (LuaObjectValue.STORED.containsKey(object)) {
            if (LuaObjectValue.STORED.get(object).get() != null) {
                return (LuaObjectValue<T>) LuaObjectValue.STORED.get(object).get();
            }

            LuaObjectValue.STORED.remove(object);
        }

        return new LuaObjectValue<>(object);
    }

    /**
     * Returns the {@link LuaObjectValue} representation of
     * a Java object, or nil if it could not be created.
     * 
     * <p>Used for anything wanting to ensure null object
     * values don't cause an exception.</p>
     * 
     * @param obj the object to wrap
     * 
     * @return a LuaObjectValue, or nil
     */
    public static LuaValue orNil(Object obj) {
        LuaObjectValue<?> val = LuaObjectValue.of(obj);
        if (val == null) {
            return LuaValue.NIL;
        }

        return val;
    }

    // ---------------- object ----------------

    private String typename;
    private LuaMeta meta;
    private T ref;

    private LuaObjectValue(T object) {
        this.ref = object;

        LuaObjectValue.STORED.put(object, new WeakReference<>(this));
        LuaMeta meta = LuaMesh.getMeta(object.getClass());
        if (meta != null) {
            this.meta = meta;
            this.typename = meta.getName();
            this.setmetatable(meta.getMetatable());
        } else {
            this.typename = LuaMeta.convertClassName(object.getClass(), null);
        }
    }

    // ---------------- g/s object params ----------------

    /**
     * Returns the typename associated with this
     * LuaObjectValue.
     * 
     * <p>This does not function similarly to
     * {@link LuaValue#typename()}.</p>
     * 
     * @return this LuaObjectValue's typename
     */
    public String getTypename() {
        return this.typename;
    }

    /**
     * Returns the Java object held by this
     * {@link LuaObjectValue}.
     * 
     * @return the Java object held by this value
     */
    public T getObject() {
        return this.ref;
    }

    /**
     * Returns the {@link LuaMeta} assigned to this
     * {@link LuaObjectValue} as its primary identifier.
     * 
     * @return the LuaObjectMeta assigned to this object
     *         value
     */
    public LuaMeta getMeta() {
        return this.meta;
    }
}
