package com.nextcloud.spotbugs.impl;

import com.nextcloud.spotbugs.Utils;
import com.nextcloud.spotbugs.impl.examples.GoodInterfaceUsed;
import com.nextcloud.spotbugs.impl.examples.BadPrivateImplUsed;
import com.nextcloud.spotbugs.impl.examples.GoodSelfCall;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PrivateImplementationCallTest {

    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void privateImplReferenced() {
        Path[] paths = new Path[] {
                Utils.get(BadPrivateImplUsed.class),
        };
        Utils.addAux(spotbugs);
        BugCollection bugCollection = spotbugs.performAnalysis(paths);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(PrivateImplementationCall.ISSUE_TYPE)
                .build();
        assertThat(bugCollection, containsExactly(10, bugTypeMatcher));
        for(BugInstance i : bugCollection.getCollection()) {
            assertEquals(PrivateImplementationCall.ISSUE_TYPE, i.getType());
        }
    }

    @Test
    public void subclassOfImplClass() {
        Path[] paths = new Path[] {
                Utils.get(BadPrivateImplUsed.Subclass.class),
        };
        Utils.addAux(spotbugs);
        BugCollection bugCollection = spotbugs.performAnalysis(paths);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(PrivateImplementationCall.ISSUE_TYPE)
                .build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));
        for(BugInstance i : bugCollection.getCollection()) {
            assertEquals(PrivateImplementationCall.ISSUE_TYPE, i.getType());
        }
    }

    @Test
    public void selfCallToImpl() {
        Path[] paths = new Path[] {
                Utils.get(GoodSelfCall.SelfCallingImpl.class),
        };
        BugCollection bugCollection = spotbugs.performAnalysis(paths);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(PrivateImplementationCall.ISSUE_TYPE)
                .build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void interfaceReferenced() {
        Path[] paths = new Path[] {
                Utils.get(GoodInterfaceUsed.class),
                Utils.get(GoodInterfaceUsed.SomeProvider.class)
        };
        Utils.addAux(spotbugs);
        BugCollection bugCollection = spotbugs.performAnalysis(paths);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(PrivateImplementationCall.ISSUE_TYPE)
                .build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }
}
