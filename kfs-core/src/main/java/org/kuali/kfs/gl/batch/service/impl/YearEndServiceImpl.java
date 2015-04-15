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
package org.kuali.kfs.gl.batch.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kuali.kfs.coa.service.BalanceTypeService;
import org.kuali.kfs.coa.service.ObjectTypeService;
import org.kuali.kfs.coa.service.PriorYearAccountService;
import org.kuali.kfs.coa.service.SubFundGroupService;
import org.kuali.kfs.gl.GeneralLedgerConstants;
import org.kuali.kfs.gl.batch.BalanceForwardRuleHelper;
import org.kuali.kfs.gl.batch.NominalActivityClosingHelper;
import org.kuali.kfs.gl.batch.dataaccess.YearEndDao;
import org.kuali.kfs.gl.batch.service.EncumbranceClosingOriginEntryGenerationService;
import org.kuali.kfs.gl.batch.service.YearEndService;
import org.kuali.kfs.gl.batch.service.impl.exception.FatalErrorException;
import org.kuali.kfs.gl.businessobject.Balance;
import org.kuali.kfs.gl.businessobject.Encumbrance;
import org.kuali.kfs.gl.businessobject.OriginEntryFull;
import org.kuali.kfs.gl.dataaccess.EncumbranceDao;
import org.kuali.kfs.gl.report.LedgerSummaryReport;
import org.kuali.kfs.gl.service.BalanceService;
import org.kuali.kfs.gl.service.OriginEntryGroupService;
import org.kuali.kfs.gl.service.OriginEntryService;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.service.ReportWriterService;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.krad.service.PersistenceService;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class implements the logic to perform year end tasks.
 */
