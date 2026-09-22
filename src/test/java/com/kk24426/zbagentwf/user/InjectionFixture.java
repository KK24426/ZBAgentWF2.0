/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：仅用于测试的用户接口与调用方，不进入分发 JAR。
 */
package com.kk24426.zbagentwf.user;

import org.springframework.stereotype.Component;

@Component
public class InjectionFixture {
    public interface Port { String get(); }
    private final Port port;
    public InjectionFixture(Port port) { this.port = port; }
    public String value() { return port.get(); }
}
