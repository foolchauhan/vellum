function doGet(e) {
  if (!e || !e.parameter) {
    return ContentService.createTextOutput("Web App is running. Connect to this URL from the Vellum Android app.")
      .setMimeType(ContentService.MimeType.TEXT);
  }
  
  var action = e.parameter.action;
  var email = e.parameter.email;
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  if (!spreadsheet) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: "No active spreadsheet found. Please open Apps Script from inside a Google Sheet (Extensions -> Apps Script)."}))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  setupSheets(spreadsheet);
  
  if (email) {
    saveUser(spreadsheet, email, e.parameter.displayName, e.parameter.photoUrl);
  }
  
  if (action === "sync") {
    return ContentService.createTextOutput(JSON.stringify(getSyncData(spreadsheet, email)))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "join") {
    var shareCode = e.parameter.shareCode;
    return ContentService.createTextOutput(JSON.stringify(joinAccount(spreadsheet, email, shareCode)))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  return ContentService.createTextOutput(JSON.stringify({status: "error", message: "Unknown action"}))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: "No post data found"}))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  var data = JSON.parse(e.postData.contents);
  var email = data.email;
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  if (!spreadsheet) {
    return ContentService.createTextOutput(JSON.stringify({status: "error", message: "No active spreadsheet found. Please open Apps Script from inside a Google Sheet (Extensions -> Apps Script)."}))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  setupSheets(spreadsheet);
  
  if (email) {
    saveUser(spreadsheet, email, data.displayName, data.photoUrl);
  }
  
  // 1. Process deletions
  if (data.deleted_transactions && data.deleted_transactions.length > 0) {
    deleteRowsByIds(spreadsheet.getSheetByName("transactions"), data.deleted_transactions, 0);
  }
  if (data.deleted_categories && data.deleted_categories.length > 0) {
    deleteRowsByIds(spreadsheet.getSheetByName("categories"), data.deleted_categories, 0);
  }
  if (data.deleted_accounts && data.deleted_accounts.length > 0) {
    deleteAccountsWithCascade(spreadsheet, email, data.deleted_accounts);
  }
  
  // 2. Save sync data
  saveSyncData(spreadsheet, email, data);
  
  // 3. Manage backup trigger based on preferences
  manageBackupTrigger();
  
  return ContentService.createTextOutput(JSON.stringify({status: "success", message: "Synced successfully"}))
    .setMimeType(ContentService.MimeType.JSON);
}

function setupSheets(ss) {
  if (!ss) {
    ss = SpreadsheetApp.getActiveSpreadsheet();
  }
  if (!ss) {
    throw new Error("No active spreadsheet found. Please make sure this script is bound to a Google Sheet (Extensions -> Apps Script).");
  }
  
  var sheetsConfig = {
    "transactions": ["id", "amount", "type", "categoryId", "categoryName", "accountId", "accountName", "note", "timestamp", "userEmail"],
    "categories": ["id", "name", "type", "icon", "isDefault", "chartColor", "userEmail"],
    "accounts": ["id", "name", "icon", "isDefault", "color", "shareCode", "ownerEmail", "userEmail"],
    "preferences": ["key", "value", "userEmail"],
    "shares": ["shareCode", "userEmail"],
    "users": ["email", "displayName", "photoUrl", "lastSeen"]
  };
  
  for (var name in sheetsConfig) {
    var sheet = ss.getSheetByName(name);
    if (!sheet) {
      sheet = ss.insertSheet(name);
    }
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(sheetsConfig[name]);
    }
  }
  
  manageBackupTrigger();
}

