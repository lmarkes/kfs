/*
 * Copyright 2008 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.module.budget.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kuali.core.service.KualiConfigurationService;
import org.kuali.kfs.KFSPropertyConstants;
import org.kuali.module.budget.BCConstants;
import org.kuali.module.budget.BCKeyConstants;
import org.kuali.module.budget.bo.BudgetConstructionMonthSummary;
import org.kuali.module.budget.bo.BudgetConstructionOrgMonthSummaryReport;
import org.kuali.module.budget.bo.BudgetConstructionOrgMonthSummaryReportTotal;
import org.kuali.module.budget.dao.BudgetConstructionMonthSummaryReportDao;
import org.kuali.module.budget.service.BudgetConstructionMonthSummaryReportService;
import org.kuali.module.budget.service.BudgetConstructionOrganizationReportsService;
import org.kuali.module.budget.util.BudgetConstructionReportHelper;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation of BudgetConstructionLevelSummaryReportService.
 */
@Transactional
public class BudgetConstructionMonthSummaryReportServiceImpl implements BudgetConstructionMonthSummaryReportService {

    BudgetConstructionMonthSummaryReportDao budgetConstructionMonthSummaryReportDao;
    BudgetConstructionOrganizationReportsService budgetConstructionOrganizationReportsService;
    KualiConfigurationService kualiConfigurationService;

    /**
     * @see org.kuali.module.budget.service.BudgetReportsControlListService#updateRepotsMonthSummaryTable(java.lang.String)
     */
    public void updateMonthSummaryReport(String personUserIdentifier, boolean consolidateToObjectCodeLevel) {
        budgetConstructionMonthSummaryReportDao.updateReportsMonthSummaryTable(personUserIdentifier, consolidateToObjectCodeLevel);

    }

    /**
     * sets budgetConstructionMonthSummaryReportDao
     * 
     * @param budgetConstructionMonthSummaryReportDao
     */
    public void setBudgetConstructionMonthSummaryReportDao(BudgetConstructionMonthSummaryReportDao budgetConstructionMonthSummaryReportDao) {
        this.budgetConstructionMonthSummaryReportDao = budgetConstructionMonthSummaryReportDao;
    }

    /**
     * @see org.kuali.module.budget.service.BudgetConstructionMonthSummaryReportService#buildReports(java.lang.Integer,
     *      java.util.Collection)
     */
    public Collection<BudgetConstructionOrgMonthSummaryReport> buildReports(Integer universityFiscalYear, String personUserIdentifier) {
        Collection<BudgetConstructionOrgMonthSummaryReport> reportSet = new ArrayList();

        BudgetConstructionOrgMonthSummaryReport orgMonthSummaryReportEntry;
        // build searchCriteria
        Map searchCriteria = new HashMap();
        searchCriteria.put(KFSPropertyConstants.KUALI_USER_PERSON_UNIVERSAL_IDENTIFIER, personUserIdentifier);

        // build order list
        List<String> orderList = buildOrderByList();
        Collection<BudgetConstructionMonthSummary> monthSummaryList = budgetConstructionOrganizationReportsService.getBySearchCriteriaOrderByList(BudgetConstructionMonthSummary.class, searchCriteria, orderList);


        // Making a list with same organizationChartOfAccountsCode, organizationCode, chartOfAccountsCode, subFundGroupCode
        List listForCalculateLevel = deleteDuplicated((List) monthSummaryList, 1);
        List listForCalculateCons = deleteDuplicated((List) monthSummaryList, 2);
        List listForCalculateType = deleteDuplicated((List) monthSummaryList, 3);
        List listForCalculateIncexp = deleteDuplicated((List) monthSummaryList, 4);


        // Calculate Total Section
        List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalLevelList;
        List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalConsList;
        List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalTypeList;
        List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalIncexpList;

        monthSummaryTotalLevelList = calculateLevelTotal((List) monthSummaryList, listForCalculateLevel);
        monthSummaryTotalConsList = calculateConsTotal((List) monthSummaryList, listForCalculateCons);
        monthSummaryTotalTypeList = calculateTypeTotal((List) monthSummaryList, listForCalculateType);
        monthSummaryTotalIncexpList = calculateIncexpTotal((List) monthSummaryList, listForCalculateIncexp);


        for (BudgetConstructionMonthSummary monthSummaryEntry : monthSummaryList) {
            orgMonthSummaryReportEntry = new BudgetConstructionOrgMonthSummaryReport();
            buildReportsHeader(universityFiscalYear, orgMonthSummaryReportEntry, monthSummaryEntry);
            buildReportsBody(orgMonthSummaryReportEntry, monthSummaryEntry);
            buildReportsTotal(orgMonthSummaryReportEntry, monthSummaryEntry, monthSummaryTotalLevelList, monthSummaryTotalConsList, monthSummaryTotalTypeList, monthSummaryTotalIncexpList);
            reportSet.add(orgMonthSummaryReportEntry);
        }

        return reportSet;
    }

