package com.nextcloud.spotbugs.impl.examples;

public class BadPrivateImplUsed {

    public static class Subclass extends SomeInterfaceImpl {}

    private static final SomeInterfaceImpl sImpl = new SomeInterfaceImpl();
    private final SomeInterfaceImpl mImpl = new SomeInterfaceImpl();

    void implCreated() {
        SomeInterfaceImpl i = new SomeInterfaceImpl();
        i.inc();
        System.out.print(i.get());
    }

    void staticCall() {
        sImpl.inc();
        System.out.print(sImpl.get());
    }

    void memberCall() {
        mImpl.inc();
        System.out.print(mImpl.get());
    }

    void parameter(SomeInterfaceImpl i) {
        // nothing
    }

}
