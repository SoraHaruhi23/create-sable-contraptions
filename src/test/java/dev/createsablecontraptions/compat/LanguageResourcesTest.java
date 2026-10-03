package dev.createsablecontraptions.compat;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LanguageResourcesTest {
    private Map<String,String> strings(String locale) throws Exception {
        var text = Files.readString(Path.of("src/main/resources/assets/create_sable_contraptions/lang/"+locale+".json"));
        var matcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(text);
        var result = new HashMap<String,String>();
        while (matcher.find()) assertNull(result.put(matcher.group(1),matcher.group(2)),"Duplicate translation key");
        return result;
    }
    @Test void supportedLanguagesHaveMatchingKeysAndPlaceholders() throws Exception {
        var english = strings("en_us");
        assertTrue(english.containsKey("csc.config.clearance.tooltip"));
        for (String locale : List.of("zh_cn","zh_tw","ja_jp")) {
            var translated = strings(locale);
            assertEquals(english.keySet(),translated.keySet(),locale);
            for (String key : english.keySet()) {
                assertFalse(translated.get(key).isBlank(),locale+":"+key);
                assertEquals(english.get(key).split("%s",-1).length,translated.get(key).split("%s",-1).length,locale+":"+key);
            }
        }
    }
}
