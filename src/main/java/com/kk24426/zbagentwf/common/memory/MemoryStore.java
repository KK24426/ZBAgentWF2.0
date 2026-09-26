/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：按分类和标识保存本进程内的对象引用。
 */
package com.kk24426.zbagentwf.common.memory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 无持久化或自动过期；线程安全范围仅为容器操作，对象内部由业务方协调。 */
public final class MemoryStore {
    private final ConcurrentHashMap<Key, Object> values = new ConcurrentHashMap<>();

    public <T> void put(String namespace, String id, T value) {
        values.put(new Key(namespace, id), Objects.requireNonNull(value, "存储值不能为空。"));
    }

    public <T> Optional<T> get(String namespace, String id, Class<T> type) {
        Objects.requireNonNull(type, "读取类型不能为空。");
        Object value = values.get(new Key(namespace, id));
        if (value != null && !type.isInstance(value)) throw new IllegalArgumentException("存储值类型不匹配。");
        return Optional.ofNullable(type.cast(value));
    }

    public void remove(String namespace, String id) { values.remove(new Key(namespace, id)); }

    // 使用组合键，避免字符串分隔符本身出现在分类或标识中时发生碰撞。
    private record Key(String namespace, String id) {
        private Key {
            if (namespace == null || namespace.isBlank() || id == null || id.isBlank()) {
                throw new IllegalArgumentException("存储分类和标识不能为空白。");
            }
        }
    }
}
