package com.example.bms.mapper;

import com.example.bms.domain.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    List<SysUserRole> selectAll();
    SysUserRole selectById(Long id);
    void insert(SysUserRole sysUserRole);
    void delete(Long id);
}
