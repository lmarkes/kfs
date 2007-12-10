/*
 * Copyright 2007 The Kuali Foundation.
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
package org.kuali.module.labor.service.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.kuali.core.util.KualiDecimal;
import org.kuali.module.gl.util.OJBUtility;
import org.kuali.module.labor.bo.EmployeeFunding;
import org.kuali.module.labor.bo.LaborBalanceSummary;
import org.kuali.module.labor.bo.LaborTransaction;
import org.kuali.module.labor.bo.LedgerBalance;
import org.kuali.module.labor.bo.LedgerBalanceForYearEndBalanceForward;
import org.kuali.module.labor.dao.LaborLedgerBalanceDao;
import org.kuali.module.labor.service.LaborCalculatedSalaryFoundationTrackerService;
import org.kuali.module.labor.service.LaborLedgerBalanceService;
import org.kuali.module.labor.util.DebitCreditUtil;
import org.kuali.module.labor.util.ObjectUtil;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class LaborLedgerBalanceServiceImpl implements LaborLedgerBalanceService {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LaborLedgerBalanceServiceImpl.class);

    private LaborLedgerBalanceDao laborLedgerBalanceDao;
    private LaborCalculatedSalaryFoundationTrackerService laborCalculatedSalaryFoundationTrackerService;

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findBalancesForFiscalYear(java.lang.Integer)
     */
    public Iterator<LedgerBalance> findBalancesForFiscalYear(Integer fiscalYear) {
        return laborLedgerBalanceDao.findBalancesForFiscalYear(fiscalYear);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findBalancesForFiscalYear(java.lang.Integer, java.util.Map)
     */
    public Iterator<LedgerBalance> findBalancesForFiscalYear(Integer fiscalYear, Map<String, String> fieldValues) {
        return laborLedgerBalanceDao.findBalancesForFiscalYear(fiscalYear, fieldValues);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findBalance(java.util.Map, boolean)
     */
    public Iterator findBalance(Map fieldValues, boolean isConsolidated) {
        LOG.debug("findBalance() started");
        return laborLedgerBalanceDao.findBalance(fieldValues, isConsolidated);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#getBalanceRecordCount(java.util.Map, boolean)
     */
    public Integer getBalanceRecordCount(Map fieldValues, boolean isConsolidated) {
        LOG.debug("getBalanceRecordCount() started");

        Integer recordCount = null;
        if (!isConsolidated) {
            recordCount = OJBUtility.getResultSizeFromMap(fieldValues, new LedgerBalance()).intValue();
        }
        else {
            Iterator recordCountIterator = laborLedgerBalanceDao.getConsolidatedBalanceRecordCount(fieldValues);
            List recordCountList = IteratorUtils.toList(recordCountIterator);
            recordCount = recordCountList.size();
        }
        return recordCount;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findLedgerBalance(java.util.Collection,
     *      org.kuali.module.labor.bo.LaborTransaction)
     */
    public <T extends LedgerBalance> T findLedgerBalance(Collection<T> ledgerBalanceCollection, LaborTransaction transaction, List<String> keyList) {
        for (T ledgerBalance : ledgerBalanceCollection) {
            boolean found = ObjectUtil.compareObject(ledgerBalance, transaction, keyList);
            if (found) {
                return ledgerBalance;
            }
        }
        return null;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findLedgerBalance(java.util.Collection,
     *      org.kuali.module.labor.bo.LaborTransaction)
     */
    public <T extends LedgerBalance> T findLedgerBalance(Collection<T> ledgerBalanceCollection, LaborTransaction transaction) {
        for (T ledgerBalance : ledgerBalanceCollection) {
            boolean found = ObjectUtil.compareObject(ledgerBalance, transaction, ledgerBalance.getPrimaryKeyList());
            if (found) {
                return ledgerBalance;
            }
        }
        return null;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#updateLedgerBalance(org.kuali.module.labor.bo.LedgerBalance,
     *      org.kuali.module.labor.bo.LaborTransaction)
     */
    public <T extends LedgerBalance> void updateLedgerBalance(T ledgerBalance, LaborTransaction transaction) {
        String debitCreditCode = transaction.getTransactionDebitCreditCode();
        KualiDecimal amount = transaction.getTransactionLedgerEntryAmount();
        amount = DebitCreditUtil.getNumericAmount(amount, debitCreditCode);
        ledgerBalance.addAmount(transaction.getUniversityFiscalPeriodCode(), amount);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#addLedgerBalance(java.util.Collection,
     *      org.kuali.module.labor.bo.LaborTransaction)
     */
    public LedgerBalance addLedgerBalance(Collection<LedgerBalance> ledgerBalanceCollection, LaborTransaction transaction) {
        LedgerBalance ledgerBalance = this.findLedgerBalance(ledgerBalanceCollection, transaction);

        if (ledgerBalance == null) {
            LedgerBalance newLedgerBalance = new LedgerBalance();
            ObjectUtil.buildObject(newLedgerBalance, transaction);
            updateLedgerBalance(newLedgerBalance, transaction);

            ledgerBalanceCollection.add(newLedgerBalance);
            return newLedgerBalance;
        }
        return null;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findEmployeeFunding(java.util.Map)
     */
    public List<EmployeeFunding> findEmployeeFunding(Map fieldValues, boolean isConsolidated) {
        List<EmployeeFunding> currentFundsCollection = laborLedgerBalanceDao.findCurrentEmployeeFunds(fieldValues);
        List<EmployeeFunding> encumbranceFundsCollection = laborLedgerBalanceDao.findEncumbranceEmployeeFunds(fieldValues);

        // merge encumberance with the current funds
        for (EmployeeFunding encumbranceFunding : encumbranceFundsCollection) {
            KualiDecimal encumbrance = encumbranceFunding.getAccountLineAnnualBalanceAmount().add(encumbranceFunding.getContractsGrantsBeginningBalanceAmount());
            encumbranceFunding.setOutstandingEncumbrance(encumbrance);

            if (currentFundsCollection.contains(encumbranceFunding)) {
                int index = currentFundsCollection.indexOf(encumbranceFunding);
                currentFundsCollection.get(index).setOutstandingEncumbrance(encumbranceFunding.getOutstandingEncumbrance());
            }
            else {
                currentFundsCollection.add(encumbranceFunding);
            }
        }

        // update the employee fundings
        for (EmployeeFunding employeeFunding : currentFundsCollection) {
            employeeFunding.setPersonName(employeeFunding.getEmplid());
            employeeFunding.setCurrentAmount(employeeFunding.getAccountLineAnnualBalanceAmount());
        }
        return currentFundsCollection;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findEmployeeFundingWithCSFTracker(java.util.Map)
     */
    public List<EmployeeFunding> findEmployeeFundingWithCSFTracker(Map fieldValues, boolean isConsolidated) {
        List<EmployeeFunding> currentFundsCollection = this.findEmployeeFunding(fieldValues, isConsolidated);
        List<EmployeeFunding> CSFTrackersCollection = laborCalculatedSalaryFoundationTrackerService.findCSFTrackersAsEmployeeFunding(fieldValues, isConsolidated);

        for (EmployeeFunding CSFTrackerAsEmployeeFunding : CSFTrackersCollection) {
            if (currentFundsCollection.contains(CSFTrackerAsEmployeeFunding)) {
                int index = currentFundsCollection.indexOf(CSFTrackerAsEmployeeFunding);
                EmployeeFunding currentFunds = currentFundsCollection.get(index);

                currentFunds.setCsfDeleteCode(CSFTrackerAsEmployeeFunding.getCsfDeleteCode());
                currentFunds.setCsfTimePercent(CSFTrackerAsEmployeeFunding.getCsfTimePercent());
                currentFunds.setCsfFundingStatusCode(CSFTrackerAsEmployeeFunding.getCsfFundingStatusCode());
                currentFunds.setCsfAmount(CSFTrackerAsEmployeeFunding.getCsfAmount());
                currentFunds.setCsfFullTimeEmploymentQuantity(CSFTrackerAsEmployeeFunding.getCsfFullTimeEmploymentQuantity());
            }
            else {
                currentFundsCollection.add(CSFTrackerAsEmployeeFunding);
            }
        }
        return currentFundsCollection;
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findBalanceSummary(java.lang.Integer, java.util.Collection)
     */
    public List<LaborBalanceSummary> findBalanceSummary(Integer fiscalYear, Collection<String> balanceTypes) {
        return laborLedgerBalanceDao.findBalanceSummary(fiscalYear, balanceTypes);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#save(org.kuali.module.labor.bo.LedgerBalance)
     */
    public void save(LedgerBalance ledgerBalance) {
        laborLedgerBalanceDao.save(ledgerBalance);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findBalancesForFiscalYear(java.lang.Integer, java.util.Map,
     *      java.util.List, java.util.List)
     */
    public Iterator<LedgerBalanceForYearEndBalanceForward> findBalancesForFiscalYear(Integer fiscalYear, Map<String, String> fieldValues, List<String> subFundGroupCodes, List<String> fundGroupCodes) {
        return laborLedgerBalanceDao.findBalancesForFiscalYear(fiscalYear, fieldValues, subFundGroupCodes, fundGroupCodes);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findAccountsInFundGroups(java.lang.Integer, java.util.Map,
     *      java.util.List, java.util.List)
     */
    public List<List<String>> findAccountsInFundGroups(Integer fiscalYear, Map<String, String> fieldValues, List<String> subFundGroupCodes, List<String> fundGroupCodes) {
        return laborLedgerBalanceDao.findAccountsInFundGroups(fiscalYear, fieldValues, subFundGroupCodes, fundGroupCodes);
    }

    /**
     * @see org.kuali.module.labor.service.LaborLedgerBalanceService#findLedgerBalances(java.util.Map, java.util.Map, java.util.Set,
     *      java.util.List, java.util.List)
     */
    public Collection<LedgerBalance> findLedgerBalances(Map<String, String> fieldValues, Map<String, String> exclusiveFieldValues, Set<Integer> fiscalYears, List<String> balanceTypeList, List<String> positionObjectGroupCodes) {
        return laborLedgerBalanceDao.findLedgerBalances(fieldValues, exclusiveFieldValues, fiscalYears, balanceTypeList, positionObjectGroupCodes);
    }

    /**
     * Sets the laborLedgerBalanceDao attribute value.
     * 
     * @param laborLedgerBalanceDao The laborLedgerBalanceDao to set.
     */
    public void setLaborLedgerBalanceDao(LaborLedgerBalanceDao laborLedgerBalanceDao) {
        this.laborLedgerBalanceDao = laborLedgerBalanceDao;
    }

    /**
     * Sets the laborCalculatedSalaryFoundationTrackerService attribute value.
     * 
     * @param laborCalculatedSalaryFoundationTrackerService The laborCalculatedSalaryFoundationTrackerService to set.
     */
    public void setLaborCalculatedSalaryFoundationTrackerService(LaborCalculatedSalaryFoundationTrackerService laborCalculatedSalaryFoundationTrackerService) {
        this.laborCalculatedSalaryFoundationTrackerService = laborCalculatedSalaryFoundationTrackerService;
    }
}
