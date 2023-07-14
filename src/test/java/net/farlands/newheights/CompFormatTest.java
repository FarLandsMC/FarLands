package net.farlands.newheights;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static net.farlands.sanctuary.util.ComponentUtils.format;

public class CompFormatTest {

    private static void a(String expected, String format, Object... values) {
        String a = ComponentUtils.toText(format(format, values));
        Assert.assertEquals(expected, a);
    }

    @Test
    public void strings() {
        a("hello", "hello");
        a("{}", "{}");
        a("hello world", "{}", "hello world");
        a("hello world", "hello {}", "world");
        a("hello world world", "hello {} {0}", "world");
        a("5.00000", "{::%.5f}", 5.0);

    }

    @Test
    public void otherStuff() {
        a("[a, b, c]", "{::[]}", List.of('a', 'b', 'c'));
        a("[a, b, c]", "{::[]}", (Object) new Character[]{ 'a', 'b', 'c' });
        a("a, b, and c", "{}", (Object) new Character[]{ 'a', 'b', 'c' });
        a("a and b", "{}", (Object) new Character[]{ 'a', 'b' });
        a("a", "{}", (Object) new Character[]{ 'a' });
        a("a, b, and c", "{}", List.of('a', 'b', 'c'));
        a("Dev", "{}", Rank.DEV);
        a("Jr. Builder", "{}", Rank.JR_BUILDER);
        a("overworld", "{}", Worlds.OVERWORLD);
        a("nether", "{}", Worlds.NETHER);
    }

    public void eq(String expected, Component comp) {
        Assert.assertEquals(expected, ComponentUtils.toText(comp));
    }

    @Test
    public void documentation() {
        eq(                 "hello", format("{}", "hello"));
        eq(          "Hello world!", format("Hello {}!", "world"));
        eq("Hello world and earth!", format("Hello {} and {}!", "world", "earth"));

        eq(    "Hello world world!", format("Hello {} {0}!", "world"));

        eq(          "Hello world!", format("Hello {:red}!", "world")); // World is red
        eq(    "Hello world world!", format("Hello {} {0:red}!", "world")); // Second world is red
        eq(          "Hello world!", format("Hello {:red bold}!", "world")); // world is red & bold

        eq(          "Hello world!", format("Hello {:red !bold}!", "world")); // world is red & not bold

        eq(                  "5000", format("{}", 5000));
        eq(                 "5,000", format("{::%,d}", 5000));
        eq(                "5 nums", format("{0} num{0::s}", 5));
        eq(             "[1, 2, 3]", format("{::[]}", List.of(1, 2, 3)));
        eq(          "[01, 02, 03]", format("{::[%02d]}", List.of(1, 2, 3)));

        eq(             "Hello {}!", format("Hello \\{}!", "world"));
        eq(              "Hello }!", format("Hello {::\\}}!", "world"));
        eq(              "Hello {!", format("Hello {::{}!", "world"));
        eq(              "5 points", format("{} point{0::s}", 5));
        eq(               "1 point", format("{} point{0::s}", 1));
        eq(           "Hello world", format("Hello {::?world:earth}", true));
        eq(           "Hello earth", format("Hello {::?world:earth}", false));
    }

}
