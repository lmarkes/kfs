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

/*
function accountNameLookup( anyFieldOnAwardAccount ) {
    var elPrefix = findElPrefix( anyFieldOnAwardAccount.name );
    var chartOfAccountsCode = dwr.util.getValue( elPrefix + ".organizationOwnerChartOfAccountsCode" ).toUpperCase().trim();
    var accountNumber = dwr.util.getValue( elPrefix + ".organizationOwnerAccountNumber" ).toUpperCase().trim();
    var targetFieldName = elPrefix + ".organizationOwnerAccount.accountName";
    if (chartOfAccountsCode == "" || accountNumber == "") {
        clearRecipients( targetFieldName );
    } else {
        var dwrReply = makeDwrSingleReply( "organizationOwnerAccount", "accountName", targetFieldName);
        AccountService.getByPrimaryIdWithCaching( chartOfAccountsCode, accountNumber, dwrReply);
    }
}
*/

function onblur_chartCode( chartCodeField ) {
    var accountNumberFieldName = findAccountNumberFieldName(chartCodeField.name);
    var orgCodeFieldName = findOrgCodeFieldName(chartCodeField.name);
    var chartCode = getElementValue(chartCodeField.name);    
    var accountNumber = getElementValue(accountNumberFieldName);    

    // no need to check accounts_can_cross_charts since if that's false the onblur function won't be called
    //alert ("accountNumberFieldName = " + accountNumberFieldName + ", orgCodeFieldName = " + orgCodeFieldName + ",\n chartCode = " + chartCode + ", accountNumber = " + accountNumber);
	lookupOrgCode(chartCode, accountNumber, orgCodeFieldName);
}

function onblur_accountNumber_org( accountNumberField ) {
    var chartCodeFieldName = findChartCodeFieldName(accountNumberField.name);
    var orgCodeFieldName = findOrgCodeFieldName(accountNumberField.name);
    var accountNumber = getElementValue(accountNumberField.name);    
	//alert ("chartCodeFieldName = " + chartCodeFieldName + ", orgCodeFieldName = " + orgCodeFieldName);

	var dwrReply = {
		callback: function (param) {
			if ( typeof param == 'boolean' && param == true) {	
			    var chartCode = getElementValue(chartCodeFieldName);
			    lookupOrgCode(chartCode, accountNumber, orgCodeFieldName);
			}
			else {
				loadChartOrg(accountNumber, chartCodeFieldName, accountNumberField.name, orgCodeFieldName);
			}
		},	
		errorHandler:function( errorMessage ) { 
			window.status = errorMessage;
		}
	};
	AccountService.accountsCanCrossCharts(dwrReply);	
}

function loadChartOrg( accountNumber, chartCodeFieldName, accountNumberFieldName, orgCodeFieldName ) {
	if (accountNumber == "") {
		clearRecipients(chartCodeFieldName);    
		clearRecipients(orgCodeFieldName);
	}
	else {
		var dwrReply = {
			callback: function (data) {
				//alert ("chartCode = " + data.chartOfAccountsCode + ", accountNumber = " + accountNumber + ", orgCode = " + data.organizationCode);
				if ( data != null && typeof data == 'object' ) {   
					setRecipientValue(chartCodeFieldName, data.chartOfAccountsCode);
					setRecipientValue(orgCodeFieldName, data.organizationCode);
				}
				else {
					clearRecipients(chartCodeFieldName); 
					setRecipientValue(orgCodeFieldName, wrapError( "account not found" ), true);
				}
			},
			errorHandler:function( errorMessage ) {
				clearRecipients(chartCodeFieldName); 
	            setRecipientValue(orgCodeFieldName, wrapError( "account not found" ), true);
				window.status = errorMessage;
			}
		};
		AccountService.getUniqueAccountForAccountNumber(accountNumber, dwrReply);	    
	}
}

function lookupOrgCode( chartCode, accountNumber, orgCodeFieldName ) {
    if (chartCode == "" || accountNumber == "") {
        clearRecipients(orgCodeFieldName);
    } else {
        var dwrReply = makeDwrSingleReply("account", "organizationCode", orgCodeFieldName);
        AccountService.getByPrimaryIdWithCaching(chartCode, accountNumber, dwrReply);
    }
}

