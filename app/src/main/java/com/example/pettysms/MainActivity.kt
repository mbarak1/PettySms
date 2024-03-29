package com.example.pettysms

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.pettysms.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors


class MainActivity : AppCompatActivity(), OnActionModeInteraction {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val SMS_SEND_PERMISSION_CODE = 125
    private val SMS_RECEIVE_PERMISSION_CODE = 126
    private val SMS_READ_PERMISSION_CODE = 127
    private val REQUEST_SMS_PERMISSION = 123
    private val READ_EXTERNAL_STORAGE_CODE = 1
    private val WRITE_EXTERNAL_STORAGE_CODE = 2
    private var check_fragment = "home"
    private var isServiceScheduled = false


    override fun onCreate(savedInstanceState: Bundle?) {

        DynamicColors.applyToActivitiesIfAvailable(this.application)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)

        var bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        //val dynamic_available: String = DynamicColors.isDynamicColorAvailable().toString()
        //Toast.makeText(this,dynamic_available,100).show()

        //DynamicColors.isDynamicColorAvailable()

        // Check and request runtime permissions
        if (ContextCompat.checkSelfPermission(
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
        }







    bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.page_1 -> {
                    check_fragment = "home"
                    onDestroyActionMode()
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
            isServiceScheduled = true
        }

        setContentView(binding.root)






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
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
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

    override fun onRequestPermissionsResult(
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
    }

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

