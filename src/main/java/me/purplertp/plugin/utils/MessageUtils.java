package me.purplertp.plugin.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class MessageUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String format(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, ChatColor.of((String)("#" + hex)).toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)sb.toString());
    }
}

