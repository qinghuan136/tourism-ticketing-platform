package com.qinghuan.user;

import com.qinghuan.common.response.ApiResponse;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.remote.user.CurrentUserProfileDTO;
import com.qinghuan.pojo.remote.user.VisitorForOrderDTO;
import com.qinghuan.visitor.VisitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 仅供已认证服务调用的用户领域接口，网关会拦截外部 /internal/** 请求。 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final VisitorService visitorService;
    private final UserService userService;

    public InternalUserController(VisitorService visitorService, UserService userService) {
        this.visitorService = visitorService;
        this.userService = userService;
    }

    /** 返回当前登录游客可用于下单的参观人资料。 */
    @GetMapping("/active-visitors")
    public ApiResponse<List<VisitorForOrderDTO>> listActiveVisitorsForOrder() {
        List<VisitorForOrderDTO> visitors = visitorService.listActiveVisitorsForOrder().stream()
                .map(visitor -> new VisitorForOrderDTO(
                        visitor.getId(), visitor.getName(), visitor.getIdType(),
                        visitor.getIdNumber(), visitor.getFingerprint()))
                .toList();
        return ApiResponse.success(visitors);
    }

    /** 订单创建时返回当前登录购买人的名称和手机号快照。 */
    @GetMapping("/current-profile")
    public ApiResponse<CurrentUserProfileDTO> getCurrentProfile() {
        UserAccount account = userService.getAccountSnapshot(UserContext.getRequired().userId());
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return ApiResponse.success(new CurrentUserProfileDTO(account.getDisplayName(), account.getPhone()));
    }
}
