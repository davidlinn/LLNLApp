/*
This code handles the JSON request sent by the QRActivity and MapsActivity classes.  There are two different ways that this code handles
the request, depending on the RequestType parameter of the JSON query.  
1.
If the user has scanned a QR code, the request came from QRActivity.  The scan information is recorded in the sheet.  The prerequisite for 
the scan, the character (letter) of the scan type, and the stop ID of the scan are returned in the JSON response.  The prerequistie is name of 
the puzzle for which the user must have submitted a correct answer for to qualify for recieving points for that bonus scan.
2.
If the request is to get the waypoints (GPS latitudes and longitudes of the puzzles and bonus QR codes), the request came from MapsActivity.  
These values are obtained from the appropriate columns in the Google sheet along with how many of these correspond to puzzles, and these are 
returned in the JSON response.
original from: http://mashe.hawksey.info/2014/07/google-sheets-as-a-database-insert-with-apps-script-using-postget-methods-with-ajax-example/
original gist: https://gist.github.com/willpatera/ee41ae374d3c9839c2d6 
Tim Player and Richie Harris - 4/7/2019
*/ 

function doGet(e){
  return handleResponse(e);
}

var SCRIPT_PROP = PropertiesService.getScriptProperties(); // new property service

//columns numbers
var PUZZLE_STOPS_COL = 11;
var LATITUDE_COL = 12;
var LONGITUDE_COL = 13;
var PREREQ_COL = 14;

