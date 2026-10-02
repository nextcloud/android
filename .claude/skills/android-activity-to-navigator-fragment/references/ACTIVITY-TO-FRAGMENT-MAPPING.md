# Activity API → Navigator Fragment Equivalent

| In the Activity | In the fragment |
|---|---|
| `class X : DrawerActivity()` / `FileActivity()` | `class XFragment : Fragment()` |
| `onCreate`: `setContentView(binding.root)` | `onCreateView`: `binding = FragmentXBinding.inflate(inflater, container, false)` |
| View setup in `onCreate` | `onViewCreated` |
| Non-view setup in `onCreate` (intent extras, config) | `onCreate` (guard extras with `savedInstanceState == null`) |
| `onDestroy` cleanup tied to views/receivers registered in `onCreate` | `onDestroyView` |
| `onStart`/`onStop`/`onResume` | same callbacks on the fragment |
| `setupToolbar()`, `updateActionBarTitleAndHomeButtonByString`, `setupDrawer(menuItemId)` | delete; `NavigatorScreen.actionBarStyle()` + host |
| `getMenuItemId()`, `highlightNavigationViewItem()` | delete; `NavigatorScreen.menuItemId()` + host |
| `onCreateOptionsMenu` / `onOptionsItemSelected` | `MenuProvider` added with `viewLifecycleOwner, Lifecycle.State.RESUMED`; return `false` for ids you do not own |
| `android.R.id.home` → toggle drawer | delete; the host handles it |
| `OnBackPressedCallback` for internal navigation | `NavigatorOnBackPressListener` |
| `mDrawerToggle.isDrawerIndicatorEnabled = x` | `navigatorActivity?.setDrawerIndicatorEnabled(x)` |
| `addDrawerListener(l)` | `activity?.findViewById<DrawerLayout>(R.id.drawer_layout)?.addDrawerListener(l)` |
| `startActionMode(cb)` | `navigatorActivity?.startActionMode(cb)` |
| `findViewById(R.id.sort_button)` (toolbar views) | `activity?.findViewById(...)`; the toolbar belongs to the host |
| `supportFragmentManager` for dialogs | `childFragmentManager`, or `requireActivity().supportFragmentManager` when the dialog calls back into the activity |
| `this` as `Context` | `requireContext()` (inside callbacks: capture before launching) |
| `this` as `Activity` (snackbars, PopupMenu, adapters) | `requireActivity()` / typed host |
| `lifecycleScope` | `lifecycleScope` (fragment lifetime; survives while on back stack) or `viewLifecycleOwner.lifecycleScope` (view work only) |
| `user`, `storageManager`, `clientRepository`, `capabilities` | `navigatorActivity?.user`, `.storageManager`, `.clientRepository`, `.capabilities` |
| `setUser(user)` | `navigatorActivity?.setUser(user)` (`BaseActivity.setUser` is public) |
| `startActivityForResult` + `onActivityResult` | `registerForActivityResult(StartActivityForResult())` as a fragment property |
| `finish()` | `activity?.finish()` |
| `DisplayUtils.showSnackMessage(this, …)` | `DisplayUtils.showSnackMessage(requireView(), …)` / `SnackbarUtil.show(this, …)` |
| inner `BroadcastReceiver` class | `private val receiver = object : BroadcastReceiver() { … }`, avoids an extra type in the file |
| `@Inject var x: T? = null` with `@JvmField` | `@Inject lateinit var x: T` |
| Activity fields inherited from the base (`viewThemeUtils`, `preferences`, `connectivityService`, `accountManager`, `backgroundJobManager`) | `@Inject lateinit var` them in the fragment |

## Dialog listener lookups

Dialogs that cast `activity as Listener` break when the listener is now a fragment.
Trashbin fixed `SortingOrderDialogFragment` with this fallback chain:

```kotlin
val listener = parentFragment as? OnSortingOrderListener
    ?: activity as? OnSortingOrderListener
    ?: requireActivity().supportFragmentManager.fragments
        .firstOrNull { it is OnSortingOrderListener } as? OnSortingOrderListener
```

Check every dialog the screen opens for the same pattern.
