package com.local.swipepilot;

import org.junit.Test;

/** JUnit entry points for the same checks used by the offline Java runner. */
public final class PolicyUnitTest {
    @Test public void conservativeAdDetection() { PolicyTest.detectionTests(); }
    @Test public void adWaitingAndRecovery() { PolicyTest.waitingTests(); }
    @Test public void sessionLifetime() { PolicyTest.sessionTests(); }
    @Test public void previewAndRewardReturn() { PolicyTest.returnTests(); }
    @Test public void bedtimePopup() { PolicyTest.popupTests(); }
}
