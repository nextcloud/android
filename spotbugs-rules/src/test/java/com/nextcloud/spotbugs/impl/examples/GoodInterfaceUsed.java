package com.nextcloud.spotbugs.impl.examples;

import javax.inject.Provider;

public class GoodInterfaceUsed {

    public static class SomeProvider implements Provider<SomeInterface> {
        @Override
        public SomeInterface get() {
            return new SomeInterfaceImpl();
        }
    }

    private SomeProvider provider = new SomeProvider();

    void createdAndCalled() {
        SomeInterface i = provider.get();
        i.inc();
        System.out.println(i.get());
    }

    void methodUsingInterface(SomeInterface i) {
        i.inc();
        System.out.println(i.get());
    }

    @Provides
    SomeInterface provideInstance() {
        SomeInterfaceImpl instanceFromFactoryMethod = SomeInterfaceImpl.create();
        instanceFromFactoryMethod.inc();
        System.out.println(instanceFromFactoryMethod.get());

        SomeInterfaceImpl instanceFromNew = new SomeInterfaceImpl();
        instanceFromNew.inc();
        System.out.println(instanceFromNew.get());

        return instanceFromNew;
    }
}