function getSyncData(ss, email) {
  var sharesSheet = ss.getSheetByName("shares");
  var sharesData = sharesSheet.getDataRange().getValues();
  var joinedCodes = [];
  for (var i = 1; i < sharesData.length; i++) {
    if (sharesData[i][1] === email) {
      joinedCodes.push(sharesData[i][0]);
    }
  }
  
  var accountsSheet = ss.getSheetByName("accounts");
  var accountsData = accountsSheet.getDataRange().getValues();
  var accountsList = [];
  var matchedAccountIds = {};
  // Track which accounts are truly shared (not owned by requesting user)
  var sharedAccountIds = {};
  
  for (var i = 1; i < accountsData.length; i++) {
    var row = accountsData[i];
    var accOwner = row[6];
    var accShareCode = row[5];
    var accUserEmail = row[7];
    if (accOwner === email || accUserEmail === email || joinedCodes.indexOf(accShareCode) !== -1) {
      accountsList.push({
        id: row[0],
        name: row[1],
        icon: row[2],
        isDefault: row[3] === true || row[3] === "true",
        color: row[4],
        shareCode: row[5],
        ownerEmail: row[6],
        userEmail: row[7]
      });
      matchedAccountIds[row[0]] = true;
      // Track shared accounts (joined via share code where user is not the owner)
      if (accOwner !== email && joinedCodes.indexOf(accShareCode) !== -1) {
        sharedAccountIds[row[0]] = true;
      }
    }
  }
  
  var txSheet = ss.getSheetByName("transactions");
  var txData = txSheet.getDataRange().getValues();
  var txList = [];
  // Collect category IDs referenced by transactions in shared accounts (for cross-user category propagation)
  var sharedTxCategoryIds = {};
  for (var i = 1; i < txData.length; i++) {
    var row = txData[i];
    var txAccId = row[5];
    var txOwner = row[9];
    if (txOwner === email || matchedAccountIds[txAccId]) {
      txList.push({
        id: row[0],
        amount: parseFloat(row[1]),
        type: row[2],
        categoryId: row[3],
        categoryName: row[4],
        accountId: row[5],
        accountName: row[6],
        note: row[7],
        timestamp: parseInt(row[8]),
        userEmail: row[9]
      });
      // If this transaction is in a shared account (not owned by user), track its category
      if (sharedAccountIds[txAccId] || (matchedAccountIds[txAccId] && txOwner !== email)) {
        sharedTxCategoryIds[row[3]] = true;
      }
    }
  }
  
  var catSheet = ss.getSheetByName("categories");
  var catData = catSheet.getDataRange().getValues();
  var catList = [];
  var catIdsSeen = {};
  // First pass: collect the user's own categories and global defaults
  for (var i = 1; i < catData.length; i++) {
    var row = catData[i];
    var catOwner = row[6];
    var catId = row[0];
    if (catOwner === email || row[4] === true || row[4] === "true") {
      catIdsSeen[catId] = true;
      catList.push({
        id: catId,
        name: row[1],
        type: row[2],
        icon: row[3],
        isDefault: row[4] === true || row[4] === "true",
        chartColor: row[5],
        userEmail: row[6]
      });
    }
  }
  
  // Shared Category Propagation (2nd pass):
  // Include any categories referenced by shared account transactions that the
  // requesting user doesn't already have. This ensures all shared account members
  // see consistent category names/icons even when the category belongs to another user.
  // Categories are propagated read-only — the original owner's email is preserved.
  if (Object.keys(sharedTxCategoryIds).length > 0) {
    for (var i = 1; i < catData.length; i++) {
      var row = catData[i];
      var catId = row[0];
      if (sharedTxCategoryIds[catId] && !catIdsSeen[catId]) {
        catIdsSeen[catId] = true;
        catList.push({
          id: catId,
          name: row[1],
          type: row[2],
          icon: row[3],
          isDefault: row[4] === true || row[4] === "true",
          chartColor: row[5],
          userEmail: row[6]  // Keep original owner so client knows it's a propagated category
        });
      }
    }
  }
  
  var prefSheet = ss.getSheetByName("preferences");
  var prefData = prefSheet.getDataRange().getValues();
  var prefList = [];
  for (var i = 1; i < prefData.length; i++) {
    var row = prefData[i];
    if (row[2] === email) {
      prefList.push({
        key: row[0],
        value: row[1],
        userEmail: row[2]
      });
    }
  }
  
  return {
    transactions: txList,
    categories: catList,
    accounts: accountsList,
    preferences: prefList
  };
}

function saveSyncData(ss, email, data) {
  if (data.transactions) upsertRows(ss.getSheetByName("transactions"), data.transactions, 0, "transactions", email);
  if (data.categories) upsertRows(ss.getSheetByName("categories"), data.categories, 0, "categories", email);
  if (data.accounts) upsertRows(ss.getSheetByName("accounts"), data.accounts, 0, "accounts", email);
  if (data.preferences) upsertRows(ss.getSheetByName("preferences"), data.preferences, 0, "preferences", email);
}

function upsertRows(sheet, items, idColIndex, sheetName, email) {
  if (items.length === 0) return;
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();
  var headers = values[0];
  
  var keyToRowMap = {};
  for (var i = 1; i < values.length; i++) {
    if (sheetName === "preferences") {
      var rowKey = values[i][0] + "_" + values[i][2]; // key + "_" + userEmail
      keyToRowMap[rowKey] = i + 1;
    } else {
      var id = values[i][idColIndex];
      if (id) keyToRowMap[id] = i + 1;
    }
  }
  
  items.forEach(function(item) {
    if (!item.userEmail && email) {
      item.userEmail = email;
    }
    // Protect shared account userEmail and ownerEmail values
    if (sheetName === "accounts" && item.shareCode && item.shareCode !== "") {
      item.userEmail = item.ownerEmail;
    }
    var rowData = convertItemToRow(item, headers);
    var uniqueKey = (sheetName === "preferences") ? (item.key + "_" + item.userEmail) : (item.id || item.key);
    var rowNum = keyToRowMap[uniqueKey];
    if (rowNum) {
      sheet.getRange(rowNum, 1, 1, rowData.length).setValues([rowData]);
    } else {
      sheet.appendRow(rowData);
    }
  });
}

