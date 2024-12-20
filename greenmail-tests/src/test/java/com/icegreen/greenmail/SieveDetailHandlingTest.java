/*
 * Copyright (C) Shock Media B.V. - All Rights Reserved. Unauthorized copying
 * and/or distribution of this file, via any medium, is strictly prohibited.
 *
 * Proprietary and confidential
 */

package com.icegreen.greenmail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Test that, when Sieve detail handling is enabled, mail gets delivered to the
 * correct user.
 */
public class SieveDetailHandlingTest {

    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
        .withConfiguration(new GreenMailConfiguration().withSieveIgnoreDetail());

    @Test
    void testPlusSubaddressHandling() {
        GreenMailUtil.sendTextEmailTest("foo+baz@bar.com", "here@localhost", "sub1", "msg1");

        assertThat(greenMail.getUserManager().listUser()).hasSize(1);
        assertThat(greenMail.getUserManager().getUserByEmail("foo@bar.com")).isNotNull();
        assertThat(greenMail.getUserManager().getUserByEmail("foo+baz@bar.com")).isNull();
    }

    @Test
    void testMinusSubaddressHandling() {
        GreenMailUtil.sendTextEmailTest("foo--baz@bar.com", "here@localhost", "sub1", "msg1");

        assertThat(greenMail.getUserManager().listUser()).hasSize(1);
        assertThat(greenMail.getUserManager().getUserByEmail("baz@bar.com")).isNotNull();
        assertThat(greenMail.getUserManager().getUserByEmail("foo--baz@bar.com")).isNull();
    }

    @Test
    void testSubaddressHandlingDisabled() {
        greenMail.getUserManager().setSieveIgnoreDetail(false);
        GreenMailUtil.sendTextEmailTest("foo+baz@bar.com", "here@localhost", "sub2", "msg2");

        assertThat(greenMail.getUserManager().listUser()).hasSize(1);
        assertThat(greenMail.getUserManager().getUserByEmail("foo@bar.com")).isNull();
        assertThat(greenMail.getUserManager().getUserByEmail("foo+baz@bar.com")).isNotNull();

    }

}
