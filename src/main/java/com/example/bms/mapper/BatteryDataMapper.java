package com.example.bms.mapper;

import com.example.bms.domain.BatteryDataPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BatteryDataMapper {
    void insert(BatteryDataPoint dataPoint);
    BatteryDataPoint selectLatestByUuidAndType(@Param("deviceUuid") String deviceUuid, @Param("dataTypeId") Long dataTypeId);
    List<BatteryDataPoint> selectRecentByUuidAndType(@Param("deviceUuid") String deviceUuid, @Param("dataTypeId") Long dataTypeId, @Param("limit") int limit);
    List<String> selectDistinctDeviceUuids();
}