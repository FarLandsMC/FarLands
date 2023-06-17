package net.farlands.newheights;

import net.farlands.sanctuary.util.Range;
import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    public static class RangeParserTest {

        @Test
        public void testBoundRangeExclusive() {
            var r = Range.from("2..5");
            Assert.assertFalse(r.contains(-100));
            Assert.assertFalse(r.contains(0));
            Assert.assertFalse(r.contains(1));
            Assert.assertTrue(r.contains(2));
            Assert.assertTrue(r.contains(3));
            Assert.assertTrue(r.contains(4));
            Assert.assertFalse(r.contains(5));
            Assert.assertFalse(r.contains(6));
        }

        @Test
        public void testBoundRangeInclusive() {
            var r = Range.from("2..=5");
            Assert.assertFalse(r.contains(-100));
            Assert.assertFalse(r.contains(0));
            Assert.assertFalse(r.contains(1));
            Assert.assertTrue(r.contains(2));
            Assert.assertTrue(r.contains(3));
            Assert.assertTrue(r.contains(4));
            Assert.assertTrue(r.contains(5));
            Assert.assertFalse(r.contains(6));
        }

        @Test
        public void testUnboundHighRange() {
            var r = Range.from("2..");
            Assert.assertFalse(r.contains(-100));
            Assert.assertFalse(r.contains(0));
            Assert.assertFalse(r.contains(1));
            Assert.assertTrue(r.contains(2));
            Assert.assertTrue(r.contains(3));
            Assert.assertTrue(r.contains(4));
            Assert.assertTrue(r.contains(5));
            Assert.assertTrue(r.contains(6));
            Assert.assertTrue(r.contains(100));
            Assert.assertTrue(r.contains(10000));
            Assert.assertTrue(r.contains(1123123));
            Assert.assertTrue(r.contains(12354810));
        }

        @Test
        public void testUnboundLowRange() {
            var r = Range.from("..5");
            Assert.assertTrue(r.contains(-100));
            Assert.assertTrue(r.contains(0));
            Assert.assertTrue(r.contains(1));
            Assert.assertTrue(r.contains(2));
            Assert.assertTrue(r.contains(3));
            Assert.assertTrue(r.contains(4));
            Assert.assertFalse(r.contains(5));
            Assert.assertFalse(r.contains(6));
            Assert.assertFalse(r.contains(100));
            Assert.assertFalse(r.contains(10000));
            Assert.assertFalse(r.contains(1123123));
            Assert.assertFalse(r.contains(12354810));
        }

        @Test
        public void testUnboundRange() {
            var r = Range.from("..");
            Assert.assertTrue(r.contains(-100));
            Assert.assertTrue(r.contains(0));
            Assert.assertTrue(r.contains(1));
            Assert.assertTrue(r.contains(2));
            Assert.assertTrue(r.contains(3));
            Assert.assertTrue(r.contains(4));
            Assert.assertTrue(r.contains(5));
            Assert.assertTrue(r.contains(6));
            Assert.assertTrue(r.contains(100));
            Assert.assertTrue(r.contains(10000));
            Assert.assertTrue(r.contains(1123123));
            Assert.assertTrue(r.contains(12354810));
        }

        @Test
        public void testRangeToString() {
            String[] strs = {
                "..",
                "..=5",
                "..8",
                "5..8",
                "0..8",
                "0..=8",
            };
            for (String t : strs) {
                Assert.assertEquals("For input: " + t, t, Range.from(t).toString());
            }
        }
    }
}
