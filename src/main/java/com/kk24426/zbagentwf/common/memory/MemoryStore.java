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

    /**
     * 按分类和ID保存非空对象原引用，同键写入替换原值；不复制、不持久化，也不关闭旧对象。
     * @throws IllegalArgumentException 分类或ID为空白
     * @throws NullPointerException value为null
     */
    public <T> void put(String namespace, String id, T value) {
        values.put(new Key(namespace, id), Objects.requireNonNull(value, "存储值不能为空。"));
    }

    /**
     * 按分类和ID查询并核验调用方声明的类型；命中返回原引用，内部对象并发由调用方协调。
     * @param type 非空的期望类型，用于运行时校验
     * @return 未命中为空Optional，不自动创建对象
     * @throws IllegalArgumentException 键无效或已保存对象类型不匹配
     */
    public <T> Optional<T> get(String namespace, String id, Class<T> type) {
        Objects.requireNonNull(type, "读取类型不能为空。");
        Object value = values.get(new Key(namespace, id));
        if (value != null && !type.isInstance(value)) throw new IllegalArgumentException("存储值类型不匹配。");
        return Optional.ofNullable(type.cast(value));
    }

    /**
     * 只删除键对应的内存引用，未命中无操作；不删除文件、不停止执行器，也不级联处理业务对象。
     */
    public void remove(String namespace, String id) { values.remove(new Key(namespace, id)); }

    /** 分类和实体标识组成的精确键，避免拼接分隔符与原始键内容发生碰撞。 */
    private record Key(String namespace, String id) {
        /** 拒绝空值或纯空白键，保留原始字符串，不裁剪或转换大小写。 */
        private Key {
            if (namespace == null || namespace.isBlank() || id == null || id.isBlank()) {
                throw new IllegalArgumentException("存储分类和标识不能为空白。");
            }
        }
    }
}
