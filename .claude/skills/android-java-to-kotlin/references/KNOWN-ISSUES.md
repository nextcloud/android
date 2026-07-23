# Known Issues and Common Pitfalls

A reference of common issues encountered during Java-to-Kotlin conversion, with solutions.

### Kotlin Keyword Conflicts

Java identifiers that are reserved keywords in Kotlin will cause compilation errors after conversion.

**Affected keywords:** `when`, `in`, `is`, `object`, `fun`, `val`, `var`, `typealias`, `as`

**Solution:** Backtick-escape them in Kotlin:

```java
// Java
public void when(String event) { ... }
public boolean in(List<String> items) { ... }
```

```kotlin
// Kotlin — backtick-escaped
fun `when`(event: String) { ... }
fun `in`(items: List<String>): Boolean { ... }
```

When the API is internal (not exposed to other modules), prefer renaming the identifier to a non-keyword alternative instead of using backticks. For example, rename `when` to `onEvent` or `in` to `contains`.

### SAM Conversion Ambiguity

When a Java method has overloads that each accept a different SAM (Single Abstract Method) interface, Kotlin's trailing lambda syntax becomes ambiguous. The compiler cannot determine which SAM interface the lambda should implement.

```java
// Java — overloaded method accepting different SAM types
public class TaskExecutor {
    void submit(Runnable task) { ... }
    void submit(Callable<String> task) { ... }
}
```

```kotlin
// Kotlin — WRONG: ambiguous, won't compile
executor.submit { doWork() }

// Kotlin — CORRECT: explicit SAM constructor
executor.submit(Runnable { doWork() })
executor.submit(Callable { computeResult() })
```

Use explicit SAM constructor calls whenever there are overloaded methods accepting different functional interfaces.

### Platform Types

Java types without nullability annotations (`@Nullable`, `@NotNull`, `@NonNull`) become "platform types" (`T!`) in Kotlin. Platform types bypass Kotlin's null-safety system — they are neither nullable nor non-null, and null checks are deferred to runtime.

```java
// Java — no nullability annotations
public String getName() { return name; }
public List<String> getItems() { return items; }
```

```kotlin
// Kotlin — BAD: platform types left in converted code
val name = obj.name       // inferred as String! — unsafe
val items = obj.items     // inferred as List<String!>! — unsafe

// Kotlin — GOOD: explicit nullability based on code analysis
val name: String = obj.name              // if provably non-null
val name: String? = obj.name             // if could be null
val items: List<String> = obj.items      // if neither list nor elements are null
```

Always add explicit type declarations to eliminate platform types. Analyze the Java source code, documentation, and call sites to determine the correct nullability.

### @JvmStatic / @JvmField / @JvmOverloads

When converted Kotlin code is still called from Java, use JVM interop annotations to maintain a clean Java API:

**`@JvmStatic`** — Makes companion object functions accessible as static methods from Java:

```kotlin
class Config {
    companion object {
        @JvmStatic
        fun getInstance(): Config = ...
    }
}
```

```java
// Java callers can use: Config.getInstance()
// Without @JvmStatic they would need: Config.Companion.getInstance()
```

**`@JvmField`** — Exposes a property as a direct field rather than through getter/setter:

```kotlin
class Constants {
    companion object {
        @JvmField
        val DEFAULT_TIMEOUT = 30_000L
    }
}
```

```java
// Java callers can use: Constants.DEFAULT_TIMEOUT
// Without @JvmField they would need: Constants.Companion.getDEFAULT_TIMEOUT()
```

**`@JvmOverloads`** — Generates Java overloads for functions with default parameters:

```kotlin
@JvmOverloads
fun connect(host: String, port: Int = 443, secure: Boolean = true) { ... }
```

```java
// Java sees three overloads:
// connect(String host)
// connect(String host, int port)
// connect(String host, int port, boolean secure)
```

### Checked Exceptions

Kotlin does not have checked exceptions. When Kotlin code is called from Java, the Java compiler will not know about thrown exceptions unless annotated with `@Throws`:

```kotlin
// Without @Throws, Java callers cannot catch IOException in a catch block
// (the Java compiler will say "exception is never thrown in the corresponding try block")

@Throws(IOException::class)
fun readFile(path: String): String {
    return File(path).readText()
}
```

Add `@Throws` to every Kotlin function that throws checked exceptions and is called from Java code.

### Wildcard Generics

Java wildcard types map to Kotlin's variance annotations:

| Java | Kotlin | Description |
|------|--------|-------------|
| `? extends T` | `out T` | Covariance (producer) |
| `? super T` | `in T` | Contravariance (consumer) |
| Raw type `List` | `List<Any?>` | Add explicit type parameter |

