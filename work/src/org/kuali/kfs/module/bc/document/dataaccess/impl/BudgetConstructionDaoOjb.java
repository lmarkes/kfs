/*
 * Copyright 2006-2007 The Kuali Foundation.
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
package org.kuali.module.budget.dao.ojb;

import java.util.Collection;
import java.util.Iterator;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryFactory;
import org.apache.ojb.broker.query.ReportQueryByCriteria;
import org.kuali.module.budget.bo.BudgetConstructionFundingLock;
import org.kuali.module.budget.bo.BudgetConstructionHeader;
import org.kuali.module.budget.bo.BudgetConstructionPosition;
import org.kuali.module.budget.dao.BudgetConstructionDao;
import org.springmodules.orm.ojb.support.PersistenceBrokerDaoSupport;

/**
 * This class is the OJB implementation of the BudgetConstructionDao interface.
 *
 * 
 */
public class BudgetConstructionDaoOjb extends PersistenceBrokerDaoSupport implements BudgetConstructionDao  {

    /**
     * This gets a BudgetConstructionHeader using the candidate key
     * chart, account, subaccount, fiscalyear
     * 
     * @param chartOfAccountsCode
     * @param accountNumber
     * @param subAccountNumber
     * @param fiscalYear
     * @return BudgetConstructionHeader
     */
    public BudgetConstructionHeader getByCandidateKey(String chartOfAccountsCode, String accountNumber, String subAccountNumber, Integer fiscalYear) {
        Criteria criteria = new Criteria();
        criteria.addEqualTo("chartOfAccountsCode", chartOfAccountsCode);
        criteria.addEqualTo("accountNumber", accountNumber);
        criteria.addEqualTo("subAccountNumber",subAccountNumber);
        criteria.addEqualTo("universityFiscalYear", fiscalYear);

        return (BudgetConstructionHeader) getPersistenceBrokerTemplate().getObjectByQuery(QueryFactory.newQuery(BudgetConstructionHeader.class, criteria));
    }

    /**
     * This saves a BudgetConstructionHeader object to the database
     *
     * @param bcHeader
     */
    public void saveBudgetConstructionHeader(BudgetConstructionHeader bcHeader) {
        getPersistenceBrokerTemplate().store(bcHeader);
    }

    /**
     * This gets a BudgetConstructionFundingLock using the primary key
     * chart, account, subaccount, fiscalyear, pUId
     *
     * @param chartOfAccountsCode
     * @param accountNumber
     * @param subAccountNumber
     * @param fiscalYear
     * @param personUniversalIdentifier
     * @return BudgetConstructionFundingLock
     */
    public BudgetConstructionFundingLock getByPrimaryId(String chartOfAccountsCode, String accountNumber, String subAccountNumber, Integer fiscalYear, String personUniversalIdentifier) {
        //LOG.debug("getByPrimaryId() started");

        Criteria criteria = new Criteria();
        criteria.addEqualTo("appointmentFundingLockUserId", personUniversalIdentifier);
        criteria.addEqualTo("chartOfAccountsCode", chartOfAccountsCode);
        criteria.addEqualTo("accountNumber", accountNumber);
        criteria.addEqualTo("subAccountNumber",subAccountNumber);
        criteria.addEqualTo("universityFiscalYear", fiscalYear);

        return (BudgetConstructionFundingLock) getPersistenceBrokerTemplate().getObjectByQuery(QueryFactory.newQuery(BudgetConstructionFundingLock.class, criteria));
    }

    /**
     * This saves a BudgetConstructionFundingLock to the database
     *
     * @param budgetConstructionFundingLock
     */
    public void saveBudgetConstructionFundingLock(BudgetConstructionFundingLock budgetConstructionFundingLock) {
        getPersistenceBrokerTemplate().store(budgetConstructionFundingLock);
    }

    /**
     * This deletes a BudgetConstructionFundingLock from the database
     *
     * @param budgetConstructionFundingLock
     */
    public void deleteBudgetConstructionFundingLock(BudgetConstructionFundingLock budgetConstructionFundingLock) {
        getPersistenceBrokerTemplate().delete(budgetConstructionFundingLock);
    }

