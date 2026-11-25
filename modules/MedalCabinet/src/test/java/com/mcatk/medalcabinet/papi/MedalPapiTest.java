package com.mcatk.medalcabinet.papi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MedalPapiTest {
    @Test
    void expansionSurvivesPlaceholderReload() {
        assertTrue(new MedalPapi().persist());
    }
}
