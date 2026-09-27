package com.kk24426.zbagentwf.common;

import java.util.List;

/**
 * 基础的Dao
 */
public interface DataBeanDao<T extends DataBean> {
	public List<T> selectAll();

	public T selectById(Long Id);

	public int update(T t);

	public int update(List<T> list);
	
	/**
	 * 插入时自动生成ID
	 * @param t 更新对象
	 * @return 更新件数
	 */
	public int insert(T t);
	
	/**
	 * 插入时自动生成ID
	 * @param t 更新对象
	 * @return 更新件数
	 */
	public int insertAll(List<T> list);
	
}
