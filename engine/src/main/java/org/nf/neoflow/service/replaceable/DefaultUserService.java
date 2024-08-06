package org.nf.neoflow.service.replaceable;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 默认用户服务
 * @author PC8650
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "neo.customizationLock", havingValue = "false", matchIfMissing = true)
public class DefaultUserService {
}
