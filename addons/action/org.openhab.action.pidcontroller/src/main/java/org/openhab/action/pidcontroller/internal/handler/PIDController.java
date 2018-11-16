/**
 * Copyright (c) 2015-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.action.pidcontroller.internal.handler;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PIDController} provides the necessary methods for retrieving part(s) of the PID calculations
 * and it provides the method for the overall PID calculations. It also resets the PID controller
 *
 * @author George Erhan - Initial contribution
 * @author Hilbrand Bouwkamp - Adapated for new rule engine
 */
@NonNullByDefault
class PIDController {
    private static final double TWO = 2;
    private static final double PID_RANGE_DEFAULT = 510.0;

    private Logger logger = LoggerFactory.getLogger(PIDController.class);

    private final double outputLowerLimit;
    private final double outputUpperLimit;
    // private final BigDecimal loopTime;
    // private final double kiBase;
    // private final double kdBase;
    private double setPoint;

    private double derivativeResult;
    private double derivativePart;
    private double integralPart;
    private double proportionalPart;
    private double error;
    private double integralResult;
    private double previousError;
    private double output;

    /**
     * Ultimate gain variable Ku defined as 1/M, where M = amplitude ratio.
     */
    private double ku;
    private double kp;
    private double ki;
    private double kd;

    public PIDController(BigDecimal outputLowerLimit, BigDecimal outputUpperLimit, BigDecimal kpAdjuster,
            BigDecimal kiAdjuster, BigDecimal kdAdjuster) {
        this.outputLowerLimit = outputLowerLimit.doubleValue();
        this.outputUpperLimit = outputUpperLimit.doubleValue();
        ku = (this.outputUpperLimit - this.outputLowerLimit) / PID_RANGE_DEFAULT;
        kp = kpAdjuster.doubleValue();// * ku;
        // kiBase = kiAdjuster.doubleValue() * ku * TWO;
        // kdBase = kdAdjuster.doubleValue() * ku;
        ki = kiAdjuster.doubleValue(); // kiBase / dt;
        kd = kdAdjuster.doubleValue(); // kdBase * dt;
    }

    public boolean needsCalculation(BigDecimal input) {
        return (setPoint - input.doubleValue()) != 0;
    }

    public void setSetPoint(BigDecimal setPoint) {
        this.setPoint = setPoint.doubleValue();
    }

    public BigDecimal calculate(BigDecimal input, BigDecimal dtBig) {
        final double dt = dtBig.doubleValue();
        error = setPoint - input.doubleValue();
        proportionalPart = kp * error;
        final double maxIntegral = (Math.abs(outputUpperLimit) - Math.abs(proportionalPart)) / ki;
        derivativeResult = (error - previousError) / dt;
        integralResult += error * dt;

        if (Math.abs(integralResult) > Math.abs(maxIntegral)) {
            integralResult = output < 0 ? -maxIntegral : maxIntegral;
        }
        if ((integralResult < 0 && error > 0) || (integralResult > 0 && error < 0)) {
            integralResult = 0;
        }
        proportionalPart = kp * error;
        integralPart = ki * integralResult;
        derivativePart = kd * derivativeResult;
        logger.debug("PID error: {}, Parts: Kp:{}, Ki:{}, Kd:{}, ", error, proportionalPart, integralPart,
                derivativePart);
        output = proportionalPart + integralPart + derivativePart;
        previousError = error;
        return BigDecimal.valueOf(output);
    }

    // public BigDecimal getProportionalPart() {
    // return proportionalPart;
    // }
    //
    // public BigDecimal getIntegralPart() {
    // return integralPart;
    // }
    //
    // public BigDecimal getDerivativePart() {
    // return derivativePart;
    // }

    public void reset() {
        derivativeResult = 0;
        error = 0;
        integralResult = 0;
        previousError = 0;
    }
}