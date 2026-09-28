/*
 * 创建日期：2026-09-28
 * 更新日期：2026-09-28
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：保存数据库主键、时间、逻辑删除及版本元数据。
 */
package com.kk24426.zbagentwf.common;

import java.util.Date;

/** 公共持久化元数据；普通存取不触发数据库操作，生成值由DAO提交成功后发布。 */
public class DataBean {
	/** 数据库生成Long主键；插入提交前为null，不是业务UUID。 */
	private Long id;
	/** 创建时间 */
	private Date creationData;
	/** 最后更新时间 */
	private Date lastupdateData;
	/** 删除标识(默认不做物理删除,只做逻辑删除) */
	private boolean delFlg;
	/** 乐观锁版本；DAO成功更新提交后加1，普通setter不执行业务更新。 */
	private int version;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreationData() {
		return creationData;
	}

	public void setCreationData(Date creationData) {
		this.creationData = creationData;
	}

	public Date getLastupdateData() {
		return lastupdateData;
	}

	public void setLastupdateData(Date lastupdateData) {
		this.lastupdateData = lastupdateData;
	}

	public boolean isDelFlg() {
		return delFlg;
	}

	public void setDelFlg(boolean delFlg) {
		this.delFlg = delFlg;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
