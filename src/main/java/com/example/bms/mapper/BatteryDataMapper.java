package com.example.bms.mapper;

import com.example.bms.domain.BatteryData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BatteryDataMapper {
    int insert(BatteryData data);
    BatteryData selectLatest();
    BatteryData selectLatestByUuid(@Param("deviceUuid") String deviceUuid);
    List<BatteryData> selectRecent(@Param("limit") int limit);
    List<BatteryData> selectRecentByUuid(@Param("deviceUuid") String deviceUuid, @Param("limit") int limit);
}