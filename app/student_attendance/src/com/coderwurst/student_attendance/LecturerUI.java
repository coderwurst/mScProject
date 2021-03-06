package com.coderwurst.student_attendance;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ***********************
 * Created by IntelliJ IDEA
 * User: andrew
 * Date: 14/07/2014
 * Time: 08:36
 * Version: V7.0
 * SPRINT 6 - USER INTERFACE FOR STAFF MEMBERS TO OFFER AUTO, MANUAL SIGN-IN FUNCTIONS & QR-CODE RETRIEVAL
 * ************************
 */
public class LecturerUI extends Activity implements View.OnClickListener

{
    // buttons to provide lecturer functions
    private Button btnManSignin;
    private Button btnAutoSignin;
    private Button btnGetQR;
    private Button btnRecall;

    private TextView serverStatus;      // used to inform user if server connection has been established
    private TextView moduleNumber;      // used to show currently selected module code
    private int scanID = 0;             // int to store type of scan


    // server IP address retrieved from MainScreenActivity protected variable
    private static String serverAddress = MainScreenActivity.serverIP;

    protected static boolean serverAvailable;           // determine if internet connection is available
    private String serverResponse = "";

    private Context context = this;                     // context used in checking connectivity

    protected static String recModuleID = null;         // string to hold selected module ID
    protected static String recClassType = null;        // string to hold the class type

    // tags for log statements
    private static final String TAG = "lecturer ui";

    /**
     * This activity remains open in the background whilst the user accesses other UIs. One of which is
     * the find QR code function. If a user selects a QR-Code from the list, it is stores in the protected
     * variable 'recModuleID'. Upon returning to this activity, the onResume method is called, and updates
     * the text field to show this information (if necessary). It is also used to recheck server connectivity
     * each time the user returns to the screen, and updates te features as necessary
     */

