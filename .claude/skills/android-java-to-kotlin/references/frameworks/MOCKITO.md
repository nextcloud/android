# Mockito to MockK Conversion Guide

## When This Applies

Detected when imports match `org.mockito.*`.

While Mockito works in Kotlin, MockK is the idiomatic Kotlin mocking library. Convert
to MockK when the project already uses MockK or is doing a full Kotlin migration. If
the project wants to keep Mockito, convert only the Java syntax to Kotlin syntax using
the `mockito-kotlin` helper library.

## Key Rules

### 1. MockK conversion table

| Mockito | MockK |
|---|---|
| `Mockito.mock(Foo.class)` | `mockk<Foo>()` |
| `@Mock Foo foo` | `@MockK lateinit var foo: Foo` (with `@ExtendWith(MockKExtension::class)`) |
| `when(foo.bar()).thenReturn(x)` | `every { foo.bar() } returns x` |
| `when(foo.bar()).thenThrow(e)` | `every { foo.bar() } throws e` |
| `when(foo.bar()).thenAnswer { }` | `every { foo.bar() } answers { }` |
| `doNothing().when(foo).bar()` | `justRun { foo.bar() }` |
| `verify(foo).bar()` | `verify { foo.bar() }` |
| `verify(foo, times(2)).bar()` | `verify(exactly = 2) { foo.bar() }` |
| `verify(foo, never()).bar()` | `verify(exactly = 0) { foo.bar() }` |
| `ArgumentCaptor<T>` | `slot<T>()` and `capture(slot)` |
| `any()` | `any()` |
| `eq(x)` | `eq(x)` (often not needed — MockK matches exact values by default) |
| `Mockito.spy(obj)` | `spyk(obj)` |
| `@InjectMocks` | No direct equivalent — use constructor injection |
| `verifyNoMoreInteractions(foo)` | `confirmVerified(foo)` |

### 2. Coroutine support in MockK

For suspending functions, use `coEvery` and `coVerify` instead of `every` and `verify`:
```kotlin
coEvery { foo.suspendBar() } returns x
coVerify { foo.suspendBar() }
```

### 3. Keeping Mockito (syntax-only conversion)

If keeping Mockito, use the `mockito-kotlin` library (`org.mockito.kotlin`) for
Kotlin-friendly wrappers:
- `mock<Foo>()` instead of `Mockito.mock(Foo::class.java)` — uses reified generics.
- `whenever(foo.bar())` instead of `` Mockito.`when`(foo.bar()) `` — avoids backtick-
  escaping `when` (it is a Kotlin keyword).
- `argumentCaptor<T>()` — type-safe captor via reified generics.
- `any()` — properly handles Kotlin's non-null types.

### 4. Relaxed mocks

MockK supports relaxed mocks that return default values without explicit stubbing:
`mockk<Foo>(relaxed = true)`. This has no direct Mockito equivalent (Mockito's
`RETURNS_DEFAULTS` is the closest).

---

## Example 1: Converting to MockK

### Java Input

```java
package com.acme.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests for OrderService using Mockito mocks.
 */
public class OrderServiceTest {

    private UserRepository userRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(userRepository, orderRepository);
    }

    @Test
    public void testCreateOrderForUser() {
        User user = new User(1L, "Alice");
        when(userRepository.findById(1L)).thenReturn(user);

        orderService.createOrder(1L, "ITEM-100");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals("ITEM-100", captor.getValue().getItemCode());
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    public void testGetOrderCount() {
        when(orderRepository.countByUserId(anyLong())).thenReturn(5);

        int count = orderService.getOrderCount(1L);

        assertEquals(5, count);
        verify(orderRepository).countByUserId(1L);
    }
}
```

### Kotlin Output (MockK)

```kotlin
package com.acme.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for OrderService using MockK mocks.
 */
class OrderServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val orderService = OrderService(userRepository, orderRepository)

    @Test
    fun `should create order for user`() {
        val user = User(1L, "Alice")
        every { userRepository.findById(1L) } returns user
        every { orderRepository.save(any()) } returns Unit

        orderService.createOrder(1L, "ITEM-100")

        val orderSlot = slot<Order>()
        verify { orderRepository.save(capture(orderSlot)) }
        assertEquals("ITEM-100", orderSlot.captured.itemCode)
        assertEquals(1L, orderSlot.captured.userId)
    }

    @Test
    fun `should return order count`() {
        every { orderRepository.countByUserId(any()) } returns 5

        val count = orderService.getOrderCount(1L)

        assertEquals(5, count)
        verify { orderRepository.countByUserId(1L) }
    }
}
```

**Key points:**
- `mock(Foo.class)` → `mockk<Foo>()` using reified generics.
- `@Before` setUp is eliminated — mocks are initialized inline with property
  declarations. This works because MockK mocks do not require a runner.
- `when(...).thenReturn(...)` → `every { ... } returns ...`.
- `ArgumentCaptor` → `slot<T>()` with `capture(slot)`, accessed via `slot.captured`.
- `anyLong()` → `any()` (MockK's `any()` handles all types).
- `verify(foo).bar()` → `verify { foo.bar() }`.

---

## Example 2: Keeping Mockito (mockito-kotlin syntax)

### Java Input

```java
package com.acme.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for PricingService using Mockito.
 */
public class PricingServiceTest {

    private PriceRepository priceRepository;
    private PricingService pricingService;

    @Before
    public void setUp() {
        priceRepository = mock(PriceRepository.class);
        pricingService = new PricingService(priceRepository);
    }

    @Test
    public void testGetPrice() {
        when(priceRepository.findPriceByItemCode("ITEM-1")).thenReturn(9.99);
        double price = pricingService.getPrice("ITEM-1");
        assertEquals(9.99, price, 0.001);
        verify(priceRepository).findPriceByItemCode("ITEM-1");
    }
}
```

### Kotlin Output (mockito-kotlin)

```kotlin
package com.acme.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

/**
 * Tests for PricingService using Mockito.
 */
class PricingServiceTest {

    private val priceRepository = mock<PriceRepository>()
    private val pricingService = PricingService(priceRepository)

    @Test
    fun `should return price for item`() {
        whenever(priceRepository.findPriceByItemCode("ITEM-1")).thenReturn(9.99)

        val price = pricingService.getPrice("ITEM-1")

        assertEquals(9.99, price, 0.001)
        verify(priceRepository).findPriceByItemCode("ITEM-1")
    }
}
```

**Key points:**
- `mock(Foo.class)` → `mock<Foo>()` from `org.mockito.kotlin` (reified generics).
- `when(...)` → `whenever(...)` to avoid backtick-escaping the `when` keyword.
- `verify` stays the same — `org.mockito.kotlin.verify` wraps Mockito's verify.
- The `setUp` method is eliminated — mocks are initialized inline.
- `assertEquals` with a delta parameter works the same way from kotlin.test.
