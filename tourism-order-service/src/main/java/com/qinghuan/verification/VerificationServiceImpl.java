package com.qinghuan.verification;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.VerificationPageQueryDTO;
import com.qinghuan.pojo.dto.VerificationRequestDTO;
import com.qinghuan.pojo.entity.VerificationRecord;
import com.qinghuan.pojo.enums.TicketStatus;
import com.qinghuan.pojo.enums.VerificationResult;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.TicketVerificationInfo;
import com.qinghuan.pojo.vo.VerificationRecordVO;
import com.qinghuan.pojo.vo.VerificationResultVO;
import com.qinghuan.pojo.vo.VerificationTicketVO;
import com.qinghuan.ticket.TicketService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class VerificationServiceImpl implements VerificationService {

    private final VerificationMapper verificationMapper;
    private final TicketService ticketService;

    public VerificationServiceImpl(VerificationMapper verificationMapper,
                                   TicketService ticketService) {
        this.verificationMapper = verificationMapper;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public VerificationResultVO verify(VerificationRequestDTO requestDTO) {
        LoginUser loginUser = UserContext.getRequired();

        VerificationRecord existing =
                verificationMapper.findByRequestNo(requestDTO.requestNo());
        if (existing != null) {
            return existingResult(existing, requestDTO.ticketCode(), loginUser.venueId());
        }

        TicketVerificationInfo ticket = ticketService.findForVerification(
                requestDTO.ticketCode(), loginUser.venueId());
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "票券不存在");
        }

        LocalDateTime verifiedAt = LocalDateTime.now();
        String failureReason = failureReason(ticket, verifiedAt);

        VerificationRecord record = new VerificationRecord();
        record.setRequestNo(requestDTO.requestNo());
        record.setTicketId(ticket.getId());
        record.setVerifierId(loginUser.userId());
        // 当前认证上下文中的账号名是核销记录的展示快照，避免列表查询跨服务取用户资料。
        record.setVerifierNameSnapshot(loginUser.loginName());
        record.setResult(failureReason == null
                ? VerificationResult.SUCCESS
                : VerificationResult.FAILED);
        record.setFailureReason(failureReason);
        record.setDeviceNo(requestDTO.deviceNo());
        record.setVerifiedAt(verifiedAt);

        try {
            // 先占用 requestNo，避免同一请求号并发核销两张不同票券。
            verificationMapper.insertRecord(record);
        } catch (DuplicateKeyException exception) {
            // 两个相同 requestNo 并发到达时，唯一约束决定最终只保留一条结果。
            VerificationRecord concurrent =
                    verificationMapper.findByRequestNoForUpdate(requestDTO.requestNo());
            if (concurrent == null) {
                throw exception;
            }
            return existingResult(
                    concurrent, requestDTO.ticketCode(), loginUser.venueId());
        }

        if (failureReason == null) {
            // 条件更新是重复扫码和不同请求并发核销之间唯一的成功入口。
            if (ticketService.markUsed(ticket.getId(), verifiedAt) == 1) {
                ticket.setStatus(TicketStatus.USED);
                ticket.setVerifiedAt(verifiedAt);
            } else {
                ticket = ticketService.findForVerification(
                        ticket.getId(), loginUser.venueId());
                failureReason = failureReason(ticket, verifiedAt);
                if (failureReason == null) {
                    failureReason = "票券状态发生变化";
                }

                record.setResult(VerificationResult.FAILED);
                record.setFailureReason(failureReason);
                verificationMapper.updateResult(record);
            }
        }
        return toResult(record, ticket);
    }

    @Override
    public PageResult<VerificationRecordVO> pageRecords(
            VerificationPageQueryDTO queryDTO) {
        queryDTO.setTicketCode(StringUtils.hasText(queryDTO.getTicketCode())
                ? queryDTO.getTicketCode().trim()
                : null);

        PageHelper.startPage(queryDTO.getPage(), queryDTO.getSize());
        Page<VerificationRecordVO> page = (Page<VerificationRecordVO>)
                verificationMapper.listRecords(
                        UserContext.getRequired().venueId(), queryDTO);
        return new PageResult<>(
                page.getResult(), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    private VerificationResultVO existingResult(
            VerificationRecord record, String ticketCode, Long venueId) {
        TicketVerificationInfo ticket =
                ticketService.findForVerification(record.getTicketId(), venueId);
        if (ticket == null || !ticket.getTicketCode().equals(ticketCode)) {
            throw new BusinessException(ErrorCode.CONFLICT, "核销请求号已被使用");
        }
        return toResult(record, ticket);
    }

    private String failureReason(TicketVerificationInfo ticket, LocalDateTime now) {
        if (ticket.getStatus() == TicketStatus.USED) {
            return "票券已核销";
        }
        if (ticket.getStatus() == TicketStatus.VOID) {
            return "票券已作废";
        }
        if (ticket.getStatus() == TicketStatus.REFUNDING) {
            return "票券退款处理中";
        }
        if (ticket.getStatus() == TicketStatus.EXPIRED
                || !now.isBefore(ticket.getValidUntil())) {
            return "票券已过期";
        }
        if (now.isBefore(ticket.getValidFrom())) {
            return "票券尚未生效";
        }
        return null;
    }

    private VerificationResultVO toResult(
            VerificationRecord record, TicketVerificationInfo ticket) {
        VerificationTicketVO ticketVO = new VerificationTicketVO();
        ticketVO.setId(ticket.getId());
        ticketVO.setTicketCode(ticket.getTicketCode());
        ticketVO.setStatus(ticket.getStatus());
        ticketVO.setVenueName(ticket.getVenueName());
        ticketVO.setVisitorName(ticket.getVisitorName());
        ticketVO.setTicketTypeName(ticket.getTicketTypeName());
        ticketVO.setVisitDate(ticket.getVisitDate());
        ticketVO.setStartTime(ticket.getStartTime());
        ticketVO.setEndTime(ticket.getEndTime());

        VerificationResultVO result = new VerificationResultVO();
        result.setRequestNo(record.getRequestNo());
        result.setResult(record.getResult());
        result.setFailureReason(record.getFailureReason());
        result.setVerifiedAt(record.getVerifiedAt());
        result.setTicket(ticketVO);
        return result;
    }
}
