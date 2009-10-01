/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.business.advance.entities;

import java.math.BigDecimal;

import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;
import org.navalplanner.business.common.BaseEntity;
import org.navalplanner.business.orders.entities.OrderElement;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */

public class AdvanceType extends BaseEntity {

    public static AdvanceType create() {
        AdvanceType advanceType = new AdvanceType();
        advanceType.setNewObject(true);
        return advanceType;
    }

    public static AdvanceType create(String unitName,
            BigDecimal defaultMaxValue, boolean updatable,
            BigDecimal unitPrecision, boolean active, boolean percentage) {
        AdvanceType advanceType = new AdvanceType(unitName, defaultMaxValue, updatable,
                        unitPrecision, active, percentage);
        advanceType.setNewObject(true);
        return advanceType;
    }

    @NotEmpty
    private String unitName;

    @NotNull
    private BigDecimal defaultMaxValue;

    @NotNull
    private boolean updatable = true;

    @NotNull
    private BigDecimal unitPrecision;

    @NotNull
    private boolean active = true;

    @NotNull
    private boolean percentage = false;

    /**
     * Constructor for hibernate. Do not use!
     */
    public AdvanceType() {

    }

    private AdvanceType(String unitName, BigDecimal defaultMaxValue,
            boolean updatable, BigDecimal unitPrecision, boolean active,
            boolean percentage) {
        this.unitName = unitName;
        this.percentage = percentage;
        setDefaultMaxValue(defaultMaxValue);
        this.defaultMaxValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.updatable = updatable;
        this.unitPrecision = unitPrecision;
        this.unitPrecision.setScale(4, BigDecimal.ROUND_HALF_UP);
        this.active = active;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public void setDefaultMaxValue(BigDecimal defaultMaxValue) {
        if (defaultMaxValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "The maximum value must be greater than 0");
        }
        if (percentage) {
            if (defaultMaxValue.compareTo(new BigDecimal(100)) > 0) {
                throw new IllegalArgumentException(
                        "The maximum value for percentage is 100");
            }
        }
        this.defaultMaxValue = defaultMaxValue;
        this.defaultMaxValue.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getDefaultMaxValue() {
        return this.defaultMaxValue;
    }

    public boolean isUpdatable() {
        return this.updatable;
    }

    public boolean isImmutable() {
        return !this.updatable;
    }

    public void setUnitPrecision(BigDecimal precision) {
        this.unitPrecision = precision;
        this.unitPrecision.setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getUnitPrecision() {
        return this.unitPrecision;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getActive() {
        return this.active;
    }

    public String getType() {
        if (isUpdatable())
            return "De Usuario";
        return "Predefinido";
    }

    public void doPropagateAdvaceToParent(OrderElement orderElement) {
    }

    public boolean isPrecisionValid(BigDecimal precision) {
        if ((this.defaultMaxValue == null) || (precision == null))
            return true;
        if (this.defaultMaxValue.compareTo(precision) < 0)
            return false;
        return true;

    }

    public boolean isDefaultMaxValueValid(BigDecimal defaultMaxValue) {
        if ((this.unitPrecision == null) || (defaultMaxValue == null))
            return true;
        if (this.unitPrecision.compareTo(defaultMaxValue) > 0)
            return false;
        return true;
    }

    public static boolean equivalentInDB(AdvanceType type, AdvanceType otherType) {
        if (type.getId() == null || otherType.getId() == null)
            return false;
        return type.getId().equals(otherType.getId());
    }

    public void setPercentage(boolean percentage) {
        if (percentage) {
            if (defaultMaxValue.compareTo(new BigDecimal(100)) > 0) {
                throw new IllegalArgumentException(
                        "The maximum value for percentage is 100");
            }
        }
        this.percentage = percentage;
    }

    public boolean getPercentage() {
        return percentage;
    }

}
