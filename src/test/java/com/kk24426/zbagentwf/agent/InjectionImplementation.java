/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：仅用于测试的跨包 Service 注入实现。
 */
package com.kk24426.zbagentwf.agent;

import com.kk24426.zbagentwf.user.InjectionFixture;
import org.springframework.stereotype.Service;

@Service
public class InjectionImplementation implements InjectionFixture.Port {
    public String get() { return "injected"; }
}
