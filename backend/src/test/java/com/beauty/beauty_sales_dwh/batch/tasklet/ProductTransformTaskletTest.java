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

import com.beauty.beauty_sales_dwh.config.AppVendorProperties;
import com.beauty.beauty_sales_dwh.mapper.ProductTransformMapper;

@ExtendWith(MockitoExtension.class)
public class ProductTransformTaskletTest {

    @InjectMocks
    private ProductTransformTasklet productTransformTasklet;

    @Mock
    private ProductTransformMapper mapper;

    @Mock
    private AppVendorProperties vendorProperties;

    @Mock
    private StepContribution stepContribution;

    @Mock
    private ChunkContext chunkContext;

    private final Long COMPANY_ID = 456L;

    @BeforeEach
    void setUp() {
        // AppVendorPropertiesのモック設定
        when(vendorProperties.getId()).thenReturn(String.valueOf(COMPANY_ID));
    }

    @Test
    void testExecute_callsMappersWithCompanyId() throws Exception {
        // --- Mock Setup ---
        OffsetDateTime mockDate = OffsetDateTime.now();
        when(mapper.findMaxFetchedAt(COMPANY_ID)).thenReturn(mockDate);
        when(mapper.upsertCategoryGroupsFromRaw(COMPANY_ID, mockDate)).thenReturn(3);
        when(mapper.upsertProductsFromRaw(COMPANY_ID, mockDate)).thenReturn(10);

        // --- Execute ---
        productTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMaxFetchedAtが正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxFetchedAt(COMPANY_ID);
        
        // 2. upsertCategoryGroupsFromRawが正しいcompanyIdと日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCategoryGroupsFromRaw(COMPANY_ID, mockDate);
        
        // 3. upsertProductsFromRawが正しいcompanyIdと日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertProductsFromRaw(COMPANY_ID, mockDate);
    }

    @Test
    void testExecute_handlesNullDate() throws Exception {
        // --- Mock Setup ---
        when(mapper.findMaxFetchedAt(COMPANY_ID)).thenReturn(null);
        when(mapper.upsertCategoryGroupsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class))).thenReturn(2);
        when(mapper.upsertProductsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class))).thenReturn(8);

        // --- Execute ---
        productTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMaxFetchedAtが正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxFetchedAt(COMPANY_ID);
        
        // 2. upsertCategoryGroupsFromRawが正しいcompanyIdで、かつ任意の日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCategoryGroupsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class));
        
        // 3. upsertProductsFromRawが正しいcompanyIdで、かつ任意の日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertProductsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class));
    }
}
