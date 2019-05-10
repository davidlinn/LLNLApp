/* 
This code handles the JSON request sent by the PuzzleInputActivity class.  There are two different ways that this code handles
the request, depending on the RequestType parameter of the JSON query.  
1.
If the user is submitting an answer to a puzzle,the submitted answer is recorded in the sheet along with other information about the user's submission,
and checked against the corresponding entry for that puzzle in the Actual Answer column.  The result isreturned in the "correct?" field of the response.  
Addtionally, the records of submitted answers in the sheet are searched for another instance of when the user had submitted a correct answer.  
The result is returned in the "alreadyCompleted? field.
2.
If the request is to get the puzzle names for the spinner in the Tau UI, the code returns an array of the names of the puzzles from the 
Puzzle ID column.
Tim Player and Richie Harris - 2/26/2017
*/

function doGet(e){
  return handleResponse(e);
}

var SCRIPT_PROP = PropertiesService.getScriptProperties(); // new property service

//columns numbers
var SENSOR_ID_COL = 1;
var PUZZLEID_COL = 3;
var SUBMITTED_ANSWER_COL = 4;
var PUZZLEID_ANS_COL = 6;
var ACTUAL_ANSWER_COL = 7;
var NUM_SUBMITTED_ITEMS = 4;

function handleResponse(e) {
  // shortly after my original solution Google announced the LockService[1]
  // this prevents concurrent access overwritting data
  // [1] http://googleappsdeveloper.blogspot.co.uk/2011/10/concurrency-and-google-apps-script.html
  // we want a public lock, one that locks for all invocations
  var SHEET_NAME = e.parameter["Sheet"];
  
  var lock = LockService.getUserLock();
  lock.waitLock(30000);  // wait 30 seconds before conceding defeat.
  
  if (lock.hasLock()) try {
    // next set where we write the data - you could write to multiple/alternate destinations
    var id = SCRIPT_PROP.getProperty("key");
    var doc = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = doc.getSheetByName(SHEET_NAME);
    
    // If the RequestType is AnswerSubmission, the user is submitting an answer to a puzzle.  This block adds the answer submission 
    // to the spreadsheet and returns a JSON message saying whether the answer was correct, and if the user has already submitted a correct answer
    if (e.parameter["RequestType"] == "AnswerSubmission") {
      // we'll assume header is in row 1 but you can override with header_row in GET/POST data
      var headRow = e.parameter.header_row || 1;
      var headers = sheet.getRange(1, 1, 1, NUM_SUBMITTED_ITEMS).getValues()[0]; //grabs all of the headers of the Sheet
      var nextRow = sheet.getLastRow()+1; // get next row
      var row = []; 
      var returnstate;
      
      // loop through the header columns
      for (i in headers){
        if (headers[i] == "Timestamp"){ // special case if you include a 'Timestamp' column
          row.push(new Date());
        } else { // else use header name to get data
          row.push(e.parameter[headers[i]]); // goes through all of the headers, searching the JSON request 'e' for that header.
          //it pushes the result (either empty or the value) to 'row'. 
        }
      }
      
      //check answer
      var puzzleID = e.parameter["PuzzleID"];
      var sensorID = e.parameter["SensorID"];
      var ans = e.parameter["SubmittedAnswer"];
      var wasAnswerCorrect; 
      
      if(checkAnswer(sheet, puzzleID, ans)){
        wasAnswerCorrect = "Correct!";
      }else{
        wasAnswerCorrect = "I'm sorry, your answer was incorrect.";
      }
      
      // check if the user has already submitted a correct answer to this puzzle
      var wasAlreadyCompleted = checkIfAlreadyCompleted(sheet, puzzleID, sensorID);
      
      //Add the row to the spreadsheet
      // more efficient to set values as [][] array than individually
      sheet.getRange(nextRow, 1, 1, row.length).setValues([row]);
      
      // return json success results
      returnstate =  ContentService
            .createTextOutput(JSON.stringify({"result":"success", "row": nextRow, "correct?" : wasAnswerCorrect, "alreadyCompleted?" : wasAlreadyCompleted}))
            .setMimeType(ContentService.MimeType.JSON);
      
    // If the RequestType is GetPuzzleNames, Tau is requesting the names of the puzzles so the spinner in the UI can be populated
    }else if (e.parameter["RequestType"] == "GetPuzzleNames"){
      returnstate =  handleGetPuzzleNames(e, sheet);
    }
    
  } catch(e){
    // if error return this
    returnstate =  ContentService
          .createTextOutput(JSON.stringify({"result":"error", "error": e}))
          .setMimeType(ContentService.MimeType.JSON);
  
  } finally { //release lock
    return returnstate;
    lock.releaseLock();
  }
}

