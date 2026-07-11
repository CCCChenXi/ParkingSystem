package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.client.vo.message.MessageVO;
import com.xigeandwillian.parkingsystem.common.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageConverter {

    MessageVO toVO(Message message);

    List<MessageVO> toVOList(List<Message> messages);
}
