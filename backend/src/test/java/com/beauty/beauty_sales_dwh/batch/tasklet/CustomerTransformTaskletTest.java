package com.beauty.beauty_sales_dwh.batch.tasklet;

import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.CustomerTransformMapper;

@ExtendWith(MockitoExtension.class)
public class CustomerTransformTaskletTest {

    @InjectMocks
    private CustomerTransformTasklet customerTransformTasklet;

    @Mock
    private CustomerTransformMapper mapper;

    @Mock
    private AppVendorProperties vendorProperties;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    private final Long COMPANY_ID = 123L;

    @BeforeEach
    void setUp() {
        // AppVendorPropertiesのモック設定
        when(vendorProperties.getId()).thenReturn(String.valueOf(COMPANY_ID));
    }

    @Test
    void testExecute_callsMappersWithCompanyId() throws Exception {
        // --- Mock Setup ---
        // findMaxFetchedAtが呼ばれたら、適当な日付を返す
        OffsetDateTime mockDate = OffsetDateTime.now();
        when(mapper.findMaxFetchedAt(COMPANY_ID)).thenReturn(mockDate);
        
        // upsertCustomersFromRawが呼ばれたら、適当な件数を返す
        when(mapper.upsertCustomersFromRaw(COMPANY_ID, mockDate)).thenReturn(5);

        // --- Execute ---
        customerTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMaxFetchedAtが正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxFetchedAt(COMPANY_ID);
        
        // 2. upsertCustomersFromRawが正しいcompanyIdと日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCustomersFromRaw(COMPANY_ID, mockDate);
    }
    
    @Test
    void testExecute_handlesNullDate() throws Exception {
        // --- Mock Setup ---
        // findMaxFetchedAtがnullを返すように設定
        when(mapper.findMaxFetchedAt(COMPANY_ID)).thenReturn(null);

        // upsertCustomersFromRawが呼ばれたら、適当な件数を返す
        // any(OffsetDateTime.class) を使って、日付は問わないようにする
        when(mapper.upsertCustomersFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class))).thenReturn(10);

        // --- Execute ---
        customerTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMaxFetchedAtが正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxFetchedAt(COMPANY_ID);
        
        // 2. upsertCustomersFromRawが正しいcompanyIdで、かつ任意の日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCustomersFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class));
    }
}
