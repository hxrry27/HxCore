package dev.hxrry.hxcore.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colours {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // &#RRGGBB or §#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("[&§]#([A-Fa-f0-9]{6})");

    // legacy code -> minimessage tag
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
        Map.entry('0', "<black>"),        Map.entry('1', "<dark_blue>"),
        Map.entry('2', "<dark_green>"),   Map.entry('3', "<dark_aqua>"),
        Map.entry('4', "<dark_red>"),     Map.entry('5', "<dark_purple>"),
        Map.entry('6', "<gold>"),         Map.entry('7', "<gray>"),
        Map.entry('8', "<dark_gray>"),    Map.entry('9', "<blue>"),
        Map.entry('a', "<green>"),        Map.entry('b', "<aqua>"),
        Map.entry('c', "<red>"),          Map.entry('d', "<light_purple>"),
        Map.entry('e', "<yellow>"),       Map.entry('f', "<white>"),
        Map.entry('k', "<obfuscated>"),   Map.entry('l', "<bold>"),
        Map.entry('m', "<strikethrough>"),Map.entry('n', "<underlined>"),
        Map.entry('o', "<italic>"),       Map.entry('r', "<reset>")
    );

    
    // parse any of minimessage, legacy &/§, &#hex, or a mix into a component
     
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        text = convertHex(text);
        text = convertLegacy(text);

        try {
            return MINI.deserialize(text);
        } catch (Exception e) {
            // minimessage is lenient, but if something truly malformed slips through,
            // showing the raw text beats showing nothing (i hope)
            return Component.text(text);
        }
    }

    private static String convertHex(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<color:#" + matcher.group(1) + ">");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String convertLegacy(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                String tag = LEGACY_TAGS.get(Character.toLowerCase(text.charAt(i + 1)));
                if (tag != null) {
                    out.append(tag);
                    i++; // consume the code character too
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    public static String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    public static String strip(String text) {
        if (text == null) return null;
        return PlainTextComponentSerializer.plainText().serialize(parse(text));
    }

    public static Component gradient(String text, String startHex, String endHex) {
        return MINI.deserialize("<gradient:#" + startHex + ":#" + endHex + ">" + text + "</gradient>");
    }

    public static Component rainbow(String text) {
        return MINI.deserialize("<rainbow>" + text + "</rainbow>");
    }

    public static void send(org.bukkit.command.CommandSender receiver, String message) {
        receiver.sendMessage(parse(message));
    }
}
