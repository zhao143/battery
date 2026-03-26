package com.example.bms.mapper;

import com.example.bms.domain.UserDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDeviceMapper {
    List<UserDevice> selectByUserId(@Param("userId") Long userId);
    List<UserDevice> selectByDeviceId(@Param("deviceId") Long deviceId);
    int insert(UserDevice userDevice);
    int delete(@Param("userId") Long userId, @Param("deviceId") Long deviceId);
}
