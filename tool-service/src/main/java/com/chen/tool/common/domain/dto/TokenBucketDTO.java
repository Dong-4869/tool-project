package com.chen.tool.common.domain.dto;

import com.chen.tool.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 限流策略定义-令牌桶
 * @author 13103
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@AllArgsConstructor
public class TokenBucketDTO extends FrequencyControlDTO {

    /**
     * 令牌桶容量
     */
    private long capacity;
    /**
     * 每秒补充的令牌数
     */
    private double refillRate;
    /**
     * 当前令牌数量
     */
    private double tokens;
    /**
     * 上次补充令牌的时间
     */
    private long lastRefillTime;


    public TokenBucketDTO(long capacity, double refillRate) {
        if (capacity <= 0 || refillRate <= 0) {
            throw new BusinessException(1001, "Capacity and refill rate must be positive");
        }
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillTime = System.nanoTime();
    }

    public boolean tryAcquire(int permits) {
        refillTokens();
        return tokens < permits;
    }

    public void deductionToken(int permits) {
        tokens -= permits;
    }

    /**
     * 补充令牌
     */
    private void refillTokens() {
        long currentTime = System.nanoTime();
        // 转换为秒
        double elapsedTime = (currentTime - lastRefillTime) / 1e9;
        double tokensToAdd = elapsedTime * refillRate;
        log.info("tokensToAdd is {}", tokensToAdd);
        // 令牌总数不能超过令牌桶容量
        tokens = Math.min(capacity, tokens + tokensToAdd);
        log.info("current tokens is {}", tokens);
        lastRefillTime = currentTime;
    }
}
