/*
This code handles the JSON request sent by the ActiveHoursActivity and LeaderboardActivity classes.  There are two different ways that this code handles
the request, depending on the RequestType parameter of the JSON query.  
1.
If the user has opened the scoring page (ActiveHoursActivity) the request came from ActiveHoursActivity.  The new points information is recorded in 
the sheet.  The total number of points that the user has in the sheet is returned in the JSON response.
2.
If the user has opened the leaderboard (LeaderboardActivty) the request came from LeaderboardActivity.  The names and scores of the top five players
are returned in the JSON response.
David Linn - dlinn@hmc.edu - 11/27/18 
Tim Player and Richie Harris - 4/7/2019
*/

function doGet(e){
  return handleResponse(e);
}

var SCRIPT_PROP = PropertiesService.getScriptProperties(); // new property service

//columns numbers
var SENSOR_ID_COL = 2;
var ACTIVE_MINS_COL = 3;
var QR_CODES_COL = 4;
var POINTS_COL = 6;
var NAMES_COL = 1;

function handleResponse(e) {
  // shortly after my original solution Google announced the LockService[1]
  // this prevents concurrent access overwritting data
  // [1] http://googleappsdeveloper.blogspot.co.uk/2011/10/concurrency-and-google-apps-script.html
  // we want a public lock, one that locks for all invocations
  var SHEET_NAME = e.parameter["Sheet"];
  var lock = LockService.getPublicLock();
  lock.waitLock(5000);  // wait 5 seconds before conceding defeat.
  
  try {
    // next set where we write the data - you could write to multiple/alternate destinations
    var id = SCRIPT_PROP.getProperty("key");
    //var doc = SpreadsheetApp.openById(id);
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = doc.getSheetByName(SHEET_NAME);
    
    // we'll assume header is in row 1 but you can override with header_row in GET/POST data
    var headRow = e.parameter.header_row || 1;
    var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    
    // This block handles the new scoring update request from the user's phone.  It returns the new point total that the user has in the sheet.
    // Requires SensorID
    if (e.parameter["RequestType"] == "DataPush") {
      var newActiveMinPoints = e.parameter["ActiveMinPoints"];
      var newQRCodePoints = e.parameter["QRCodePoints"];
      
      // get all of the possible sensor IDs
      var sensorIDArray = sheet.getRange(2,SENSOR_ID_COL,sheet.getLastRow(),1).getDisplayValues();
      
      // Find the row corresponding to the sensor ID of the user
      var rowToEdit = search(sensorIDArray,e.parameter["SensorID"])+2;
      if (rowToEdit < 2)
        throw "User/SensorID#" + e.parameter["SensorID"] + " not found in Google Sheet";
      
      // Update the values in the sheet
      var cellToEdit = sheet.getRange(rowToEdit,ACTIVE_MINS_COL,1,1);
      cellToEdit.setValue(newActiveMinPoints);
      
      cellToEdit = sheet.getRange(rowToEdit,QR_CODES_COL,1,1);
      cellToEdit.setValue(newQRCodePoints);
      
      // Get the new point total from the sheet
      var totalPoints = sheet.getRange(rowToEdit,POINTS_COL,1,1).getDisplayValue();
      
      // return json success results
      return ContentService
      .createTextOutput(JSON.stringify({"result":"success", "remotePoints":totalPoints}))
      .setMimeType(ContentService.MimeType.JSON);
    }
    //Request Leaderboard Information
    else if (e.parameter["RequestType"] == "Leaderboard") {
      var leaderboardObject = leaderboard(sheet);
      var leaderboardNames = leaderboardObject.names;
      var leaderboardScores = leaderboardObject.scores;
      return ContentService
      .createTextOutput(JSON.stringify({"result":"success", "leaderboardNames":leaderboardNames, "leaderboardScores":leaderboardScores}))
      .setMimeType(ContentService.MimeType.JSON);
    }
  } catch(e){
    // if error return this
    return ContentService
          .createTextOutput(JSON.stringify({"result":"error", "error": e}))
          .setMimeType(ContentService.MimeType.JSON);
  } finally { //release lock
    lock.releaseLock();
  }
}

/**
* searches an array for an item and returns the index at which that item is found in the array.  The first index is zero
*
* @param array the array to be searched
* @param item the item to be searched for
* @return the index at which item is found in array
*/
function search(array,item) {
    for (var j=0; j<array.length; j++) {
        if (array[j][0].match(item)) return j;
    }
    return -1;
}

/**
* finds the players with the top five scores and returns an object containing their names and scores
*
* @param sheet the sheet holding the user point information
* @return an object with a names array and a scores array
*/
function leaderboard(sheet) {
  var scoresArray = sheet.getRange(2,POINTS_COL,sheet.getLastRow(),1).getDisplayValues();
  var namesArray = sheet.getRange(2,NAMES_COL,sheet.getLastRow(),1).getDisplayValues();
  var returnObj = {names:[[""],[""],[""],[""],[""]],scores:[[-1], [-1], [-1], [-1], [-1]]};
  for (i = 0; i < scoresArray.length; i++) {
    //Bubble sort algorithm that compares every element of the NAMES_COL column 
    //to the leaderboard
    if (Number(scoresArray[i])>Number(returnObj.scores[4][0])) {
      var j;
      for (j = 4; j > 0; j--) {
        if (Number(scoresArray[i])<Number(returnObj.scores[j-1])) {
          returnObj.scores[j] = scoresArray[i];
          returnObj.names[j] = namesArray[i];
          break;
        }
        else {
          returnObj.scores[j] = returnObj.scores[j-1];
          returnObj.names[j] = returnObj.names[j-1];
        }
      }
      if (j==0) {
        returnObj.scores[j] = scoresArray[i];
        returnObj.names[j] = namesArray[i];
      }
    }
  }
  return returnObj;
}

/**
* deprecated setup function
*/
function setup() {
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    SCRIPT_PROP.setProperty("key", doc.getId());
}