@Transactional
public class YearEndServiceImpl implements YearEndService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(YearEndServiceImpl.class);

    protected EncumbranceDao encumbranceDao;
    protected OriginEntryService originEntryService;
    protected OriginEntryGroupService originEntryGroupService;
    protected DateTimeService dateTimeService;
    protected BalanceService balanceService;
    protected BalanceTypeService balanceTypeService;
    protected ObjectTypeService objectTypeService;
    protected ParameterService parameterService;
    protected ConfigurationService configurationService;
    protected PriorYearAccountService priorYearAccountService;
    protected SubFundGroupService subFundGroupService;
    protected PersistenceService persistenceService;
    protected YearEndDao yearEndDao;
    protected String batchFileDirectoryName;
    protected EncumbranceClosingOriginEntryGenerationService encumbranceClosingOriginEntryGenerationService;
    protected ReportWriterService nominalActivityClosingReportWriterService;
    protected ReportWriterService balanceForwardReportWriterService;
    protected ReportWriterService encumbranceClosingReportWriterService;

    public static final String TRANSACTION_DATE_FORMAT_STRING = "yyyy-MM-dd";

    /**
     * Constructs a YearEndServiceImpl, and that's about it.
     */
    public YearEndServiceImpl() {
        super();
    }

    /**
     * This class actually generates all the origin entries for nominal activity closing and saves them to the proper origin entry
     * group. Note: Much (but no longer all) of the original COBOL program this code is based off of is within the comments.
     *
     * @param nominalClosingOriginEntryGroup the origin entry group to save the generated nominal closing entries to
     * @param nominalClosingJobParameters a map of parameters for the job:
     * @param nominalClosingCounts various statistical counts
     * @see org.kuali.kfs.gl.batch.service.YearEndService#closeNominalActivity()
     */
    @Override
    public void closeNominalActivity(String nominalClosingFileName, Map nominalClosingJobParameters) {

        Integer varFiscalYear = (Integer) nominalClosingJobParameters.get(GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR);
        NominalActivityClosingHelper closingHelper = new NominalActivityClosingHelper(varFiscalYear, (Date) nominalClosingJobParameters.get(GeneralLedgerConstants.ColumnNames.UNIV_DT), parameterService, configurationService, objectTypeService);

        closingHelper.addNominalClosingJobParameters(nominalClosingJobParameters);

        Map<String, Integer> nominalActivityClosingCounts = new HashMap<String, Integer>();

        Iterator<Balance> balanceIterator = null;
        if (closingHelper.isAnnualClosingChartParamterBlank()) {
            //process all charts, either ANNUAL_CLOSING_CHARTS parameter did not exist or there were no values specified
            nominalActivityClosingCounts.put("globalReadCount", new Integer(balanceService.countBalancesForFiscalYear(varFiscalYear)));
            balanceIterator = balanceService.findNominalActivityBalancesForFiscalYear(varFiscalYear);
        }
        else {
            //ANNUAL_CLOSING_CHARTS parameter was detected and contained values
            nominalActivityClosingCounts.put("globalReadCount", new Integer(balanceService.countBalancesForFiscalYear(varFiscalYear, (List<String>) nominalClosingJobParameters.get(GeneralLedgerConstants.ColumnNames.CHART_OF_ACCOUNTS_CODE))));
            balanceIterator = balanceService.findNominalActivityBalancesForFiscalYear(varFiscalYear, (List<String>) nominalClosingJobParameters.get(GeneralLedgerConstants.ColumnNames.CHART_OF_ACCOUNTS_CODE));
        }

        String accountNumberHold = null;

        boolean selectSw = false;

        nominalActivityClosingCounts.put("globalSelectCount", new Integer(0));
        nominalActivityClosingCounts.put("sequenceNumber", new Integer(0));
        nominalActivityClosingCounts.put("sequenceWriteCount", new Integer(0));
        nominalActivityClosingCounts.put("sequenceCheckCount", new Integer(0));

        boolean nonFatalErrorFlag = false;

        //create files
        File nominalClosingFile = new File(batchFileDirectoryName + File.separator + nominalClosingFileName);
        PrintStream nominalClosingPs = null;

        try {
            nominalClosingPs = new PrintStream(nominalClosingFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("nominalClosingFile Files doesn't exist " + nominalClosingFileName);
        }

        LedgerSummaryReport ledgerReport = new LedgerSummaryReport();

        while (balanceIterator.hasNext()) {

            Balance balance = balanceIterator.next();
            balance.refreshReferenceObject("option");

            selectSw = true;

            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Balance selected.");
                }
                if (balance.getAccountNumber().equals(accountNumberHold)) {
                    incrementCount(nominalActivityClosingCounts, "sequenceNumber");
                }
                else {
                    nominalActivityClosingCounts.put("sequenceNumber", new Integer(1));
                }
                incrementCount(nominalActivityClosingCounts, "globalSelectCount");
                OriginEntryFull activityEntry = closingHelper.generateActivityEntry(balance, new Integer(1));
                originEntryService.createEntry(activityEntry, nominalClosingPs);
                ledgerReport.summarizeEntry(activityEntry);
                incrementCount(nominalActivityClosingCounts, "sequenceWriteCount");
                nominalActivityClosingCounts.put("sequenceCheckCount", new Integer(nominalActivityClosingCounts.get("sequenceWriteCount").intValue()));
                if (0 == nominalActivityClosingCounts.get("sequenceCheckCount").intValue() % 1000) {
                    LOG.info(new StringBuffer("  SEQUENTIAL RECORDS WRITTEN = ").append(nominalActivityClosingCounts.get("sequenceCheckCount")).toString());
                }
                OriginEntryFull offsetEntry = closingHelper.generateOffset(balance, new Integer(1));
                originEntryService.createEntry(offsetEntry, nominalClosingPs);
                ledgerReport.summarizeEntry(offsetEntry);
                incrementCount(nominalActivityClosingCounts, "sequenceWriteCount");
                nominalActivityClosingCounts.put("sequenceCheckCount", new Integer(nominalActivityClosingCounts.get("sequenceWriteCount").intValue()));
                if (0 == nominalActivityClosingCounts.get("sequenceCheckCount").intValue() % 1000) {
                    LOG.info(new StringBuffer(" ORIGIN ENTRIES INSERTED = ").append(nominalActivityClosingCounts.get("sequenceCheckCount")).toString());
                }
                if (nominalActivityClosingCounts.get("globalSelectCount").intValue() % 1000 == 0) {
                //    persistenceService.clearCache();
                }
                accountNumberHold = balance.getAccountNumber();
            }
            catch (FatalErrorException fee) {
                LOG.warn("Failed to create entry pair for balance.", fee);
            }
        }
        nominalActivityClosingCounts.put("nonFatalCount", closingHelper.getNonFatalErrorCount());
        nominalClosingPs.close();

        // now write parameters
        for (Object jobParameterKeyAsObject : nominalClosingJobParameters.keySet()) {
            if (jobParameterKeyAsObject != null) {
                final String jobParameterKey = jobParameterKeyAsObject.toString();
                getNominalActivityClosingReportWriterService().writeParameterLine("%32s %10s", jobParameterKey, nominalClosingJobParameters.get(jobParameterKey));
            }
        }

        // now write statistics
        getNominalActivityClosingReportWriterService().writeStatisticLine("NUMBER OF GLBL RECORDS READ       %9d", nominalActivityClosingCounts.get("globalReadCount"));
        getNominalActivityClosingReportWriterService().writeStatisticLine("NUMBER OF GLBL RECORDS SELECTED   %9d", nominalActivityClosingCounts.get("globalSelectCount"));
        getNominalActivityClosingReportWriterService().writeStatisticLine("NUMBER OF SEQ RECORDS WRITTEN     %9d", nominalActivityClosingCounts.get("sequenceWriteCount"));
        getNominalActivityClosingReportWriterService().pageBreak();

        // finally, put a header on the ledger report and write it
        getNominalActivityClosingReportWriterService().writeSubTitle(configurationService.getPropertyValueAsString(KFSKeyConstants.MESSAGE_REPORT_YEAR_END_NOMINAL_ACTIVITY_CLOSING_LEDGER_TITLE_LINE));
        ledgerReport.writeReport(getNominalActivityClosingReportWriterService());
    }

    /**
     * A method that increments a count within a Map by 1
     *
     * @param counts a Map of count statistics
     * @param countName the name of the specific count ot update
     * @return the newly incremented amount
     */
    protected int incrementCount(Map<String, Integer> counts, String countName) {
        Integer value = counts.get(countName);
        int incremented = value.intValue() + 1;
        counts.put(countName, new Integer(incremented));
        return incremented;
    }

    /**
     * This method handles executing the loop over all balances, and generating reports on the balance forwarding process as a
     * whole. This method delegates all of the specific logic in terms of what balances to forward, according to what criteria, how
     * origin entries are generated, etc. This relationship makes YearEndServiceImpl and BalanceForwardRuleHelper heavily dependent
     * upon one another in terms of expected behavior.
     *
     * @param balanceForwardsUnclosedPriorYearAccountGroup the origin entry group to save balance forwarding origin entries with
     *        open accounts
     * @param balanceForwardsClosedPriorYearAccountGroup the origin entry group to save balance forwarding origin entries with
     *        closed accounts
     * @param balanceForwardRuleHelper the BalanceForwardRuleHelper which holds the important state - the job parameters and
     *        statistics - for the job to run
     */
    @Override
    public void forwardBalances(String balanceForwardsUnclosedFileName, String balanceForwardsclosedFileName, BalanceForwardRuleHelper balanceForwardRuleHelper) {
        LOG.debug("forwardBalances() started");

        // The rule helper maintains the state of the overall processing of the entire
        // set of year end balances. This state is available via balanceForwardRuleHelper.getState().
        // The helper and this class (YearEndServiceImpl) are heavily dependent upon one
        // another in terms of expected behavior and shared responsibilities.
        balanceForwardRuleHelper.setPriorYearAccountService(priorYearAccountService);
        balanceForwardRuleHelper.setSubFundGroupService(subFundGroupService);
        balanceForwardRuleHelper.setOriginEntryService(originEntryService);

        if (balanceForwardRuleHelper.isAnnualClosingChartParamterBlank()) {
            //execute delivered foundation code, either ANNUAL_CLOSING_CHARTS parameter did not exist or there were no values specified
            balanceForwardRuleHelper.getState().setGlobalReadCount(balanceService.countBalancesForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear()));
        }
        else {
            //ANNUAL_CLOSING_CHARTS parameter was detected and contained values
            balanceForwardRuleHelper.getState().setGlobalReadCount(balanceService.countBalancesForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear(), balanceForwardRuleHelper.getAnnualClosingCharts()));
        }

        Balance balance;

        //create files
        File unclosedOutputFile = new File(batchFileDirectoryName + File.separator + balanceForwardsUnclosedFileName);
        File closedOutputFile = new File(batchFileDirectoryName + File.separator + balanceForwardsclosedFileName);
        PrintStream unclosedPs = null;
        PrintStream closedPs = null;

        try {
            unclosedPs = new PrintStream(unclosedOutputFile);
            closedPs = new PrintStream(closedOutputFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("balanceForwards Files don't exist " + balanceForwardsUnclosedFileName + " and " + balanceForwardsclosedFileName);
        }

        // do the general forwards
        Iterator<Balance> generalBalances;
        if (balanceForwardRuleHelper.isAnnualClosingChartParamterBlank()) {
            //execute delivered foundation code, either ANNUAL_CLOSING_CHARTS parameter did not exist or there were no values specified
            generalBalances = balanceService.findGeneralBalancesToForwardForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear());
            LOG.info("doing general forwards for fiscal year");
        }
        else {
            //ANNUAL_CLOSING_CHARTS parameter was detected and contained values
            generalBalances = balanceService.findGeneralBalancesToForwardForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear(), balanceForwardRuleHelper.getAnnualClosingCharts());
            LOG.info("doing general forwards for fiscal year and charts");
        }

        while (generalBalances.hasNext()) {
            balance = generalBalances.next();
            balanceForwardRuleHelper.processGeneralForwardBalance(balance, closedPs, unclosedPs);
            if (balanceForwardRuleHelper.getState().getGlobalSelectCount() % 1000 == 0) {
              //  persistenceService.clearCache();
            }
        }

        // do the cumulative forwards
        Iterator<Balance> cumulativeBalances;
        if (balanceForwardRuleHelper.isAnnualClosingChartParamterBlank()) {
            //execute delivered foundation code, either ANNUAL_CLOSING_CHARTS parameter did not exist or there were no values specified
            cumulativeBalances = balanceService.findCumulativeBalancesToForwardForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear());
            LOG.info("doing cumulative forwards for fiscal year");
        }
        else {
            //ANNUAL_CLOSING_CHARTS parameter was detected and contained values
            cumulativeBalances = balanceService.findCumulativeBalancesToForwardForFiscalYear(balanceForwardRuleHelper.getClosingFiscalYear(), balanceForwardRuleHelper.getAnnualClosingCharts());
            LOG.info("doing cumulative forwards for fiscal year and charts");
        }

        while (cumulativeBalances.hasNext()) {
            balance = cumulativeBalances.next();
            balanceForwardRuleHelper.processCumulativeForwardBalance(balance, closedPs, unclosedPs);
            if (balanceForwardRuleHelper.getState().getGlobalSelectCount() % 1000 == 0) {
            //    persistenceService.clearCache();
            }
        }

        // write parameters
        getBalanceForwardReportWriterService().writeParameterLine("%32s %10s", GeneralLedgerConstants.ANNUAL_CLOSING_TRANSACTION_DATE_PARM, balanceForwardRuleHelper.getTransactionDate().toString());
        getBalanceForwardReportWriterService().writeParameterLine("%32s %10s", GeneralLedgerConstants.ANNUAL_CLOSING_FISCAL_YEAR_PARM, balanceForwardRuleHelper.getClosingFiscalYear().toString());
        getBalanceForwardReportWriterService().writeParameterLine("%32s %10s", GeneralLedgerConstants.ANNUAL_CLOSING_CHARTS_PARAM, balanceForwardRuleHelper.getAnnualClosingCharts().toString());
        getBalanceForwardReportWriterService().writeParameterLine("%32s %10s", KFSConstants.SystemGroupParameterNames.GL_ANNUAL_CLOSING_DOC_TYPE, balanceForwardRuleHelper.getAnnualClosingDocType());
        getBalanceForwardReportWriterService().writeParameterLine("%32s %10s", KFSConstants.SystemGroupParameterNames.GL_ORIGINATION_CODE, balanceForwardRuleHelper.getGlOriginationCode());

        // write statistics
        getBalanceForwardReportWriterService().writeStatisticLine("NUMBER OF GLBL RECORDS READ....: %10d", balanceForwardRuleHelper.getState().getGlobalReadCount());
        getBalanceForwardReportWriterService().writeStatisticLine("NUMBER OF GLBL RECORDS SELECTED: %10d", balanceForwardRuleHelper.getState().getGlobalSelectCount());
        getBalanceForwardReportWriterService().writeStatisticLine("NUMBER OF SEQ RECORDS WRITTEN..: %10d", balanceForwardRuleHelper.getState().getSequenceWriteCount());
        getBalanceForwardReportWriterService().writeStatisticLine("RECORDS FOR CLOSED ACCOUNTS....: %10d", balanceForwardRuleHelper.getState().getSequenceClosedCount());
        getBalanceForwardReportWriterService().pageBreak();

        // write ledger reports
        getBalanceForwardReportWriterService().writeSubTitle(configurationService.getPropertyValueAsString(KFSKeyConstants.MESSAGE_REPORT_YEAR_END_BALANCE_FORWARD_OPEN_ACCOUNT_LEDGER_TITLE_LINE));
        balanceForwardRuleHelper.writeOpenAccountBalanceForwardLedgerSummaryReport(getBalanceForwardReportWriterService());
        getBalanceForwardReportWriterService().writeNewLines(4);
        getBalanceForwardReportWriterService().writeSubTitle(configurationService.getPropertyValueAsString(KFSKeyConstants.MESSAGE_REPORT_YEAR_END_BALANCE_FORWARD_CLOSED_ACCOUNT_LEDGER_TITLE_LINE));
        balanceForwardRuleHelper.writeClosedAccountBalanceForwardLedgerSummaryReport(getBalanceForwardReportWriterService());
    }

    /**
     * Create origin entries to carry forward all open encumbrances from the closing fiscal year into the opening fiscal year.
     *
     * @param originEntryGroup the origin entry group where generated origin entries should be saved
     * @param jobParameters the parameters necessary to run this job: the fiscal year to close and the university date the job was
     *        run
     * @param forwardEncumbrancesCounts the statistical counts generated by this job
     */
    @Override
    public void forwardEncumbrances(String encumbranceForwardFileName, Map jobParameters, Map<String, Integer> counts) {
        LOG.debug("forwardEncumbrances() started");

        // counters for the report
        counts.put("encumbrancesRead", new Integer(0));
        counts.put("encumbrancesSelected", new Integer(0));
        counts.put("originEntriesWritten", new Integer(0));

        LedgerSummaryReport forwardEncumbranceLedgerReport = new LedgerSummaryReport();

        //create files
        File encumbranceForwardFile = new File(batchFileDirectoryName + File.separator + encumbranceForwardFileName);
        PrintStream encumbranceForwardPs = null;

        try {
            encumbranceForwardPs = new PrintStream(encumbranceForwardFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("forwardEncumbrances Files doesn't exist " + encumbranceForwardFileName);
        }

        //values from ANNUAL_CLOSING_CHARTS parameter, parameter may not be defined(execute foundation code) or may not have values specified(execute foundation code) or may be defined with values specified(execute Cornell mod)
        List<String> charts = (List<String>) jobParameters.get(GeneralLedgerConstants.ColumnNames.CHART_OF_ACCOUNTS_CODE);
        Iterator encumbranceIterator;
        if (charts.isEmpty()) {
            //execute delivered foundation code
            // encumbranceDao will return all encumbrances for the fiscal year sorted properly by all of the appropriate keys.
            encumbranceIterator = encumbranceDao.getEncumbrancesToClose((Integer) jobParameters.get(GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR));
        }
        else {
            // encumbranceDao will return all encumbrances for the fiscal year and specified charts sorted properly by all of the appropriate keys.
            encumbranceIterator = encumbranceDao.getEncumbrancesToClose((Integer) jobParameters.get(GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR), (List<String>) jobParameters.get(GeneralLedgerConstants.ColumnNames.CHART_OF_ACCOUNTS_CODE));
        }

        while (encumbranceIterator.hasNext()) {

            Encumbrance encumbrance = (Encumbrance) encumbranceIterator.next();
            incrementCount(counts, "encumbrancesRead");

            // if the encumbrance is not completely relieved
            if (getEncumbranceClosingOriginEntryGenerationService().shouldForwardEncumbrance(encumbrance)) {

                incrementCount(counts, "encumbrancesSelected");

                // build a pair of origin entries to carry forward the encumbrance.
                OriginEntryOffsetPair beginningBalanceEntryPair = getEncumbranceClosingOriginEntryGenerationService().createBeginningBalanceEntryOffsetPair(encumbrance, (Integer) jobParameters.get(GeneralLedgerConstants.ColumnNames.UNIVERSITY_FISCAL_YEAR), (Date) jobParameters.get(GeneralLedgerConstants.ColumnNames.UNIV_DT));

                if (beginningBalanceEntryPair.isFatalErrorFlag()) {

                    continue;

                }
                else {
                    // save the entries.
                    originEntryService.createEntry(beginningBalanceEntryPair.getEntry(), encumbranceForwardPs);
                    forwardEncumbranceLedgerReport.summarizeEntry(beginningBalanceEntryPair.getEntry());
                    originEntryService.createEntry(beginningBalanceEntryPair.getOffset(), encumbranceForwardPs);
                    forwardEncumbranceLedgerReport.summarizeEntry(beginningBalanceEntryPair.getOffset());
                    incrementCount(counts, "originEntriesWritten");
                    incrementCount(counts, "originEntriesWritten");
                    if (0 == counts.get("originEntriesWritten").intValue() % 1000) {
                        LOG.info(new StringBuffer(" ORIGIN ENTRIES INSERTED = ").append(counts.get("originEntriesWritten")).toString());
                    }
                }

                // handle cost sharing if appropriate.
                boolean isEligibleForCostShare = false;
                try {
                    isEligibleForCostShare = this.getEncumbranceClosingOriginEntryGenerationService().shouldForwardCostShareForEncumbrance(beginningBalanceEntryPair.getEntry(), beginningBalanceEntryPair.getOffset(), encumbrance, beginningBalanceEntryPair.getEntry().getFinancialObjectTypeCode());
                }
                catch (FatalErrorException fee) {
                    LOG.info(fee.getMessage());
                }

                if (isEligibleForCostShare) {
                    // build and save an additional pair of origin entries to carry forward the encumbrance.
                    OriginEntryOffsetPair costShareBeginningBalanceEntryPair = getEncumbranceClosingOriginEntryGenerationService().createCostShareBeginningBalanceEntryOffsetPair(encumbrance, (Date) jobParameters.get(GeneralLedgerConstants.ColumnNames.UNIV_DT));
                    if (!costShareBeginningBalanceEntryPair.isFatalErrorFlag()) {
                        // save the cost share entries.
                        originEntryService.createEntry(costShareBeginningBalanceEntryPair.getEntry(), encumbranceForwardPs);
                        forwardEncumbranceLedgerReport.summarizeEntry(costShareBeginningBalanceEntryPair.getEntry());
                        originEntryService.createEntry(costShareBeginningBalanceEntryPair.getOffset(), encumbranceForwardPs);
                        forwardEncumbranceLedgerReport.summarizeEntry(costShareBeginningBalanceEntryPair.getOffset());
                        incrementCount(counts, "originEntriesWritten");
                        incrementCount(counts, "originEntriesWritten");
                        if (0 == counts.get("originEntriesWritten").intValue() % 1000) {
                            LOG.info(new StringBuffer(" ORIGIN ENTRIES INSERTED = ").append(counts.get("originEntriesWritten")).toString());
                        }
                    }
                }
            }
            if (counts.get("encumbrancesSelected").intValue() % 1000 == 0) {
             //   persistenceService.clearCache();
            }
        }
        encumbranceForwardPs.close();

        // write job parameters
        for (Object jobParameterKeyAsObject : jobParameters.keySet()) {
            if (jobParameterKeyAsObject != null) {
                final String jobParameterKey = jobParameterKeyAsObject.toString();
                getEncumbranceClosingReportWriterService().writeParameterLine("%32s %10s", jobParameterKey, jobParameters.get(jobParameterKey));
            }
        }

        // write statistics
        getEncumbranceClosingReportWriterService().writeStatisticLine("NUMBER OF ENCUMBRANCE RECORDS READ:     %10d", counts.get("encumbrancesRead"));
        getEncumbranceClosingReportWriterService().writeStatisticLine("NUMBER OF ENCUMBRANCE RECORDS SELECTED  %10d", counts.get("encumbrancesSelected"));
        getEncumbranceClosingReportWriterService().writeStatisticLine("NUMBER OF SEQ RECORDS WRITTEN           %10d", counts.get("originEntriesWritten"));
        getEncumbranceClosingReportWriterService().pageBreak();

        // write ledger summary report
        getEncumbranceClosingReportWriterService().writeSubTitle(configurationService.getPropertyValueAsString(KFSKeyConstants.MESSAGE_REPORT_YEAR_END_ENCUMBRANCE_FORWARDS_LEDGER_TITLE_LINE));
        forwardEncumbranceLedgerReport.writeReport(getEncumbranceClosingReportWriterService());
    }

    /**
     * @param balanceFiscalYear the fiscal year to find balances encumbrances for
     * @see org.kuali.kfs.gl.batch.service.YearEndService#logAllMissingPriorYearAccounts(java.lang.Integer)
     */
    @Override
    public void logAllMissingPriorYearAccounts(Integer fiscalYear) {
        Set<Map<String, String>> missingPriorYearAccountKeys = yearEndDao.findKeysOfMissingPriorYearAccountsForBalances(fiscalYear);
        missingPriorYearAccountKeys.addAll(yearEndDao.findKeysOfMissingPriorYearAccountsForOpenEncumbrances(fiscalYear));
        for (Map<String, String> key : missingPriorYearAccountKeys) {
            LOG.info("PRIOR YEAR ACCOUNT MISSING FOR " + key.get("chartOfAccountsCode") + "-" + key.get("accountNumber"));
        }
    }

    /**
     * @see org.kuali.kfs.gl.batch.service.YearEndService#logAllMissingPriorYearAccounts(java.lang.Integer, java.util.List)
     */
    @Override
    public void logAllMissingPriorYearAccounts(Integer fiscalYear, List<String> charts) {
        Set<Map<String, String>> missingPriorYearAccountKeys = yearEndDao.findKeysOfMissingPriorYearAccountsForBalances(fiscalYear, charts);
        missingPriorYearAccountKeys.addAll(yearEndDao.findKeysOfMissingPriorYearAccountsForOpenEncumbrances(fiscalYear, charts));
        for (Map<String, String> key : missingPriorYearAccountKeys) {
            LOG.info("PRIOR YEAR ACCOUNT MISSING FOR " + key.get("chartOfAccountsCode") + "-" + key.get("accountNumber"));
        }
    }

    /**
     * @param balanceFiscalYear the fiscal year to find balances encumbrances for
     * @see org.kuali.kfs.gl.batch.service.YearEndService#logAllMissingSubFundGroups(java.lang.Integer)
     */
    @Override
    public void logAllMissingSubFundGroups(Integer fiscalYear) {
        Set missingSubFundGroupKeys = yearEndDao.findKeysOfMissingSubFundGroupsForBalances(fiscalYear);
        missingSubFundGroupKeys.addAll(yearEndDao.findKeysOfMissingSubFundGroupsForOpenEncumbrances(fiscalYear));
        for (Object key : missingSubFundGroupKeys) {
            LOG.info("SUB FUND GROUP MISSING FOR " + (String) ((Map) key).get("subFundGroupCode"));
        }

    }

    /**
     * @see org.kuali.kfs.gl.batch.service.YearEndService#logAllMissingSubFundGroups(java.lang.Integer, java.util.List)
     */
    @Override
    public void logAllMissingSubFundGroups(Integer fiscalYear, List<String> charts) {
        Set missingSubFundGroupKeys = yearEndDao.findKeysOfMissingSubFundGroupsForBalances(fiscalYear, charts);
        missingSubFundGroupKeys.addAll(yearEndDao.findKeysOfMissingSubFundGroupsForOpenEncumbrances(fiscalYear, charts));
        for (Object key : missingSubFundGroupKeys) {
            LOG.info("SUB FUND GROUP MISSING FOR " + (String) ((Map) key).get("subFundGroupCode"));
        }

    }

    /**
     * Sets the encumbranceDao attribute, allowing the injection of an implementation of the data access object that uses a specific
     * O/R mechanism.
     *
     * @param encumbranceDao the implementation of encumbranceDao to set
     * @see org.kuali.kfs.gl.dataaccess.EncumbranceDao
     */
    public void setEncumbranceDao(EncumbranceDao encumbranceDao) {
        this.encumbranceDao = encumbranceDao;
    }

    /**
     * Sets the originEntryService attribute, allowing the injection of an implementation of that service
     *
     * @param originEntryService the implementation of originEntryService to set
     * @see org.kuali.kfs.gl.service.OriginEntryService
     */
    public void setOriginEntryService(OriginEntryService originEntryService) {
        this.originEntryService = originEntryService;
    }

    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    public void setOriginEntryGroupService(OriginEntryGroupService originEntryGroupService) {
        this.originEntryGroupService = originEntryGroupService;
    }

    public void setBalanceService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    public void setBalanceTypeService(BalanceTypeService balanceTypeService) {
        this.balanceTypeService = balanceTypeService;
    }

    public void setObjectTypeService(ObjectTypeService objectTypeService) {
        this.objectTypeService = objectTypeService;
    }


    public void setParameterService(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    public void setPriorYearAccountService(PriorYearAccountService priorYearAccountService) {
        this.priorYearAccountService = priorYearAccountService;
    }

    public void setSubFundGroupService(SubFundGroupService subFundGroupService) {
        this.subFundGroupService = subFundGroupService;
    }

    /**
     * Sets the persistenceService attribute value.
     *
     * @param persistenceService The persistenceService to set.
     */
    public void setPersistenceService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public void setYearEndDao(YearEndDao yearEndDao) {
        this.yearEndDao = yearEndDao;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setBatchFileDirectoryName(String batchFileDirectoryName) {
        this.batchFileDirectoryName = batchFileDirectoryName;
    }

    /**
     * Gets the encumbranceClosingOriginEntryGenerationService attribute.
     * @return Returns the encumbranceClosingOriginEntryGenerationService.
     */
    public EncumbranceClosingOriginEntryGenerationService getEncumbranceClosingOriginEntryGenerationService() {
        return encumbranceClosingOriginEntryGenerationService;
    }

    /**
     * Sets the encumbranceClosingOriginEntryGenerationService attribute value.
     * @param encumbranceClosingOriginEntryGenerationService The encumbranceClosingOriginEntryGenerationService to set.
     */
    public void setEncumbranceClosingOriginEntryGenerationService(EncumbranceClosingOriginEntryGenerationService encumbranceClosingOriginEntryGenerationService) {
        this.encumbranceClosingOriginEntryGenerationService = encumbranceClosingOriginEntryGenerationService;
    }

    /**
     * Gets the nominalActivityClosingReportWriterService attribute.
     * @return Returns the nominalActivityClosingReportWriterService.
     */
    public ReportWriterService getNominalActivityClosingReportWriterService() {
        return nominalActivityClosingReportWriterService;
    }

    /**
     * Sets the nominalActivityClosingReportWriterService attribute value.
     * @param nominalActivityClosingReportWriterService The nominalActivityClosingReportWriterService to set.
     */
    public void setNominalActivityClosingReportWriterService(ReportWriterService nominalActivityClosingReportWriterService) {
        this.nominalActivityClosingReportWriterService = nominalActivityClosingReportWriterService;
    }

    /**
     * Gets the balanceForwardReportWriterService attribute.
     * @return Returns the balanceForwardReportWriterService.
     */
    public ReportWriterService getBalanceForwardReportWriterService() {
        return balanceForwardReportWriterService;
    }

    /**
     * Sets the balanceForwardReportWriterService attribute value.
     * @param balanceForwardReportWriterService The balanceForwardReportWriterService to set.
     */
    public void setBalanceForwardReportWriterService(ReportWriterService balanceForwardReportWriterService) {
        this.balanceForwardReportWriterService = balanceForwardReportWriterService;
    }

    /**
     * Gets the encumbranceClosingReportWriterService attribute.
     * @return Returns the encumbranceClosingReportWriterService.
     */
    public ReportWriterService getEncumbranceClosingReportWriterService() {
        return encumbranceClosingReportWriterService;
    }

    /**
     * Sets the encumbranceClosingReportWriterService attribute value.
     * @param encumbranceClosingReportWriterService The encumbranceClosingReportWriterService to set.
     */
    public void setEncumbranceClosingReportWriterService(ReportWriterService encumbranceClosingReportWriterService) {
        this.encumbranceClosingReportWriterService = encumbranceClosingReportWriterService;
    }
}