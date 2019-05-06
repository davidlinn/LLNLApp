# Tau Puzzle Hunt Deployment System

Tau refers to an Android App and Google Scripts backend code created by Harvey Mudd College students for Lawrence Livermore National Laboratory. The Tau system allows organizers to set up a puzzle hunt to incentivize users to walk around an area while carrying Kromek D3S radiation sensors to collect training data. On the app,users can submit answers, view their score, and scan QR codes among other features. The Google Scripts backend records the metadata of QR code scans (chiefly, time and GPS location to pair with the radiation data), keeps track of user scores, and checks submitted answers.

Also included in this repository is a set of Python data visualization tools for analyzing data collected via the Tau system and DTECT.

## Installation

#### Tau App
1. In Android Studio, open the App Components/TauApp folder as a project. 
2. Run the project using the green triangle button. Select either an Android emulator or a physical Android phone as the destination. 

#### Google Sheets
1. Create new Google Sheets named "Answer Submission", "Ground Truth", and "UserDatabase". Name the sheet tab "MSTR".
2. Populate each sheet with the column headers shown in the Excel files located in the Server Components directory.
3. For each sheet, select Tools -> Script editor to create a new script. Copy the contents of Code.gs (located in the folder for each sheet) into its respective script editor.
4. Select View -> Show manifest file to cause `appsscript.json` to appear in the script editor. Copy the contents of the local file into that script editor to enable permissions.
5. Select File -> Manage Versions and create a numbered version of your script.
6. Select Publish -> Deploy as web app to activate the sheet-connected Google Script and recieve a URL to submit JSON requests to.

## Usage
After installing the app and creating the Google Sheets, the system is ready to use.

Note: you must create QR codes with URLs. You must create puzzles. We have already done this. We will tell you how. 

## Development
Where to find string resources


## Other documentation
JavaDoc, final report, docs we used to make this.

## Authors
David Linn, Joshua Morgan, Richie Harris, Scott Montague, Lydia Sylla,Tim Player

Questions? tplayer@hmc.edu and rkharris@hmc.edu