    /**
     * builds report Header
     * 
     * @param BudgetConstructionMonthSummary bcas
     */
    public void buildReportsHeader(Integer universityFiscalYear, BudgetConstructionOrgMonthSummaryReport orgMonthSummaryReportEntry, BudgetConstructionMonthSummary monthSummary) {
        String orgChartDesc = monthSummary.getOrganizationChartOfAccounts().getFinChartOfAccountDescription();
        String chartDesc = monthSummary.getChartOfAccounts().getFinChartOfAccountDescription();
        String orgName = monthSummary.getOrganization().getOrganizationName();
        String reportChartDesc = monthSummary.getChartOfAccounts().getReportsToChartOfAccounts().getFinChartOfAccountDescription();
        String subFundGroupName = monthSummary.getSubFundGroup().getSubFundGroupCode();
        String subFundGroupDes = monthSummary.getSubFundGroup().getSubFundGroupDescription();
        String fundGroupName = monthSummary.getSubFundGroup().getFundGroupCode();
        String fundGroupDes = monthSummary.getSubFundGroup().getFundGroup().getName();

        Integer prevFiscalyear = universityFiscalYear - 1;
        orgMonthSummaryReportEntry.setFiscalYear(prevFiscalyear.toString() + " - " + universityFiscalYear.toString().substring(2, 4));
        orgMonthSummaryReportEntry.setOrgChartOfAccountsCode(monthSummary.getOrganizationChartOfAccountsCode());

        if (orgChartDesc == null) {
            orgMonthSummaryReportEntry.setOrgChartOfAccountDescription(kualiConfigurationService.getPropertyString(BCKeyConstants.ERROR_REPORT_GETTING_CHART_DESCRIPTION));
        }
        else {
            orgMonthSummaryReportEntry.setOrgChartOfAccountDescription(orgChartDesc);
        }

        orgMonthSummaryReportEntry.setOrganizationCode(monthSummary.getOrganizationCode());
        if (orgName == null) {
            orgMonthSummaryReportEntry.setOrganizationName(kualiConfigurationService.getPropertyString(BCKeyConstants.ERROR_REPORT_GETTING_ORGANIZATION_NAME));
        }
        else {
            orgMonthSummaryReportEntry.setOrganizationName(orgName);
        }

        orgMonthSummaryReportEntry.setChartOfAccountsCode(monthSummary.getChartOfAccountsCode());
        if (chartDesc == null) {
            orgMonthSummaryReportEntry.setChartOfAccountDescription(kualiConfigurationService.getPropertyString(BCKeyConstants.ERROR_REPORT_GETTING_CHART_DESCRIPTION));
        }
        else {
            orgMonthSummaryReportEntry.setChartOfAccountDescription(chartDesc);
        }

        orgMonthSummaryReportEntry.setFundGroupCode(monthSummary.getSubFundGroup().getFundGroupCode());
        if (fundGroupDes == null) {
            orgMonthSummaryReportEntry.setFundGroupName(kualiConfigurationService.getPropertyString(BCKeyConstants.ERROR_REPORT_GETTING_FUNDGROUP_NAME));
        }
        else {
            orgMonthSummaryReportEntry.setFundGroupName(fundGroupDes);
        }

        orgMonthSummaryReportEntry.setSubFundGroupCode(monthSummary.getSubFundGroupCode());
        if (subFundGroupDes == null) {
            orgMonthSummaryReportEntry.setSubFundGroupDescription(kualiConfigurationService.getPropertyString(BCKeyConstants.ERROR_REPORT_GETTING_SUBFUNDGROUP_DESCRIPTION));
        }
        else {
            orgMonthSummaryReportEntry.setSubFundGroupDescription(subFundGroupDes);
        }

        Integer prevPrevFiscalyear = prevFiscalyear - 1;
        orgMonthSummaryReportEntry.setHeader1("Object Level Name");
        orgMonthSummaryReportEntry.setHeader2("FTE");
        orgMonthSummaryReportEntry.setHeader3("Amount");
        orgMonthSummaryReportEntry.setHeader4("Amount");
        orgMonthSummaryReportEntry.setHeader5(kualiConfigurationService.getPropertyString(BCKeyConstants.MSG_REPORT_HEADER_CHANGE));
        orgMonthSummaryReportEntry.setHeader6(kualiConfigurationService.getPropertyString(BCKeyConstants.MSG_REPORT_HEADER_CHANGE));
        orgMonthSummaryReportEntry.setConsHdr("");

        // For page break for objectMonthCode
        //orgMonthSummaryReportEntry.setFinancialObjectLevelCode(monthSummary.getFinancialObjectLevelCode());
        orgMonthSummaryReportEntry.setIncomeExpenseCode(monthSummary.getIncomeExpenseCode());
        orgMonthSummaryReportEntry.setFinancialConsolidationSortCode(monthSummary.getFinancialConsolidationSortCode());
        orgMonthSummaryReportEntry.setFinancialLevelSortCode(monthSummary.getFinancialLevelSortCode());
        orgMonthSummaryReportEntry.setFinancialObjectCode(monthSummary.getFinancialObjectCode());
        orgMonthSummaryReportEntry.setFinancialSubObjectCode(monthSummary.getFinancialSubObjectCode());
    }