function convertItemToRow(item, headers) {
  return headers.map(function(header) {
    var val = item[header];
    return val === undefined ? "" : val;
  });
}

function joinAccount(ss, email, shareCode) {
  var accountsSheet = ss.getSheetByName("accounts");
  var accountsData = accountsSheet.getDataRange().getValues();
  var matchedAccount = null;
  
  for (var i = 1; i < accountsData.length; i++) {
    if (accountsData[i][5] === shareCode) {
      matchedAccount = {
        id: accountsData[i][0],
        name: accountsData[i][1],
        icon: accountsData[i][2],
        isDefault: false,
        color: accountsData[i][4],
        ownerEmail: accountsData[i][6],
        shareCode: shareCode
      };
      break;
    }
  }
  
  if (!matchedAccount) {
    return {status: "error", message: "Account share code not found"};
  }
  
  var sharesSheet = ss.getSheetByName("shares");
  var sharesData = sharesSheet.getDataRange().getValues();
  var alreadyJoined = false;
  for (var i = 1; i < sharesData.length; i++) {
    if (sharesData[i][0] === shareCode && sharesData[i][1] === email) {
      alreadyJoined = true;
      break;
    }
  }
  if (!alreadyJoined) {
    sharesSheet.appendRow([shareCode, email]);
  }
  
  return {status: "success", account: matchedAccount};
}

function saveUser(ss, email, displayName, photoUrl) {
  var sheet = ss.getSheetByName("users");
  if (!sheet) return;
  
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();
  var headers = values[0];
  
  var emailColIdx = headers.indexOf("email");
  var displayNameColIdx = headers.indexOf("displayName");
  var photoUrlColIdx = headers.indexOf("photoUrl");
  var lastSeenColIdx = headers.indexOf("lastSeen");
  
  var rowNum = -1;
  for (var i = 1; i < values.length; i++) {
    if (values[i][emailColIdx] === email) {
      rowNum = i + 1;
      break;
    }
  }
  
  var timestamp = new Date().getTime();
  var rowData = [];
  headers.forEach(function(header) {
    if (header === "email") rowData.push(email);
    else if (header === "displayName") rowData.push(displayName || "");
    else if (header === "photoUrl") rowData.push(photoUrl || "");
    else if (header === "lastSeen") rowData.push(timestamp);
    else rowData.push("");
  });
  
  if (rowNum !== -1) {
    sheet.getRange(rowNum, 1, 1, rowData.length).setValues([rowData]);
  } else {
    sheet.appendRow(rowData);
  }
}

function deleteAccountsWithCascade(ss, email, accountIds) {
  var accountsSheet = ss.getSheetByName("accounts");
  var accountsData = accountsSheet.getDataRange().getValues();
  
  accountIds.forEach(function(accId) {
    var rowIndex = -1;
    var accShareCode = "";
    var accOwner = "";
    for (var i = 1; i < accountsData.length; i++) {
      if (accountsData[i][0] === accId) {
        rowIndex = i + 1;
        accShareCode = accountsData[i][5];
        accOwner = accountsData[i][6];
        break;
      }
    }
    
    if (rowIndex !== -1) {
      if (accOwner === email) {
        accountsSheet.deleteRow(rowIndex);
        if (accShareCode) {
          deleteSharesForCode(ss, accShareCode);
        }
        deleteTransactionsForAccount(ss, accId);
      } else {
        if (accShareCode) {
          deleteShareForUser(ss, accShareCode, email);
        }
      }
    }
  });
}

function deleteSharesForCode(ss, shareCode) {
  var sharesSheet = ss.getSheetByName("shares");
  var sharesData = sharesSheet.getDataRange().getValues();
  for (var i = sharesData.length - 1; i >= 1; i--) {
    if (sharesData[i][0] === shareCode) {
      sharesSheet.deleteRow(i + 1);
    }
  }
}

function deleteShareForUser(ss, shareCode, email) {
  var sharesSheet = ss.getSheetByName("shares");
  var sharesData = sharesSheet.getDataRange().getValues();
  for (var i = sharesData.length - 1; i >= 1; i--) {
    if (sharesData[i][0] === shareCode && sharesData[i][1] === email) {
      sharesSheet.deleteRow(i + 1);
    }
  }
}

function deleteTransactionsForAccount(ss, accountId) {
  var txSheet = ss.getSheetByName("transactions");
  var txData = txSheet.getDataRange().getValues();
  for (var i = txData.length - 1; i >= 1; i--) {
    if (txData[i][5] === accountId) {
      txSheet.deleteRow(i + 1);
    }
  }
}

