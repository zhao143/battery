package com.example.bms.mapper;

import com.example.bms.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser selectById(@Param("id") Long id);
    SysUser selectByUsername(@Param("username") String username);
    List<SysUser> selectAll();
    int insert(SysUser user);
    int update(SysUser user);
    int deleteById(@Param("id") Long id);
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
