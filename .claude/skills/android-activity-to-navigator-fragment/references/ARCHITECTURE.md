# Navigator Architecture

All classes live in `app/src/main/java/com/owncloud/android/ui/navigation/`.

```
NavigatorActivity (DrawerActivity)          activity_navigator.xml
 ├─ toolbar_standard (appbar)                 DrawerLayout
 ├─ FragmentContainerView  ◄── Navigator ──►  ├─ CoordinatorLayout
 └─ drawer                                     │   ├─ appbar
                                               │   └─ fragment_container_view
                                               └─ drawer
```

## `NavigatorScreen`: the destination registry

A `sealed class NavigatorScreen(val tag: String, val hasDrawer: Boolean = true) : Parcelable`.
Each destination is a `@Parcelize object`, so it can travel in an `Intent` extra and is
compared by identity in `when`.

| Member | Purpose |
|---|---|
| `tag` | Used as the fragment back-stack entry name. Must be unique |
| `hasDrawer` | `true`: hamburger + unlocked drawer. `false`: back arrow + `LOCK_MODE_LOCKED_CLOSED` |
| `fromTag(tag)` | Turns a back-stack entry name back into a screen. Used after configuration changes |
| `menuItemId()` | Drawer item to highlight (`-1` for none) |
| `actionBarStyle()` | `ActionBarStyle.Plain` → `setupToolbar()`, `Search` → `setupHomeSearchToolbarWithSortAndListButtons()`, plus title res |
| `toFragment()` | Creates a fresh fragment instance |

## `Navigator`: the fragment stack

- `push(screen)`: ignores the call if a fragment with that tag already exists. Otherwise it
  does `replace(container, screen.toFragment())` + `addToBackStack(screen.tag)`.
- `pop()`: `popBackStack()` and returns the new top screen.
- `getTopScreen()`: reads the name of the last back-stack entry and maps it with `fromTag`.
  This keeps working after process death or rotation, because the `FragmentManager`
  restores the back stack. The in-memory `stack` does not survive that.

## `NavigatorActivity`: the host

- `onCreate`: inflates `ActivityNavigatorBinding`, then `requireNotNull`s the
  `EXTRA_SCREEN` extra. It either `push`es the screen (first launch) or restores the action
  bar from `getTopScreen()` (re-creation).
- `onAttachFragment`: `AndroidSupportInjection.inject(fragment)` for **every** attached
  fragment. This is why converted fragments need a `@ContributesAndroidInjector` entry in
  `ComponentsModule`, even when they do not implement `Injectable`.
- `setupActionBar(screen)`: toolbar style, title, drawer or back arrow, drawer lock mode.
  Called on every `push`/`pop`.
- Back press (`OnBackPressedCallback`): close the drawer → let the top
  `NavigatorOnBackPressListener` intercept → `finish()` if only one entry is left → `pop()`.
- Home button (`onOptionsItemSelected(android.R.id.home)`): listener intercept → back if
  `!hasDrawer` → toggle the drawer otherwise.
- `getMenuItemId()` / `onResume`: highlight the drawer item of the top screen.
- `NavigatorActivity.intent(context, screen)`: the **only** correct way to build a launch
  intent.

## `DrawerActivity.pushFragment(screen)`

```java
public void pushFragment(NavigatorScreen screen) {
    if (this instanceof NavigatorActivity navigatorActivity) {
        navigatorActivity.push(screen);          // already in the navigator: swap fragment
    } else {
        startActivity(NavigatorActivity.Companion.intent(this, screen)); // e.g. from FileDisplayActivity
    }
}
```

Drawer item clicks call this, so moving between converted screens swaps fragments
inside a single `NavigatorActivity` instead of starting a new Activity each time.

## `NavigatorOnBackPressListener`

```kotlin
interface NavigatorOnBackPressListener {
    fun canInterceptBackPress(): Boolean
    fun interceptBackPress()
}
```

Implement it in a fragment that has its own navigation inside the screen. For example,
`TrashbinFragment` goes up one folder while `trashbinPresenter.isRoot == false`. The host
checks it for both the system back gesture and the toolbar home button.
