package com.example.bms.mapper;

import com.example.bms.domain.ThresholdSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ThresholdSettingMapper {
    ThresholdSetting selectLatest();
    ThresholdSetting selectByDeviceUuid(@Param("deviceUuid") String deviceUuid);
    int insert(ThresholdSetting t);
    int insertWithVMax(ThresholdSetting t);
    int insertWithVMin(ThresholdSetting t);
    int insertWithIMax(ThresholdSetting t);
    int insertWithTMax(ThresholdSetting t);
    int insertAll(ThresholdSetting t);
    int updateVMax(@Param("id") long id, @Param("v") double v);
    int updateVMin(@Param("id") long id, @Param("v") double v);
    int updateIMax(@Param("id") long id, @Param("v") double v);
    int updateTMax(@Param("id") long id, @Param("v") int v);
    int updateAll(@Param("id") long id, @Param("vMax") Double vMax, @Param("vMin") Double vMin, @Param("iMax") Double iMax, @Param("tMax") Integer tMax);
}