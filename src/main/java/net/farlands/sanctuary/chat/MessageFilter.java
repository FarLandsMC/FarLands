package net.farlands.sanctuary.chat;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.util.FLUtils;
import net.farlands.sanctuary.util.Logging;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageFilter {

    public static final MessageFilter INSTANCE = new MessageFilter();

    private final Map<String, Boolean> words;
    private final List<String> replacements;
    private final Random rng;

    private void init() {
        try {
            Stream.of(FarLands.getDataHandler().getDataTextFile("censor-dict.txt").split("\n")).forEach(line -> {
                boolean ac = line.startsWith("AC:");
                words.put(ac ? line.substring(3) : line, ac);
            });
            replacements.addAll(Arrays.asList(FarLands.getDataHandler().getDataTextFile("censor-replacements.txt").split("\n")));
        } catch (IOException ex) {
            Logging.error("Failed to load words and replacements for message filter words.");
            throw new RuntimeException(ex);
        }
    }

    MessageFilter() {
        this.words = new HashMap<>();
        this.replacements = new ArrayList<>();
        this.rng = new Random();
        init();
    }

    public String censor(String s) {
        String censored = s.toLowerCase();
        for (String word : words.keySet()) {
            censored = censored.replaceAll("(^|\\W)\\Q" + word + "\\E($|\\W)", getRandomReplacement());
        }
        return FLUtils.matchCase(s, censored);
    }

    public Component censor(Component component) {
        for (String word : words.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList())) {
            component = component.replaceText(
                TextReplacementConfig.builder()
                    .match("(?i)(^|\\W)\\Q" + word + "\\E($|\\W)")
                    .replacement(getRandomReplacement())
                    .build()
            );
        }
        return component;
    }

    public boolean isProfane(String s) {
        return !s.equals(censor(s));
    }

    public boolean autoCensor(String s) {
        String censored = s.toLowerCase();
        for (String word : words.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList())) {
            censored = censored.replaceAll("(?i)(^|\\W)\\Q" + word + "\\E($|\\W)", " ");
        }
        return !s.equalsIgnoreCase(censored);
    }

    String getRandomReplacement() {
        return ' ' + replacements.get(rng.nextInt(replacements.size())) + ' ';
    }
}

