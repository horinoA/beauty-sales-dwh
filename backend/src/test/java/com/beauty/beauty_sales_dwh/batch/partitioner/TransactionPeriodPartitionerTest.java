package com.beauty.beauty_sales_dwh.batch.partitioner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import com.beauty.beauty_sales_dwh.mapper.RawTransactionMapper;

@ExtendWith(MockitoExtension.class)
class TransactionPeriodPartitionerTest {

    @Mock
    private RawTransactionMapper rawTransactionMapper;

    private TransactionPeriodPartitioner partitioner;
    private final Long companyId = 1L;

    @BeforeEach
    void setUp() {
        partitioner = new TransactionPeriodPartitioner(rawTransactionMapper, companyId);
    }

    @Test
    @DisplayName("初回実行時（データなし）：3年前の月初から今月分までのパーティションが生成されること")
    void partition_InitialRun() {
        // Arrange: Mapperがnullを返す（初回）
        when(rawTransactionMapper.findMaxFetchedAt(companyId)).thenReturn(null);

        // Act
        Map<String, ExecutionContext> result = partitioner.partition(1);

        // Assert
        // 3年前〜今月までの月数（約37パーティション）が生成されているか
        assertTrue(result.size() >= 36, "36ヶ月分以上のパーティションが生成されるはず");
        
        // 最初のパーティションが3年前の月初であること
        ExecutionContext firstPartition = result.get("partition0");
        LocalDate expectedStart = LocalDate.now().minusYears(3).withDayOfMonth(1);
        assertEquals(expectedStart.toString(), firstPartition.getString("from"));
        
        // 最後のパーティションが今日（または今月末）を含むこと
        ExecutionContext lastPartition = result.get("partition" + (result.size() - 1));
        LocalDate today = LocalDate.now();
        assertTrue(LocalDate.parse(lastPartition.getString("to")).isAfter(today.minusDays(32)), 
                "最後のパーティションは直近の期間であるはず");
    }

    @Test
    @DisplayName("差分実行時（データあり）：最終取得日から現在までのパーティションが生成されること")
    void partition_IncrementalRun() {
        // Arrange: 2ヶ月前を最終取得日とする
        LocalDate lastFetchedDate = LocalDate.now().minusMonths(2);
        OffsetDateTime maxFetchedAt = lastFetchedDate.atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        when(rawTransactionMapper.findMaxFetchedAt(companyId)).thenReturn(maxFetchedAt);

        // Act
        Map<String, ExecutionContext> result = partitioner.partition(1);

        // Assert
        // 2ヶ月前、1ヶ月前、今月の約3パーティションが生成されるはず
        assertTrue(result.size() >= 1 && result.size() <= 4);
        
        // 最初のパーティションの開始日がMapperから返った日であること
        ExecutionContext firstPartition = result.get("partition0");
        assertEquals(lastFetchedDate.toString(), firstPartition.getString("from"));
    }
}
