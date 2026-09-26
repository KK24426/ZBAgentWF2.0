/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证共享内存存储的类型、分类、引用和并发边界。
 */
package com.kk24426.zbagentwf.common.memory;

import static org.junit.jupiter.api.Assertions.*;
import com.kk24426.zbagentwf.common.project.bean.Project;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class MemoryStoreTest {
    @Test void namespacesTypesAndExplicitRemovalAreIndependent() {
        var store = new MemoryStore(); var project = new Project();
        store.put("project", "same", project); store.put("draft", "same", "草稿");
        assertSame(project, store.get("project", "same", Project.class).orElseThrow());
        project.setProjectId("updated");
        assertEquals("updated", store.get("project", "same", Project.class).orElseThrow().getProjectId());
        assertThrows(IllegalArgumentException.class, () -> store.get("project", "same", String.class));
        store.remove("project", "same");
        assertTrue(store.get("project", "same", Project.class).isEmpty());
        assertEquals("草稿", store.get("draft", "same", String.class).orElseThrow());
        assertTrue(new MemoryStore().get("draft", "same", String.class).isEmpty());
        store.put("a:b", "c", 1); store.put("a", "b:c", 2);
        assertEquals(1, store.get("a:b", "c", Integer.class).orElseThrow());
        assertEquals(2, store.get("a", "b:c", Integer.class).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> store.put(" ", "id", "value"));
        assertThrows(IllegalArgumentException.class, () -> store.get("project", null, Project.class));
        assertThrows(NullPointerException.class, () -> store.put("project", "id", null));
    }

    @Test void concurrentCallersKeepSeparateEntries() throws Exception {
        var store = new MemoryStore();
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < 100; i++) {
                String id = "id-" + i;
                futures.add(threads.submit(() -> {
                    store.put("data", id, id);
                    assertEquals(id, store.get("data", id, String.class).orElseThrow());
                    store.remove("data", id);
                    assertTrue(store.get("data", id, String.class).isEmpty());
                }));
            }
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
        }
    }
}
