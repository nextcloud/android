# Nextcloud Android SpotBugs Plugin

This plugin adds some custom, Nextcloud Android specific checks.

## NXRAC - Restricted API Call

This checks verifies if developers use Android platform APIs
that have custom Nextcloud equivalents.

## NXPIC - Private Interface  Call

Call to a class that most likely is a private implementation, ie. has `*Impl` suffix,
such as `AppPreferencesImpl`. Those classes should be dependency-injected
and accessed via their interfaces.

# Building

This plugin uses Maven build system.
To build it install Maven and type:

```shell
mvn package
```

A `jar` will be placed in `target` directory. Copy this `jar` file
to Nextcloud Android application source code, where `SpotBugs` can pick it
up.
