package com.greatwall.business.db.user.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.greatwall.business.db.user.form.UserLoginForm;
import com.greatwall.business.db.user.model.UserModel;
import com.greatwall.business.db.user.vo.UserSessionVo;

import xingkong.db.mybatis.mapper.MybatisMapper;

/**
 *
 * @author xingkong
 */
@Repository
public interface UserMapper extends MybatisMapper<UserModel> {

	UserSessionVo login(UserLoginForm userLoginForm);
	
	void deleteUserByMobile(UserModel userModel);
	
	List<UserModel> queryByTrem(UserModel userModel, Pagination page);

}