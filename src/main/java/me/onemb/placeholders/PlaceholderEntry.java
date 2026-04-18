package me.onemb.placeholders;

import java.util.List;

record PlaceholderEntry(
    String key,
    String category,
    String configPath,
    String description,
    PlaceholderType type,
    String staticValue,
    String builtinSource,
    boolean categoryEnabled,
    boolean rotatingEnabled,
    List<String> rotatingValues
) {

    PlaceholderEntry {
        rotatingValues = List.copyOf(rotatingValues);
    }

    boolean isLiveEligible() {
        return categoryEnabled && switch (type) {
            case STATIC, BUILTIN -> true;
            case ROTATING -> rotatingEnabled && !rotatingValues.isEmpty();
        };
    }

    String fingerprint() {
        return key
            + "|"
            + category
            + "|"
            + description
            + "|"
            + type
            + "|"
            + staticValue
            + "|"
            + builtinSource
            + "|"
            + categoryEnabled
            + "|"
            + rotatingEnabled
            + "|"
            + rotatingValues;
    }
}
