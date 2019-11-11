package com.nextcloud.spotbugs.impl.examples;

public class SomeInterfaceImpl implements SomeInterface {

    private int counter;

    public static SomeInterfaceImpl create() {
        return new SomeInterfaceImpl();
    }

    public SomeInterfaceImpl() {
        this.counter = 1;
    }

    @Override
    public void inc() {
        counter++;
    }

    @Override
    public int get() {
        return counter;
    }
}
