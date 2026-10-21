---
name: android-activity-to-navigator-fragment
description: >
  Use when converting a standalone Activity (usually a DrawerActivity or FileActivity
  subclass such as a drawer destination) into a Fragment hosted by NavigatorActivity, when
  the user mentions "navigator", "NavigatorActivity", "NavigatorScreen", "convert activity
  to fragment", "move screen into the new navigation", or names a drawer screen that still
  has its own Activity. Covers registering the NavigatorScreen, building the fragment and
  its layout, decoupling collaborators from the old Activity type, rewiring every entry
  point (drawer, PendingIntents, broadcast receivers, deep links), DI and manifest cleanup,
  and migrating the instrumented screenshot test.
license: AGPL-3.0-or-later
SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
metadata:
  author: Nextcloud Android
  version: "1.0.0"
---

# Activity → NavigatorActivity Fragment Conversion

Historically every drawer destination was its own `DrawerActivity` subclass, each with its
own toolbar, `DrawerLayout` and back handling. `NavigatorActivity` replaces them with **one
host Activity** that owns the toolbar and drawer, and swaps **fragments** for the
destinations. Converted so far: Community (#16652), Activities (#16753), Notifications
(#16800), Trashbin (#16879), Upload list.

Read `references/ARCHITECTURE.md` once before the first conversion. It explains how
`Navigator`, `NavigatorScreen`, `NavigatorActivity` and `DrawerActivity.pushFragment` work
together.

## The Prime Directive: Behaviour Must Not Change

The screen must look and behave exactly as before from every entry point: drawer item,
notification tap, notification action, deep link, rotation, back press, drawer toggle and
menu actions. This is a structural refactor. If you find a bug, report it instead of
fixing it in the same change.

## Workflow

### Step 0: Inventory (do not edit yet)

1. Read the whole Activity, its layout and its menu.
2. `git grep -n "<ActivityName>"` across `app/src`, including `AndroidManifest.xml`,
   `ComponentsModule.java`, KDoc links, `androidTest` and screenshot file names. Every hit is
   either an **entry point** you must rewire or a **reference** you must update.
3. List what the Activity takes from its **base class**. This is the step most likely to go
   wrong. `NavigatorActivity` is a `DrawerActivity`, **not** a `FileActivity`. Anything from
   `FileActivity` (`fileOperationsHelper`, `OperationsService` binding,
   `onRemoteOperationFinish`, `requestCredentialsUpdate`, `showLoadingDialog`,
   `connectivityService` field, `backgroundJobManager` field, `EXTRA_FILE`/`EXTRA_USER`
   handling in `onCreate`) does not exist in the host. See `references/HOST-CAPABILITIES.md`.
4. List the intent extras the Activity, or its base class, reads. Each one must keep working
   when it arrives on the `NavigatorActivity` intent.
5. Note any `onActivityResult`, `BroadcastReceiver`, `OnBackPressedCallback`,
   `onOptionsItemSelected`, `ActionMode` or drawer listeners. Each one needs a fragment
   equivalent (see `references/ACTIVITY-TO-FRAGMENT-MAPPING.md`).

If a collaborator needs a capability the host does not have, **stop and ask the
developer** whether to (a) move the flow into the fragment and extract the shared pieces
into a helper used by both places, or (b) change the host. Option (a) is the default. It
was chosen for the upload list's credential check (`CredentialsUpdateHelper`).

### Step 1: Register the screen in `NavigatorScreen`

Add the screen in all **five** places in `ui/navigation/NavigatorScreen.kt`. The `when`
expressions are exhaustive, so the compiler catches a missing branch, except in `fromTag`:

1. `@Parcelize object X : NavigatorScreen(X_TAG[, hasDrawer = false])`
2. `private const val X_TAG = "X"` in the companion
3. `fromTag`: `X_TAG -> X` (**not compiler-checked**; missing it breaks restoring after rotation)
4. `menuItemId()`: the drawer item id, or `-1` for screens outside the drawer
5. `actionBarStyle()`: `ActionBarStyle.Plain to R.string.<existing title>`
6. `toFragment()`: `X -> XFragment()`

Use `hasDrawer = false` for screens with a back arrow instead of a hamburger (Notifications).

### Step 2: Fragment layout

Copy the Activity layout to `fragment_<name>.xml` and strip the shell. The host already
provides it:

- remove the root `DrawerLayout`, the `<include layout="@layout/toolbar_standard"/>` and the
  `<include layout="@layout/drawer"/>`;
- remove `layout_below="@id/appbar"`. The host's `FragmentContainerView` already sits below
  the app bar;
- keep every view id the code or tests use, so the generated binding properties match;
- keep the original SPDX copyright lines and add a line for the current year (derived file).

Delete the old layout once nothing references it.

### Step 3: Write the fragment

Follow `references/ACTIVITY-TO-FRAGMENT-MAPPING.md`. The essentials:

- `class XFragment : Fragment()`, with `@Inject lateinit var` dependencies. Injection is done
  by `NavigatorActivity.onAttachFragment`, so `Injectable` is optional.
- Nullable binding: `private var binding: FragmentXBinding? = null`. Set it to `null` in
  `onDestroyView`. Use `val binding = binding ?: return` in every function that touches views
  (#16783 fixed a crash after a configuration change caused by `_binding!!`).
- Host access: `getTypedActivity(NavigatorActivity::class.java)` (or `DrawerActivity` /
  `BaseActivity` when that is all you need) for `user`, `storageManager`, `clientRepository`,
  `setUser`, `setDrawerIndicatorEnabled`, `startActionMode`.
- Menu: `requireActivity().addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)`.
  Do **not** handle `android.R.id.home`. The host does that.
- Back press: implement `NavigatorOnBackPressListener` only if the screen has internal
  navigation (Trashbin folders).
- Remove `getMenuItemId()`, `setupDrawer()`, `setupToolbar()`, `highlightNavigationViewItem()`
  and title setup. `NavigatorScreen` + `NavigatorActivity.setupActionBar` handle all of these.
- Keep files under 300 lines. Extract cohesive flows (conflict handling, credential checks,
  the menu provider) into their own classes in the fragment's package.

### Step 4: Decouple collaborators from the old Activity

Adapters, helpers and listeners typed as `XActivity` or `FileActivity` must take the
narrowest type that has what they use. Usually that is `DrawerActivity`, `BaseActivity`,
`NavigatorActivity` or a plain `Context`. Calls to capabilities the host lacks become
callbacks on the click or listener interface, implemented by the fragment
(e.g. `UploadListItemOnClick.onCredentialErrorClick(user)`).

### Step 5: Rewire every entry point

**Always** build the intent with `NavigatorActivity.intent(context, NavigatorScreen.X)`.
`NavigatorActivity.onCreate` calls `requireNotNull` on the screen extra, so a bare
`Intent(context, NavigatorActivity::class.java)` crashes (#17193).

| Old call site | New call site |
|---|---|
| `DrawerActivity.onNavigationItemClicked`: `startActivity(X.class, FLAG_ACTIVITY_CLEAR_TOP)` | `pushFragment(NavigatorScreen.X.INSTANCE)` |
| Buttons in other DrawerActivities: `startActivity(X::class.java)` | `pushFragment(NavigatorScreen.X)` |
| `handleDeepLink`: `startActivity(X.class)` | `pushFragment(NavigatorScreen.X.INSTANCE)` |
| `PendingIntent` / `BroadcastReceiver` / Worker: `Intent(context, X::class.java)` | `NavigatorActivity.intent(context, NavigatorScreen.X).apply { flags = … }` |
| `X.createIntent(...)` factory | inline `NavigatorActivity.intent(...)` + only the extras the fragment reads |

Keep the original flags (`FLAG_ACTIVITY_NEW_TASK`, `FLAG_ACTIVITY_CLEAR_TOP`, …). Extras the
old base class consumed (e.g. `FileActivity.EXTRA_USER` → `setUser`) must now be read by the
fragment from `activity?.intent`, once, when `savedInstanceState == null`.

### Step 6: DI, manifest, cleanup

- `ComponentsModule.java`: remove `abstract XActivity xActivity();` and add
  `@ContributesAndroidInjector abstract XFragment xFragment();`. Without this, injection
  fails at runtime.
- `AndroidManifest.xml`: remove the `<activity>` entry.
- Delete the Activity, update KDoc/Javadoc `[link]`s that pointed at it, and remove now-unused
  imports in files you edited.

### Step 7: Migrate the instrumented test

Rename `XActivityIT` to `XFragmentIT` and launch through the host:

```kotlin
val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.X)
ActivityScenario.launch<NavigatorActivity>(intent).use { scenario -> … }
```

To call into the fragment, find it with `sut.supportFragmentManager.fragments.filterIsInstance<XFragment>()`
(see `TrashbinFragmentIT.launchFragment`). **Rename the reference screenshots with `git mv`**
(`app/screenshots/generic/debug/<pkg>.XActivityIT_*.png` → `XFragmentIT_*.png`) so the
screenshot test compares against the existing images instead of failing.

### Step 8: Verify

1. `./gradlew :app:compileGplayDebugKotlin :app:compileGplayDebugJavaWithJavac :app:compileGplayDebugAndroidTestKotlin`
2. `./gradlew spotlessKotlinCheck detekt lintGplayDebug spotbugsGplayDebug`, then fix findings
   in every file you touched.
3. `git grep -n "<ActivityName>\|<old_layout_name>"` returns nothing.
4. Manual checklist (hand it to the developer if you cannot run the app): open from the
   drawer, from every notification or PendingIntent, from the deep link; rotate on the
   screen; switch to another drawer destination and press back; toggle the drawer with the
   hamburger; use every menu action; check light and dark themes.

## References

- `references/ARCHITECTURE.md`: how the navigator works.
- `references/ACTIVITY-TO-FRAGMENT-MAPPING.md`: Activity API → fragment equivalent.
- `references/HOST-CAPABILITIES.md`: what `NavigatorActivity` provides and what it does not.
- `references/PITFALLS.md`: bugs that the earlier conversions ran into.
- `assets/worked-example.md`: the upload list conversion, step by step.
