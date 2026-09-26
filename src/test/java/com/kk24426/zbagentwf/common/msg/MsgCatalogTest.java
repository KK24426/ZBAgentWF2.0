/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证三语消息、纯文本参数、启动校验与外部覆盖快照。
 */
package com.kk24426.zbagentwf.common.msg;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MsgCatalogTest {
    @TempDir Path temp;

    @Test
    void loadsThreeLanguagesIndependentlyOfJvmDefaultAndPreservesArguments() throws Exception {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPANESE);
            MsgCatalog messages = MsgCatalog.load(temp.resolve("absent"));
            assertEquals("发送中…", messages.get("chat.sending", Locale.SIMPLIFIED_CHINESE));
            assertEquals("Sending…", messages.get("chat.sending", Locale.US));
            assertEquals("送信中…", messages.get("chat.sending", Locale.JAPAN));
            assertEquals("发送中…", messages.get("chat.sending", Locale.FRENCH));
            assertEquals("发送中…", messages.get("chat.sending", null));
            String literal = "<b>$1\\{0}'请求</b>";
            assertEquals("Request ID: " + literal, messages.get("chat.requestId", Locale.ENGLISH, literal));
            assertThrows(IllegalArgumentException.class, () -> messages.get("chat.requestId", Locale.ENGLISH));
            assertThrows(IllegalArgumentException.class, () -> messages.get("unknown.key", Locale.ENGLISH));
            var exported = messages.browserMessages();
            assertEquals(exported.get("en").keySet(), exported.get("ja").keySet());
            assertEquals(exported.get("en").keySet(), exported.get("zh-CN").keySet());
            assertTrue(exported.values().stream().allMatch(m -> m.keySet().stream().noneMatch(k -> k.startsWith("http."))));
            assertThrows(UnsupportedOperationException.class, () -> exported.get("en").put("chat.send", "changed"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void partialUtf8OverrideIsPerLanguageAndOnlyChangesAfterReload() throws Exception {
        Path file = temp.resolve("msg_en.properties");
        Files.writeString(file, "chat.sending=自定义 Sending 日本語\nchat.requestId=ID '{0}'\n");
        MsgCatalog first = MsgCatalog.load(temp);
        assertEquals("自定义 Sending 日本語", first.get("chat.sending", Locale.ENGLISH));
        assertEquals("Send request", first.get("chat.send", Locale.ENGLISH));
        assertEquals("送信中…", first.get("chat.sending", Locale.JAPANESE));
        assertEquals("ID 'value'", first.get("chat.requestId", Locale.ENGLISH, "value"));
        Files.writeString(file, "chat.sending=Changed\n");
        assertEquals("自定义 Sending 日本語", first.get("chat.sending", Locale.ENGLISH));
        assertEquals("Changed", MsgCatalog.load(temp).get("chat.sending", Locale.ENGLISH));
    }

    @Test
    void unknownDuplicateBlankMalformedAndMismatchedOverridesFailWithoutEchoingValues() throws Exception {
        for (String content : new String[]{
                "private.unknown=private-config-value\n", "chat.send=private-config-value\nchat.send=duplicate\n",
                "chat.send=   \n", "chat.requestId=private-config-value\n", "chat.requestId={1}\n",
                "chat.send={bad}\n", "chat.send=\\uZZZZ\n", "chat.send={99999999999999999999}\n"}) {
            Files.writeString(temp.resolve("msg_en.properties"), content);
            var failure = assertThrows(RuntimeException.class, () -> MsgCatalog.load(temp));
            assertFalse(failure.toString().contains("private-config-value"));
            assertFalse(failure.toString().contains("private.unknown"));
        }
    }

    @Test
    void existingUnreadableAsFileAndInvalidUtf8AreNotTreatedAsMissing() throws Exception {
        Path directoryFile = Files.writeString(temp.resolve("not-a-directory"), "fixture");
        assertThrows(IOException.class, () -> MsgCatalog.load(directoryFile));
        Path file = Files.createDirectory(temp.resolve("msg_en.properties"));
        assertThrows(IOException.class, () -> MsgCatalog.load(temp));
        Files.delete(file);
        Files.write(file, new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThrows(IOException.class, () -> MsgCatalog.load(temp));
    }

    @Test
    void invalidArgumentNumberRetainsSafeCauseAndStack() throws Exception {
        Files.writeString(temp.resolve("msg_en.properties"), "chat.send={99999999999999999999}\n");
        var failure = assertThrows(IllegalStateException.class, () -> MsgCatalog.load(temp));
        assertInstanceOf(NumberFormatException.class, failure.getCause());
        assertTrue(failure.getCause().getStackTrace().length > 0);
        assertFalse(failure.getCause().getMessage().contains("99999999999999999999"));
    }
}