function handleResponse(e) {
  // shortly after my original solution Google announced the LockService[1]
  // this prevents concurrent access overwritting data
  // [1] http://googleappsdeveloper.blogspot.co.uk/2011/10/concurrency-and-google-apps-script.html
  // we want a public lock, one that locks for all invocations
  var SHEET_NAME = e.parameter["Sheet"];
  var lock = LockService.getScriptLock();
  lock.waitLock(5000);  // wait 5 seconds before conceding defeat.
  
  try {
    // next set where we write the data - you could write to multiple/alternate destinations
    var doc = SpreadsheetApp.openById(SCRIPT_PROP.getProperty("key"));
    var sheet = doc.getSheetByName(SHEET_NAME);
    
    // This block handles the situation where the user scans a QR code.  It records the scan in the spreadsheet and returns the scan type, full stop
    // ID, and corresponding prerequisite for the scan
    if (e.parameter["RequestType"] == "QRScan") {
      // we'll assume header is in row 1 but you can override with header_row in GET/POST data
      var headRow = e.parameter.header_row || 1;
      var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
      var nextRow = sheet.getLastRow()+1; // get next row
      var row = []; 
      // loop through the header columns
      for (i in headers){
        if (headers[i] == "Timestamp"){ // special case if you include a 'Timestamp' column
          row.push(new Date());
        } else { // else use header name to get data
          row.push(e.parameter[headers[i]]);
        }
      }
      // more efficient to set values as [][] array than individually
      sheet.getRange(nextRow, 1, 1, row.length).setValues([row]);
    
      // obtain the ScanType and full 6 character StopID
      var scanType = e.parameter["ScanType"];
      var stopID = e.parameter["Sheet"] + e.parameter["Stop"];
      
      // find whether there was a prereq for that puzzle
      var prereq = getPrereq(sheet, stopID);
    
      // return json success results
      return ContentService
      .createTextOutput(JSON.stringify({"result":"success", "row": nextRow, "Prerequisite" : prereq, "ScanType": scanType, "StopID": stopID}))
          .setMimeType(ContentService.MimeType.JSON);
    }
    
    // This block handles the situation where Tau requests the locations of the puzzles and bonus QR codes.  It returns these values in arrays
    // along with how many of them correspond to puzzle locations
    else if (e.parameter["RequestType"] == "GetWaypoints"){
      var numPuzzleStops = getNumPuzzleStops(sheet);
      var latitudes = getLatLong(sheet, LATITUDE_COL);
      var longitudes = getLatLong(sheet, LONGITUDE_COL);
      // return json success results
      return ContentService
      .createTextOutput(JSON.stringify({"result":"success", "numPuzzleStops": numPuzzleStops, "latitudes": latitudes, "longitudes": longitudes}))
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
* Gets the number of puzzle stops.  This is used so that Tau can label the location markers corresponding to whether the location is a puzzle stop or
* a bonus QR code stop.
*
* @param sheet the sheet holding the records of scans and the locations of the stops
* @return the number of latitude and longitude pairs which represent the location of puzzles as opposed to the location of bonus QR codes
*/
function getNumPuzzleStops(sheet) {
  var puzzleStopsCol = sheet.getRange(2, PUZZLE_STOPS_COL,sheet.getLastRow(),1).getDisplayValues();
  var index = search(puzzleStopsCol, "Bonus Stops:");
  var numPuzzles = index;
  return numPuzzles;
}

/**
* Gets either all of the latitudes or all of the longitudes of all of the stops.
*
* @param sheet the sheet holding the records of scans and the locations of the stops
* @param col the column containing either all of the latitudes or all of the longitudes
* @return an array containing either all of the latitudes of the stops or all of the longitudes of the stops.
*/
function getLatLong(sheet, col) {
  var puzzleStopsCol = sheet.getRange(2, PUZZLE_STOPS_COL,sheet.getLastRow(),1).getDisplayValues();
  var lastRow = search(puzzleStopsCol, "Sources:")+1;
  var allLatLong = sheet.getRange(2,col,lastRow,1).getDisplayValues();
  var latLong = [];
  for (x in allLatLong) {
    latLong.push(allLatLong[x][0]);
  }
  //remove the empty strings that result if sheet.getLastRow() is too big
  latLong = removeUndefinedStrings(latLong);
  latLong = removeEmptyStrings(latLong);
  return latLong;
}

/**
* Gets the prerequisite for the corresponding puzzle stop.  If there is no prerequisite, the empty string is returned.
*
* @param sheet the sheet holding the records of scans and the locations of the stops
* @param id the full 6 character id of puzzle stop which we are getting the prerequisite for
* @return The string name of the puzzle ID that the user is required to have submitted a correct answer to in order to recieve points for the bonus scan
*/
function getPrereq(sheet, id) {
  //load arrays of all the puzzle stops and their corresponding prerequisites
  var allPuzzleStops = sheet.getRange(2,PUZZLE_STOPS_COL,sheet.getLastRow(),1).getDisplayValues();
  var allPrereqs = sheet.getRange(2,PREREQ_COL,sheet.getLastRow(),1).getDisplayValues();
  
  //find index of the submitted ID in allPuzzleIDs
  var index = search(allPuzzleStops, id);
  if (index == -1){
    throw "Puzzle Stop" + id + " not found in Google Sheet";
  }
  
  // return the prerequiste corresponding to that index
  return allPrereqs[index][0];
  
}

/**
* Removes the empty strings in an array
*
* @param arr the array to be modified
* @return the array arr without any empty strings
*/
function removeEmptyStrings(arr){
  //given a 1D array of strings arr, removes all the empty strings.
  var returnarr = [];
  var x;
  for (x in arr) {
    if(arr[x]){
      returnarr.push(arr[x]);
    }
  }
  
  return returnarr;
}

/**
* Removes the undefined strings in an array
*
* @param arr the array to be modified
* @return the array arr without any undefined strings
*/
function removeUndefinedStrings(arr){
  //given a 1D array of strings arr, removes all the undefined strings.
  var returnarr = [];
  var x;
  for (x in arr) {
    if(arr[x] != "undefined"){
      returnarr.push(arr[x]);
    }
  }
  
  return returnarr;
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
        if (array[j][0] === item) return j; //2D array!
    }
    return -1;
}

/**
* deprecated setup function
*/
function setup() {
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    SCRIPT_PROP.setProperty("key", doc.getId());
}
