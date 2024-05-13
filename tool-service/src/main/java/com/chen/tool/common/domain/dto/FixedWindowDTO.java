package com.chen.tool.common.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 限流策略定义-固定时间
 * @author 13103
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FixedWindowDTO extends FrequencyControlDTO {

    /**
     * 频控时间范围，默认单位秒
     */
    private Integer time;
}
