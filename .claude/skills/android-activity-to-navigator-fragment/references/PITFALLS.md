# Pitfalls Found in Earlier Conversions

## Bare intents crash the host (#17193)

`NotificationWork` was changed to `Intent(context, NavigatorActivity::class.java)` without
a screen. `NavigatorActivity.onCreate` calls `requireNotNull` on `EXTRA_SCREEN`, so tapping
the notification crashed. Always use `NavigatorActivity.intent(context, NavigatorScreen.X)`.
Check **every** `PendingIntent`, `BroadcastReceiver`, Worker and notification manager that
referenced the old Activity.

## `binding!!` after a configuration change (#16783)

The `_binding!!` / `val binding get() = _binding!!` pattern crashed when a callback
(presenter, coroutine, receiver) fired after `onDestroyView`. Use a nullable
`private var binding` and `val binding = binding ?: return` in each function.

## Screen not restored after rotation (#16783)

`NavigatorActivity` only re-applies the action bar and drawer highlight if
`NavigatorScreen.fromTag` knows the tag. `fromTag` is not an exhaustive `when` over the
sealed class, so the compiler does not catch a missing entry.

## Dependency injection

`NavigatorActivity.onAttachFragment` injects every fragment. That only works if
`ComponentsModule` has a `@ContributesAndroidInjector abstract XFragment xFragment();`.
Without it you get an `IllegalArgumentException: No injector factory bound` at runtime, not at
compile time. `addFragmentOnAttachListener` / lifecycle callbacks did not behave the same,
which is why the deprecated `onAttachFragment` is used.

## Toolbar views belong to the host

Sort and grid buttons, the info box and the search bar live in the host's `toolbar_standard`.
Reach them with `activity?.findViewById`. Set their visibility explicitly, because the
previous screen may have changed it.

## Dialog callbacks

Dialogs that do `activity as Listener` silently stop working when the listener is now a
fragment (see the `SortingOrderDialogFragment` fix in `ACTIVITY-TO-FRAGMENT-MAPPING.md`).

## Lifecycle scope choice

Long-running work started by the user (retrying an upload, executing an action) should use
the fragment's `lifecycleScope`. That keeps it running while the fragment sits on the back
stack with its view destroyed. Only pure view work should use
`viewLifecycleOwner.lifecycleScope`. Always null-check the binding when the work returns.

## Receivers and registrations

Anything registered in the Activity's `onCreate` and released in `onDestroy` (e.g.
`UploadWarningCard.register/unregister`) must move to `onViewCreated`/`onDestroyView`.
Otherwise it leaks across back-stack transitions or registers twice.

## Screenshot tests

Renaming `XActivityIT` → `XFragmentIT` changes the screenshot names. `git mv` the PNGs in
`app/screenshots/generic/debug/` to match, or CI reports missing references.

## File size and type rules

`AGENTS.md` caps files at 300 lines and wants one type per file. Activities converted to
fragments often exceed 300 lines. Split cohesive flows (menu provider, conflict handler,
credentials handler) into separate classes in the fragment's package.
