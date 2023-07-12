package net.farlands.newheights;

import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.Worlds;
import net.farlands.sanctuary.util.ComponentUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CompFormatTest {

    private static void a(String expected, String format, Object... values) {
        String a = ComponentUtils.toText(ComponentUtils.format(format, values));
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

}
