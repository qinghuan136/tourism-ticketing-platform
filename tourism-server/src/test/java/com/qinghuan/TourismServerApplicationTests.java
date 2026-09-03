package com.qinghuan;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TourismServerApplicationTests {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private MybatisProperties mybatisProperties;

    @Test
    void contextLoads() {
        assertTrue(mybatisProperties.resolveMapperLocations().length > 0,
                "未找到 MyBatis XML 资源");
        assertTrue(sqlSessionFactory.getConfiguration().hasStatement(
                "com.qinghuan.coupon.CouponMapper.listActivitiesToPreheat"));
    }

}