function findChartCodeFieldName( accountNumberFieldName ) {
	var index = accountNumberFieldName.indexOf("AccountNumber");    
	var chartCodeFieldName = accountNumberFieldName.substring(0, index) + "ChartOfAccountsCode";
	return chartCodeFieldName;
}    

function findAccountNumberFieldName( chartCodeFieldName ) {
	var index = chartCodeFieldName.indexOf("ChartOfAccountsCode");0
	var accountNumberFieldName = chartCodeFieldName.substring(0, index) + "AccountNumber";
	return accountNumberFieldName;
}    

function findOrgCodeFieldName( accountFieldName ) {
	var idxChart = accountFieldName.indexOf("ChartOfAccountsCode");
	var idxAcct = accountFieldName.indexOf("AccountNumber");
	var index = idxChart > idxAcct ? idxChart : idxAcct // one of the indices shall be > 0, the other < 0
	var orgCodeFieldName = accountFieldName.substring(0, index) + "Account.organizationCode";
	return orgCodeFieldName;
}    

function onblur_accountNumber_pay( accountNumberField ) {
	var accountNumberFieldName = accountNumberField.name;
	var coaCodeFieldName = findElPrefix(accountNumberFieldName) + ".chartOfAccountsCode";
    var accountNumber = getElementValue( accountNumberFieldName );	    
	//alert("coaCodeFieldName = " + coaCodeFieldName + ", accountNumberFieldName = " + accountNumberFieldName);

	var dwrReply = {
		callback: function (param) {
			if ( typeof param == "boolean" && param == false) {	
				loadChartCode(accountNumber, coaCodeFieldName);
			}
		},	
		errorHandler:function( errorMessage ) { 
			window.status = errorMessage;
		}
	};
	AccountService.accountsCanCrossCharts(dwrReply);	
}

function loadChartCode( accountNumber, coaCodeFieldName ) {
	if (accountNumber == "") {
		clearRecipients(coaCodeFieldName);    		
	}
	else {
		var dwrReply = {
				callback: function (data) {
				//alert("chartOfAccountsCode = " + data.chartOfAccountsCode + ", accountNumber = " + accountNumber);
				if ( data != null && typeof data == "object" ) {   
					//var coaValue = data.chartOfAccountsCode + " - " + data.chartOfAccounts.finChartOfAccountDescription;
					setRecipientValue( coaCodeFieldName, data.chartOfAccountsCode );
				}
				else {
					clearRecipients(coaCodeFieldName); 
				}
			},
			errorHandler:function( errorMessage ) { 
				clearRecipients(coaCodeFieldName); 
				window.status = errorMessage;
			}
		};
		AccountService.getUniqueAccountForAccountNumber( accountNumber, dwrReply );	    
	}
}

function makeDwrSingleReply( boName, propertyName, targetFieldName ) {
    var friendlyBoName = boName.replace(/([A-Z])/g, ' $1').toLowerCase();
    return {
        callback:function(data) {
            if (data != null && typeof data == 'object') {
                setRecipientValue( targetFieldName, data[propertyName] );
            } else {
                setRecipientValue( targetFieldName, wrapError( friendlyBoName + " not found" ), true );
            }
        },
        errorHandler:function(errorMessage) {
            setRecipientValue( targetFieldName, wrapError( friendlyBoName + " not found" ), true );
        }
    };
}

function onblur_postingYearAndPeriodCode(field, callbackFunction) {
	var postedDate = getElementValue(field.name).trim();
	
	if (postedDate != "") {
		var dwrReply = {
			callback :callbackFunction,
			errorHandler : function(errorMessage) {
				setRecipientValue(
						"document.newMaintainableObject.add.assetPaymentDetails.postingYear",
						wrapError("Fiscal Year not found based on Posted Date above"), true);
				setRecipientValue(
						"document.newMaintainableObject.add.assetPaymentDetails.postingPeriodCode",
						wrapError("Fiscal Period not found based on Posted Date above"), true);
			}
		};
       AccountingPeriodService.getByStringDate(postedDate, dwrReply);
	}
}

function postingYearAndPeriodCode_Callback(data) {
	setRecipientValue(
			"document.newMaintainableObject.add.assetPaymentDetails.postingYear",
			data.universityFiscalYear);
	setRecipientValue(
			"document.newMaintainableObject.add.assetPaymentDetails.postingPeriodCode",
			data.universityFiscalPeriodCode);
}