    /**
     * builds report body
     * 
     * @param BudgetConstructionMonthSummary bcas
     */
    public void buildReportsBody(BudgetConstructionOrgMonthSummaryReport orgMonthSummaryReportEntry, BudgetConstructionMonthSummary monthSummary) {
        
        orgMonthSummaryReportEntry.setFinancialObjectCodeName("TEMP NAME");
        orgMonthSummaryReportEntry.setAccountLineAnnualBalanceAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getAccountLineAnnualBalanceAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth1LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth1LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth2LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth2LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth3LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth3LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth4LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth4LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth5LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth5LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth6LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth6LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth7LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth7LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth8LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth8LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth9LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth9LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth10LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth10LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth11LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth11LineAmount()));
        orgMonthSummaryReportEntry.setFinancialDocumentMonth12LineAmount(BudgetConstructionReportHelper.convertKualiInteger(monthSummary.getFinancialDocumentMonth12LineAmount()));

        
        
    }

    /**
     * builds report total
     * 
     * @param BudgetConstructionMonthSummary bcas
     * @param List reportTotalList
     */     
    public void buildReportsTotal(BudgetConstructionOrgMonthSummaryReport orgMonthSummaryReportEntry, BudgetConstructionMonthSummary monthSummary, List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalLevelList, List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalConsList, List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalTypeList, List<BudgetConstructionOrgMonthSummaryReportTotal> monthSummaryTotalIncexpList) {

        Integer differenceAccountLineAnnualBalanceAmount = new Integer(0);
        Integer differenceMonth1LineAmount = new Integer(0);
        Integer differenceMonth2LineAmount = new Integer(0);
        Integer differenceMonth3LineAmount = new Integer(0);
        Integer differenceMonth4LineAmount = new Integer(0);
        Integer differenceMonth5LineAmount = new Integer(0);
        Integer differenceMonth6LineAmount = new Integer(0);
        Integer differenceMonth7LineAmount = new Integer(0);
        Integer differenceMonth8LineAmount = new Integer(0);
        Integer differenceMonth9LineAmount = new Integer(0);
        Integer differenceMonth10LineAmount = new Integer(0);
        Integer differenceMonth11LineAmount = new Integer(0);
        Integer differenceMonth12LineAmount = new Integer(0);
        
        for (BudgetConstructionOrgMonthSummaryReportTotal levelTotal : monthSummaryTotalLevelList) {
            if (isSameMonthSummaryEntryForLevel(monthSummary, levelTotal.getBudgetConstructionMonthSummary())) {
                orgMonthSummaryReportEntry.setLevelAccountLineAnnualBalanceAmount(levelTotal.getLevelAccountLineAnnualBalanceAmount());
                orgMonthSummaryReportEntry.setLevelMonth1LineAmount(levelTotal.getLevelMonth1LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth2LineAmount(levelTotal.getLevelMonth2LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth3LineAmount(levelTotal.getLevelMonth3LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth4LineAmount(levelTotal.getLevelMonth4LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth5LineAmount(levelTotal.getLevelMonth5LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth6LineAmount(levelTotal.getLevelMonth6LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth7LineAmount(levelTotal.getLevelMonth7LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth8LineAmount(levelTotal.getLevelMonth8LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth9LineAmount(levelTotal.getLevelMonth9LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth10LineAmount(levelTotal.getLevelMonth10LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth11LineAmount(levelTotal.getLevelMonth11LineAmount());
                orgMonthSummaryReportEntry.setLevelMonth12LineAmount(levelTotal.getLevelMonth12LineAmount());
                
            }
        }

        for (BudgetConstructionOrgMonthSummaryReportTotal consTotal : monthSummaryTotalConsList) {
            if (isSameMonthSummaryEntryForCons(monthSummary, consTotal.getBudgetConstructionMonthSummary())) {
                orgMonthSummaryReportEntry.setConsAccountLineAnnualBalanceAmount(consTotal.getConsAccountLineAnnualBalanceAmount());
                orgMonthSummaryReportEntry.setConsMonth1LineAmount(consTotal.getConsMonth1LineAmount());
                orgMonthSummaryReportEntry.setConsMonth2LineAmount(consTotal.getConsMonth2LineAmount());
                orgMonthSummaryReportEntry.setConsMonth3LineAmount(consTotal.getConsMonth3LineAmount());
                orgMonthSummaryReportEntry.setConsMonth4LineAmount(consTotal.getConsMonth4LineAmount());
                orgMonthSummaryReportEntry.setConsMonth5LineAmount(consTotal.getConsMonth5LineAmount());
                orgMonthSummaryReportEntry.setConsMonth6LineAmount(consTotal.getConsMonth6LineAmount());
                orgMonthSummaryReportEntry.setConsMonth7LineAmount(consTotal.getConsMonth7LineAmount());
                orgMonthSummaryReportEntry.setConsMonth8LineAmount(consTotal.getConsMonth8LineAmount());
                orgMonthSummaryReportEntry.setConsMonth9LineAmount(consTotal.getConsMonth9LineAmount());
                orgMonthSummaryReportEntry.setConsMonth10LineAmount(consTotal.getConsMonth10LineAmount());
                orgMonthSummaryReportEntry.setConsMonth11LineAmount(consTotal.getConsMonth11LineAmount());
                orgMonthSummaryReportEntry.setConsMonth12LineAmount(consTotal.getConsMonth12LineAmount());
            }
        }

        for (BudgetConstructionOrgMonthSummaryReportTotal typeTotal : monthSummaryTotalTypeList) {
            if (isSameMonthSummaryEntryForType(monthSummary, typeTotal.getBudgetConstructionMonthSummary())) {
                orgMonthSummaryReportEntry.setTypeAccountLineAnnualBalanceAmount(typeTotal.getTypeAccountLineAnnualBalanceAmount());
                orgMonthSummaryReportEntry.setTypeMonth1LineAmount(typeTotal.getTypeMonth1LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth2LineAmount(typeTotal.getTypeMonth2LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth3LineAmount(typeTotal.getTypeMonth3LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth4LineAmount(typeTotal.getTypeMonth4LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth5LineAmount(typeTotal.getTypeMonth5LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth6LineAmount(typeTotal.getTypeMonth6LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth7LineAmount(typeTotal.getTypeMonth7LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth8LineAmount(typeTotal.getTypeMonth8LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth9LineAmount(typeTotal.getTypeMonth9LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth10LineAmount(typeTotal.getTypeMonth10LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth11LineAmount(typeTotal.getTypeMonth11LineAmount());
                orgMonthSummaryReportEntry.setTypeMonth12LineAmount(typeTotal.getTypeMonth12LineAmount());

            }
        }

        for (BudgetConstructionOrgMonthSummaryReportTotal incexpTotal : monthSummaryTotalIncexpList) {
            if (isSameMonthSummaryEntryForIncexp(monthSummary, incexpTotal.getBudgetConstructionMonthSummary())) {
                orgMonthSummaryReportEntry.setRevAccountLineAnnualBalanceAmount(incexpTotal.getRevAccountLineAnnualBalanceAmount());
                orgMonthSummaryReportEntry.setRevMonth1LineAmount(incexpTotal.getRevMonth1LineAmount());
                orgMonthSummaryReportEntry.setRevMonth2LineAmount(incexpTotal.getRevMonth2LineAmount());
                orgMonthSummaryReportEntry.setRevMonth3LineAmount(incexpTotal.getRevMonth3LineAmount());
                orgMonthSummaryReportEntry.setRevMonth4LineAmount(incexpTotal.getRevMonth4LineAmount());
                orgMonthSummaryReportEntry.setRevMonth5LineAmount(incexpTotal.getRevMonth5LineAmount());
                orgMonthSummaryReportEntry.setRevMonth6LineAmount(incexpTotal.getRevMonth6LineAmount());
                orgMonthSummaryReportEntry.setRevMonth7LineAmount(incexpTotal.getRevMonth7LineAmount());
                orgMonthSummaryReportEntry.setRevMonth8LineAmount(incexpTotal.getRevMonth8LineAmount());
                orgMonthSummaryReportEntry.setRevMonth9LineAmount(incexpTotal.getRevMonth9LineAmount());
                orgMonthSummaryReportEntry.setRevMonth10LineAmount(incexpTotal.getRevMonth10LineAmount());
                orgMonthSummaryReportEntry.setRevMonth11LineAmount(incexpTotal.getRevMonth11LineAmount());
                orgMonthSummaryReportEntry.setRevMonth12LineAmount(incexpTotal.getRevMonth12LineAmount());
                
                orgMonthSummaryReportEntry.setExpAccountLineAnnualBalanceAmount(incexpTotal.getExpAccountLineAnnualBalanceAmount());
                orgMonthSummaryReportEntry.setExpMonth1LineAmount(incexpTotal.getExpMonth1LineAmount());
                orgMonthSummaryReportEntry.setExpMonth2LineAmount(incexpTotal.getExpMonth2LineAmount());
                orgMonthSummaryReportEntry.setExpMonth3LineAmount(incexpTotal.getExpMonth3LineAmount());
                orgMonthSummaryReportEntry.setExpMonth4LineAmount(incexpTotal.getExpMonth4LineAmount());
                orgMonthSummaryReportEntry.setExpMonth5LineAmount(incexpTotal.getExpMonth5LineAmount());
                orgMonthSummaryReportEntry.setExpMonth6LineAmount(incexpTotal.getExpMonth6LineAmount());
                orgMonthSummaryReportEntry.setExpMonth7LineAmount(incexpTotal.getExpMonth7LineAmount());
                orgMonthSummaryReportEntry.setExpMonth8LineAmount(incexpTotal.getExpMonth8LineAmount());
                orgMonthSummaryReportEntry.setExpMonth9LineAmount(incexpTotal.getExpMonth9LineAmount());
                orgMonthSummaryReportEntry.setExpMonth10LineAmount(incexpTotal.getExpMonth10LineAmount());
                orgMonthSummaryReportEntry.setExpMonth11LineAmount(incexpTotal.getExpMonth11LineAmount());
                orgMonthSummaryReportEntry.setExpMonth12LineAmount(incexpTotal.getExpMonth12LineAmount());
                
                differenceAccountLineAnnualBalanceAmount = incexpTotal.getRevAccountLineAnnualBalanceAmount() - incexpTotal.getExpAccountLineAnnualBalanceAmount();
                differenceMonth1LineAmount = incexpTotal.getRevMonth1LineAmount() - incexpTotal.getExpMonth1LineAmount();
                differenceMonth2LineAmount = incexpTotal.getRevMonth2LineAmount() - incexpTotal.getExpMonth2LineAmount();
                differenceMonth3LineAmount = incexpTotal.getRevMonth3LineAmount() - incexpTotal.getExpMonth3LineAmount();
                differenceMonth4LineAmount = incexpTotal.getRevMonth4LineAmount() - incexpTotal.getExpMonth4LineAmount();
                differenceMonth5LineAmount = incexpTotal.getRevMonth5LineAmount() - incexpTotal.getExpMonth5LineAmount();
                differenceMonth6LineAmount = incexpTotal.getRevMonth6LineAmount() - incexpTotal.getExpMonth6LineAmount();
                differenceMonth7LineAmount = incexpTotal.getRevMonth7LineAmount() - incexpTotal.getExpMonth7LineAmount();
                differenceMonth8LineAmount = incexpTotal.getRevMonth8LineAmount() - incexpTotal.getExpMonth8LineAmount();
                differenceMonth9LineAmount = incexpTotal.getRevMonth9LineAmount() - incexpTotal.getExpMonth9LineAmount();
                differenceMonth10LineAmount = incexpTotal.getRevMonth10LineAmount() - incexpTotal.getExpMonth10LineAmount();
                differenceMonth11LineAmount = incexpTotal.getRevMonth11LineAmount() - incexpTotal.getExpMonth11LineAmount();
                differenceMonth12LineAmount = incexpTotal.getRevMonth12LineAmount() - incexpTotal.getExpMonth12LineAmount();
                
                orgMonthSummaryReportEntry.setDifferenceAccountLineAnnualBalanceAmount(differenceAccountLineAnnualBalanceAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth1LineAmount(differenceMonth1LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth2LineAmount(differenceMonth2LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth3LineAmount(differenceMonth3LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth4LineAmount(differenceMonth4LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth5LineAmount(differenceMonth5LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth6LineAmount(differenceMonth6LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth7LineAmount(differenceMonth7LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth8LineAmount(differenceMonth8LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth9LineAmount(differenceMonth9LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth10LineAmount(differenceMonth10LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth11LineAmount(differenceMonth11LineAmount);
                orgMonthSummaryReportEntry.setDifferenceMonth12LineAmount(differenceMonth12LineAmount);
            }
        }

    }


    public List calculateLevelTotal(List<BudgetConstructionMonthSummary> bcmsList, List<BudgetConstructionMonthSummary> simpleList) {
        Integer levelAccountLineAnnualBalanceAmount = new Integer(0);
        Integer levelMonth1LineAmount = new Integer(0);
        Integer levelMonth2LineAmount = new Integer(0);
        Integer levelMonth3LineAmount = new Integer(0);
        Integer levelMonth4LineAmount = new Integer(0);
        Integer levelMonth5LineAmount = new Integer(0);
        Integer levelMonth6LineAmount = new Integer(0);
        Integer levelMonth7LineAmount = new Integer(0);
        Integer levelMonth8LineAmount = new Integer(0);
        Integer levelMonth9LineAmount = new Integer(0);
        Integer levelMonth10LineAmount = new Integer(0);
        Integer levelMonth11LineAmount = new Integer(0);
        Integer levelMonth12LineAmount = new Integer(0);

        List returnList = new ArrayList();
        for (BudgetConstructionMonthSummary simpleBclsEntry : simpleList) {
            BudgetConstructionOrgMonthSummaryReportTotal bcMonthTotal = new BudgetConstructionOrgMonthSummaryReportTotal();
            for (BudgetConstructionMonthSummary bcmsListEntry : bcmsList) {
                if (isSameMonthSummaryEntryForLevel(simpleBclsEntry, bcmsListEntry)) {
                    levelAccountLineAnnualBalanceAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getAccountLineAnnualBalanceAmount());
                    levelMonth1LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth1LineAmount());
                    levelMonth2LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth2LineAmount());
                    levelMonth3LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth3LineAmount());
                    levelMonth4LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth4LineAmount());
                    levelMonth5LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth5LineAmount());
                    levelMonth6LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth6LineAmount());
                    levelMonth7LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth7LineAmount());
                    levelMonth8LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth8LineAmount());
                    levelMonth9LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth9LineAmount());
                    levelMonth10LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth10LineAmount());
                    levelMonth11LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth11LineAmount());
                    levelMonth12LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth12LineAmount());     
                }
            }
            bcMonthTotal.setBudgetConstructionMonthSummary(simpleBclsEntry);
            bcMonthTotal.setLevelAccountLineAnnualBalanceAmount(levelAccountLineAnnualBalanceAmount);
            bcMonthTotal.setLevelMonth1LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth2LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth3LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth4LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth5LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth6LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth7LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth8LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth9LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth10LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth11LineAmount(levelMonth1LineAmount);
            bcMonthTotal.setLevelMonth12LineAmount(levelMonth1LineAmount);
            returnList.add(bcMonthTotal);
            
            levelAccountLineAnnualBalanceAmount = new Integer(0);
            levelMonth1LineAmount = new Integer(0);
            levelMonth2LineAmount = new Integer(0);
            levelMonth3LineAmount = new Integer(0);
            levelMonth4LineAmount = new Integer(0);
            levelMonth5LineAmount = new Integer(0);
            levelMonth6LineAmount = new Integer(0);
            levelMonth7LineAmount = new Integer(0);
            levelMonth8LineAmount = new Integer(0);
            levelMonth9LineAmount = new Integer(0);
            levelMonth10LineAmount = new Integer(0);
            levelMonth11LineAmount = new Integer(0);
            levelMonth12LineAmount = new Integer(0);
        }
        return returnList;
    }


    public List calculateConsTotal(List<BudgetConstructionMonthSummary> bcmsList, List<BudgetConstructionMonthSummary> simpleList) {
        
        Integer consAccountLineAnnualBalanceAmount = new Integer(0);
        Integer consMonth1LineAmount = new Integer(0);
        Integer consMonth2LineAmount = new Integer(0);
        Integer consMonth3LineAmount = new Integer(0);
        Integer consMonth4LineAmount = new Integer(0);
        Integer consMonth5LineAmount = new Integer(0);
        Integer consMonth6LineAmount = new Integer(0);
        Integer consMonth7LineAmount = new Integer(0);
        Integer consMonth8LineAmount = new Integer(0);
        Integer consMonth9LineAmount = new Integer(0);
        Integer consMonth10LineAmount = new Integer(0);
        Integer consMonth11LineAmount = new Integer(0);
        Integer consMonth12LineAmount = new Integer(0);

        List returnList = new ArrayList();


        for (BudgetConstructionMonthSummary simpleBclsEntry : simpleList) {
            BudgetConstructionOrgMonthSummaryReportTotal bcMonthTotal = new BudgetConstructionOrgMonthSummaryReportTotal();
            for (BudgetConstructionMonthSummary bcmsListEntry : bcmsList) {
                if (isSameMonthSummaryEntryForCons(simpleBclsEntry, bcmsListEntry)) {
                    consAccountLineAnnualBalanceAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getAccountLineAnnualBalanceAmount());
                    consMonth1LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth1LineAmount());
                    consMonth2LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth2LineAmount());
                    consMonth3LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth3LineAmount());
                    consMonth4LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth4LineAmount());
                    consMonth5LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth5LineAmount());
                    consMonth6LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth6LineAmount());
                    consMonth7LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth7LineAmount());
                    consMonth8LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth8LineAmount());
                    consMonth9LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth9LineAmount());
                    consMonth10LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth10LineAmount());
                    consMonth11LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth11LineAmount());
                    consMonth12LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth12LineAmount());   

                }
            }
            bcMonthTotal.setBudgetConstructionMonthSummary(simpleBclsEntry);
            bcMonthTotal.setConsAccountLineAnnualBalanceAmount(consAccountLineAnnualBalanceAmount);
            bcMonthTotal.setConsMonth1LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth2LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth3LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth4LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth5LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth6LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth7LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth8LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth9LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth10LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth11LineAmount(consMonth1LineAmount);
            bcMonthTotal.setConsMonth12LineAmount(consMonth1LineAmount);
            returnList.add(bcMonthTotal);
            
            consAccountLineAnnualBalanceAmount = new Integer(0);
            consMonth1LineAmount = new Integer(0);
            consMonth2LineAmount = new Integer(0);
            consMonth3LineAmount = new Integer(0);
            consMonth4LineAmount = new Integer(0);
            consMonth5LineAmount = new Integer(0);
            consMonth6LineAmount = new Integer(0);
            consMonth7LineAmount = new Integer(0);
            consMonth8LineAmount = new Integer(0);
            consMonth9LineAmount = new Integer(0);
            consMonth10LineAmount = new Integer(0);
            consMonth11LineAmount = new Integer(0);
            consMonth12LineAmount = new Integer(0);

        }


        return returnList;
    }


    public List calculateTypeTotal(List<BudgetConstructionMonthSummary> bcmsList, List<BudgetConstructionMonthSummary> simpleList) {

        Integer typeAccountLineAnnualBalanceAmount = new Integer(0);
        Integer typeMonth1LineAmount = new Integer(0);
        Integer typeMonth2LineAmount = new Integer(0);
        Integer typeMonth3LineAmount = new Integer(0);
        Integer typeMonth4LineAmount = new Integer(0);
        Integer typeMonth5LineAmount = new Integer(0);
        Integer typeMonth6LineAmount = new Integer(0);
        Integer typeMonth7LineAmount = new Integer(0);
        Integer typeMonth8LineAmount = new Integer(0);
        Integer typeMonth9LineAmount = new Integer(0);
        Integer typeMonth10LineAmount = new Integer(0);
        Integer typeMonth11LineAmount = new Integer(0);
        Integer typeMonth12LineAmount = new Integer(0);

        List returnList = new ArrayList();
        for (BudgetConstructionMonthSummary simpleBclsEntry : simpleList) {
            BudgetConstructionOrgMonthSummaryReportTotal bcMonthTotal = new BudgetConstructionOrgMonthSummaryReportTotal();
            for (BudgetConstructionMonthSummary bcmsListEntry : bcmsList) {
                if (isSameMonthSummaryEntryForType(simpleBclsEntry, bcmsListEntry)) {
                    typeAccountLineAnnualBalanceAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getAccountLineAnnualBalanceAmount());
                    typeMonth1LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth1LineAmount());
                    typeMonth2LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth2LineAmount());
                    typeMonth3LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth3LineAmount());
                    typeMonth4LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth4LineAmount());
                    typeMonth5LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth5LineAmount());
                    typeMonth6LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth6LineAmount());
                    typeMonth7LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth7LineAmount());
                    typeMonth8LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth8LineAmount());
                    typeMonth9LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth9LineAmount());
                    typeMonth10LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth10LineAmount());
                    typeMonth11LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth11LineAmount());
                    typeMonth12LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth12LineAmount());   
                }
            }
            
            
            bcMonthTotal.setBudgetConstructionMonthSummary(simpleBclsEntry);
            bcMonthTotal.setTypeAccountLineAnnualBalanceAmount(typeAccountLineAnnualBalanceAmount);
            bcMonthTotal.setTypeMonth1LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth2LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth3LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth4LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth5LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth6LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth7LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth8LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth9LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth10LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth11LineAmount(typeMonth1LineAmount);
            bcMonthTotal.setTypeMonth12LineAmount(typeMonth1LineAmount);
            returnList.add(bcMonthTotal);
            
            typeAccountLineAnnualBalanceAmount = new Integer(0);
            typeMonth1LineAmount = new Integer(0);
            typeMonth2LineAmount = new Integer(0);
            typeMonth3LineAmount = new Integer(0);
            typeMonth4LineAmount = new Integer(0);
            typeMonth5LineAmount = new Integer(0);
            typeMonth6LineAmount = new Integer(0);
            typeMonth7LineAmount = new Integer(0);
            typeMonth8LineAmount = new Integer(0);
            typeMonth9LineAmount = new Integer(0);
            typeMonth10LineAmount = new Integer(0);
            typeMonth11LineAmount = new Integer(0);
            typeMonth12LineAmount = new Integer(0);
            
        }

        return returnList;
    }


    public List calculateIncexpTotal(List<BudgetConstructionMonthSummary> bcmsList, List<BudgetConstructionMonthSummary> simpleList) {

        Integer revAccountLineAnnualBalanceAmount = new Integer(0);
        Integer revMonth1LineAmount = new Integer(0);
        Integer revMonth2LineAmount = new Integer(0);
        Integer revMonth3LineAmount = new Integer(0);
        Integer revMonth4LineAmount = new Integer(0);
        Integer revMonth5LineAmount = new Integer(0);
        Integer revMonth6LineAmount = new Integer(0);
        Integer revMonth7LineAmount = new Integer(0);
        Integer revMonth8LineAmount = new Integer(0);
        Integer revMonth9LineAmount = new Integer(0);
        Integer revMonth10LineAmount = new Integer(0);
        Integer revMonth11LineAmount = new Integer(0);
        Integer revMonth12LineAmount = new Integer(0);
        
        Integer expAccountLineAnnualBalanceAmount = new Integer(0);
        Integer expMonth1LineAmount = new Integer(0);
        Integer expMonth2LineAmount = new Integer(0);
        Integer expMonth3LineAmount = new Integer(0);
        Integer expMonth4LineAmount = new Integer(0);
        Integer expMonth5LineAmount = new Integer(0);
        Integer expMonth6LineAmount = new Integer(0);
        Integer expMonth7LineAmount = new Integer(0);
        Integer expMonth8LineAmount = new Integer(0);
        Integer expMonth9LineAmount = new Integer(0);
        Integer expMonth10LineAmount = new Integer(0);
        Integer expMonth11LineAmount = new Integer(0);
        Integer expMonth12LineAmount = new Integer(0);
        

        List returnList = new ArrayList();

        for (BudgetConstructionMonthSummary simpleBclsEntry : simpleList) {
            BudgetConstructionOrgMonthSummaryReportTotal bcMonthTotal = new BudgetConstructionOrgMonthSummaryReportTotal();
            for (BudgetConstructionMonthSummary bcmsListEntry : bcmsList) {
                if (isSameMonthSummaryEntryForIncexp(simpleBclsEntry, bcmsListEntry)) {
                    if (bcmsListEntry.getIncomeExpenseCode().equals(BCConstants.Report.INCOME_EXP_TYPE_A)){
                        revAccountLineAnnualBalanceAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getAccountLineAnnualBalanceAmount());
                        revMonth1LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth1LineAmount());
                        revMonth2LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth2LineAmount());
                        revMonth3LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth3LineAmount());
                        revMonth4LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth4LineAmount());
                        revMonth5LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth5LineAmount());
                        revMonth6LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth6LineAmount());
                        revMonth7LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth7LineAmount());
                        revMonth8LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth8LineAmount());
                        revMonth9LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth9LineAmount());
                        revMonth10LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth10LineAmount());
                        revMonth11LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth11LineAmount());
                        revMonth12LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth12LineAmount());
                    } else {
                        expAccountLineAnnualBalanceAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getAccountLineAnnualBalanceAmount());
                        expMonth1LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth1LineAmount());
                        expMonth2LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth2LineAmount());
                        expMonth3LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth3LineAmount());
                        expMonth4LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth4LineAmount());
                        expMonth5LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth5LineAmount());
                        expMonth6LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth6LineAmount());
                        expMonth7LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth7LineAmount());
                        expMonth8LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth8LineAmount());
                        expMonth9LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth9LineAmount());
                        expMonth10LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth10LineAmount());
                        expMonth11LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth11LineAmount());
                        expMonth12LineAmount += BudgetConstructionReportHelper.convertKualiInteger(bcmsListEntry.getFinancialDocumentMonth12LineAmount());
                    }

                }
            }
            
            bcMonthTotal.setBudgetConstructionMonthSummary(simpleBclsEntry);
            bcMonthTotal.setRevAccountLineAnnualBalanceAmount(revAccountLineAnnualBalanceAmount);
            bcMonthTotal.setRevMonth1LineAmount(revMonth1LineAmount);
            bcMonthTotal.setRevMonth2LineAmount(revMonth2LineAmount);
            bcMonthTotal.setRevMonth3LineAmount(revMonth3LineAmount);
            bcMonthTotal.setRevMonth4LineAmount(revMonth4LineAmount);
            bcMonthTotal.setRevMonth5LineAmount(revMonth5LineAmount);
            bcMonthTotal.setRevMonth6LineAmount(revMonth6LineAmount);
            bcMonthTotal.setRevMonth7LineAmount(revMonth7LineAmount);
            bcMonthTotal.setRevMonth8LineAmount(revMonth8LineAmount);
            bcMonthTotal.setRevMonth9LineAmount(revMonth9LineAmount);
            bcMonthTotal.setRevMonth10LineAmount(revMonth10LineAmount);
            bcMonthTotal.setRevMonth11LineAmount(revMonth11LineAmount);
            bcMonthTotal.setRevMonth12LineAmount(revMonth12LineAmount);
            
            bcMonthTotal.setBudgetConstructionMonthSummary(simpleBclsEntry);
            bcMonthTotal.setExpAccountLineAnnualBalanceAmount(expAccountLineAnnualBalanceAmount);
            bcMonthTotal.setExpMonth1LineAmount(expMonth1LineAmount);
            bcMonthTotal.setExpMonth2LineAmount(expMonth2LineAmount);
            bcMonthTotal.setExpMonth3LineAmount(expMonth3LineAmount);
            bcMonthTotal.setExpMonth4LineAmount(expMonth4LineAmount);
            bcMonthTotal.setExpMonth5LineAmount(expMonth5LineAmount);
            bcMonthTotal.setExpMonth6LineAmount(expMonth6LineAmount);
            bcMonthTotal.setExpMonth7LineAmount(expMonth7LineAmount);
            bcMonthTotal.setExpMonth8LineAmount(expMonth8LineAmount);
            bcMonthTotal.setExpMonth9LineAmount(expMonth9LineAmount);
            bcMonthTotal.setExpMonth10LineAmount(expMonth10LineAmount);
            bcMonthTotal.setExpMonth11LineAmount(expMonth11LineAmount);
            bcMonthTotal.setExpMonth12LineAmount(expMonth12LineAmount);
            returnList.add(bcMonthTotal);
            
            revAccountLineAnnualBalanceAmount = new Integer(0);
            revMonth1LineAmount = new Integer(0);
            revMonth2LineAmount = new Integer(0);
            revMonth3LineAmount = new Integer(0);
            revMonth4LineAmount = new Integer(0);
            revMonth5LineAmount = new Integer(0);
            revMonth6LineAmount = new Integer(0);
            revMonth7LineAmount = new Integer(0);
            revMonth8LineAmount = new Integer(0);
            revMonth9LineAmount = new Integer(0);
            revMonth10LineAmount = new Integer(0);
            revMonth11LineAmount = new Integer(0);
            revMonth12LineAmount = new Integer(0);
            
            expAccountLineAnnualBalanceAmount = new Integer(0);
            expMonth1LineAmount = new Integer(0);
            expMonth2LineAmount = new Integer(0);
            expMonth3LineAmount = new Integer(0);
            expMonth4LineAmount = new Integer(0);
            expMonth5LineAmount = new Integer(0);
            expMonth6LineAmount = new Integer(0);
            expMonth7LineAmount = new Integer(0);
            expMonth8LineAmount = new Integer(0);
            expMonth9LineAmount = new Integer(0);
            expMonth10LineAmount = new Integer(0);
            expMonth11LineAmount = new Integer(0);
            expMonth12LineAmount = new Integer(0);
        }


        return returnList;
    }


    public boolean isSameMonthSummaryEntryForLevel(BudgetConstructionMonthSummary firstBcms, BudgetConstructionMonthSummary secondBcms) {
        if (isSameMonthSummaryEntryForCons(firstBcms, secondBcms) && firstBcms.getFinancialLevelSortCode().equals(secondBcms.getFinancialLevelSortCode())) {
            return true;
        }
        else
            return false;
    }


    public boolean isSameMonthSummaryEntryForCons(BudgetConstructionMonthSummary firstBcms, BudgetConstructionMonthSummary secondBcms) {
        if (isSameMonthSummaryEntryForType(firstBcms, secondBcms) && firstBcms.getFinancialConsolidationSortCode().equals(secondBcms.getFinancialConsolidationSortCode())) {
            return true;
        }
        else
            return false;
    }


    public boolean isSameMonthSummaryEntryForType(BudgetConstructionMonthSummary firstBcms, BudgetConstructionMonthSummary secondBcms) {
        if (isSameMonthSummaryEntryForIncexp(firstBcms, secondBcms) && firstBcms.getIncomeExpenseCode().equals(secondBcms.getIncomeExpenseCode())) {
            return true;
        }

        else
            return false;
    }

    public boolean isSameMonthSummaryEntryForIncexp(BudgetConstructionMonthSummary firstBcms, BudgetConstructionMonthSummary secondBcms) {
        if (firstBcms.getOrganizationChartOfAccountsCode().equals(secondBcms.getOrganizationChartOfAccountsCode()) && firstBcms.getOrganizationCode().equals(secondBcms.getOrganizationCode()) && firstBcms.getSubFundGroupCode().equals(secondBcms.getSubFundGroupCode()) && firstBcms.getChartOfAccountsCode().equals(secondBcms.getChartOfAccountsCode())) {
            return true;
        }

        else
            return false;
    }

    /**
     * Deletes duplicated entry from list
     * 
     * @param List list
     * @return a list that all duplicated entries were deleted
     */
    public List deleteDuplicated(List list, int mode) {

        // mode 1 is for getting a list of cons
        // mode 2 is for getting a list of gexp and type
        // mode 3 is for getting a list of total

        int count = 0;
        BudgetConstructionMonthSummary monthSummaryEntry = null;
        BudgetConstructionMonthSummary monthSummaryEntryAux = null;
        List returnList = new ArrayList();
        if ((list != null) && (list.size() > 0)) {
            monthSummaryEntry = (BudgetConstructionMonthSummary) list.get(count);
            monthSummaryEntryAux = (BudgetConstructionMonthSummary) list.get(count);
            returnList.add(monthSummaryEntry);
            count++;
            while (count < list.size()) {
                monthSummaryEntry = (BudgetConstructionMonthSummary) list.get(count);
                switch (mode) {
                    case 1: {
                        if (!isSameMonthSummaryEntryForLevel(monthSummaryEntry, monthSummaryEntryAux)) {
                            returnList.add(monthSummaryEntry);
                            monthSummaryEntryAux = monthSummaryEntry;
                        }
                    }
                    case 2: {
                        if (!isSameMonthSummaryEntryForCons(monthSummaryEntry, monthSummaryEntryAux)) {
                            returnList.add(monthSummaryEntry);
                            monthSummaryEntryAux = monthSummaryEntry;
                        }
                    }
                    case 3: {
                        if (!isSameMonthSummaryEntryForType(monthSummaryEntry, monthSummaryEntryAux)) {
                            returnList.add(monthSummaryEntry);
                            monthSummaryEntryAux = monthSummaryEntry;
                        }
                    }
                    case 4: {
                        if (!isSameMonthSummaryEntryForIncexp(monthSummaryEntry, monthSummaryEntryAux)) {
                            returnList.add(monthSummaryEntry);
                            monthSummaryEntryAux = monthSummaryEntry;
                        }
                    }
                    
                    
                }
                count++;
            }
        }
        return returnList;
    }


    /**
     * builds orderByList for sort order.
     * 
     * @return returnList
     */
    public List<String> buildOrderByList() {
        List<String> returnList = new ArrayList();
        returnList.add(KFSPropertyConstants.ORGANIZATION_CHART_OF_ACCOUNTS_CODE);
        returnList.add(KFSPropertyConstants.ORGANIZATION_CODE);
        returnList.add(KFSPropertyConstants.SUB_FUND_GROUP_CODE);
        returnList.add(KFSPropertyConstants.CHART_OF_ACCOUNTS_CODE);
        returnList.add(KFSPropertyConstants.INCOME_EXPENSE_CODE);
        returnList.add(KFSPropertyConstants.FINANCIAL_CONSOLIDATION_SORT_CODE);
        returnList.add(KFSPropertyConstants.FINANCIAL_LEVEL_SORT_CODE);
        return returnList;
    }

    public void setBudgetConstructionOrganizationReportsService(BudgetConstructionOrganizationReportsService budgetConstructionOrganizationReportsService) {
        this.budgetConstructionOrganizationReportsService = budgetConstructionOrganizationReportsService;
    }

    public void setKualiConfigurationService(KualiConfigurationService kualiConfigurationService) {
        this.kualiConfigurationService = kualiConfigurationService;
    }
}