/**
* Searches the records of past answer submissions for an entry that has the correct answer to the puzzle submitted by the same sensor ID.
* 
* @param sheet the sheet holding the records of submitted answers and answer bank
* @param puzzleID the name of the puzzle that is being checked to have been completed
* @param sensorID the sensor ID of the user
* @return a boolean for whether a correct answer has already been submitted by this sensor ID for this puzzle
*/
function checkIfAlreadyCompleted(sheet, puzzleID, sensorID){
  //access all previous answer submissions
  //note: these are 2D arrays of Strings
  var allSensorIDs = sheet.getRange(2,SENSOR_ID_COL,sheet.getLastRow(),1).getDisplayValues();
  var allAnswers   = sheet.getRange(2,SUBMITTED_ANSWER_COL,sheet.getLastRow(),1).getDisplayValues();
  var allPuzzleIDs = sheet.getRange(2,PUZZLEID_COL,sheet.getLastRow(),1).getDisplayValues(); //submitted puzzle IDs
  
  //search for a previous answer submission for this puzzle 
  var i;
  //go through all previous submissions
  for (i = 0; i < allSensorIDs.length; i++) { 
    //if it was submitted by the same sensor ID, for the same puzzle...
    if(allSensorIDs[i][0] === sensorID && allPuzzleIDs[i][0] === puzzleID){ 
      //was the previously-submitted answer correct?
      if(checkAnswer(sheet, puzzleID, allAnswers[i][0]) === true){
        return true;
      }
    }
  }
  return false; 
}

/**
* Compares the submitted answer to the answer in the Answer bank of the Google sheet to see if the answer was correct or not
*
* @param sheet the sheet holding the records of submitted answers and answer bank
* @param id the Puzzle ID of the puzzle for which the answer is being submitted
* @param ans the answer submitted by the user
* @return true or false depending on whether the submitted answer matches the actual answer for that puzzle
*/
function checkAnswer(sheet, id, ans) {
  //load the answer bank: an array of all the puzzle ids and their corresponding answers
  var allPuzzleIDs = sheet.getRange(2,PUZZLEID_ANS_COL,sheet.getLastRow(),1).getDisplayValues();
  var allAnswers = sheet.getRange(2,ACTUAL_ANSWER_COL,sheet.getLastRow(),1).getDisplayValues();
  
  //find index of the submitted ID in allPuzzleIDs
  var index = search(allPuzzleIDs, id);
  if (index == -1){
    throw "PuzzleID" + id + " not found in Google Sheet";
  }
  
  var actualAnswer = allAnswers[index][0];
  
  return ans === actualAnswer;
}

/**
* Creates and returns an array containing all of the Puzzle IDs from the answer bank of the Google sheet
*
* @param e the JSON request
* @param sheet the sheet holding the records of submitted answers and answer bank
* @return a JSON object listing the names of all the puzzles in the answer bank.
*/
function handleGetPuzzleNames(e, sheet){  
  //load the puzzle IDs from the answer bank. A 2D string array of names.
  var allPuzzleIDs = sheet.getRange(2,PUZZLEID_ANS_COL,sheet.getLastRow(),1).getDisplayValues();
  
  //create a one-dimensional string array of names
  var puzzleIDArray = [];
  for (x in allPuzzleIDs) {
    puzzleIDArray.push(allPuzzleIDs[x][0]);
  }
  
  //remove the empty strings that result if sheet.getLastRow() is too big
  puzzleIDArray = removeEmptyStrings(puzzleIDArray);
  
  // return json results
  return ContentService
          .createTextOutput(JSON.stringify({"result":"success", "puzzleNames" : puzzleIDArray}))
          .setMimeType(ContentService.MimeType.JSON);
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
* searches an array for an item and returns the index at which that item is found in the array.  The first index is zero
*
* @param array the array to be searched
* @param item the item to be searched for
* @return the index at which item is found in array
*/
function search(array,item) {
    for (var j=0; j<array.length; j++) {
        if (array[j][0] === item) return j;
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

/**
* testing function for testCheckAnswer()
*/
function testCheckAnswer(){
  var doc = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = doc.getSheetByName("Sheet1");
  
  var ans = checkAnswer(sheet, "PUZZLEB", "yikes");
  
  return;
}
