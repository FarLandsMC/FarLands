package net.farlands.sanctuary.data.struct;

import java.util.Arrays;

/**
 * Handles player pronouns.
 */
public record Pronouns(
    SubjectPronoun subject,
    ObjectPronoun object,
    boolean showOnDiscord
) {

    public Pronouns(SubjectPronoun subject, ObjectPronoun object) {
        this(subject, object, false);
    }

    public Pronouns withShowOnDiscord(boolean showOnDiscord) {
        return new Pronouns(
            this.subject,
            this.object,
            showOnDiscord
        );
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean parenthesis) {
        if (subject == null) {
            return null;
        }
        return parenthesis ? (
            subject.hasNoObject() || object == null ?
                "(" + subject.getHumanName() + ")" :
                "(" + subject.getHumanName() + "/" + object.getHumanName() + ")"
        ) : (
            subject.hasNoObject() || object == null ?
                subject.getHumanName() :
                subject.getHumanName() + "/" + object.getHumanName()
        );
    }

    // Following pronoun options based off of https://academicguides.waldenu.edu/diversity-inclusion/pronouns

    public enum SubjectPronoun {
        SHE("She"),
        HE("He"),
        THEY("They"),
        XE("Xe"),
        ZE("Ze"),
        E("E"),
        ALL("All"),
        OTHER("Other", true);

        public static final SubjectPronoun[] VALUES = values();

        private final String  humanName;
        private final boolean noObject; // If there is no object that goes with this subject, like "Other"

        SubjectPronoun(String humanName, boolean noObject) {
            this.humanName = humanName;
            this.noObject = noObject;
        }

        SubjectPronoun(String humanName) {
            this(humanName, false);
        }

        public String getHumanName() {
            return humanName;
        }

        public boolean hasNoObject() {
            return noObject;
        }

        public static SubjectPronoun findByHumanName(String humanName) throws IllegalArgumentException {
            return Arrays.stream(VALUES)
                .filter(sp -> sp.getHumanName().equalsIgnoreCase(humanName))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException("SubjectPronouns has no human name \"" + humanName + "\"")
                );

        }
    }

    public enum ObjectPronoun {
        HER("Her"),
        HIM("Him"),
        THEM("Them"),
        XEM("Xem"),
        HIR("Hir"),
        ZIR("Zir"),
        EM("Em"),
        ANY("Any");

        public static final ObjectPronoun[] VALUES = values();

        private final String humanName;

        ObjectPronoun(String humanName) {
            this.humanName = humanName;
        }

        public String getHumanName() {
            return humanName;
        }

        public static ObjectPronoun findByHumanName(String humanName) throws IllegalArgumentException {
            return Arrays.stream(VALUES)
                .filter(op -> op.getHumanName().equalsIgnoreCase(humanName))
                .findFirst()
                .orElseThrow(
                    () -> new IllegalArgumentException("ObjectPronouns has no human name \"" + humanName + "\"")
                );

        }
    }
}
