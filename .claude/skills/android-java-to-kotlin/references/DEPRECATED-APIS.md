<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Retiring Deprecated Android APIs During Conversion

A Java→Kotlin conversion is the right moment to replace deprecated Android APIs the IDE
converter leaves untouched. Each replacement below is behaviour-preserving. Examples are
real conversions (nextcloud/android PRs #16878, #16792).

## `startActivityForResult` / `onActivityResult` → Activity Result API

The request-code + `onActivityResult` protocol is deprecated. Register a launcher at
construction time and receive the result in its callback.

```kotlin
// BEFORE
startActivityForResult(action, SELECT_LOCATION_REQUEST_CODE)
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == SELECT_LOCATION_REQUEST_CODE && data != null) { handle(data) }
}

// AFTER
private val folderPickerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        handle(result.data)
    }
}

// launch it:
folderPickerLauncher.launch(intent)
```

Type-safe, no manual request-code bookkeeping, and it survives process death because
registration is declarative. Register during initialization (a field initializer or
`onCreate`/`onViewCreated`) — never inside a click handler, or the registration is lost.

## `onCreateOptionsMenu` / `onOptionsItemSelected` → `MenuProvider`

`setHasOptionsMenu(true)` plus the two menu overrides are deprecated on `Fragment`. Add a
`MenuProvider` bound to the view lifecycle instead.

```kotlin
// AFTER
val menuHost: MenuHost = requireActivity()
menuHost.addMenuProvider(object : MenuProvider {
    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) =
        inflater.inflate(R.menu.gallery_menu, menu)

    override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_select_all -> { selectAll(); true }
        else -> false
    }
}, viewLifecycleOwner, Lifecycle.State.RESUMED)
```

Passing `viewLifecycleOwner` + `Lifecycle.State.RESUMED` auto-adds and removes the menu as
the view's lifecycle changes — no leak, no manual `setHasOptionsMenu`. (PR #16878)

## `java.util.Observable` — Keep or Migrate?

`java.util.Observable` / `Observer` are deprecated (Java 9+). But a conversion is
behaviour-locked, so **do not** silently swap the notification mechanism — existing Java
observers rely on `setChanged()` / `notifyObservers()`. PR #16792 deliberately KEPT it:

```kotlin
class UploadsStorageManager(...) : Observable() {
    fun notifyObserversNow() {
        Handler(Looper.getMainLooper()).post {
            setChanged()
            notifyObservers()
        }
    }
}
```

Migrating to `StateFlow` / `SharedFlow` changes the observation contract and every call
site — that is a separate, opt-in refactor, not part of a 1:1 conversion. Note the
deprecation, propose the flow migration as a follow-up, and keep the current mechanism
unless the developer scopes the larger change.
