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
  if (data.left_accounts && data.left_accounts.length > 0) {
    processLeftAccounts(spreadsheet, email, data.left_accounts);
  }
  if (data.deleted_accounts && data.deleted_accounts.length > 0) {
    processDeletedAccounts(spreadsheet, email, data.deleted_accounts);
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
    "transactions": ["id", "amount", "type", "categoryId", "categoryName", "accountId", "accountName", "note", "timestamp", "userEmail", "updatedAt", "isDeleted", "deletedAt"],
    "categories": ["id", "name", "type", "icon", "isDefault", "chartColor", "userEmail", "updatedAt", "isDeleted", "deletedAt"],
    "accounts": ["id", "name", "icon", "isDefault", "color", "shareCode", "ownerEmail", "userEmail", "updatedAt", "isDeleted", "deletedAt", "carryOver"],
    "preferences": ["key", "value", "userEmail"],
    "shares": ["shareCode", "userEmail"],
    "users": ["email", "displayName", "photoUrl", "lastSeen"],
    "sticky_notes": ["id", "content", "colorHex", "createdAt", "userEmail", "updatedAt", "isDeleted", "deletedAt"]
  };
  
  for (var name in sheetsConfig) {
    var sheet = ss.getSheetByName(name);
    if (!sheet) {
      sheet = ss.insertSheet(name);
    }
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(sheetsConfig[name]);
    } else {
      // Dynamic upgrade: append missing header columns
      var headers = sheet.getRange(1, 1, 1, Math.max(1, sheet.getLastColumn())).getValues()[0];
      var expectedHeaders = sheetsConfig[name];
      var missingHeaders = expectedHeaders.filter(function(h) { return headers.indexOf(h) === -1; });
      if (missingHeaders.length > 0) {
        var startCol = sheet.getLastColumn() + 1;
        sheet.getRange(1, startCol, 1, missingHeaders.length).setValues([missingHeaders]);
      }
    }
  }
  
  manageBackupTrigger();
}

function upgradeSheetsSchema() {
  var spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  setupSheets(spreadsheet);
  Logger.log("Sheets schema upgraded successfully without affecting existing data.");
}


function rowToObject(row, headers) {
  var obj = {};
  headers.forEach(function(header, idx) {
    var val = row[idx];
    if (val === undefined) {
      val = "";
    }
    // Parse types
    if (header === "amount" || header === "timestamp" || header === "updatedAt" || header === "deletedAt" || header === "lastSeen") {
      if (val === "" || val === null) {
        obj[header] = (header === "deletedAt") ? null : 0;
      } else {
        obj[header] = parseFloat(val);
      }
    } else if (header === "isDefault" || header === "isDeleted" || header === "carryOver") {
      obj[header] = (val === true || val === "true" || val === 1 || val === "1");
    } else {
      obj[header] = val;
    }
  });
  return obj;
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
  var accountsHeaders = accountsData[0];
  var accountsList = [];
  var matchedAccountIds = {};
  var sharedAccountIds = {};
  
  for (var i = 1; i < accountsData.length; i++) {
    var obj = rowToObject(accountsData[i], accountsHeaders);
    var accOwner = obj.ownerEmail;
    var accShareCode = obj.shareCode;
    var accUserEmail = obj.userEmail;
    if (accOwner === email || accUserEmail === email || joinedCodes.indexOf(accShareCode) !== -1) {
      accountsList.push(obj);
      matchedAccountIds[obj.id] = true;
      if (accOwner !== email && joinedCodes.indexOf(accShareCode) !== -1) {
        sharedAccountIds[obj.id] = true;
      }
    }
  }
  
  var txSheet = ss.getSheetByName("transactions");
  var txData = txSheet.getDataRange().getValues();
  var txHeaders = txData[0];
  var txList = [];
  var sharedTxCategoryIds = {};
  for (var i = 1; i < txData.length; i++) {
    var obj = rowToObject(txData[i], txHeaders);
    var txAccId = obj.accountId;
    var txOwner = obj.userEmail;
    if (txOwner === email || matchedAccountIds[txAccId]) {
      txList.push(obj);
      if (sharedAccountIds[txAccId] || (matchedAccountIds[txAccId] && txOwner !== email)) {
        sharedTxCategoryIds[obj.categoryId] = true;
      }
    }
  }
  
  var catSheet = ss.getSheetByName("categories");
  var catData = catSheet.getDataRange().getValues();
  var catHeaders = catData[0];
  var catList = [];
  var catIdsSeen = {};
  for (var i = 1; i < catData.length; i++) {
    var obj = rowToObject(catData[i], catHeaders);
    var catOwner = obj.userEmail;
    var catId = obj.id;
    if (catOwner === email || obj.isDefault) {
      catIdsSeen[catId] = true;
      catList.push(obj);
    }
  }
  
  if (Object.keys(sharedTxCategoryIds).length > 0) {
    for (var i = 1; i < catData.length; i++) {
      var obj = rowToObject(catData[i], catHeaders);
      var catId = obj.id;
      if (sharedTxCategoryIds[catId] && !catIdsSeen[catId]) {
        catIdsSeen[catId] = true;
        catList.push(obj);
      }
    }
  }
  
  var notesSheet = ss.getSheetByName("sticky_notes");
  var notesList = [];
  if (notesSheet) {
    var notesData = notesSheet.getDataRange().getValues();
    var notesHeaders = notesData[0];
    for (var i = 1; i < notesData.length; i++) {
      var obj = rowToObject(notesData[i], notesHeaders);
      if (obj.userEmail === email) {
        notesList.push(obj);
      }
    }
  }
  
  return {
    transactions: txList,
    categories: catList,
    accounts: accountsList,
    preferences: prefList,
    sticky_notes: notesList
  };
}

