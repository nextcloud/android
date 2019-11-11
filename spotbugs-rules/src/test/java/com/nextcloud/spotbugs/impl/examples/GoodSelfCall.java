package com.nextcloud.spotbugs.impl.examples;

public class GoodSelfCall {

    public static class SelfCallingImpl {

        public int otherMethod() {
            return method() + 1;
        }

        public int method() {
            return 0;
        }
    }
}
