package com.kk24426.zbagentwf.common.agent.bean;

/** 普通数据对象；三个属性独立保存，允许 null 和空字符串，不校验或转换输入。 */
public class AgentBean {
    /** Agent 模型提供方，如 ChatGPT、Claude、Deepseek 等。 */
    private String brand;
    /** 具体的模型，如 chatGPT、deepseek、豆包等。 */
    private String name;
    /** 具体的版本，如 GPT-5.6 sol。 */
    private String ver;

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }
}
