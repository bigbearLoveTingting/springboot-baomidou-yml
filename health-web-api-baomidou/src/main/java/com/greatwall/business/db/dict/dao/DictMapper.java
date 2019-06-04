package com.greatwall.business.db.dict.dao;

import java.util.List;

import com.greatwall.business.db.dict.model.DictModel;

import xingkong.db.mybatis.mapper.MybatisMapper;

public interface DictMapper extends MybatisMapper<DictModel> {
	
	List<DictModel> queryByTrem(DictModel dictModel);
	
	void deleteDict(DictModel dictModel);
	

}