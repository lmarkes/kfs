/*
 * The Kuali Financial System, a comprehensive financial management system for higher education.
 * 
 * Copyright 2005-2014 The Kuali Foundation
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kuali.kfs.module.tem.service;

import java.util.List;

import org.kuali.kfs.module.tem.businessobject.PerDiem;
import org.kuali.kfs.module.tem.document.TravelDocument;
import org.kuali.kfs.module.tem.document.web.struts.TravelFormBase;
import org.kuali.rice.core.api.util.type.KualiDecimal;

/**
 * define the common method calls around Per Diem objects
 */
public interface PerDiemService {

    /**
     * break down meals and incidental if needed
     *
     * @param perDiemList the given list of per diem records
     */
    public <T extends PerDiem> void breakDownMealsIncidental(List<T> perDiemList);

    /**
     * break down meals and incidental if needed
     *
     * @param perDiem the given per diem
     */
    public <T extends PerDiem> void breakDownMealsIncidental(T perDiem);

    /**
     * update the trip type of the given per diem
     *
     * @param perDiemList the given list of per diem records
     */
    public <T extends PerDiem> void updateTripType(List<T> perDiemList);

    /**
     * update the trip type of the given per diem
     *
     * @param perDiem the given per diem
     */
    public <T extends PerDiem> void updateTripType(T perDiem);


    /**
     * find the previous per diem for the given new per diem
     *
     * @param perDiem the given new per diem
     * @return the previous per diem of the given new per diem
     */
    public <T extends PerDiem> List<PerDiem> retrievePreviousPerDiem(T perDiem);



    /**
     * check whether the given per diem exists in the database
     *
     * @param perDiem the given per diem
     * @return true if the given per diem exists in the database
     */
    public <T extends PerDiem> boolean hasExistingPerDiem(T perDiem);

    /**
     * Set the PerDiem Categories display on a TravelForm and whether or not display detail breakdown on meals
     * For TA and TR docs
     *
     * @param form
     */
    public void setPerDiemCategoriesAndBreakdown(TravelFormBase form);

    public KualiDecimal getMealsAndIncidentalsGrandTotal(TravelDocument travelDocument);

    public KualiDecimal getLodgingGrandTotal(TravelDocument travelDocument);

    public KualiDecimal getMileageTotalGrandTotal(TravelDocument travelDocument);

    public KualiDecimal getDailyTotalGrandTotal(TravelDocument travelDocument);

    public Integer getMilesGrandTotal(TravelDocument travelDocument);

    /**
     * @return true if the KFS-TEM / Document / PER_DIEM_CATEGORIES says that per diem is handling lodging; false otherwise
     */
    public boolean isPerDiemHandlingLodging();

    /**
     * Finds the active per diem record for the given destination id and date
     * @param primaryDestinationId the id of the destination to find the per diem for
     * @param perDiemDate the date we want the per diem to be active on
     * @param effectiveDate the date to use when testing the per diem's effective date records
     * @return the retrieved per diem or null if a record was not found
     */
    public PerDiem getPerDiem(int primaryDestinationId, java.sql.Timestamp perDiemDate, java.sql.Date effectiveDate);

    /**
     * Determines if:
     * <ul>
     * <li>A current mileage rate for the KFS-TEM / Document / PER_DIEM_MILEAGE_RATE_EXPENSE_TYPE_CODE is available; if it is not, then per diem cannot be created
     * </ul>
     * @param form the form with the document on it, which may help in making such a decision
     */
    public boolean isMileageRateAvailableForAllPerDiem(TravelDocument doc);

    /**
     * Looks up, from the parameter KFS-TEM / Document / PER_DIEM_MILEAGE_RATE_EXPENSE_TYPE_CODE what the default per diem mileage expense type is
     * @return the default mileage rate expense type code to use on per diem
     */
    public String getDefaultPerDiemMileageRateExpenseType();
}