function deleteRowsByIds(sheet, ids, idColIndex) {
  if (!ids || ids.length === 0) return;
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();
  for (var i = values.length - 1; i >= 1; i--) {
    var id = values[i][idColIndex];
    if (id && ids.indexOf(id) !== -1) {
      sheet.deleteRow(i + 1);
    }
  }
}

function manageBackupTrigger() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;
  
  var isEnabled = false;
  var prefSheet = ss.getSheetByName("preferences");
  if (prefSheet) {
    var data = prefSheet.getDataRange().getValues();
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] === "auto_backup" && data[i][1] === "On") {
        isEnabled = true;
        break;
      }
    }
  }
  
  var triggers = ScriptApp.getProjectTriggers();
  var trigger = null;
  for (var i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === "runDailyBackup") {
      trigger = triggers[i];
      break;
    }
  }
  
  if (isEnabled) {
    if (!trigger) {
      // Create time-driven trigger daily at 11:30 PM (23:30)
      ScriptApp.newTrigger("runDailyBackup")
        .timeBased()
        .everyDays(1)
        .atHour(23)
        .nearMinute(30)
        .create();
      Logger.log("Daily backup trigger created.");
    }
  } else {
    if (trigger) {
      ScriptApp.deleteTrigger(trigger);
      Logger.log("Daily backup trigger removed.");
    }
  }
}

function runDailyBackup() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;
  
  // Double-check active preferences before executing backup
  var isEnabled = false;
  var prefSheet = ss.getSheetByName("preferences");
  if (prefSheet) {
    var data = prefSheet.getDataRange().getValues();
    for (var i = 1; i < data.length; i++) {
      if (data[i][0] === "auto_backup" && data[i][1] === "On") {
        isEnabled = true;
        break;
      }
    }
  }
  
  if (!isEnabled) {
    Logger.log("Backup skipped: auto_backup preference is Off.");
    return;
  }
  
  var sheets = ["transactions", "categories", "accounts", "preferences", "shares", "users"];
  sheets.forEach(function(sheetName) {
    var originalSheet = ss.getSheetByName(sheetName);
    if (!originalSheet) return;
    var backupSheetName = "backup_" + sheetName;
    var backupSheet = ss.getSheetByName(backupSheetName);
    if (!backupSheet) {
      backupSheet = ss.insertSheet(backupSheetName);
    } else {
      backupSheet.clear();
    }
    
    var dataRange = originalSheet.getDataRange();
    if (originalSheet.getLastRow() > 0 && originalSheet.getLastColumn() > 0) {
      dataRange.copyTo(backupSheet.getRange(1, 1));
    }
  });
  Logger.log("Daily backup completed successfully.");
}

function resetSpreadsheet() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) return;
  
  var sheetsConfig = {
    "transactions": ["id", "amount", "type", "categoryId", "categoryName", "accountId", "accountName", "note", "timestamp", "userEmail"],
    "categories": ["id", "name", "type", "icon", "isDefault", "chartColor", "userEmail"],
    "accounts": ["id", "name", "icon", "isDefault", "color", "shareCode", "ownerEmail", "userEmail"],
    "preferences": ["key", "value", "userEmail"],
    "shares": ["shareCode", "userEmail"],
    "users": ["email", "displayName", "photoUrl", "lastSeen"]
  };
  
  // 1. Delete standard sheets first (re-creating them clean later)
  var sheets = ss.getSheets();
  sheets.forEach(function(sheet) {
    var name = sheet.getName();
    // Keep at least one sheet temporarily so we don't delete the last remaining sheet
    if (ss.getSheets().length > 1) {
      try {
        ss.deleteSheet(sheet);
      } catch (e) {
        Logger.log("Could not delete sheet: " + name);
      }
    }
  });
  
  // 2. Re-create all standard sheets with standard headers
  for (var name in sheetsConfig) {
    var sheet = ss.getSheetByName(name);
    if (!sheet) {
      sheet = ss.insertSheet(name);
    }
    sheet.clear();
    sheet.appendRow(sheetsConfig[name]);
  }
  
  // 3. Delete any remaining backup or non-conforming sheets
  var remainingSheets = ss.getSheets();
  remainingSheets.forEach(function(sheet) {
    var name = sheet.getName();
    if (name.indexOf("backup_") === 0 || !(name in sheetsConfig)) {
      try {
        ss.deleteSheet(sheet);
      } catch (e) {
        Logger.log("Could not delete sheet: " + name);
      }
    }
  });
  
  // 4. Delete all existing triggers
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    ScriptApp.deleteTrigger(triggers[i]);
  }
  
  Logger.log("Spreadsheet and triggers reset successfully.");
}