function saveSyncData(ss, email, data) {
  if (data.transactions) upsertRows(ss.getSheetByName("transactions"), data.transactions, 0, "transactions", email);
  if (data.categories) upsertRows(ss.getSheetByName("categories"), data.categories, 0, "categories", email);
  if (data.accounts) upsertRows(ss.getSheetByName("accounts"), data.accounts, 0, "accounts", email);
  if (data.preferences) upsertRows(ss.getSheetByName("preferences"), data.preferences, 0, "preferences", email);
  if (data.sticky_notes) {
    var notesSheet = ss.getSheetByName("sticky_notes");
    if (notesSheet) {
      upsertRows(notesSheet, data.sticky_notes, 0, "sticky_notes", email);
    }
  }
}

function upsertRows(sheet, items, idColIndex, sheetName, email) {
  if (items.length === 0) return;
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();
  var headers = values[0];
  
  var keyToRowMap = {};
  for (var i = 1; i < values.length; i++) {
    if (sheetName === "preferences") {
      var rowKey = values[i][0] + "_" + values[i][2];
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

function processLeftAccounts(ss, email, accountIds) {
  var accountsSheet = ss.getSheetByName("accounts");
  var accountsData = accountsSheet.getDataRange().getValues();
  
  accountIds.forEach(function(accId) {
    var accShareCode = "";
    for (var i = 1; i < accountsData.length; i++) {
      if (accountsData[i][0] === accId) {
        accShareCode = accountsData[i][5];
        break;
      }
    }
    if (accShareCode) {
      deleteShareForUser(ss, accShareCode, email);
    }
  });
}

function processDeletedAccounts(ss, email, accountIds) {
  var sharesSheet = ss.getSheetByName("shares");
  var sharesData = sharesSheet.getDataRange().getValues();
  
  var accountsSheet = ss.getSheetByName("accounts");
  var accountsData = accountsSheet.getDataRange().getValues();
  
  accountIds.forEach(function(accId) {
    var accRowIdx = -1;
    var accShareCode = "";
    var accOwner = "";
    for (var i = 1; i < accountsData.length; i++) {
      if (accountsData[i][0] === accId) {
        accRowIdx = i + 1;
        accShareCode = accountsData[i][5];
        accOwner = accountsData[i][6];
        break;
      }
    }
    
    if (accRowIdx !== -1) {
      if (accOwner === email) {
        // Owner delete: Check if other members are active
        if (accShareCode) {
          var otherMembers = [];
          for (var j = 1; j < sharesData.length; j++) {
            if (sharesData[j][0] === accShareCode && sharesData[j][1] !== email) {
              otherMembers.push(sharesData[j][1]);
            }
          }
          if (otherMembers.length > 0) {
            throw new Error("Cannot delete account: other members (" + otherMembers.join(", ") + ") are still active.");
          }
        }
        cascadeTombstoneTransactions(ss, accId);
      }
    }
  });
}

function cascadeTombstoneTransactions(ss, accountId) {
  var txSheet = ss.getSheetByName("transactions");
  var txData = txSheet.getDataRange().getValues();
  var txHeaders = txData[0];
  var accNameColIdx = txHeaders.indexOf("accountName");
  var accIdColIdx = txHeaders.indexOf("accountId");
  
  if (accIdColIdx === -1 || accNameColIdx === -1) return;
  
  var now = new Date().getTime();
  var updatedAtColIdx = txHeaders.indexOf("updatedAt");
  
  for (var i = 1; i < txData.length; i++) {
    if (txData[i][accIdColIdx] === accountId) {
      txSheet.getRange(i + 1, accNameColIdx + 1).setValue("(Deleted Account)");
      if (updatedAtColIdx !== -1) {
        txSheet.getRange(i + 1, updatedAtColIdx + 1).setValue(now);
      }
    }
  }
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
  
  var sheets = ["transactions", "categories", "accounts", "preferences", "shares", "users", "sticky_notes"];
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
    "transactions": ["id", "amount", "type", "categoryId", "categoryName", "accountId", "accountName", "note", "timestamp", "userEmail", "updatedAt", "isDeleted", "deletedAt"],
    "categories": ["id", "name", "type", "icon", "isDefault", "chartColor", "userEmail", "updatedAt", "isDeleted", "deletedAt"],
    "accounts": ["id", "name", "icon", "isDefault", "color", "shareCode", "ownerEmail", "userEmail", "updatedAt", "isDeleted", "deletedAt", "carryOver"],
    "preferences": ["key", "value", "userEmail"],
    "shares": ["shareCode", "userEmail"],
    "users": ["email", "displayName", "photoUrl", "lastSeen"],
    "sticky_notes": ["id", "content", "colorHex", "createdAt", "userEmail", "updatedAt", "isDeleted", "deletedAt"]
  };
  
  var sheets = ss.getSheets();
  sheets.forEach(function(sheet) {
    var name = sheet.getName();
    if (ss.getSheets().length > 1) {
      try {
        ss.deleteSheet(sheet);
      } catch (e) {
        Logger.log("Could not delete sheet: " + name);
      }
    }
  });
  
  for (var name in sheetsConfig) {
    var sheet = ss.getSheetByName(name);
    if (!sheet) {
      sheet = ss.insertSheet(name);
    }
    sheet.clear();
    sheet.appendRow(sheetsConfig[name]);
  }
  
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
  
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    ScriptApp.deleteTrigger(triggers[i]);
  }
  
  Logger.log("Spreadsheet and triggers reset successfully.");
}
