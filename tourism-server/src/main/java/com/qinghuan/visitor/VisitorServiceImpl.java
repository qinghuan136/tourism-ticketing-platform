package com.qinghuan.visitor;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.VisitorCreateDTO;
import com.qinghuan.pojo.dto.VisitorUpdateDTO;
import com.qinghuan.pojo.entity.Visitor;
import com.qinghuan.pojo.enums.VisitorStatus;
import com.qinghuan.pojo.vo.VisitorVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VisitorServiceImpl implements VisitorService {

    private final VisitorMapper visitorMapper;
    private final VisitorFingerprintGenerator fingerprintGenerator;

    public VisitorServiceImpl(VisitorMapper visitorMapper,
                              VisitorFingerprintGenerator fingerprintGenerator) {
        this.visitorMapper = visitorMapper;
        this.fingerprintGenerator = fingerprintGenerator;
    }

    @Override
    public List<VisitorVO> listVisitors(VisitorStatus status) {
        return visitorMapper.list(UserContext.getUserId(), status).stream()
                .map(VisitorVO::from)
                .toList();
    }

    @Override
    @Transactional
    public Long createVisitor(VisitorCreateDTO createDTO) {
        Visitor visitor = new Visitor();
        visitor.setUserId(UserContext.getUserId());
        visitor.setName(createDTO.getName());
        visitor.setIdType(createDTO.getIdType());
        visitor.setIdNumber(createDTO.getIdNumber());
        visitor.setPhone(normalizePhone(createDTO.getPhone()));
        visitor.setStatus(VisitorStatus.ACTIVE);

        try {
            visitorMapper.insert(visitor);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "该证件对应的参观人已存在");
        }
        // 与 visitor 插入处于同一事务，避免存在无法参与下单的参观人记录。
        visitorMapper.insertIdentity(
                visitor.getId(), fingerprintGenerator.generate(createDTO.getIdType(), createDTO.getIdNumber()));
        return visitor.getId();
    }

    @Override
    public VisitorVO getVisitor(Long visitorId) {
        Visitor visitor = visitorMapper.findById(visitorId, UserContext.getUserId());
        if (visitor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参观人不存在");
        }
        return VisitorVO.from(visitor);
    }

    @Override
    @Transactional
    public void updateVisitor(Long visitorId, VisitorUpdateDTO updateDTO) {
        updateDTO.setPhone(normalizePhone(updateDTO.getPhone()));
        int updatedRows = visitorMapper.update(
                visitorId, UserContext.getUserId(), updateDTO);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参观人不存在");
        }
    }

    @Override
    @Transactional
    public void updateVisitorStatus(Long visitorId, VisitorStatus status) {
        int updatedRows = visitorMapper.updateStatus(
                visitorId, UserContext.getUserId(), status);
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参观人不存在");
        }
    }

    @Override
    public List<VisitorForOrder> listActiveVisitorsForOrder() {
        return visitorMapper.listActiveForOrder(UserContext.getUserId());
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
