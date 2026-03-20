package com.beauty.beauty_sales_dwh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 名寄せ（Identity Resolution）に関するMyBatisマッパー
 */
@Mapper
public interface IdentityResolutionMapper {

    /**
     * 重複している可能性のある顧客を抽出し、sys.merge_candidatesテーブルに挿入します。
     * スコアが80点以上のものを候補として登録します。
     * 既に(app_company_id, source, target)の組み合わせが存在する場合はスキップします。
     *
     * @param companyId アプリケーション会社ID
     * @return 挿入された候補数
     */
    int findAndInsertMergeCandidates(@Param("companyId") Long companyId);
}
