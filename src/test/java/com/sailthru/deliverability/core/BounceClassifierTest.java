package com.sailthru.deliverability.core;

import com.sailthru.deliverability.api.BounceRule;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class BounceClassifierTest {

    private BounceClassifier bounceClassifier;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testLoadBounces() {
        bounceClassifier = new BounceClassifier("bounce_rules_valid.txt");

        List<BounceRule> expectedBounceRules = setUpExpectedBounces();
        List<BounceRule> bounceRules = bounceClassifier.getBounceRuleCollection();

        List<String> expectedStringList = expectedBounceRules.stream().map(BounceRule::toString).collect(Collectors.toList());
        List<String> actualStringList = bounceRules.stream().map(BounceRule::toString).collect(Collectors.toList());

        assertEquals(expectedStringList, actualStringList);
    }

    @Test
    public void testLoadBouncesInvalidCategories() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Failed to load bounce rules.");
        bounceClassifier = new BounceClassifier("bounce_rules_invalid.txt");
    }

    @Test
    public void testLoadBouncesMissingFile() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Failed to load bounce rules.");
        bounceClassifier = new BounceClassifier("non_existent_file.txt");
    }

    private ArrayList<BounceRule> setUpExpectedBounces() {
        BounceRule b0 = new BounceRule("/0\\.1\\.2/", "NOANSWERFROMHOST", "SOFTBOUNCE");
        BounceRule b1 = new BounceRule("/2\\.4\\.5/", "BADDOMAIN", "HARDBOUNCE");
        BounceRule b2 = new BounceRule("/6\\.7\\.8/", "ROUTINGERRORS", "SOFTBOUNCE");
        BounceRule b3 = new BounceRule("/9\\.0\\.1/", "POLICYRELATED", "SOFTBOUNCE");
        return Lists.newArrayList(b0, b1, b2, b3);
    }
}
