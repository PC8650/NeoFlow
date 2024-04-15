package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.exception.NeoUserException;
import com.nf.neoflow.interfaces.UserChoose;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 获取用户
 * @author PC8650
 */
@Data
@Component
public class BaseUserChoose {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private UserChoose userChooseService;

    /**
     * 获取或校验用户信息
     * @param params
     * @return
     */
    public UserBaseInfo user(String... params) {
        if (config.getBaseUserChoose()) {
            return getUser();
        }

        if (params.length == 2) {
            check(params[0], params[1]);
        } else if (params.length == 1) {
            check(params[0]);
        }else {
            throw new NeoUserException("参数错误");
        }
        return null;
    }

    /**
     * 获取用户
     * @return 用户信息
     */
    private UserBaseInfo getUser() {
        UserBaseInfo user = userChooseService.getUser();
        if (Objects.isNull(user)) {
            throw new NeoUserException("未获取到用户信息");
        }
        if (StringUtils.isBlank(user.getId()) || StringUtils.isBlank(user.getName())) {
            throw new NeoUserException("用户信息不完整");
        }
        return user;
    }

    private void check(String param) {
        if (StringUtils.isBlank(param)) {
            throw new NeoUserException("用户信息缺失");
        }
    }

    private void check(String param1, String param2) {
        if (StringUtils.isBlank(param1) || StringUtils.isBlank(param2)) {
            throw new NeoUserException("用户信息缺失");
        }
    }

}