```java
// Java
public void process(List<? extends Number> numbers) { ... }
public void addAll(List<? super Integer> target) { ... }
public void legacy(List items) { ... }  // raw type
```

```kotlin
// Kotlin
fun process(numbers: List<out Number>) { ... }
fun addAll(target: MutableList<in Int>) { ... }
fun legacy(items: List<Any?>) { ... }  // explicit type parameter
```

For raw types, analyze the code to determine the most specific type parameter rather than defaulting to `Any?`.

### Static Members

Java's `static` keyword has no direct equivalent in Kotlin. Use the following mappings:

**Static methods** — Use companion object functions, or top-level functions if they don't need class state:

```java
// Java
public class StringUtils {
    public static String capitalize(String s) { ... }
}
```

```kotlin
// Kotlin — top-level function (preferred when no class state needed)
fun capitalize(s: String): String { ... }

// Kotlin — companion object (when logically tied to the class)
class StringUtils {
    companion object {
        fun capitalize(s: String): String { ... }
    }
}
```

**Static constants** — Use `const val` for compile-time constants (primitives and String), `val` for object constants:

```kotlin
class HttpStatus {
    companion object {
        const val OK = 200                        // primitive — const val
        const val NOT_FOUND_MESSAGE = "Not Found" // String — const val
        val DEFAULT_HEADERS = mapOf("Accept" to "application/json") // object — val
    }
}
```

**Static initializers** — Use companion object `init {}` block or top-level code:

```kotlin
class Registry {
    companion object {
        private val handlers = mutableMapOf<String, Handler>()
        init {
            handlers["default"] = DefaultHandler()
        }
    }
}
```

### Synchronized Blocks

Java's `synchronized` constructs map to Kotlin as follows:

**Synchronized blocks** — Use Kotlin's `synchronized()` function:

```java
// Java
synchronized (lock) {
    sharedState.update();
}
```

```kotlin
// Kotlin
synchronized(lock) {
    sharedState.update()
}
```

**Synchronized methods** — Use the `@Synchronized` annotation:

```java
// Java
public synchronized void update() { ... }
```

```kotlin
// Kotlin
@Synchronized
fun update() { ... }
```

### Anonymous Inner Classes

**Single Abstract Method (SAM) interfaces** — Convert to lambda syntax:

```java
// Java
executor.submit(new Runnable() {
    @Override
    public void run() {
        doWork();
    }
});
```

```kotlin
// Kotlin
executor.submit(Runnable { doWork() })
```

**Multiple methods or abstract classes** — Use `object` expression:

```java
// Java
view.addListener(new ViewListener() {
    @Override
    public void onOpen() { ... }
    @Override
    public void onClose() { ... }
});
```

```kotlin
// Kotlin
view.addListener(object : ViewListener {
    override fun onOpen() { ... }
    override fun onClose() { ... }
})
```

### Array Handling

Java arrays map to Kotlin types as follows:

| Java | Kotlin | Notes |
|------|--------|-------|
| `String[]` | `Array<String>` | Reference type arrays |
| `int[]` | `IntArray` | Primitive array (not `Array<Int>`) |
| `long[]` | `LongArray` | Primitive array |
| `double[]` | `DoubleArray` | Primitive array |
| `boolean[]` | `BooleanArray` | Primitive array |
| `Object[]` | `Array<Any?>` | |
| `new int[10]` | `IntArray(10)` | Array creation |
| `new String[10]` | `arrayOfNulls<String>(10)` | Nullable element array |
| `String... args` | `vararg args: String` | Varargs parameter |

Using `Array<Int>` instead of `IntArray` causes boxing overhead — always use the specialized primitive array types.

### Ternary Operator

Kotlin has no ternary operator. Use `if`/`else` as an expression:

```java
// Java
String label = (count > 0) ? "Items: " + count : "Empty";
```

```kotlin
// Kotlin
val label = if (count > 0) "Items: $count" else "Empty"
```

### instanceof

Java's `instanceof` maps to Kotlin's `is` keyword. Kotlin supports smart casting, so an explicit cast after an `is` check is unnecessary:

```java
// Java
if (shape instanceof Circle) {
    Circle circle = (Circle) shape;
    double area = circle.getArea();
}
```

```kotlin
// Kotlin — smart cast, no explicit cast needed
if (shape is Circle) {
    val area = shape.area  // shape is automatically cast to Circle
}
```

### try-with-resources

Java's try-with-resources maps to Kotlin's `.use {}` extension function:

```java
// Java
try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
    String line = reader.readLine();
    process(line);
}
```

```kotlin
// Kotlin
BufferedReader(FileReader(path)).use { reader ->
    val line = reader.readLine()
    process(line)
}
```

The `.use {}` function works on any `Closeable` or `AutoCloseable` instance and guarantees the resource is closed even if an exception is thrown.
