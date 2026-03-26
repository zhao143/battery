package com.example.bms.mapper;

import com.example.bms.domain.AlarmHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmHistoryMapper {
    int insert(AlarmHistory h);
    List<AlarmHistory> selectAll();
    int updateProcessStatus(@org.apache.ibatis.annotations.Param("id") long id, @org.apache.ibatis.annotations.Param("status") int status);
}