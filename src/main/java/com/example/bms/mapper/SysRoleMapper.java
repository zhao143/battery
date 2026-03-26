package com.example.bms.mapper;

import com.example.bms.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    List<SysRole> selectByUserId(@Param("userId") Long userId);
    List<SysRole> selectAll();
    int insert(SysRole role);
    int deleteById(@Param("id") Long id);
    List<SysRole> selectByRoleKey(@Param("roleKey") String roleKey);
}
