package net.farlands.newheights;

import net.farlands.sanctuary.data.struct.Badges;
import org.junit.Assert;
import org.junit.Test;

public class BadgesTest {
    @Test
    public void emptyConstructor() {
        var b = new Badges();
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void of() {
        Badges b;

        b = Badges.of();
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));

        b = Badges.of(Badges.Badge.VOTER);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));

        b = Badges.of(Badges.Badge.BIRTHDAY);
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));

        b = Badges.of(Badges.Badge.VOTER, Badges.Badge.BIRTHDAY);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void with1() {
        var b = new Badges()
            .with(Badges.Badge.VOTER);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void with2() {
        var b = new Badges()
            .with(Badges.Badge.BIRTHDAY);
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void with3() {
        var b = new Badges()
            .with(Badges.Badge.VOTER)
            .with(Badges.Badge.BIRTHDAY);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void correctBits() {
        Badges b;

        b = new Badges(0b00);
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));

        b = new Badges(0b01);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertFalse(b.has(Badges.Badge.BIRTHDAY));

        b = new Badges(0b10);
        Assert.assertFalse(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));

        b = new Badges(0b11);
        Assert.assertTrue(b.has(Badges.Badge.VOTER));
        Assert.assertTrue(b.has(Badges.Badge.BIRTHDAY));
    }

    @Test
    public void badgeToString() {
        Badges b;

        b = new Badges();
        Assert.assertEquals("[]", b.toString());

        b = Badges.of(Badges.Badge.VOTER);
        Assert.assertEquals("[VOTER]", b.toString());

        b = Badges.of(Badges.Badge.VOTER, Badges.Badge.BIRTHDAY);
        Assert.assertEquals("[VOTER, BIRTHDAY]", b.toString());
    }

    @Test
    public void variantsNotMoved() {
        Assert.assertEquals(Badges.Badge.VOTER.ordinal(), 0);
        Assert.assertEquals(Badges.Badge.BIRTHDAY.ordinal(), 1);
    }
}
