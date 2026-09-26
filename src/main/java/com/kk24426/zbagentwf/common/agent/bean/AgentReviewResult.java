package com.kk24426.zbagentwf.common.agent.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * AgentReview后返回的结果
 */
public class AgentReviewResult {
	/** review是否通过，fase:不通过,true:通过 */
	private boolean isApprove;

	/**
	 * 不通过的理由，原因可能有多个，所以使用集合
	 */
	private List<String> reasons = new ArrayList<String>();

	/**
	 * 虽然通过，但是可能有建议优化的地方
	 */
	private List<String> suggestions = new ArrayList<String>();
}
