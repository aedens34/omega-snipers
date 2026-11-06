package com.example.omegasnipers;

import java.util.ArrayList;
import java.util.List;

/**
 * Purely cosmetic name/lore generator. Combines an adjective-ish prefix with
 * a mythic noun suffix to produce well over 100 distinct, original weapon
 * names ("Omega Wraithfang MK-IV", "Omega Voidreaper MK-IX", ...) with no
 * real brand names or real-world weapon model names used anywhere.
 *
 * 12 prefixes x 10 suffixes = 120 combinations, each assigned a tier 1-10
 * (tier = (index % 10) + 1) so damage scales and the rarest names are rarer.
 */
public final class OmegaLore {

    public record Entry(String displayName, String loreLine, int tier) {
    }

    private static final String[] PREFIXES = {
            "Wraith", "Void", "Storm", "Ashen", "Ember", "Frost",
            "Astral", "Umbral", "Radiant", "Fallen", "Hollow", "Eclipse"
    };

    private static final String[] SUFFIXES = {
            "fang", "reaper", "piercer", "caller", "whisper", "bringer",
            "warden", "herald", "sentinel", "oracle"
    };

    private static final String[] LORE_LINES = {
            "Forged in a forgotten armory, it never misses what it marks.",
            "Legends say it chooses its own targets before the trigger is even pulled.",
            "A single pull is said to echo across every battlefield at once.",
            "No wall, no distance, no excuse has ever stopped this weapon.",
            "Its scope sees through more than just glass and steel.",
            "Carried by those who were tired of aiming.",
            "The last sound its targets hear is silence.",
            "Said to be cursed - or blessed, depending on which end you're on.",
            "Its true range has never been measured, because it has never been needed.",
            "Some weapons find their target. This one finds everyone.",
    };

    private OmegaLore() {
    }

    /**
     * Produces {@code count} entries. The 120 base prefix/suffix combinations
     * repeat (with an increasing " (IIVariant" style roman-numeral suffix) if
     * count is pushed above 120 via config.
     */
    public static List<Entry> generate(int count) {
        List<Entry> out = new ArrayList<>(count);
        int baseCombos = PREFIXES.length * SUFFIXES.length; // 120

        for (int i = 0; i < count; i++) {
            int comboIndex = i % baseCombos;
            int cycle = i / baseCombos; // 0 for the first pass through all 120 names

            String prefix = PREFIXES[comboIndex / SUFFIXES.length];
            String suffix = SUFFIXES[comboIndex % SUFFIXES.length];
            int tier = (comboIndex % 10) + 1;

            String mk = toRoman(((comboIndex / 10) % 10) + 1);
            String name = "Omega " + prefix + suffix + " MK-" + mk;
            if (cycle > 0) {
                name += " (" + toRoman(cycle + 1) + ")";
            }

            String lore = LORE_LINES[comboIndex % LORE_LINES.length];
            out.add(new Entry(name, lore, tier));
        }
        return out;
    }

    private static String toRoman(int number) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number >= 1 && number <= romans.length) {
            return romans[number - 1];
        }
        return String.valueOf(number);
    }
}
