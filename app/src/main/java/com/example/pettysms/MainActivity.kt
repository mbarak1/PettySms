package com.example.pettysms

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.pettysms.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.example.pettysms.queue.QuickBooksWorker


class MainActivity : AppCompatActivity(), OnActionModeInteraction {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val SMS_SEND_PERMISSION_CODE = 125
    private val SMS_RECEIVE_PERMISSION_CODE = 126
    private val SMS_READ_PERMISSION_CODE = 127
    private val REQUEST_SMS_PERMISSION = 123
    private val READ_EXTERNAL_STORAGE_CODE = 1
    private val WRITE_EXTERNAL_STORAGE_CODE = 2
    private val INTERNET_CODE = 3
    private val PERMISSIONS_REQUEST_CODE = 1
    private var check_fragment = "home"
    private var activityName = "MainActivity"
    private var isServiceScheduled = false


    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.USE_FULL_SCREEN_INTENT,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,  // Change to specific media types if necessary
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.CAMERA
    )


    override fun onCreate(savedInstanceState: Bundle?) {

        DynamicColors.applyToActivitiesIfAvailable(this.application)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)

        var bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        //val dynamic_available: String = DynamicColors.isDynamicColorAvailable().toString()
        //Toast.makeText(this,dynamic_available,100).show()

        //DynamicColors.isDynamicColorAvailable()

        setContentView(binding.root)

        // Initialize the navController and appBarConfiguration
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.HomeFragment, R.id.MpesaFragment, R.id.settingsFragment, R.id.pettyCashFragment)
        )

        // Check and request runtime permissions
        /*if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                REQUEST_SMS_PERMISSION
            )
        }*/

        checkAndRequestPermissions()

        Log.d(activityName, "Inside Main Activity")


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_DOCUMENTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.MANAGE_DOCUMENTS),
                INTERNET_CODE
            )
            Log.d(activityName, "Camera Permission Permitted")
        }else{
            Log.d(activityName, "Camera Permission Denied")
        }











    bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.page_1 -> {
                    check_fragment = "home"
                    onDestroyActionMode()
                    checkPermission(Manifest.permission.INTERNET, INTERNET_CODE)
                    Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.HomeFragment)
                    //actionbar.setTitle("Hi" + " Mbarak")
                    //setSupportActionBar(actionbar)
                    //Toast.makeText(this,supportFragmentManager.findFragmentById(R.id.MpesaFragment).toString(),Toast.LENGTH_SHORT).show()

                    true
                }
                R.id.page_2 -> {

                    //actionbar.setTitle("Mpesa" + " Tool")
                    //setSupportActionBar(actionbar)
                    check_fragment = "mpesa"


                    checkPermission(Manifest.permission.RECEIVE_SMS, SMS_RECEIVE_PERMISSION_CODE)
                    checkPermission(Manifest.permission.SEND_SMS, SMS_SEND_PERMISSION_CODE)
                    checkPermission(Manifest.permission.READ_SMS, SMS_READ_PERMISSION_CODE)
                    checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_CODE)
                    checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE)


                    val requiredPermissionSmsSend = Manifest.permission.SEND_SMS
                    val checkValSend: Int = this.checkCallingOrSelfPermission(requiredPermissionSmsSend)
                    val requiredPermissionSmsReceived = Manifest.permission.RECEIVE_SMS
                    val checkValReceive: Int = this.checkCallingOrSelfPermission(requiredPermissionSmsReceived)
                    val requiredPermissionSmsRead = Manifest.permission.READ_SMS
                    val checkValRead = this.checkCallingOrSelfPermission(requiredPermissionSmsRead)
                    if (checkValSend == PackageManager.PERMISSION_GRANTED && checkValReceive == PackageManager.PERMISSION_GRANTED && checkValRead == PackageManager.PERMISSION_GRANTED){
                        Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.MpesaFragment)
                        //val a = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)?.childFragmentManager?.fragments?.get(0)
                        //Toast.makeText(this,a.toString(),Toast.LENGTH_SHORT).show()

                    }
                    else{
                        checkPermission(Manifest.permission.RECEIVE_SMS, SMS_RECEIVE_PERMISSION_CODE)
                        checkPermission(Manifest.permission.SEND_SMS, SMS_SEND_PERMISSION_CODE)
                        checkPermission(Manifest.permission.READ_SMS, SMS_READ_PERMISSION_CODE)
                        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_CODE)
                        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE)

                    }



                    true


                }
                R.id.page_3 -> {
                    onDestroyActionMode()
                    Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.settingsFragment)
                    true
                }

                R.id.page_4 -> {
                    check_fragment = "petty_cash"
                    onDestroyActionMode()
                    Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.pettyCashFragment)
                    //actionbar.setTitle("Hi" + " Mbarak")
                    //setSupportActionBar(actionbar)
                    //Toast.makeText(this,supportFragmentManager.findFragmentById(R.id.MpesaFragment).toString(),Toast.LENGTH_SHORT).show()

                    true
                }

                else -> false
            }
        }

        bottomNav.setOnItemReselectedListener {}


        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        println("balla gani:" + dbHelper.hasTables(db))




        if (dbHelper.hasTables(db) && !isServiceScheduled) {
            println("ola cela")
                scheduleJob()
                scheduleSyncMainPettyCashValues()
                isServiceScheduled = true
        }

        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("mpesa_first_launch", false)

        println("Mpesa First Launch: " + isFirstLaunch)

    }

    private fun scheduleSyncMainPettyCashValues() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiverSyncValues::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Set the alarm to trigger every minute
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            300 * 1000,  // 15 minute interval
            pendingIntent
        )

    }


    private fun scheduleJob() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Set the alarm to trigger every minute
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            60 * 1000,  // 1 minute interval
            pendingIntent
        )
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            // All permissions are granted, proceed with your logic
            proceedWithAppLogic()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    // All permissions are granted, proceed with your logic
                    proceedWithAppLogic()
                } else {
                    // Handle the case where permissions are not granted
                    showPermissionDeniedMessage()
                }
            }
        }
    }

    private fun proceedWithAppLogic() {
        // Schedule the SMS service
        if (!isServiceScheduled) {
            scheduleJob()
            isServiceScheduled = true
        }
        
        // Initialize the QuickBooks sync worker at startup
        QuickBooksWorker.initializeAtStartup(this)
        Log.d(activityName, "QuickBooks sync worker initialized at startup")
        
        // Schedule the sync values service
        scheduleSyncMainPettyCashValues()
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(this, "Some permissions are denied. The app may not function correctly.", Toast.LENGTH_SHORT).show()
    }





    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (check_fragment == "home"){
            menuInflater.inflate(R.menu.menu_home, menu)
        }
        else if (check_fragment == "mpesa"){
            menuInflater.inflate(R.menu.menu_mpesa, menu)
        }
        //menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }*/

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return if (::appBarConfiguration.isInitialized) {
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } else {
            navController.navigateUp() || super.onSupportNavigateUp()
        }
    }

    override fun onResume() {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        if (dbHelper.hasTables(db) && !isServiceScheduled) {
            println("ole celuza")
            scheduleJob()
            isServiceScheduled = true

        }
        super.onResume()
    }

    // Function to check and request permission.
    fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {

            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
        if (requestCode == SMS_SEND_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "SMS Send Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "SMS Send Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == SMS_RECEIVE_PERMISSION_CODE) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@MainActivity, "SMS Receive Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "SMS Receive Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }else if (requestCode == SMS_READ_PERMISSION_CODE) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@MainActivity, "SMS Receive Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this@MainActivity, "SMS Receive Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }*/

    override fun onDestroyActionMode() {
        // Code to destroy the ActionMode in the fragment
        println("call 1")
        val fragment = supportFragmentManager.findFragmentByTag("mpesa")
        println(fragment.toString())
        if (fragment != null && fragment is MpesaFragment) {
            println("call 2")
            fragment.destroyActionMode()
        }
    }

}

