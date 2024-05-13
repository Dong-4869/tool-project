package com.chen.tool.common.aspect;


import cn.hutool.core.text.CharSequenceUtil;
import com.chen.tool.common.annotation.FrequencyControl;
import com.chen.tool.common.domain.dto.FixedWindowDTO;
import com.chen.tool.common.domain.dto.FrequencyControlDTO;
import com.chen.tool.common.domain.dto.SlidingWindowDTO;
import com.chen.tool.common.domain.dto.TokenBucketDTO;
import com.chen.tool.common.enums.FrequencyControlConstant;
import com.chen.tool.common.frequencycontrol.AbstractFrequencyControlService;
import com.chen.tool.common.frequencycontrol.FrequencyControlStrategyFactory;
import com.chen.tool.common.util.SpElUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 频控实现
 */
@Slf4j
@Aspect
@Component
public class FrequencyControlAspect {
    @Around("@annotation(com.chen.tool.common.annotation.FrequencyControl)||@annotation(com.chen.tool.common.annotation.FrequencyControlContainer)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        FrequencyControl[] annotationsByType = method.getAnnotationsByType(FrequencyControl.class);
        Map<String, FrequencyControl> keyMap = new HashMap<>(16);
        String strategyName = FrequencyControlConstant.TOTAL_COUNT_WITH_IN_FIX_TIME;
        for (int i = 0; i < annotationsByType.length; i++) {
            // 获取频控注解
            FrequencyControl frequencyControl = annotationsByType[i];
            // 默认方法限定名 + 注解排名（可能多个）
            String prefix = CharSequenceUtil.isBlank(frequencyControl.prefixKey()) ?
                    method.toGenericString() + ":index:" + i : frequencyControl.prefixKey();
            String key = "";
            switch (frequencyControl.target()) {
                case INPUT:
                    key = frequencyControl.spEl();
                    break;
                case EL:
                    key = SpElUtils.parseSpEl(method, joinPoint.getArgs(), frequencyControl.spEl());
                    break;
                case IP:
                    // 替换成获取IP方法
                    key = "ip";
                    break;
                case UID:
                    // 替换成获取用户ID方法
                    key = "uid";
            }
            keyMap.put(prefix + ":" + key, frequencyControl);
            strategyName = frequencyControl.strategy();
        }
        // 将注解的参数转换为编程式调用需要的参数
        if (FrequencyControlConstant.TOTAL_COUNT_WITH_IN_FIX_TIME.equals(strategyName)) {
            // 调用编程式注解 固定窗口
            List<FrequencyControlDTO> frequencyControlList = keyMap.entrySet().stream().map(entrySet -> buildFixedWindowDTO(entrySet.getKey(), entrySet.getValue())).collect(Collectors.toList());
            return executeWithFrequencyControlList(strategyName, frequencyControlList, joinPoint::proceed);
        } else if (FrequencyControlConstant.TOKEN_BUCKET.equals(strategyName)) {
            // 调用编程式注解 令牌桶
            List<TokenBucketDTO> frequencyControlList = keyMap.entrySet().stream().map(entrySet -> buildTokenBucketDTO(entrySet.getKey(), entrySet.getValue())).collect(Collectors.toList());
            return executeWithFrequencyControlList(strategyName, frequencyControlList, joinPoint::proceed);
        } else {
            // 调用编程式注解 滑动窗口
            List<SlidingWindowDTO> frequencyControlList = keyMap.entrySet().stream().map(entrySet -> buildSlidingWindowFrequencyControlDTO(entrySet.getKey(), entrySet.getValue())).collect(Collectors.toList());
            return executeWithFrequencyControlList(strategyName, frequencyControlList, joinPoint::proceed);
        }
    }

    /**
     * 多限流策略的编程式调用方法调用方法
     *
     * @param strategyName         策略名称
     * @param frequencyControlList 频控列表 包含每一个频率控制的定义以及顺序
     * @param supplier             函数式入参-代表每个频控方法执行的不同的业务逻辑
     * @return 业务方法执行的返回值
     * @throws Throwable 被限流或者限流策略定义错误
     */
    public static <T, K extends FrequencyControlDTO> T executeWithFrequencyControlList(String strategyName, List<K> frequencyControlList,
                                                                                       AbstractFrequencyControlService.SupplierThrowWithoutParam<T> supplier) throws Throwable {
        AbstractFrequencyControlService<K> frequencyController = FrequencyControlStrategyFactory.getFrequencyControllerByName(strategyName);
        return frequencyController.executeWithFrequencyControlList(frequencyControlList, supplier);
    }

    /**
     * 将注解参数转换为编程式调用所需要的参数
     * @param key              频率控制Key
     * @param frequencyControl 注解
     * @return 编程式调用所需要的参数-FrequencyControlDTO
     */
    private SlidingWindowDTO buildSlidingWindowFrequencyControlDTO(String key, FrequencyControl frequencyControl) {
        SlidingWindowDTO frequencyControlDTO = new SlidingWindowDTO();
        frequencyControlDTO.setWindowSize(frequencyControl.windowSize());
        frequencyControlDTO.setPeriod(frequencyControl.period());
        frequencyControlDTO.setCount(frequencyControl.count());
        frequencyControlDTO.setUnit(frequencyControl.unit());
        frequencyControlDTO.setKey(key);
        return frequencyControlDTO;
    }

    /**
     * 将注解参数转换为编程式调用所需要的参数
     * @param key              频率控制Key
     * @param frequencyControl 注解
     * @return 编程式调用所需要的参数-FrequencyControlDTO
     */
    private TokenBucketDTO buildTokenBucketDTO(String key, FrequencyControl frequencyControl) {
        TokenBucketDTO tokenBucketDTO = new TokenBucketDTO(frequencyControl.capacity(), frequencyControl.refillRate());
        tokenBucketDTO.setKey(key);
        return tokenBucketDTO;
    }

    /**
     * 将注解参数转换为编程式调用所需要的参数
     * @param key              频率控制Key
     * @param frequencyControl 注解
     * @return 编程式调用所需要的参数-FrequencyControlDTO
     */
    private FixedWindowDTO buildFixedWindowDTO(String key, FrequencyControl frequencyControl) {
        FixedWindowDTO fixedWindowDTO = new FixedWindowDTO();
        fixedWindowDTO.setCount(frequencyControl.count());
        fixedWindowDTO.setTime(frequencyControl.time());
        fixedWindowDTO.setUnit(frequencyControl.unit());
        fixedWindowDTO.setKey(key);
        return fixedWindowDTO;
    }
}
