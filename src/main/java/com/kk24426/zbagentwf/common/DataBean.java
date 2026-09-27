package com.kk24426.zbagentwf.common;

import java.util.Date;

/**
 * 数据模型的父类,包含通用字段
 */
public class DataBean {
	/** ID */
	private Long id;
	/** 创建时间 */
	private Date creationData;
	/** 最后更新时间 */
	private Date lastupdateData;
	/** 删除标识(默认不做物理删除,只做逻辑删除) */
	private boolean delFlg;
	/** 数据版本,每次更新时+1 */
	private int version;

	private Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	private Date getCreationData() {
		return creationData;
	}

	private void setCreationData(Date creationData) {
		this.creationData = creationData;
	}

	private Date getLastupdateData() {
		return lastupdateData;
	}

	private void setLastupdateData(Date lastupdateData) {
		this.lastupdateData = lastupdateData;
	}

	private boolean isDelFlg() {
		return delFlg;
	}

	private void setDelFlg(boolean delFlg) {
		this.delFlg = delFlg;
	}

	private int getVersion() {
		return version;
	}

	private void setVersion(int version) {
		this.version = version;
	}
}
