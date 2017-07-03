# LuaMesh

Mesh your Java objects into a Lua environment using LuaJ.

## Importing into your project

The project can be retrieved as an artifact from the [Maven Central repository.](https://search.maven.org/#artifactdetails%7Ccom.github.xemiru%7Cluamesh%7C1.2%7Cjar)

#### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile 'com.github.xemiru:luamesh:1.2'
}
```

#### Maven
```xml
<dependency>
    <groupId>com.github.xemiru</groupId>
    <artifactId>luamesh</artifactId>
    <version>1.2</version>
</dependency>
```

## Usage

All Java documentation is available at [http://xemiru.github.io/LuaMesh/latest](http://xemiru.github.io/LuaMesh/latest).

Need them for a specific version? Replace `latest` with the version you need. All documentation pages are in the [gh-pages branch](https://github.com/Xemiru/LuaMesh/tree/gh-pages).

----

LuaMesh allows you to pass your Java objects cleanly into Lua environments, managed by LuaJ. The main classes to perform this task are `LuaMesh`, `LuaType`, and `LuaObjectValue`.

All Java objects, by default, can be passed into the Lua environment when wrapped by a `LuaObjectValue`. This however only serves to let an object be carried into Lua and back with no other functionality; the Lua object will not have Lua functions that call corresponding Java methods by default.

```java
Globals g = ...;
Object obj = ...;

g.set("javaobj", LuaObjectValue.of(obj));
```

For the passed object to have usable methods, the object's class and Lua-accessible methods must be annotated with `@LuaType`.

```java
package luamesh.is.ok.iguess;

@LuaType
public class MeshedObject {

    @LuaType
    public void myMethod() {
        System.out.println("Hello, world!");
    }

}
```

Afterwards, LuaMesh needs to be told which classes are annotated with LuaType. This can be done by passing the classes' fully qualified name to the `register` method.

Once all classes are listed, the `init` method is called.

```java
public class Main {

    public static void main(String[] args) {
        LuaMesh.register("luamesh.is.ok.iguess.MeshedObject");

        // ... other classes

        LuaMesh.init();
    }

}
```

It is recommended that LuaMesh be the **FIRST** thing to be set up, as class transformations that inject the necessary code require that the target classes not be loaded by the Java Virtual Machine prior. This means references to the classes at any point must not occur before the `init` method (which means something like `LuaMesh.register(MyClass.class.getName())` would be illegal).

## Implementable Methods

The method can request LuaMesh to ensure that the object is required to have its function implemented by the object itself. This can be done by setting the `abstractt` flag in the `LuaType` annotation.

```java
@LuaMesh(abstractt = true)
public void abstractMethod() {
    // empty body; LuaMesh will completely ignore this regardless
}
```

## Libraries

Libraries are essentially the Java equivalent of Lua modules -- a table of functions. The only difference being that LuaMesh will not ensure that there is only a single instance ever of a library; that is up to the person writing the integration.

The setup for libraries are exactly the same as annotated Lua-coercible classes, only that they are required to extend the LuaLibrary class. Libraries can then be created by using `LuaObjectValue.of(Object)`.

```java
package luamesh.is.ok.iguess;

@LuaType
public class MeshedLibrary extends LuaLibrary {

    @LuaType
    public void myMethod() {
        System.out.println("Hello, world!");
    }

}
```

## Unidirectional Bindings

By default, bindings created by classes annotated with the @LuaType annotation are bi-directional -- that is, the state of the Lua object's inherited functions affect how the Java object works and vice-versa. The default inherited Lua function calls the Java method, but when replaced, Java will instead call the Lua function. An exception is if the Java method is marked implementable, in which case the Java method has no implementation and will actually error if it does not have a Lua implementation when called.

In cases where it is not possible to annotate the class with @LuaType to add Lua bindings, one can instead make a unidirectional binding. A unidirectional binding can only let Lua call to Java; the Java object has no awareness of a Lua implementation.

A unidirectional binding can be made simply by registering it using `LuaMesh.register(Class, Function<String, String>)`. The metadata is generated automagically, and one can configure which methods are passed to Lua using the filter parameter. The target class can safely be loaded prior to calling LuaMesh.init(), as no injections take place.

```java
LuaMesh.register(MyClass.class, name -> {
    switch(name) {
        case "methodA": return "functionA"; // rename the function
        case "methodB": return null; // don't give the function to lua
        default: return name; // pass all functions with their names as-is
    }
})
```

The filter parameter takes a function consuming the name of a candidate method to be given to a Lua object, and returning the new name of the function or null if it is not to be given to the object. The filter does not check for conflicts.

After registering a unidirectional binding, Lua values for instances of the registered class can be generated as usual using LuaObjectValue.of(Object).

## [LuaType annotation](https://xemiru.github.io/LuaMesh/latest/com/github/xemiru/luamesh/LuaType.html)

### Changing names

Methods to be passed as a Lua function can be set to have a different name from the Java method, by setting an overriding value to the `LuaType` annotation.

```java
@LuaType(name = "luaMethod")
public void javaMethod() {
    // now appears as "luaMethod" on the Lua object
}
```

The same can be done to the class itself, having a different name returned by the `ctype` function.

```java
@LuaType(name = "myobject")
public class MyObject {
    // ctype on Lua MyObjects now return "myobject"
}
```

If no override is passed, it will by default simply copy the declared name. This may not be favorable for some naming schemes if one wishes to follow a separate one for the Lua environment. The most common changes for naming schemes can be applied by LuaMesh directly, and can be configured by changing constants within the `LuaMesh` class.

### Metatable entries

Lua allows objects to override their behavior when passed to an operator by pushing a corresponding function to their metatables. This can be done by LuaMesh by passing the metafunction to overwrite to the `LuaType` annotation.

```java
@LuaType(entry = MetaEntry.ADD)
public MyObject madd(MyObject obj) {
    // MyObject + MyObject in lua now calls this method
}
```

## Version history

| Version | Notes |
|:-:|:--|
| 1.0 | Initial release. |
| 1.1 | Major bug fixes, Java-implemented Lua libraries |
| 1.2 | Unidirectional Lua bindings |
