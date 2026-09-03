package com.qinghuan.auth;

import com.qinghuan.auth.jwt.JwtUtils;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.RegisterDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("账号登录")
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldDelegateTouristCreationToUserService() {
        RegisterDTO request = new RegisterDTO(
                "tourist001", "123456", "小明", "13800138000");

        authService.register(request);

        verify(userService).registerTourist(request);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("已停用账号不能登录")
    void login_shouldRejectDisabledAccount() {
        UserAccount request = new UserAccount();
        request.setLoginName("disabled-user");
        request.setPassword("123456");

        UserAccount foundAccount = new UserAccount();
        foundAccount.setStatus(AccountStatus.DISABLED);
        when(userService.getAccountByLoginNameandPasswordHash(
                anyString(), anyString())).thenReturn(foundAccount);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verifyNoInteractions(jwtUtils);
    }
}