    /**
     * This gets the set of BudgetConstructionFundingLocks asssociated with a BC EDoc (account).
     * Each BudgetConstructionFundingLock has the positionNumber dummy attribute set to the
     * associated Position that is locked.  A positionNumber value of "NotFnd" indicates the
     * BudgetConstructionFundingLock is an orphan.
     *
     * @param chartOfAccountsCode
     * @param accountNumber
     * @param subAccountNumber
     * @param fiscalYear
     * @return Collection<BudgetConstructionFundingLock>
     */
    public Collection<BudgetConstructionFundingLock> getFlocksForAccount(String chartOfAccountsCode, String accountNumber, String subAccountNumber, Integer fiscalYear) {
        Collection<BudgetConstructionFundingLock> fundingLocks;
        
        Criteria criteria = new Criteria();
        criteria.addEqualTo("chartOfAccountsCode", chartOfAccountsCode);
        criteria.addEqualTo("accountNumber", accountNumber);
        criteria.addEqualTo("subAccountNumber",subAccountNumber);
        criteria.addEqualTo("universityFiscalYear", fiscalYear);
        
        fundingLocks = (Collection<BudgetConstructionFundingLock>) getPersistenceBrokerTemplate().getCollectionByQuery(QueryFactory.newQuery(BudgetConstructionFundingLock.class, criteria));
        BudgetConstructionFundingLock fundingLock;
        Iterator<BudgetConstructionFundingLock> iter = fundingLocks.iterator();
        while (iter.hasNext()){
            fundingLock = iter.next();
            fundingLock.setPositionNumber(getPositionAssociatedWithFundingLock(fundingLock));
        }

        return fundingLocks;
    }

    private String getPositionAssociatedWithFundingLock(BudgetConstructionFundingLock budgetConstructionFundingLock) {

        String positionNumber = "NotFnd";   //default if there is no associated position that is locked (orphaned)
        
        Criteria criteria = new Criteria();
        criteria.addEqualTo("pendingBudgetConstructionAppointmentFunding.chartOfAccountsCode", budgetConstructionFundingLock.getChartOfAccountsCode());
        criteria.addEqualTo("pendingBudgetConstructionAppointmentFunding.accountNumber", budgetConstructionFundingLock.getAccountNumber());
        criteria.addEqualTo("pendingBudgetConstructionAppointmentFunding.subAccountNumber",budgetConstructionFundingLock.getSubAccountNumber());
        criteria.addEqualTo("pendingBudgetConstructionAppointmentFunding.universityFiscalYear", budgetConstructionFundingLock.getUniversityFiscalYear());
        criteria.addEqualTo("positionLockUserIdentifier", budgetConstructionFundingLock.getAppointmentFundingLockUserId());
        String[] columns = new String[] { "positionNumber" };
        ReportQueryByCriteria q = QueryFactory.newReportQuery(BudgetConstructionPosition.class, columns, criteria, true);
        PersistenceBroker pb = getPersistenceBroker(true);

        Iterator<Object[]> iter = pb.getReportQueryIteratorByQuery(q);

        if (iter.hasNext()){
            Object[] objs = (Object[]) iter.next();
            if (objs[0] != null){
                positionNumber = (String) objs[0];
            }
        }
        return positionNumber;
    }
    
    /**
     * Gets a BudgetConstructionPosition from the database by the primary key
     * positionNumber, fiscalYear
     *
     * @param positionNumber
     * @param fiscalYear
     * @return BudgetConstructionPosition
     */
    public BudgetConstructionPosition getByPrimaryId(String positionNumber, Integer fiscalYear) {
        //LOG.debug("getByPrimaryId() started");

        Criteria criteria = new Criteria();
        criteria.addEqualTo("positionNumber", positionNumber);
        criteria.addEqualTo("universityFiscalYear", fiscalYear);

        return (BudgetConstructionPosition) getPersistenceBrokerTemplate().getObjectByQuery(QueryFactory.newQuery(BudgetConstructionPosition.class, criteria));
    }

    /**
     * Saves a BudgetConstructionPosition to the database
     *
     * @param bcPosition
     */
    public void saveBudgetConstructionPosition(BudgetConstructionPosition bcPosition) {
        getPersistenceBrokerTemplate().store(bcPosition);
    }

}

