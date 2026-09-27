/*
 * 创建日期：2026-09-27
 * 更新日期：2026-09-27
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存Skill 名称，不自动加载或执行。
 */
package com.kk24426.zbagentwf.common.agent.model;

/** 普通数据对象，属性原样存取。 */
public class Skill {
    private String skillName;
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
}
