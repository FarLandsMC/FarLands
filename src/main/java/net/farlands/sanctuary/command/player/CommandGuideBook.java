package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import static net.farlands.sanctuary.util.FLUtils.giveItem;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.Logging;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandGuideBook extends PlayerCommand {
    private final ItemStack book;

    public CommandGuideBook() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Get a guidebook for the server.", "/guidebook", "guidebook");
        this.book = buildBook();
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);

        // Check the cooldown
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            sendFormatted(sender, "&(red)You can use this command again in %0.",
                    TimeInterval.formatTime(cooldownTime * 50L, false));
            return true;
        }

        session.setCommandCooldown(this, 60L * 20L);
        giveItem(sender, book.clone(), true);

        return true;
    }

    private static ItemStack buildBook() {
        ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK);
        writtenBook.setItemMeta(writeBook(writtenBook));
        return writtenBook;
    }

    private static List<String> loadBook() {
        try {
            return Arrays.asList(FarLands.getDataHandler().getDataTextFile("guidebook.txt").split("\n"));
        }catch(IOException ex) {
            Logging.error("Failed to load guidebook file.");
            ex.printStackTrace(System.out);
        }
        return Collections.emptyList();
    }

    private static BookMeta writeBook(ItemStack writtenBook) {
        BookMeta bookMeta = (BookMeta) writtenBook.getItemMeta();
        bookMeta.setTitle("Guide Book");
        bookMeta.setAuthor("Farlands Staff");
        bookMeta.setPages(loadBook());
        return bookMeta;
    }
}
