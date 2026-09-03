package com.qinghuan.visitor;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.pojo.dto.VisitorCreateDTO;
import com.qinghuan.pojo.dto.VisitorUpdateDTO;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.enums.AccountRole;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorServiceImplTest {

    @Mock
    private VisitorMapper visitorMapper;

    private VisitorServiceImpl visitorService;

    @BeforeEach
    void setUp() {
        visitorService = new VisitorServiceImpl(visitorMapper);
        UserContext.set(new LoginUser(9L, "tourist", AccountRole.TOURIST, null));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createVisitor_shouldNormalizeBlankPhoneToNull() {
        VisitorCreateDTO request = createRequest("");

        visitorService.createVisitor(request);

        ArgumentCaptor<Visitor> captor = ArgumentCaptor.forClass(Visitor.class);
        verify(visitorMapper).insert(captor.capture());
        assertNull(captor.getValue().getPhone());
    }

    @Test
    void updateVisitor_shouldNormalizeBlankPhoneToNull() {
        VisitorUpdateDTO request = new VisitorUpdateDTO();
        request.setName("测试游客");
        request.setPhone("  ");
        when(visitorMapper.update(1L, 9L, request)).thenReturn(1);

        visitorService.updateVisitor(1L, request);

        assertNull(request.getPhone());
        verify(visitorMapper).update(1L, 9L, request);
    }

    @Test
    void validation_shouldAllowEmptyPhoneAndRejectInvalidPhone() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(createRequest("")).isEmpty());
            assertTrue(validator.validate(createRequest(null)).isEmpty());
            assertTrue(validator.validate(createRequest("123")).stream()
                    .anyMatch(violation -> "phone".equals(violation.getPropertyPath().toString())));
        }
    }

    private VisitorCreateDTO createRequest(String phone) {
        VisitorCreateDTO request = new VisitorCreateDTO();
        request.setName("测试游客");
        request.setIdType("ID_CARD");
        request.setIdNumber("440101199803120011");
        request.setPhone(phone);
        return request;
    }
}
