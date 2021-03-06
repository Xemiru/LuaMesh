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
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Utility class responsible for calling Java methods from
 * within Lua.
 */
public class LuaMethodBind extends VarArgFunction implements Cloneable {

    /**
     * Translates errors from Java into errors for Lua.
     *
     * @param ex the exception to translate
     *
     * @return the error message to pass
     */
    static String translateException(Throwable ex) {
        if (ex instanceof ClassCastException) {
            String[] msg = ex.getMessage().split(" ");
            String given = msg[2];
            String expected = msg[4];
            String lex = LuaMesh.getLuaName(expected);
            if(lex.equals("<unknown type>")) {
                lex = "<uncoercible Java type " + expected + ">";
            }

            return String.format("bad argument: %s expected, got %s", lex,
                    LuaMesh.getLuaName(given));
        }

        String msg = ex.getClass().getName();
        return ex.getMessage() == null ? msg : msg.concat(": ".concat(ex.getMessage()));
    }

    private MethodHandle mh;
    private boolean[] numtypes;
    private int paramCount;
    private boolean staticc;
    protected Object dinstance;
    protected Object instance;

    private LuaMethodBind() {
    }

    public LuaMethodBind(Method method) throws IllegalAccessException {
        this.mh = MethodHandles.lookup().unreflect(method);
        this.staticc = Modifier.isStatic(method.getModifiers());
        Class<?>[] types = method.getParameterTypes();
        this.paramCount = types.length;
        this.instance = null;

        this.numtypes = new boolean[this.paramCount];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Float.class) {
                this.numtypes[i] = true;
                continue;
            }

            this.numtypes[i] = false;
        }
    }

    public Varargs invoke(Object obj, Varargs args) {
        // gather parameters
        Object[] params = new Object[staticc ? paramCount : paramCount + 1];
        int offset = 0;
        for (int i = 0; i < params.length; i++) {
            if(dinstance == null) {
                if (!staticc && i == 0 && obj != null) {
                    params[0] = obj;
                    continue;
                }
            } else {
                if (!staticc && i == 0) {
                    params[0] = dinstance;
                    offset -= 1;
                    continue;
                }

                if (!staticc && i == 1 && obj != null) {
                    params[1] = obj;
                    continue;
                }
            }

            LuaValue v = args.arg((obj == null || staticc ? i + 1 : i) + offset);
            if (v.isnil()) {
                params[i] = null;
            } else {
                // turn them into Java objects
                if (staticc) {
                    params[i] = LuaUtil.toJava(v, numtypes[i]);
                } else {
                    params[i] = LuaUtil.toJava(v, i == 0 ? false : numtypes[i - 1]);
                }
            }
        }

        if (!staticc && params[0] == null) {
            // pretend to be a lua function trying to reference its omitted first param :^)
            throw new LuaError("attempt to index local 'self' (was not passed Java object)");
        }

        try {
            return LuaUtil.toLua(mh.invokeWithArguments(params));
        } catch (Throwable e) {
            if (e instanceof LuaError) {
                throw (LuaError) e; // ignore it
            }

            throw new LuaError(translateException(e));
        }
    }

    @Override
    public Varargs invoke(Varargs args) {
        return this.invoke(instance, args);
    }

    @Override
    public LuaMethodBind clone() {
        LuaMethodBind lmb = new LuaMethodBind();

        lmb.mh = this.mh;
        lmb.numtypes = this.numtypes;
        lmb.paramCount = this.paramCount;
        lmb.staticc = this.staticc;
        lmb.instance = this.instance;

        return lmb;
    }

}
