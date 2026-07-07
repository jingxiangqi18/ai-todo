package com.qijx.aitodo.task.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.qijx.aitodo.task.entity.Task;

@Mapper
public interface TaskMapper extends BaseMapper<Task>{
}
