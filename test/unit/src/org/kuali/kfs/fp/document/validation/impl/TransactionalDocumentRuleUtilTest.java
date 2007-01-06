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
package org.kuali.module.financial.rules;

import static org.kuali.core.util.SpringServiceLocator.getAccountingPeriodService;
import static org.kuali.core.util.SpringServiceLocator.getBalanceTypService;
import static org.kuali.test.util.KualiTestAssertionUtils.assertGlobalErrorMapEmpty;

import org.kuali.Constants;
import org.kuali.PropertyConstants;
import org.kuali.core.util.SpringServiceLocator;
import org.kuali.module.chart.bo.AccountingPeriod;
import org.kuali.module.chart.bo.codes.BalanceTyp;
import org.kuali.module.financial.document.JournalVoucherDocument;
import org.kuali.test.KualiTestBase;
import org.kuali.test.WithTestSpringContext;
/**
 * Class for unit testing the functionality of <code>{@link TransactionalDocumentRuleUtil}</code>
 * 
 * 
 */
@WithTestSpringContext
public class TransactionalDocumentRuleUtilTest extends KualiTestBase {

    private static final String DOES_NOT_MATTER = "doesNotMatter";

    private static long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;


    private final String ANNUAL_BALANCE_PERIOD_CODE="AB";
    private final Integer CURRENT_FISCAL_YEAR= new Integer("2004");


    // /////////////////////////////////////////////////////////////////////////
    // Fixture Methods Start Here //
    // /////////////////////////////////////////////////////////////////////////
    /**
     * Accessor method to </code>errorPropertyName</code>
     * 
     * @return String
     */
    protected String getErrorPropertyName() {
        return Constants.GLOBAL_ERRORS;
    }

    /**
     * Fixture accessor method to get <code>{@link String}</code> serialization instance of an active balance type.
     * 
     * @return String
     */
    protected String getActiveBalanceType() {
        return Constants.BALANCE_TYPE_ACTUAL;
    }

    /**
     * Fixture accessor method to get <code>{@link String}</code> serialization instance of an inactive balance type.
     * 
     * @return String
     */
    protected String getInactiveBalanceType() {
        return Constants.BALANCE_TYPE_ACTUAL;
    }


    /**
     * Fixture accessor method for the Annual Balance <code>{@link AccountingPeriod}</code>
     * 
     * @return AccountingPeriod
     */
    public AccountingPeriod getAnnualBalanceAccountingPeriod() {
        return getAccountingPeriodService().getByPeriod(ANNUAL_BALANCE_PERIOD_CODE, CURRENT_FISCAL_YEAR);
    }

    /**
     * Fixture accessor method for a closed <code>{@link AccountingPeriod}</code> instance.
     * 
     * @return AccountingPeriod
     */
    protected AccountingPeriod getClosedAccountingPeriod() {
        return null;
    }

    /**
     * Fixture accessor method for getting a <code>{@link java.sql.Date}</code> instance that is in the past.
     * 
     * @return Timestamp
     */
    private java.sql.Date getSqlDateYesterday() {
        return new java.sql.Date(SpringServiceLocator.getDateTimeService().getCurrentDate().getTime() - ONE_DAY_MILLIS);
    }

    /**
     * Fixture accessor method for getting a <code>{@link java.sql.Date}</code> instance that is in the future.
     */
    private java.sql.Date getSqlDateTomorrow() {
        return new java.sql.Date(SpringServiceLocator.getDateTimeService().getCurrentDate().getTime() + ONE_DAY_MILLIS);
    }

    /**
     * @return today's java.sql.Date
     */
    private java.sql.Date getSqlDateToday() {
        return new java.sql.Date(SpringServiceLocator.getDateTimeService().getCurrentDate().getTime());
    }

    // /////////////////////////////////////////////////////////////////////////
    // Fixture Methods End Here //
    // /////////////////////////////////////////////////////////////////////////


    // /////////////////////////////////////////////////////////////////////////
    // Test Methods Start Here //
    // /////////////////////////////////////////////////////////////////////////
    /**
     * test the <code>isValidBalanceType()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidBalanceType
     */
    public void testIsValidBalanceType_Active() {
        testIsValidBalanceType(getActiveBalanceType(), true);
    }

