package com.qinghuan.user;

import com.github.pagehelper.Page;
import com.qinghuan.pojo.dto.StaffAccountUpdateDTO;
import com.qinghuan.pojo.dto.UserAccountPageQueryDTO;
import com.qinghuan.pojo.entity.UserAccount;
import com.qinghuan.pojo.enums.AccountStatus;
import com.qinghuan.pojo.vo.UserAccountVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MybatisTest(properties = "mybatis.mapper-locations=classpath:mapper/**/*.xml")
@Sql(statements = {
        "drop table if exists user_account",
        """
        create table user_account (
            id bigint primary key,
            login_name varchar(50) not null,
            password_hash varchar(255) not null,
            display_name varchar(50) not null,
            phone varchar(20),
            role_code varchar(20) not null,
            venue_id bigint,
            status varchar(20) not null,
            created_at timestamp default current_timestamp,
            updated_at timestamp default current_timestamp
        );
        """,
        """
        insert into user_account
            (id, login_name, password_hash, display_name, phone, role_code, venue_id, status)
        values
            (1, 'staff-a', 'hash', '员工甲', '13800000001', 'STAFF', 10, 'ACTIVE'),
            (2, 'operator-a', 'hash', '运营者甲', '13800000002', 'OPERATOR', 10, 'ACTIVE'),
            (3, 'staff-b', 'hash', '员工乙', '13800000003', 'STAFF', 20, 'ACTIVE');
        """
})
@DisplayName("工作人员 Mapper")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void countAccountByLoginNameOrPhone_shouldFindExistingAccount() {
        assertEquals(1, userMapper.countAccountByLoginNameOrPhone(
                "staff-a", "13999999999"));
        assertEquals(1, userMapper.countAccountByLoginNameOrPhone(
                "new-login", "13800000002"));
        assertEquals(0, userMapper.countAccountByLoginNameOrPhone(
                "new-login", null));
    }

    @Test
    @DisplayName("修改工作人员资料的 SQL 可以正常执行")
    void updateStaffInfo_shouldExecuteValidSql() {
        StaffAccountUpdateDTO updateDTO = new StaffAccountUpdateDTO();
        updateDTO.setDisplayName("员工新姓名");
        updateDTO.setPhone("13900000001");

        int updatedRows = userMapper.updateStaffInfo(1L, 10L, updateDTO);

        assertEquals(1, updatedRows);
        assertEquals(
                "员工新姓名",
                jdbcTemplate.queryForObject(
                        "select display_name from user_account where id = 1",
                        String.class
                )
        );
    }

    @Test
    @DisplayName("工作人员状态接口不能修改同景点运营者账号")
    void updateAccount_shouldOnlyUpdateStaffRole() {
        UserAccount staff = new UserAccount();
        staff.setId(1L);
        staff.setStatus(AccountStatus.DISABLED);

        UserAccount operator = new UserAccount();
        operator.setId(2L);
        operator.setStatus(AccountStatus.DISABLED);

        assertEquals(1, userMapper.updateAccount(staff, 10L));
        assertEquals(0, userMapper.updateAccount(operator, 10L));
        assertEquals(
                "ACTIVE",
                jdbcTemplate.queryForObject(
                        "select status from user_account where id = 2",
                        String.class
                )
        );
    }

    @Test
    @DisplayName("工作人员列表只返回当前景点账号")
    void pageQuery_shouldFilterByVenue() {
        UserAccountPageQueryDTO queryDTO = new UserAccountPageQueryDTO();
        queryDTO.setRoleCode("STAFF");
        queryDTO.setVenueId(10L);

        Page<UserAccountVO> result = userMapper.pageQuery(queryDTO);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}
