package pl.inh.bottleExp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtils {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // &#RRGGBB legacy hex
    private static final Pattern LEGACY_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    // {gradient:#START:#END:text}
    private static final Pattern GRADIENT_TAG = Pattern.compile("\\{gradient:([#A-Fa-f0-9]+):([#A-Fa-f0-9]+):([^}]+)}");
    // {rainbow:text} or {rainbow:sat:bright:offset:text}
    private static final Pattern RAINBOW_TAG = Pattern.compile("\\{rainbow:(?:([0-9.]+):([0-9.]+):([0-9.]+):)?([^}]+)}");

    private ColorUtils() {}

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Universal colorizer. Handles all syntaxes in one pass:
     * <ul>
     *   <li>{gradient:#ff0000:#00ff00:Some Text}</li>
     *   <li>{rainbow:Some Text} or {rainbow:1.0:1.0:0.0:Some Text}</li>
     *   <li>MiniMessage tags: &lt;red&gt;, &lt;#hex&gt;, &lt;gradient:...&gt;</li>
     *   <li>Legacy: &amp;c, &amp;l, &amp;#RRGGBB</li>
     * </ul>
     */
    public static @NotNull Component colorize(@NotNull String text) {
        return buildComponent(text);
    }

    public static @NotNull String colorizeString(@NotNull String text) {
        return toLegacy(colorize(text));
    }

    /**
     * Colorize a list of strings into lore lines.
     * Auto-strips italic (Minecraft default for lore).
     */
    public static @NotNull List<Component> colorizeLore(@NotNull List<String> lines) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            Component c = colorize(line).decoration(TextDecoration.ITALIC, false);
            result.add(c);
        }
        return result;
    }

    public static @NotNull List<String> colorizeLoreStrings(@NotNull List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(colorizeString(line));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL PIPELINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Splits the input into segments: custom tags vs plain text,
     * processes each, then joins into one Component.
     */
    private static @NotNull Component buildComponent(@NotNull String text) {
        // Combined pattern to find any custom block tag
        Pattern combined = Pattern.compile(
                "\\{gradient:[^}]+}|\\{rainbow:[^}]+}"
        );

        TextComponent.Builder builder = Component.text();
        Matcher matcher = combined.matcher(text);
        int last = 0;

        while (matcher.find()) {
            // Process plain text before this tag
            if (matcher.start() > last) {
                String plain = text.substring(last, matcher.start());
                builder.append(parsePlain(plain));
            }

            String tag = matcher.group();
            builder.append(parseCustomTag(tag));
            last = matcher.end();
        }

        // Remaining text after last tag
        if (last < text.length()) {
            builder.append(parsePlain(text.substring(last)));
        }

        return builder.build();
    }

    /**
     * Handles plain text segments — translates legacy hex then passes to MiniMessage.
     */
    private static @NotNull Component parsePlain(@NotNull String text) {
        // &#RRGGBB → <#RRGGBB> so MiniMessage can handle it
        Matcher hexMatcher = LEGACY_HEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);

        // & codes → MiniMessage legacy_ tags
        String withLegacy = sb.toString();
        // Use legacy serializer first, then re-serialize to MiniMessage for unified handling
        // Actually: translate & → § then deserialize with LegacyComponentSerializer
        // But we want MiniMessage tags (<red> etc.) to also work in same string.
        // Strategy: replace & codes with MiniMessage equivalents
        withLegacy = translateAmpersandToMiniMessage(withLegacy);

        return MM.deserialize(withLegacy);
    }

    /**
     * Translates &x legacy codes to their MiniMessage equivalents.
     * &l → <bold>, &c → <red>, etc.
     * Unknown codes are left as-is.
     */
    private static @NotNull String translateAmpersandToMiniMessage(@NotNull String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                String tag = legacyCodeToMiniMessage(code);
                if (tag != null) {
                    sb.append(tag);
                    i++; // skip code char
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String legacyCodeToMiniMessage(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underlined>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default  -> null;
        };
    }

    /**
     * Dispatches custom {gradient:...} and {rainbow:...} tags.
     */
    private static @NotNull Component parseCustomTag(@NotNull String tag) {
        Matcher grad = GRADIENT_TAG.matcher(tag);
        if (grad.matches()) {
            TextColor start = parseColor(grad.group(1));
            TextColor end   = parseColor(grad.group(2));
            String text     = grad.group(3);
            return buildGradient(text, start, end);
        }

        Matcher rain = RAINBOW_TAG.matcher(tag);
        if (rain.matches()) {
            if (rain.group(1) != null) {
                float sat    = Float.parseFloat(rain.group(1));
                float bright = Float.parseFloat(rain.group(2));
                float offset = Float.parseFloat(rain.group(3));
                String text  = rain.group(4);
                return buildRainbow(text, sat, bright, offset);
            } else {
                String text = rain.group(4);
                return buildRainbow(text, 1.0f, 1.0f, 0.0f);
            }
        }

        // Fallback — return as plain text
        return Component.text(tag);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRADIENT & RAINBOW BUILDERS
    // ─────────────────────────────────────────────────────────────────────────

    private static @NotNull Component buildGradient(
            @NotNull String text,
            @NotNull TextColor start,
            @NotNull TextColor end
    ) {
        if (text.isEmpty()) return Component.empty();
        int len = text.length();
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < len; i++) {
            float t = (len == 1) ? 0f : (float) i / (len - 1);
            builder.append(Component.text(text.charAt(i)).color(TextColor.lerp(t, start, end)));
        }
        return builder.build();
    }

    private static @NotNull Component buildRainbow(
            @NotNull String text,
            float saturation,
            float brightness,
            float hueOffset
    ) {
        if (text.isEmpty()) return Component.empty();
        int len = text.length();
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < len; i++) {
            float hue = (hueOffset + (float) i / len) % 1.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
            builder.append(Component.text(text.charAt(i)).color(TextColor.color(rgb)));
        }
        return builder.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COLOR PARSING
    // ─────────────────────────────────────────────────────────────────────────

    private static @NotNull TextColor parseColor(@NotNull String raw) {
        String hex = raw.startsWith("#") ? raw : "#" + raw;
        TextColor color = TextColor.fromHexString(hex);
        if (color == null) throw new IllegalArgumentException("Invalid color: " + raw);
        return color;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERIALIZATION UTILS
    // ─────────────────────────────────────────────────────────────────────────

    public static @NotNull String toPlain(@NotNull Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static @NotNull String toLegacy(@NotNull Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static @NotNull String toMiniMessage(@NotNull Component component) {
        return MM.serialize(component);
    }
}