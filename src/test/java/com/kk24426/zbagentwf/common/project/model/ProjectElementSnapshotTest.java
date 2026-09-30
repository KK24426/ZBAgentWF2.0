/*
 * 创建日期：2026-09-30
 * 更新日期：2026-09-30
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：验证功能历史快照的引用列表隔离与不可变边界。
 */
package com.kk24426.zbagentwf.common.project.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kk24426.zbagentwf.common.project.model.ProjectElementSnapshot.FunctionSnapshot;

/** 只验证本轮已实现的快照行为，不用替身模拟尚未实现的发布或持久化规则。 */
class ProjectElementSnapshotTest {

    /** 调用方继续修改原列表或实体不会改写已经捕获的功能历史。 */
    @Test
    void snapshotDetachesReferencesFromMutableFunction() {
        var refs = new ArrayList<>(List.of("src/OrderService.java", "POST /orders"));
        var function = new ProjectFunction();
        function.setId(3L);
        function.setProjectModuleId(2L);
        function.setName("新增订单");
        function.setSummary("创建订单");
        function.setDescription("校验商品并保存订单");
        function.setImplementationRefs(refs);
        function.setVersion(4);

        var snapshot = new FunctionSnapshot(function.getId(), function.getProjectModuleId(),
                function.getName(), function.getSummary(), function.getDescription(),
                function.getImplementationRefs(), function.getVersion(), function.isDelFlg());
        var history = new ProjectChangeRecord();
        history.setAfterSnapshot(snapshot);

        refs.clear();
        refs.add("DELETE /orders");
        function.setName("删除订单");
        function.setImplementationRefs(null);
        function.setVersion(5);

        var saved = (FunctionSnapshot) history.getAfterSnapshot();
        assertEquals("新增订单", saved.name());
        assertEquals(4, saved.version());
        assertEquals(List.of("src/OrderService.java", "POST /orders"), saved.implementationRefs());
        assertThrows(UnsupportedOperationException.class,
                () -> saved.implementationRefs().add("PUT /orders"));
        assertThrows(UnsupportedOperationException.class,
                () -> saved.implementationRefs().set(0, "replacement"));
    }

    /** 可空引用与空引用列表保留其输入含义，新增候选不自动生成身份或版本。 */
    @Test
    void optionalReferencesPreserveNullAndEmptyWithoutGeneratingMetadata() {
        var absent = new FunctionSnapshot(null, null, "新增订单", null, null, null, null, false);
        assertNull(absent.implementationRefs());
        assertNull(absent.id());
        assertNull(absent.projectModuleId());
        assertNull(absent.version());

        var refs = new ArrayList<String>();
        var empty = new FunctionSnapshot(null, null, "新增订单", null, null, refs, null, false);
        refs.add("POST /orders");
        assertEquals(List.of(), empty.implementationRefs());
        assertThrows(UnsupportedOperationException.class, () -> empty.implementationRefs().add("new"));
    }

    /** 非空引用列表不能包含 null 元素，避免产生含无效引用槽位的不可变历史。 */
    @Test
    void referencesRejectNullElements() {
        assertThrows(NullPointerException.class, () -> new FunctionSnapshot(
                3L, 2L, "新增订单", "创建订单", "说明",
                Arrays.asList("POST /orders", null), 4, false));
    }
}
