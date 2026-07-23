# JUnit / TestNG Conversion Guide

## When This Applies

Detected when imports match `org.junit.*` or `org.testng.*`.

## Key Rules

### 1. JUnit 4 to Kotlin (with JUnit 5)

| JUnit 4 | Kotlin (JUnit 5 / kotlin.test) |
|---|---|
| `@Test` | `@Test` (from `kotlin.test` or `org.junit.jupiter.api`) |
| `@Before` | `@BeforeEach` (JUnit 5) or `@BeforeTest` (kotlin.test) |
| `@After` | `@AfterEach` (JUnit 5) or `@AfterTest` (kotlin.test) |
| `@BeforeClass` | `@BeforeAll` in companion object with `@JvmStatic` |
| `@AfterClass` | `@AfterAll` in companion object with `@JvmStatic` |
| `@RunWith` | `@ExtendWith` (JUnit 5) |
| `@Ignore` | `@Disabled` (JUnit 5) |
| `@Rule` / `@ClassRule` | `@ExtendWith` or `@RegisterExtension` |
| `Assert.assertEquals(expected, actual)` | `assertEquals(expected, actual)` (kotlin.test) |
| `Assert.assertTrue(condition)` | `assertTrue(condition)` (kotlin.test) |
| `@Test(expected = X.class)` | `assertFailsWith<X> { }` (kotlin.test) or `assertThrows<X> { }` (JUnit 5) |

### 2. JUnit 5 stays mostly the same

JUnit 5 annotations (`@Test`, `@BeforeEach`, `@AfterEach`, etc.) remain unchanged.
Focus on Kotlin idioms in the test body:

- `assertThrows<ExceptionType> { code }` — uses reified generics, no `.class` needed.
- Test classes and methods do not need to be `public` — Kotlin's default visibility
  is public, which satisfies JUnit's requirements.
- Test methods do not need `open` unless using a framework that subclasses the test
  (e.g., certain Spring test configurations).

### 3. TestNG to Kotlin

| TestNG | Kotlin (JUnit 5) |
|---|---|
| `@Test` | `@Test` |
| `@BeforeMethod` | `@BeforeEach` |
| `@AfterMethod` | `@AfterEach` |
| `@BeforeClass` | `@BeforeAll` with `@JvmStatic` in companion object |
| `@AfterClass` | `@AfterAll` with `@JvmStatic` in companion object |
| `@DataProvider` | `@ParameterizedTest` + `@MethodSource` |

### 4. Assertion style

Prefer `kotlin.test` assertions (`assertEquals`, `assertTrue`, `assertFailsWith`)
for portability across test frameworks. They delegate to the underlying framework
at runtime.

### 5. Backtick method names

Kotlin allows backtick-quoted method names for readable test names:
```kotlin
@Test
fun `should return empty list when no users exist`() { ... }
```

---

## Example: JUnit 4 Test Class to Kotlin with JUnit 5

### Java Input

```java
package com.acme.service;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the UserService class.
 */
public class UserServiceTest {

    private static DatabaseConnection db;
    private UserService userService;

    @BeforeClass
    public static void setupDatabase() {
        db = DatabaseConnection.create("test");
    }

    @Before
    public void setUp() {
        userService = new UserService(db);
    }

    @After
    public void tearDown() {
        db.clearTestData();
    }

    @Test
    public void testFindById() {
        User user = userService.findById(1L);
        assertNotNull(user);
        assertEquals("Alice", user.getName());
    }

    @Test
    public void testFindAllReturnsNonEmptyList() {
        List<User> users = userService.findAll();
        assertNotNull(users);
        assertTrue(users.size() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindByIdWithNegativeIdThrows() {
        userService.findById(-1L);
    }
}
```

### Kotlin Output

```kotlin
package com.acme.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the UserService class.
 */
class UserServiceTest {

    companion object {
        private lateinit var db: DatabaseConnection

        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            db = DatabaseConnection.create("test")
        }
    }

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(db)
    }

    @AfterEach
    fun tearDown() {
        db.clearTestData()
    }

    @Test
    fun `should find user by id`() {
        val user = userService.findById(1L)
        assertNotNull(user)
        assertEquals("Alice", user.name)
    }

    @Test
    fun `should return non-empty list from findAll`() {
        val users = userService.findAll()
        assertNotNull(users)
        assertTrue(users.isNotEmpty())
    }

    @Test
    fun `should throw IllegalArgumentException for negative id`() {
        assertFailsWith<IllegalArgumentException> {
            userService.findById(-1L)
        }
    }
}
```

**Key points:**
- JUnit 4 `@Before` / `@After` → JUnit 5 `@BeforeEach` / `@AfterEach`.
- `@BeforeClass` static method → `@BeforeAll` + `@JvmStatic` inside `companion object`.
- `@Test(expected = ...)` → `assertFailsWith<ExceptionType> { }` with reified generics.
- Static assertions become kotlin.test top-level function imports.
- Test method names use backtick syntax for readability.
- `users.size() > 0` becomes idiomatic `users.isNotEmpty()`.
- The `db` field uses `lateinit var` since it is initialized in `@BeforeAll`.
