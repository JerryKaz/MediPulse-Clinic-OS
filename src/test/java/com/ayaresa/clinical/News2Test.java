package com.ayaresa.clinical;

import com.ayaresa.domain.Consciousness;
import com.ayaresa.domain.VitalSigns;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class News2Test {

    private static VitalSigns v(int rr, int spo2, boolean o2, int sys, int pulse, Consciousness c, double temp) {
        return new VitalSigns("t", "p", null, rr, spo2, o2, sys, 80, pulse, c, temp, "test");
    }

    @Test
    void normalObservationsScoreZero() {
        News2.Result r = News2.score(v(16, 98, false, 120, 72, Consciousness.ALERT, 36.8));
        assertEquals(0, r.total());
        assertEquals(News2.Risk.LOW, r.risk());
    }

    @Test
    void bandBoundariesMatchTheRcpChart() {
        assertEquals(3, News2.respiration(8));
        assertEquals(1, News2.respiration(9));
        assertEquals(0, News2.respiration(20));
        assertEquals(2, News2.respiration(21));
        assertEquals(3, News2.respiration(25));
        assertEquals(3, News2.spo2Scale1(91));
        assertEquals(2, News2.spo2Scale1(93));
        assertEquals(1, News2.spo2Scale1(95));
        assertEquals(0, News2.spo2Scale1(96));
        assertEquals(3, News2.systolic(90));
        assertEquals(2, News2.systolic(100));
        assertEquals(1, News2.systolic(110));
        assertEquals(0, News2.systolic(219));
        assertEquals(3, News2.systolic(220));
        assertEquals(3, News2.pulse(40));
        assertEquals(1, News2.pulse(50));
        assertEquals(0, News2.pulse(90));
        assertEquals(1, News2.pulse(110));
        assertEquals(2, News2.pulse(130));
        assertEquals(3, News2.pulse(131));
        assertEquals(3, News2.temperature(35.0));
        assertEquals(1, News2.temperature(36.0));
        assertEquals(0, News2.temperature(38.0));
        assertEquals(1, News2.temperature(39.0));
        assertEquals(2, News2.temperature(39.1));
    }

    @Test
    void temperatureIsRoundedToOneDecimalFirst() {
        assertEquals(0, News2.temperature(38.04));
    }

    @Test
    void singleRedParameterIsLowMedium() {
        News2.Result r = News2.score(v(16, 98, false, 120, 72, Consciousness.NEW_CONFUSION, 36.8));
        assertEquals(3, r.total());
        assertTrue(r.hasRedScore());
        assertEquals(News2.Risk.LOW_MEDIUM, r.risk());
    }

    @Test
    void oxygenAddsTwoAndThresholdsGiveMediumAndHigh() {
        assertEquals(News2.Risk.MEDIUM, News2.score(v(22, 95, true, 120, 72, Consciousness.ALERT, 36.8)).risk()); // 2+1+2
        News2.Result high = News2.score(v(26, 89, true, 102, 118, Consciousness.ALERT, 38.9)); // 3+3+2+1+2+1
        assertEquals(12, high.total());
        assertEquals(News2.Risk.HIGH, high.risk());
    }
}
