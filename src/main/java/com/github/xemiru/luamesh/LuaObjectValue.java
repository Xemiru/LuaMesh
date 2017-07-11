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

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

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
     * @param <T> the Java type held by the object value
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
            this.initializeFields();
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

    /**
     * Converts this LuaObjectValue into a library format; that is,
     * functions no longer require a self-reference to function and
     * can simply be called normally.
     *
     * <p>The original metatable is removed and replaced with a new
     * table that satisfies the library format. The old metatable
     * is shallowly-cloned. __index is also shallowly cloned. All
     * instances of a {@link LuaMethodBind} are copied and modified
     * to support the library format. The original metatable can
     * still be found in {@link #getMeta()}.</p>
     *
     * @return a library version of this LuaObjectValue
     */
    public LuaObjectValue<T> toLibrary() {
        LuaTable mt = new LuaTable();
        LuaTable __index = new LuaTable();

        Function<LuaValue, LuaValue> convert = v -> {
            if(v instanceof LuaMethodBind) {
                LuaMethodBind lmb = ((LuaMethodBind) v).clone();
                lmb.instance = this.ref;
                return lmb;
            } else {
                return v;
            }
        };

        LuaUtil.iterate(this.meta.getMetatable(), (k, v) -> {
            if(v.istable() && k.tojstring().equals("__index")) {
                LuaUtil.iterate(v.checktable(), (kk, vv) -> {
                    __index.set(kk, convert.apply(vv));
                });
            } else {
                mt.set(k, convert.apply(v));
            }
        });

        mt.set(LuaValue.INDEX, __index);
        this.setmetatable(mt);
        return this;
    }

    // ---------------- java/lua field sync ----------------

    // if IllegalAccessExceptions happen, just rte it and cause a crash
    // because we set it to accessible in LuaMeta and i'm not sure how
    // it happens if it happens after that

    @Override
    public LuaValue rawget(LuaValue key) {
        if(key.isstring() && this.meta.fields.containsKey(key.checkjstring())) {
            try {
                return LuaUtil.toLua(getField(ref, key.checkjstring()));
            } catch(IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            return super.rawget(key);
        }
    }

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        if(key.isstring() && this.meta.fields.containsKey(key.checkjstring())) {
            try {
                Field f = field(key.checkjstring());
                Object jvalue = LuaUtil.toJava(value, fromPrimitive(f.getType()));

                setField(ref, key.checkjstring(), jvalue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            super.rawset(key, value);
        }
    }

    private Field field(String aName) {
        return this.meta.fields.get(aName);
    }

    private void initializeFields() {
        try {
            for (String luaName : this.meta.fields.keySet()) {
                this.set(luaName, LuaUtil.toLua(getField(ref, luaName)));
            }
        } catch(IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> fromPrimitive(Class<?> clazz) {
        if(clazz.isPrimitive()) {
            if(clazz == boolean.class) return Boolean.class;
            if(clazz == byte.class) return Byte.class;
            if(clazz == char.class) return Character.class;
            if(clazz == short.class) return Short.class;
            if(clazz == int.class) return Integer.class;
            if(clazz == long.class) return Long.class;
            if(clazz == double.class) return Double.class;
            if(clazz == float.class) return Float.class;
        }

        return clazz;
    }

    private Object getField(T obj, String aName) throws IllegalAccessException {
        Field f = field(aName);
        if(f == null) { return null; }

        return f.get(obj);
    }

    private void setField(T obj, String aName, Object value) throws IllegalAccessException {
        Field f = field(aName);
        if(f == null) { return; }

        Class<?> ft = f.getType();
        if(value == null) {
            if (ft.isPrimitive()) {
                if (ft == boolean.class) {
                    f.set(obj, false);
                } else if(ft == byte.class) {
                    f.set(obj, 0x00);
                } else if(ft == char.class) {
                    f.set(obj, '\u0000');
                } else if(ft == short.class) {
                    f.set(obj, (short) 0);
                } else if (ft == int.class) {
                    f.set(obj, 0);
                } else if (ft == long.class) {
                    f.set(obj, (long) 0);
                } else if (ft == float.class) {
                    f.set(obj, 0.0F);
                } else if (ft == double.class) {
                    f.set(obj, 0.0D);
                }
            } else {
                f.set(obj, null);
            }
        } else {
            try {
                f.set(obj, value);
            } catch(IllegalArgumentException e) {
                throw new LuaError("invalid value for Java field; expected " + LuaMesh.getLuaName(ft));
            }
        }
    }

}
