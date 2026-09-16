package com.qinghuan.user;

import com.github.pagehelper.Page;
import com.qinghuan.pojo.dto.StaffAccountUpdateDTO;
import com.qinghuan.pojo.dto.UserAccountPageQueryDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.vo.UserAccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    /** 检查注册使用的登录名或手机号是否已经存在。 */
    int countAccountByLoginNameOrPhone(@Param("loginName") String loginName,
                                       @Param("phone") String phone);

    //保存用户账号
    public void saveAccount(UserAccount userAccount);
    //根据登录名和密码获取账号信息
    public UserAccount getAccountByLoginNameAndPasswordHash(String loginName, String passwordHash);
    //根据登录名获取账号信息
    public UserAccount getAccountByLoginName(String loginName);

    /** 订单创建时读取当前购买人的展示快照。 */
    UserAccount findAccountSnapshotById(Long id);
    //账号分页查询
    public Page<UserAccountVO> pageQuery(UserAccountPageQueryDTO userAccountPageQueryDTO);
    //更新账号信息
    public Integer updateAccount(@Param("account") UserAccount userAccount,
                                 @Param("venueId") Long venueId);

    // 通过景点和角色限制更新范围，避免运营者修改其他景点的账号。
    int updateStaffInfo(@Param("staffId") Long staffId,
                        @Param("venueId") Long venueId,
                        @Param("updateDTO") StaffAccountUpdateDTO updateDTO);

    int resetStaffPassword(@Param("staffId") Long staffId,
                           @Param("venueId") Long venueId,
                           @Param("passwordHash") String passwordHash);

    List<Visitor> getVisitorsByUserId(Long userId);
}
