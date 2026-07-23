# Post-Conversion Verification Checklist

Use this checklist after converting each Java file to Kotlin.

## Compilation & Tests
- [ ] The `.kt` file compiles without errors
- [ ] All existing tests still pass
- [ ] No new compiler warnings introduced

## Semantic Correctness
- [ ] No new side-effects or behavioural changes
- [ ] All public API signatures preserved (method names, parameter types, return types)
- [ ] Exception behaviour unchanged (same exceptions thrown in same conditions)

## Annotations
- [ ] All annotations preserved from the original Java code
- [ ] Annotation site targets correct (`@field:`, `@get:`, `@set:`, `@param:`)
- [ ] No annotations accidentally dropped during conversion

## Imports & Package
- [ ] Package declaration matches original
- [ ] All imports carried forward (except Java types that shadow Kotlin builtins)
- [ ] No new imports added unnecessarily

## Documentation
- [ ] All Javadoc converted to KDoc format
- [ ] `{@code ...}` → backtick code in KDoc
- [ ] `{@link ...}` → `[...]` KDoc links
- [ ] `<p>` paragraph tags → blank lines
- [ ] `@param`, `@return`, `@throws` tags preserved
- [ ] Class-level and method-level documentation preserved

## Nullability & Mutability
- [ ] Non-null types used only where provably non-null
- [ ] Nullable types (`?`) used for all Java types that could be null
- [ ] `val` used for all immutable variables/properties
- [ ] `var` used only for mutable variables/properties

## Collections
- [ ] `MutableList`/`MutableSet`/`MutableMap` for Java's mutable collections
- [ ] `List`/`Set`/`Map` only where Java used immutable wrappers

## Kotlin Idioms
- [ ] Getters/setters replaced with Kotlin properties where appropriate
- [ ] String concatenation replaced with string templates where clearer
- [ ] Elvis operator used where appropriate
- [ ] `when` expression used instead of `switch`
- [ ] Smart casts used after `is` checks (no explicit casts)

## Framework-Specific (check applicable items)
- [ ] **Spring**: Classes that need proxying are `open`; `@Bean` methods are `open`
- [ ] **Lombok**: All Lombok annotations removed; replaced with Kotlin equivalents
- [ ] **Hibernate/JPA**: Entities are `open` (not data classes); no-arg constructor provided
- [ ] **Jackson**: `@field:` and `@get:` annotation site targets correct
- [ ] **RxJava**: Reactive types correctly mapped to Coroutines/Flow
- [ ] **Mockito**: `when` keyword escaped or replaced with MockK equivalent

## Git History
- [ ] File renamed via `git mv` (not delete + create)
- [ ] Rename commit separate from content change commit
