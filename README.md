# Tau Puzzle Hunt Deployment System

Tau refers to an Android App and Google Scripts backend code created by Harvey Mudd College students for Lawrence Livermore National Laboratory. The Tau system allows organizers to set up a puzzle hunt to incentivize users to walk around an area while carrying Kromek D3S radiation sensors to collect training data. On the app, users can submit answers, view their score, and scan QR codes among other features. The Google Scripts backend records the metadata of QR code scans (chiefly, time, QR stop, and GPS location to pair with the radiation data), keeps track of user scores, and checks submitted answers.

Also included in this repository is a set of Python data visualization tools for analyzing data collected via the Tau system and DTECT. Additionally, puzzle hunt content (such as puzzles and QR scan sheets) is provided.

## Installation

#### Tau App
1. In Android Studio, open the `App Components/TauApp` folder as a project. 
2. Run the project using the green triangle button. Select either an Android emulator or a physical Android phone as the destination. 

#### Puzzle Hunt Content
1. Print off the Puzzle QR Scans PDF and Bonus QR Scans PDF documents. `.doc` formats are provided for future development, but the PDFs are better formatted. 
   - Currently, the Puzzle QR Scan codes labeled "scan this QR code to access the puzzle" contain links to copies of the puzzles hosted on Harvey Mudd's Google Drive account. If you wish to change the puzzles or host them at a different location, you may use the included `makeqr.py` script to generate new QR codes or online QR code generators.

#### Google Sheets
1. Create new Google Sheets named "Answer Submission", "Ground Truth", and "UserDatabase". Name the sheet tab "MSTR".
2. Populate each sheet with the column headers shown in the Excel files located in the Server Components directory.
3. For each sheet, select `Tools -> Script editor` to create a new script. Copy the contents of `Code.gs` (located in the folder for each sheet) into its respective script editor.
4. Select `View -> Show` manifest file to cause `appsscript.json` to appear in the script editor. Copy the contents of the local file into that script editor to enable permissions.
5. Select `File -> Manage Versions` and create a numbered version of your script.
6. Select `Publish -> Deploy as web app`.  Select the new project version and allow access to anyone, even anonymous to activate the sheet-connected Google Script and recieve a URL to submit JSON requests to.

## Usage
After installing the app on phones, printing off the QR scans, and creating the Google Sheets, the system is ready to deploy. To deploy, you must place the QR code scans and radioactive sources, distribute the phones and sensors, and brief participants.

- Placing QR code scans and sources:
  - On Google Maps, locate the spots you wish to place the puzzle QR code scans, bonus QR code scans, and radioactive sources. Record the latitude and longitude of each in the Ground Truth database.
  - Physically place the QR scans and sources at the designated locations. Verify that the GPS location agrees with your specification from the spreadsheet.
- Distibruting sensors:
  - When phones are distributed to participants, make sure that the UserDatabase sheet is populated with each phone's Sensor ID (the last four digits of the Kromek D3S serial number) and the user's name. This will allow a user's score on the phone to be synchronized with the remote database.
  - Before releasing the phones, make sure the sensor is showing up on DTECT. You can use the Sigma App's "Sigma Service" page to diagnose connectivity.  To reset a user's point total to zero, go to `Settings -> Apps -> Tau -> Storage -> Clear Data`.
- Briefing participants:
  - Walk participants through the information in the "Participant Briefing" document. Ensure that they open the Maps activity to start location services. Direct them to scan the first Puzzle QR code scan and view the first puzzle. From there, they can complete the puzzle independently.

## Development
Future development teams may want to make small changes to the app, or they may want to completely redesign it. In either case, we have provided documentation with the goal of providing sufficient information for developers to reproduce or modify the architecture of the Tau system.

There are four key sources of documentation. A developer's workflow will involve first reading the high-level documentation, then delving into the specifics of implementation by directly reading the code. Here are resources representative of every level of documentation.

1. High-level documentation
   - This README document
   - The 2019 Harvey Mudd Clinic Team's Final Report
2. Architectural documentation
   - The "Tau System Architecture" document in this directory, which provides information about the app's Activities and their interactions with the Google Sheets database.
   - The JavaDoc. You must generate the JavaDoc in Android Studio by selecting `Tools -> Generate Javadoc`. This resource is the primary starting point for understanding the interaction of app components.
3. Implementation specifics
   - The code provided in `com.hmc.tau.alpha`, as well as the included comments, has been created with readability and maintainability in mind. Incoming developers should be able to parse our intention and execution from the source. Please note that, due to Android conventions, it can be hard to parse the file structure of the provided code. We recommend viewing the app code in Android Studio; it is more natural to navigate resources there instead of manually viewing `App Components/TauApp/app/src/main/java/com/hmc/tau/alpha/` on Github.
   - The Google Scripts code in the Server Components directory provides the specifics of server-side data handling.

## External Documentation

Developers new to Android or to Google Scripts will find the following sources of external documentation indispensable. In particular, we recommend reading the entire App Basics section of the Android documentation, which provides a concise introduction to the Android Activity lifecycle, descriptions of Sensor usage, and best practices for separating string and visual resources from behavioral code.

- [Android Docs](https://developer.android.com/guide)
- [Google Scripts Docs](https://developers.google.com/apps-script/overview)

## Authors
David Linn, Joshua Morgan, Richie Harris, Scott Montague, Lydia Sylla, Tim Player

Questions? tplayer@hmc.edu and rkharris@hmc.edu
