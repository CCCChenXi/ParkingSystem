package com.xigeandwillian.parkingsystem.common.mapper;

import com.xigeandwillian.parkingsystem.client.vo.wallet.WalletLogVO;
import com.xigeandwillian.parkingsystem.client.vo.wallet.WalletVO;
import com.xigeandwillian.parkingsystem.common.entity.Wallet;
import com.xigeandwillian.parkingsystem.common.entity.WalletLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WalletConverter {

    WalletVO toVO(Wallet wallet);

    WalletLogVO toLogVO(WalletLog walletLog);

    List<WalletLogVO> toLogVOList(List<WalletLog> walletLogs);
}
