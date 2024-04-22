package com.nf.neoflow.dto.test;


import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.interfaces.UserChoose;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestUserChoose implements UserChoose {
    @Override
    public UserBaseInfo getUser() {
        return null;
    }

    @Override
    public List<UserBaseInfo> getCandidateUsers(Integer operationType, List<UserBaseInfo> modelCandidateInfo) {
        UserBaseInfo userBaseInfo = new UserBaseInfo();
        userBaseInfo.setId("2");
        userBaseInfo.setName("张三");
        return List.of(userBaseInfo);
    }

    @Override
    public List<Object> getCandidateList() {
        return null;
    }
}
