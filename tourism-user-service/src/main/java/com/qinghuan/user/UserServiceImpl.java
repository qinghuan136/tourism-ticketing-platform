package com.qinghuan.user;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.annotation.RefreshCreateTimeOrUpdateTime;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.constant.cacheKeys.AccountConstant;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.dto.StaffAccountUpdateDTO;
import com.qinghuan.pojo.dto.UserAccountPageQueryDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.pojo.enums.OperationType;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.UserAccountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserMapper userMapper,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 游客自主注册，角色、景点归属和账号状态均由后端决定。 */
    @Override
    @Transactional
    public void registerTourist(RegisterDTO registerDTO) {
        if (userMapper.countAccountByLoginNameOrPhone(
                registerDTO.loginName(), registerDTO.phone()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名或手机号已经存在");
        }

        UserAccount account = new UserAccount();
        account.setLoginName(registerDTO.loginName());
        // 与现有登录逻辑保持一致，当前项目继续使用 MD5 保存密码摘要。
        account.setPasswordHash(DigestUtils.md5DigestAsHex(registerDTO.password().getBytes()));
        account.setDisplayName(registerDTO.displayName());
        account.setPhone(registerDTO.phone());
        account.setRoleCode(AccountRole.TOURIST);
        account.setVenueId(null);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        try {
            // 数据库唯一索引同时处理并发注册产生的重复账号。
            userMapper.saveAccount(account);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名或手机号已经存在");
        }
    }

    // 保存工作人员账号
    @Transactional
    @Override
    @RefreshCreateTimeOrUpdateTime(value = OperationType.INSERT)
    public void saveStaffAccount(UserAccount userAccount) {
        log.info("保存工作人员账号：{}", userAccount);
        //检查是否已存在相同账号名
        if (userMapper.getAccountByLoginName(userAccount.getLoginName()) != null) {
            log.info("试图添加重复的账号名！");
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "账号名重复！");
        }

        // 密码哈希
        userAccount.setPasswordHash(DigestUtils.md5DigestAsHex(userAccount.getPassword().getBytes()));
        // 工作人员继承运营者的VenueId
        userAccount.setVenueId(UserContext.getRequired().venueId());
        userAccount.setRoleCode(AccountRole.STAFF);
        userAccount.setStatus(AccountStatus.ACTIVE);
        userAccount.setCreatedAt(LocalDateTime.now());
        userAccount.setUpdatedAt(LocalDateTime.now());
        try {
            userMapper.saveAccount(userAccount);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名或手机号已被使用");
        }
    }

    // 根据登录名和哈希密码获取账号信息
    @Override
    public UserAccount getAccountByLoginNameandPasswordHash(String loginName, String passwordHash) {
        return userMapper.getAccountByLoginNameAndPasswordHash(loginName, passwordHash);
    }

    // 工作人员账号分页查询
    @Override
    public PageResult<UserAccountVO> StaffAccountPageQuery(UserAccountPageQueryDTO userAccountPageQueryDTO) {
        PageHelper.startPage(userAccountPageQueryDTO.getPage(), userAccountPageQueryDTO.getPageSize());
        userAccountPageQueryDTO.setRoleCode(AccountRole.STAFF.name());
        userAccountPageQueryDTO.setVenueId(UserContext.getRequired().venueId());
        Page<UserAccountVO> page = userMapper.pageQuery(userAccountPageQueryDTO);
        return new PageResult<UserAccountVO>((List<UserAccountVO>)page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    // 修改工作人员账号状态
    @RefreshCreateTimeOrUpdateTime(value = OperationType.UPDATE)
    @Transactional
    @Override
    public void changeStaffAccountStatus(AccountStatus status, Long id) {
        UserAccount userAccount = new UserAccount();
        userAccount.setId(id);
        userAccount.setStatus(status);
        Integer updateRows = userMapper.updateAccount(userAccount, UserContext.getRequired().venueId());
        if (updateRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作人员不属于当前景点");
        }

        String disabledKey = AccountConstant.disabledUserKey(id);
        if (status == AccountStatus.DISABLED) {
            stringRedisTemplate.opsForValue().set(disabledKey, "1");
        } else {
            stringRedisTemplate.delete(disabledKey);
        }
    }

    /**
     * 使用当前运营者的景点 ID 约束更新范围，避免跨景点修改工作人员资料。
     */
    @RefreshCreateTimeOrUpdateTime(value = OperationType.UPDATE)
    @Transactional
    @Override
    public void updateStaffAccount(Long staffId, StaffAccountUpdateDTO updateDTO) {
        LoginUser currentUser = UserContext.getRequired();
        if (currentUser.roleCode() != AccountRole.OPERATOR) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        try {
            int updatedRows = userMapper.updateStaffInfo(staffId, currentUser.venueId(), updateDTO);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "工作人员不存在或不属于当前景点");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已被使用");
        }
    }

    @Override
    public void resetStaffPassword(Long staffId, String password) {
        String passwordHash = DigestUtils.md5DigestAsHex(password.getBytes());
        int updated = userMapper.resetStaffPassword(
                staffId, UserContext.getRequired().venueId(), passwordHash);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作人员不存在或不属于当前景点");
        }
    }

    @Override
    public List<Visitor> getVisitorsByUserId(Long userId) {
        return userMapper.getVisitorsByUserId(userId);
    }

    @Override
    public UserAccount getAccountSnapshot(Long userId) {
        return userMapper.findAccountSnapshotById(userId);
    }

}
