# Dagger / Hilt Conversion Guide

## When This Applies

This guide applies when the Java source contains imports matching `dagger.*` or
`dagger.hilt.*`. This covers Dagger 2, Hilt for Android, and Hilt Jetpack integrations.

## Key Rules

### 1. @Inject constructor syntax

Kotlin places `@Inject` before the `constructor` keyword in the primary constructor:

```kotlin
class Foo @Inject constructor(private val bar: Bar)
```

### 2. @Module classes with @Provides methods

Keep `@Provides` methods `open`, or use `object` for modules that contain only
`@JvmStatic` provides methods (companion object pattern):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}
```

### 3. @Binds abstract methods

`@Binds` methods work in abstract classes exactly as in Java. Convert the abstract
class directly — no special Kotlin considerations.

### 4. Hilt Android annotations

`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel` — preserve these exactly
on Application, Activity, Fragment, and ViewModel classes.

### 5. Scoping annotations

`@Singleton`, `@ActivityScoped`, `@ViewModelScoped`, `@FragmentScoped` — preserve
exactly. No annotation site target is needed.

### 6. @AssistedInject / @AssistedFactory

`@AssistedInject` replaces `@Inject` on the constructor. `@Assisted` parameters
appear alongside regular injected parameters in the primary constructor:

```kotlin
class PlayerViewModel @AssistedInject constructor(
    @Assisted private val playerId: String,
    private val repository: PlayerRepository
) : ViewModel()
```

### 7. @Component / @Subcomponent interfaces

Convert directly to Kotlin interfaces. Dagger's annotation processing works
identically with Kotlin interfaces via kapt or KSP.

---

## Examples

### Example 1: Hilt ViewModel with @Inject Constructor and a @Module

**Java:**

```java
package com.acme.feature;

import androidx.lifecycle.ViewModel;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

@HiltViewModel
public class UserProfileViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AnalyticsTracker analyticsTracker;

    @Inject
    public UserProfileViewModel(UserRepository userRepository, AnalyticsTracker analyticsTracker) {
        this.userRepository = userRepository;
        this.analyticsTracker = analyticsTracker;
    }

    public LiveData<User> getUser(String userId) {
        analyticsTracker.trackProfileView(userId);
        return userRepository.getUser(userId);
    }
}

@Module
@InstallIn(SingletonComponent.class)
public class AnalyticsModule {

    @Provides
    @Singleton
    public AnalyticsTracker provideAnalyticsTracker(Application app) {
        return new AnalyticsTracker(app);
    }
}
```

**Kotlin:**

```kotlin
package com.acme.feature

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    fun getUser(userId: String): LiveData<User> {
        analyticsTracker.trackProfileView(userId)
        return userRepository.getUser(userId)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(app: Application): AnalyticsTracker {
        return AnalyticsTracker(app)
    }
}
```

Key changes:
- `@Inject` moves before the `constructor` keyword in the primary constructor.
- Constructor parameters become `private val` in the primary constructor.
- The module class becomes an `object` since it contains only static-like provides methods.
- `SingletonComponent.class` becomes `SingletonComponent::class` (Kotlin class reference).
- Java getter method `getUser` becomes a regular function `getUser` (no `get` prefix
  convention change needed here since it takes a parameter).
