package com.beauty.beauty_sales_dwh.batch.tasklet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        // 両方のfindMax...メソッドがnull以外(mockDate)を返すようにスタブを設定
        when(mapper.findMaxUpdateDataTimeFromDimCategoryGroups(COMPANY_ID)).thenReturn(mockDate);
        when(mapper.findMaxUpdateDataTimeFromDimProducts(COMPANY_ID)).thenReturn(mockDate);
        // upsert...の呼び出しもスタブしておく (戻り値は利用しないが、Strict stubbingのため)
        when(mapper.upsertCategoryGroupsFromRaw(COMPANY_ID, mockDate)).thenReturn(3);
        when(mapper.upsertProductsFromRaw(COMPANY_ID, mockDate)).thenReturn(10);

        // --- Execute ---
        productTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMax... がそれぞれ正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxUpdateDataTimeFromDimCategoryGroups(COMPANY_ID);
        verify(mapper, times(1)).findMaxUpdateDataTimeFromDimProducts(COMPANY_ID);
        
        // 2. upsert... が正しいcompanyIdと、スタブしたmockDateで1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCategoryGroupsFromRaw(COMPANY_ID, mockDate);
        verify(mapper, times(1)).upsertProductsFromRaw(COMPANY_ID, mockDate);
    }

    @Test
    void testExecute_handlesNullDate() throws Exception {
        // --- Mock Setup ---
        when(mapper.findMaxUpdateDataTimeFromDimCategoryGroups(COMPANY_ID)).thenReturn(null);
        when(mapper.upsertCategoryGroupsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class))).thenReturn(2);
        when(mapper.upsertProductsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class))).thenReturn(8);

        // --- Execute ---
        productTransformTasklet.execute(stepContribution, chunkContext);

        // --- Verification ---
        // 1. findMaxFetchedAtが正しいcompanyIdで1回呼ばれたことを確認
        verify(mapper, times(1)).findMaxUpdateDataTimeFromDimProducts(COMPANY_ID);
        
        // 2. upsertCategoryGroupsFromRawが正しいcompanyIdで、かつ任意の日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertCategoryGroupsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class));
        
        // 3. upsertProductsFromRawが正しいcompanyIdで、かつ任意の日付で1回呼ばれたことを確認
        verify(mapper, times(1)).upsertProductsFromRaw(eq(COMPANY_ID), any(OffsetDateTime.class));
    }
}
