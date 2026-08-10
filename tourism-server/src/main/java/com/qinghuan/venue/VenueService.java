package com.qinghuan.venue;

import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.vo.VenueVO;
import org.springframework.web.multipart.MultipartFile;

public interface VenueService {
    // 获取当前景点信息
    VenueVO getCurrentVenue();

    void updateCurrentVenue(VenueUpdateDTO newVenue, MultipartFile newCover);
}
