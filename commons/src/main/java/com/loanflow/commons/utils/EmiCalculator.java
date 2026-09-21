package com.loanflow.commons.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class EmiCalculator {

    private static final int           SCALE   = 2;
    private static final RoundingMode  ROUNDING = RoundingMode.HALF_UP;

    private EmiCalculator() {}

    public static BigDecimal calculateEmi(BigDecimal principal,
                                          BigDecimal annualInterestRate,
                                          int tenureMonths) {
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(
                    BigDecimal.valueOf(tenureMonths), SCALE, ROUNDING);
        }

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(1200), 10, ROUNDING);

        BigDecimal onePlusR    = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(tenureMonths,
                new MathContext(10));

        BigDecimal numerator   = principal.multiply(monthlyRate)
                .multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    public static BigDecimal debtToIncomeRatio(BigDecimal totalMonthlyEmi,
                                               BigDecimal monthlyIncome) {
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.valueOf(100);
        return totalMonthlyEmi
                .divide(monthlyIncome, 4, ROUNDING)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, ROUNDING);
    }
}