    /**
     * test the <code>isValidBalanceType()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidBalanceType
     */
    public void testIsValidBalanceType_Inactive() {
        testIsValidBalanceType(getInactiveBalanceType(), true);
    }

    /**
     * test the <code>isValidBalanceType()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidBalanceType
     */
    public void testIsValidBalanceType_Null() {
        testIsValidBalanceType(null, false);
    }

    /**
     * test the <code>isValidBalanceType()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @param btStr
     * @param expected
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidBalanceType
     */
    protected void testIsValidBalanceType(String btStr, boolean expected) {
        BalanceTyp balanceType = null;

        if (btStr != null) {
            balanceType = getBalanceTypService().getBalanceTypByCode(btStr);
        }
        assertGlobalErrorMapEmpty();
        boolean result = TransactionalDocumentRuleUtil.isValidBalanceType(balanceType,"code");
        if (expected) {
            assertGlobalErrorMapEmpty();
        }
        assertEquals("result", expected, result);
    }

    /**
     * test the <code>isValidOpenAccountingPeriod()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidOpenAccountingPeriod
     */
    public void testIsValidOpenAccountingPeriod_Open() {
        testIsValidOpenAccountingPeriod(getAnnualBalanceAccountingPeriod(), true);
    }

    /**
     * test the <code>isValidOpenAccountingPeriod()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidOpenAccountingPeriod
     */
    public void pendingTestIsValidOpenAccountingPeriod_Closed() {
        testIsValidOpenAccountingPeriod(getClosedAccountingPeriod(), false);
    }

    /**
     * test the <code>isValidOpenAccountingPeriod()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidOpenAccountingPeriod
     */
    public void testIsValidOpenAccountingPeriod_Null() {
        testIsValidOpenAccountingPeriod(null, false);
    }

    /**
     * test the <code>isValidOpenAccountingPeriod()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @param period
     * @param expected
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidOpenAccountingPeriod
     */
    protected void testIsValidOpenAccountingPeriod(AccountingPeriod period, boolean expected) {
        assertGlobalErrorMapEmpty();
        boolean result = TransactionalDocumentRuleUtil.isValidOpenAccountingPeriod(period, JournalVoucherDocument.class, PropertyConstants.ACCOUNTING_PERIOD, DOES_NOT_MATTER);
        if (expected) {
            assertGlobalErrorMapEmpty();
        }
        assertEquals("result", expected, result);
    }

    /**
     * test the <code>isValidReversalDate()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidReversalDate
     */
    public void testIsValidReversalDate_Null() {
        testIsValidReversalDate(null, true);
    }

    /**
     * test the <code>isValidReversalDate()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidReversalDate
     */
    public void testIsValidReversalDate_Past() {
        testIsValidReversalDate(getSqlDateYesterday(), false);
    }

    /**
     * test the <code>isValidReversalDate()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidReversalDate
     */
    public void testIsValidReversalDate_Future() {
        testIsValidReversalDate(getSqlDateTomorrow(), true);
    }

    public void testIsValidReversalDate_Today() {
        testIsValidReversalDate(getSqlDateToday(), true);
    }

    /**
     * test the <code>isValidReversalDate()</code> method of <code>{@link TransactionalDocumentRuleUtil}</code>
     * 
     * @param reversalDate
     * @param expected
     * @see org.kuali.module.financial.rules.TransactionalDocumentRuleUtil#isValidReversalDate
     */
    protected void testIsValidReversalDate(java.sql.Date reversalDate, boolean expected) {
        assertGlobalErrorMapEmpty();
        boolean result = TransactionalDocumentRuleUtil.isValidReversalDate(reversalDate, getErrorPropertyName());
        if (expected) {
            assertGlobalErrorMapEmpty();
        }
        assertEquals("result", expected, result);
    }

    // /////////////////////////////////////////////////////////////////////////
    // Test Methods End Here //
    // /////////////////////////////////////////////////////////////////////////
}
