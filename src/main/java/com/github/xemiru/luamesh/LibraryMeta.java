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
 * Metadata for {@link LuaLibrary} types.
 */
public class LibraryMeta {

    private String name;
    private Map<String, String> names;
    private Map<String, LuaMethodBind> methods;

    public LibraryMeta() {
        this.name = null;
        this.methods = null;
    }

    /**
     * Returns the Lua name set for the owning
     * {@link LuaLibrary}.
     * 
     * @return the library's Lua name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the Lua name for the owning {@link LuaLibrary}.
     * 
     * @param name the new Lua name
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the mapping of Java method names to Lua
     * function names.
     * 
     * @return the name mapping
     */
    public Map<String, String> getNames() {
        if (this.names == null) {
            this.names = new HashMap<>();
        }

        return this.names;
    }

    /**
     * Returns the mapping for names and method bindings.
     * 
     * <p>This is cloned into each instance of the owning
     * {@link LuaLibrary}.</p>
     * 
     * @return the method bindings for the owning LuaLibrary
     */
    public Map<String, LuaMethodBind> getBinds() {
        if (this.methods == null) {
            this.methods = new HashMap<>();
        }

        return this.methods;
    }
}
