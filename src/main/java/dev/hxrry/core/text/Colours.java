package dev.hxrry.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colours {
    
    // minimessage parser
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    
    // legacy serializer for old-style colour codes
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    
    // hex colour handling
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    // automatic detection 
    /**
     * @param text 
     * @return 
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // first, convert hex colours to minimessage format
        text = convertHexToMiniMessage(text);
        
        // then check if it's minimessage format
        if (text.contains("<") && text.contains(">")) {
            try {
                // try minimessage first
                return MINI.deserialize(text);
            } catch (Exception e) {
                // if minimessage fails fallback to legacy
            }
        }
        
        // convert legacy codes n parse
        return LEGACY.deserialize(text);
    }
    
    /**
     * @param text
     * @return 
     */
    private static String convertHexToMiniMessage(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "<color:#" + hex + ">");
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    // strips all extra shit from string
    /**
     * @param text 
     * @return 
     */
    public static String strip(String text) {
        if (text == null) return null;
        
        Component component = parse(text);
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
    
    /**
     * @param component Component to convert
     * @return Legacy formatted string with & codes
     */
    public static String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }
    
    /**
     * @param text text to gradient
     * @param startHex start colour hex (without #)
     * @param endHex end colour hex (without #)
     * @return gradient component
     */
    public static Component gradient(String text, String startHex, String endHex) {
        String miniMessage = String.format(
            "<gradient:#%s:#%s>%s</gradient>",
            startHex, endHex, text
        );
        return MINI.deserialize(miniMessage);
    }
    
    /**
     * creates rainbow text.
     * 
     * @param text Text to rainbow
     * @return Rainbow component
     */
    public static Component rainbow(String text) {
        return MINI.deserialize("<rainbow>" + text + "</rainbow>");
    }
    
    /**
     * creates a simple coloured component.
     * 
     * @param text the text
     * @param color the colour (hex or name)
     * @param decorations text decorations to apply
     * @return colored component
     */
    public static Component colored(String text, String color, TextDecoration... decorations) {
        TextColor textColor;
        
        // parse color
        if (color.startsWith("#")) {
            textColor = TextColor.fromHexString(color);
        } else {
            // try parsing as mini message colour name innit
            Component temp = MINI.deserialize("<" + color + ">test</" + color + ">");
            textColor = temp.color();
        }
        
        // build component
        Component component = Component.text(text);
        
        if (textColor != null) {
            component = component.color(textColor);
        }
        
        // add decorations
        for (TextDecoration decoration : decorations) {
            component = component.decorate(decoration);
        }
        
        return component;
    }
    
    /**
     * @param receiver who to send to (player or commandsender)
     * @param message message with color codes
     */
    public static void send(org.bukkit.command.CommandSender receiver, String message) {
        receiver.sendMessage(parse(message));
    }
    
    /**
     * Replaces placeholders in text.
     * @param text text with placeholders
     * @param replacements pairs of placeholder, value
     * @return text with replacements
     */
    public static String replace(String text, Object... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be in pairs");
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);
            text = text.replace(placeholder, value);
        }
        
        return text;
    }
    
    // experimental 
    /**
     * @param text text to center
     * @return Centered text with spaces
     */
    public static String center(String text) {
        final int maxWidth = 53;
        int spaces = (maxWidth - strip(text).length()) / 2;
        
        if (spaces <= 0) return text;
        
        return " ".repeat(spaces) + text;
    }
}