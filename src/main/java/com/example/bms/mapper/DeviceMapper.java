package com.example.bms.mapper;

import com.example.bms.domain.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {
    Device selectByUuid(@Param("uuid") String uuid);
    Device selectById(@Param("id") Long id);
    List<Device> selectAll();
    List<Device> selectByUserId(@Param("userId") Long userId);
    int insert(Device device);
    int update(Device device);
    int deleteById(@Param("id") Long id);
    int updateStatus(@Param("uuid") String uuid, @Param("status") Integer status);
    int updateUserId(@Param("id") Long id, @Param("userId") Long userId);
}