    @Override
    protected void onResume()
    {
        super.onResume();

        if (recModuleID != null)
        {
            // show scan QR button
            moduleNumber.setVisibility(View.VISIBLE);
            moduleNumber.setText("module num: " + recModuleID + "\nclass type: " + recClassType);

        } // if the lecturer has stored module info, it will be presented on screen

        new TestConnection().execute();         // test connectivity

    } // onResume


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lecturer_ui);           // opens up corresponding XML file

        /**
         * check to see if connection to University Server is available, will be called each time
         * the user reloads the UI; eg. when changing orientation. The purpose of which is to ensure
         * that server connection is available over a longer period of time (initial check in
         * main screen activity may only be called once in a day)
         **/
        new TestConnection().execute();

        // buttons for lecturer functions
        btnManSignin = (Button) findViewById(R.id.lec_man_signin);          // to enter student & class details manually
        btnAutoSignin = (Button) findViewById(R.id.lec_auto_signin);        // to scan student ID & class QR-Code
        btnGetQR = (Button) findViewById(R.id.getQRCode);                   // to retrieve a particular QR-Code
        btnRecall = (Button) findViewById(R.id.lec_recall);                 // to view files previously saved on device

        // TextViews for hold format and content info for testing purposes
        serverStatus = (TextView) findViewById(R.id.server_info);           // updated to show server connectivity
        moduleNumber = (TextView) findViewById(R.id.module_info);

        // set onClick listeners for all buttons
        btnManSignin.setOnClickListener(this);
        btnAutoSignin.setOnClickListener(this);
        btnGetQR.setOnClickListener(this);
        btnRecall.setOnClickListener(this);

        /**
         * in the event that the lecturer has not previously selected a module code and class type from the find QR
         * screen, they will be prompted to scan a module code upon opening this activity. Otherwise the previously
         * selected class details will be presented to them on screen
         **/

        if (recModuleID != null)
        {
            // show scan QR button
            moduleNumber.setVisibility(View.VISIBLE);
            moduleNumber.setText("module: " + recModuleID);

        } // if the lecturer has stored module info, it will be presented on screen

    } // onCreate

    /**
     * filesFound method to search the local storage on the device for previously stored check-in files, and update the
     * LecturerUI accordingly with the option to send the stored files to the database (if found). During production
     * a file 'scanfile.txt' was also found in this storage location. After a fresh install for usability testing, this
     * file was no longer present. Due to the lack of time for further investigation as to where the scanfile originates,
     * it was decided to eliminate the possible problems caused by its presence, by excluding it for consideration as a
     * saved check-in file (see line 134 below)
     */

    private boolean filesFound()
    {

        File file = this.getFilesDir();                 // returns storage location

        Log.d("storage location", file.toString());     // allows programmer to follow app progress in console

        // arraylist and string to store the information as it is retrieved from the stored text file
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(file.list()));
        String filename = null;

        Log.d(TAG, names.size() + " stored files: " + names);

        // first if statement to determine if there are any files stored on the device
        if (names.size() >= 1)
        {
            filename = names.get(names.size() - 1);
        } // if

        if (filename != "scanfile.txt" && filename != null)     // may be a file scanfile.txt also stored on device
        {
            filename = names.get(names.size() - 1);
            Log.d(TAG, "file to be read: " + filename);

            return true;            // data found, UI will be updated accordingly

        } else                      // if else to load stored files into studentBatch LinkedList

            return false;           // no data found

    } // filesFound


    /**
     * The behaviour of the onClick listeners will vary depending on connection with the server, for this reason the
     * code for these listeners was implemented in a slightly different way to previous classes
     */

    @Override
    public void onClick (View view)
    {
        if (serverAvailable)                    // first block in if-else statement; server is available
        {
            if (view.getId() == R.id.lec_man_signin)            // user wishes to manually check-in a student
            {
                // logcat tag to view app progress
                Log.d(TAG, "manual check in");

                // opens up manual sign in activity with text input fields
                Intent openManSignin = new Intent(getApplicationContext(), AddStudentMan.class);
                startActivity(openManSignin);

            } else if (view.getId() == R.id.lec_auto_signin)    // user wishes to recursively (auto) check-in
            {

                // logcat tag to view app progress
                Log.d(TAG, "auto check in");

                // opens up recursive sign in activity
                Intent openAutoSignin = new Intent(getApplicationContext(), RecursiveSignIn.class);
                startActivity(openAutoSignin);


            } else if (view.getId() == R.id.getQRCode)          // user wishes to view a QR-Code for his/her class
            {

                // logcat tag to view app progress
                Log.d(TAG, "retrieve QR");

                // code to retrieve QR-Image from database
                Intent openViewAllModules = new Intent(getApplicationContext(), ViewAllModules.class);
                startActivity(openViewAllModules);


            } else if (view.getId() == R.id.lec_recall)         // user wishes to send previously stored info
            {
                // logcat tag to view app progress
                Log.d(TAG, "send previously stored files");

                // to retrieve previously saved data
                Intent viewStoredData = new Intent(getApplicationContext(), LoadStoredInfo.class);
                startActivityForResult(viewStoredData,99);


            } // if - else - else

            // connection with database not available, only function able to be carried out at this time is auto check-in
        } else
        {
            if (view.getId() == R.id.lec_man_signin)            // manual check-in not available
            {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "manual check in not available at this time", Toast.LENGTH_LONG);
                toast.show();

                // logcat tag to view app progress
                Log.e(TAG, "manual check in - server not available");


            } else if (view.getId() == R.id.lec_auto_signin)    // auto sign in is available, data stored on device
            {

                // logcat tag to view app progress
                Log.e(TAG, "auto check in - data to be stored on device");

                // opens up recursive sign in activity
                Intent openAutoSignin = new Intent(getApplicationContext(), RecursiveSignIn.class);
                startActivity(openAutoSignin);


            } else if (view.getId() == R.id.getQRCode)          // get QR-Code is not available
            {

                Toast toast = Toast.makeText(getApplicationContext(),
                        "QR-Codes cannot be retrieved at this time", Toast.LENGTH_LONG);
                toast.show();

                // logcat tag to view app progress
                Log.e(TAG, "retrieve QR - server not available");

            } // if - else to determine user choice

        } // if - else to determine operation if internet is or isn't available

    }// onClick



    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        // takes the scanned in data & prepares for use within this method
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        /**
         * called when returning from this screen, to search for previously stored files as soon as connectivity
         * becomes available (the user must need not leave the app to determine this, instead if he/ she returns
         * to this UI from another screen or if the UI is changed from portrait to landscape the server connection
         * will be tested again and the UI updated accordingly (setting the send previously saved files button to
         * visable - line 276)
         */

        if(requestCode == 99)
        {
            View b = findViewById(R.id.lec_recall);
            filesFound();

            if(filesFound())
            {
                b.setVisibility(View.VISIBLE);
            } else

                b.setVisibility(View.INVISIBLE);

        } // check for saved files

        /**
         * During development an option remained to change user ID within this interface to save installation time,
         * this functionality has been removed for final hand in, however the request code for the above activity
         * result remained as while this reset user functionality was enabled, it was necessary to determine which
         * type of activity result was being received by this activity and therefore how it should proceed with
         * processing this information
         */

        else if (scanningResult != null)             // as long as something has been scanned
        {
            // toast for unit testing to show tester scan contents
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();


            Log.d(TAG, "user wishes to register as another user");

            // launching Registration Activity
            Intent i = new Intent(getApplicationContext(), InitialReg.class);

            // takes the scanned info and packs it into a bundle before sending it to the Registration class
            String scannedInfo = scanContent;
            i.putExtra("Info", scannedInfo);
            startActivity(i);

            // closing this screen
            finish();

        } else {

            Log.e(TAG, "scan failed");

            Toast toast = Toast.makeText(getApplicationContext(),
                    "no scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }// if-else to confirm scan data has been received

    }// onActivityResult


    /**
     * Background Async Task used each time the user returns to this screen to establish if the server
     * connection is available
     * */

    class TestConnection extends AsyncTask<String, String, String>
    {

        /**
         * Before starting background thread Show Progress Dialog to inform
         * user that the app is processing information. As this process was
         * completed automatically, in less than half a second and in the
         * background, it was not necessary to show the user a dialog box
         **/

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        } // onPreExecute

        /**
         * send request to server to return success confirmation - code completed with reference to
         * http://www.desousa.com.pt/blog/2012/01/testing-server-reachability-on-android, first Accessed 17.08.14
         * */

        protected String doInBackground(String... args)
        {

            // use connectivity manager and network info to establish if the device has Internet access
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            // boolean serverOnline = false;                       // assume that the server is not yet available

            if (netInfo != null && netInfo.isConnected())       // first check that there is Internet Connectivity on device
            {
                // then try and make a connection to the server
                try {
                    URL url = new URL("http://" + MainScreenActivity.serverIP);                 // server IP address
                    HttpURLConnection testServer = (HttpURLConnection) url.openConnection();    // open up connection

                    // sets the HttpURLConnection defaults for testing a conection
                    testServer.setRequestProperty("User-Agent", "Android Application");
                    testServer.setRequestProperty("Connection", "close");

                    // the connection timeout period is 5000 milliseconds (3 seconds)
                    testServer.setConnectTimeout(5000);
                    // connects to server
                    testServer.connect();

                    // the connection will then return a value, which is stored as the boolean for true if server available
                    serverAvailable = (testServer.getResponseCode() == 200);
                    serverResponse = "connection available";

                    // details have been stored and the student is checked in
                    Log.d(TAG, "connection established");

                } catch (IOException e) {

                    // otherwise an exception will be thrown as the server is not available
                    serverResponse = "connection not available, offline mode activated";
                    serverAvailable = false;
                    // failed to sign-in, PHP has returned an error
                    Log.e(TAG, e.getMessage());
                }
            } else {
                // else no internet connection is available
                serverResponse = "connection not available, offline mode activated";
                serverAvailable = false;
                Log.e(TAG, "Internet Connection Unavailable");
            } // if - else

            return null;
        }// doInBackground



        /**
         * After completing background task, the textview showing server connection was to be
         * updated accordingly. In the event there are files stored on the device waiting to
         * be sent, the corresponding menu option is activated
         **/

        protected void onPostExecute(String file_url) {


            // check for previously stored files, if found, set button visability to on
            filesFound();

            if(serverAvailable)
            {
                serverStatus.setText("server available");
            } else {

                serverStatus.setText("server offline");
            }
            // if to update server info

            // the server must also be available before files can be sent!
            if(filesFound() && serverAvailable)
            {
                View b = findViewById(R.id.lec_recall);
                b.setVisibility(View.VISIBLE);
            } // check for saved files

        } // onPostExecute
    } // TestConnection

} // LectureUI
