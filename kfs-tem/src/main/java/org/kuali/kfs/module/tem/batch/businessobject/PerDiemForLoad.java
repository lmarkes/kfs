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
package org.kuali.kfs.module.tem.batch.businessobject;

import java.util.LinkedHashMap;

import org.kuali.kfs.module.tem.businessobject.PerDiem;
import org.kuali.kfs.module.tem.businessobject.PrimaryDestination;
import org.kuali.kfs.module.tem.businessobject.TemRegion;
import org.kuali.rice.core.api.util.type.KualiDecimal;

public class PerDiemForLoad extends PerDiem {
    public static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PerDiemForLoad.class);

    private String effectiveDateAsString;
    private String seasonBeginDateAsString;
    private String seasonEndDateAsString;
    private String expirationDateAsString;
    private String unusedValueHolder;
    private String regionNameForReport;
    private String countyForReport;
    private String primaryDestinationForReport;

    public PerDiemForLoad() {
        setPrimaryDestination(new PrimaryDestination());
        getPrimaryDestination().setRegion(new TemRegion());
    }

    /**
     * Gets the unusedValueHolder attribute.
     *
     * @return Returns the unusedValueHolder.
     */
    public String getUnusedValueHolder() {
        return unusedValueHolder;
    }

    /**
     * Sets the unusedValueHolder attribute value.
     *
     * @param unusedValueHolder The unusedValueHolder to set.
     */
    public void setUnusedValueHolder(String unusedValueHolder) {
        this.unusedValueHolder = unusedValueHolder;
    }

    /**
     * Gets the seasonBeginDateAsString attribute.
     *
     * @return Returns the seasonBeginDateAsString.
     */
    public String getSeasonBeginDateAsString() {
        return seasonBeginDateAsString;
    }

    /**
     * Sets the seasonBeginDateAsString attribute value.
     *
     * @param seasonBeginDateAsString The seasonBeginDateAsString to set.
     */
    public void setSeasonBeginDateAsString(String seasonBeginDateAsString) {
        this.seasonBeginDateAsString = seasonBeginDateAsString;
    }

    /**
     * Gets the seasonEndDateAsString attribute.
     *
     * @return Returns the seasonEndDateAsString.
     */
    public String getSeasonEndDateAsString() {
        return seasonEndDateAsString;
    }

    /**
     * Sets the seasonEndDateAsString attribute value.
     *
     * @param seasonEndDateAsString The seasonEndDateAsString to set.
     */
    public void setSeasonEndDateAsString(String seasonEndDateAsString) {
        this.seasonEndDateAsString = seasonEndDateAsString;
    }

    /**
     * Gets the effectiveDateAsString attribute.
     *
     * @return Returns the effectiveDateAsString.
     */
    public String getEffectiveDateAsString() {
        return effectiveDateAsString;
    }

    /**
     * Sets the effectiveDateAsString attribute value.
     *
     * @param effectiveDateAsString The effectiveDateAsString to set.
     */
    public void setEffectiveDateAsString(String effectiveDateAsString) {
        this.effectiveDateAsString = effectiveDateAsString;
    }

    /**
     * Gets the expirationDateAsString attribute.
     * @return Returns the expirationDateAsString.
     */
    public String getExpirationDateAsString() {
        return expirationDateAsString;
    }

    /**
     * Sets the expirationDateAsString attribute value.
     * @param expirationDateAsString The expirationDateAsString to set.
     */
    public void setExpirationDateAsString(String expirationDateAsString) {
        this.expirationDateAsString = expirationDateAsString;
    }

    /**
     * Sets the localMeals attribute value.
     * @param localMeals The localMeals to set.
     */
    public void setLocalMeals(String localMeals) {
        this.setMealsAndIncidentals(new KualiDecimal(localMeals));
    }

    /**
     * Sets the localMeals attribute value.
     * @param localMeals The localMeals to set.
     */
    public void setIncidentals(String incidentals) {
        this.setIncidentals(new KualiDecimal(incidentals));
    }

    /**
     * Sets the lodging attribute value.
     * @param lodging The lodging to set.
     */
    public void setLodging(String lodging) {
        this.setLodging(new KualiDecimal(lodging));
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected LinkedHashMap toStringMapper_RICE20_REFACTORME() {
        LinkedHashMap map = super.toStringMapper_RICE20_REFACTORME();

        map.put("effectiveDateAsString", this.getEffectiveDateAsString());
        map.put("seasonBeginDateAsString", this.getSeasonBeginDateAsString());
        map.put("seasonEndDateAsString", this.getSeasonEndDateAsString());

        return map;
    }



    /**
     * Gets the regionNameForReport attribute.
     *
     * @return Returns the regionNameForReport
     */

    public String getRegionNameForReport() {
        return regionNameForReport;
    }

    /**
     * Sets the regionNameForReport attribute.
     *
     * @param regionNameForReport The regionNameForReport to set.
     */
    public void setRegionNameForReport(String regionNameForReport) {
        this.regionNameForReport = regionNameForReport;
    }

    /**
     * Gets the countyForReport attribute.
     *
     * @return Returns the countyForReport
     */

    public String getCountyForReport() {
        return countyForReport;
    }

    /**
     * Sets the countyForReport attribute.
     *
     * @param countyForReport The countyForReport to set.
     */
    public void setCountyForReport(String countyForReport) {
        this.countyForReport = countyForReport;
    }

    /**
     * Gets the primaryDestinationForReport attribute.
     *
     * @return Returns the primaryDestinationForReport
     */

    public String getPrimaryDestinationForReport() {
        return primaryDestinationForReport;
    }

    /**
     * Sets the primaryDestinationForReport attribute.
     *
     * @param primaryDestinationForReport The primaryDestinationForReport to set.
     */
    public void setPrimaryDestinationForReport(String primaryDestinationForReport) {
        this.primaryDestinationForReport = primaryDestinationForReport;
    }

    public void setRegionName(String regionName) {
        getPrimaryDestination().getRegion().setRegionName(regionName);
    }

    public void setRegionCode(String regionCode) {
        getPrimaryDestination().getRegion().setRegionCode(regionCode);

    }

    public void setPrimaryDestinationName(String primaryDestinationName) {
        getPrimaryDestination().setPrimaryDestinationName(primaryDestinationName);
    }

    public void setCounty(String county) {
        getPrimaryDestination().setCounty(county);
    }

}
