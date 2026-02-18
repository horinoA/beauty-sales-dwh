package com.beauty.beauty_sales_dwh.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.beauty.beauty_sales_dwh.domain.TransactionRawData;

@Mapper
public interface RawTransactionMapper {
    void insertRawTransaction(TransactionRawData data);
    OffsetDateTime findMaxFetchedAt(@Param("companyId") Long companyId);

    List<TransactionRawData> findUnprocessedTransactions(
        @Param("companyId") Long companyId,
        @Param("limit") int limit
    );

    void markTransactionsAsProcessed(@Param("list") List<Long> transactionIds);
}
