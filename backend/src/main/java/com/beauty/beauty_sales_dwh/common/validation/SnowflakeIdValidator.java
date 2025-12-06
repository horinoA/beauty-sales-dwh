package com.beauty.beauty_sales_dwh.common.validation;

import org.springframework.stereotype.Component;

import com.beauty.beauty_sales_dwh.common.config.SnowflakeProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SnowflakeIdValidator implements ConstraintValidator<ValidSnowflakeId, Long> {

    private final SnowflakeProperties properties;
    private long timestampShift;
    
    // 時刻ズレの許容範囲（ミリ秒）。分散システムでは数秒ズレることはよくあるため。
    private static final long TIME_TOLERANCE_MS = 5000L; 

    @Override
    public void initialize(ValidSnowflakeId constraintAnnotation) {
        this.timestampShift = properties.getNodeIdBits() + properties.getSequenceBits();
    }

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNullは別途指定してもらう方針
        }

        // 簡易チェック: 正の数であること
        if (value <= 0) {
            return false;
        }

        try {
            long timestampPart = (value >> this.timestampShift);
            long timestamp = timestampPart + properties.getEpoch();
            long now = System.currentTimeMillis();

            // 生成されたIDの時刻が「Epochより後」かつ「現在時刻 + 許容範囲 より前」であること
            return timestamp >= properties.getEpoch() 
                && timestamp <= (now + TIME_TOLERANCE_MS);
                
        } catch (Exception e) {
            return false;
        }
    }
}