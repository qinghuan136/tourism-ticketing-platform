package com.qinghuan.user;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.StaffAccountUpdateDTO;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.dto.UserAccountPageQueryDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.UserAccountVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("工作人员资料修改")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, stringRedisTemplate);
        UserContext.set(new LoginUser(1L, "operator", AccountRole.OPERATOR, 10L));
    }

    @Test
    void registerTourist_shouldSaveActiveTouristWithMd5Password() {
        RegisterDTO request = new RegisterDTO(
                "tourist001", "123456", "小明", "13800138000");
        when(userMapper.countAccountByLoginNameOrPhone(
                request.loginName(), request.phone())).thenReturn(0);

        userService.registerTourist(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userMapper).saveAccount(captor.capture());
        UserAccount saved = captor.getValue();
        assertEquals("tourist001", saved.getLoginName());
        assertEquals("e10adc3949ba59abbe56e057f20f883e", saved.getPasswordHash());
        assertEquals(AccountRole.TOURIST, saved.getRoleCode());
        assertEquals(AccountStatus.ACTIVE, saved.getStatus());
        assertEquals(null, saved.getVenueId());
    }

    @Test
    void registerTourist_shouldRejectDuplicateLoginNameOrPhone() {
        RegisterDTO request = new RegisterDTO(
                "tourist001", "123456", "小明", "13800138000");
        when(userMapper.countAccountByLoginNameOrPhone(
                request.loginName(), request.phone())).thenReturn(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.registerTourist(request));

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(userMapper, never()).saveAccount(org.mockito.ArgumentMatchers.any());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        PageHelper.clearPage();
    }

    @Test
    void updateStaffAccount_shouldUpdateCurrentVenueStaff_whenOperatorOwnsVenue() {
        StaffAccountUpdateDTO updateDTO = updateRequest();
        when(userMapper.updateStaffInfo(8L, 10L, updateDTO)).thenReturn(1);

        userService.updateStaffAccount(8L, updateDTO);

        verify(userMapper).updateStaffInfo(8L, 10L, updateDTO);
    }

    @Test
    void updateStaffAccount_shouldThrowNotFound_whenTargetIsNotInCurrentVenue() {
        StaffAccountUpdateDTO updateDTO = updateRequest();
        when(userMapper.updateStaffInfo(8L, 10L, updateDTO)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateStaffAccount(8L, updateDTO)
        );

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateStaffAccount_shouldThrowForbidden_whenCurrentUserIsNotOperator() {
        UserContext.set(new LoginUser(2L, "staff", AccountRole.STAFF, 10L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateStaffAccount(8L, updateRequest())
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void updateStaffAccount_shouldThrowConflict_whenPhoneIsAlreadyUsed() {
        StaffAccountUpdateDTO updateDTO = updateRequest();
        when(userMapper.updateStaffInfo(8L, 10L, updateDTO))
                .thenThrow(new DuplicateKeyException("duplicate phone"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateStaffAccount(8L, updateDTO)
        );

        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void staffAccountPageQuery_shouldLimitResultsToCurrentVenue() {
        UserAccountPageQueryDTO queryDTO = new UserAccountPageQueryDTO();
        when(userMapper.pageQuery(queryDTO)).thenReturn(new Page<>(1, 20));

        PageResult<UserAccountVO> result = userService.StaffAccountPageQuery(queryDTO);

        assertEquals(10L, queryDTO.getVenueId());
        assertEquals("STAFF", queryDTO.getRoleCode());
        assertEquals(1, result.page());
        assertEquals(20, result.size());
    }

    private StaffAccountUpdateDTO updateRequest() {
        StaffAccountUpdateDTO updateDTO = new StaffAccountUpdateDTO();
        updateDTO.setDisplayName("张敏");
        updateDTO.setPhone("13800138000");
        return updateDTO;
    }
}
