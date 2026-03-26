package com.example.bms.mapper;

import com.example.bms.domain.BatteryDataPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataTypeMapper {
    Long findIdByCode(@Param("typeCode") String typeCode);
    String findCodeById(@Param("id") Long id);
}