package com.example.pettysms

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.pettysms.databinding.FragmentPettyCashBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray


/**
 * A simple [Fragment] subclass.
 * Use the [PettyCashFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PettyCashFragment : Fragment() {
    private lateinit var toolbar: MaterialToolbar
    private var _binding: FragmentPettyCashBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var dotIndicator: WormDotsIndicator
    private lateinit var cashImage: ImageView
    private lateinit var cashLabel: TextView
    private lateinit var mpesaImage: ImageView
    private lateinit var dbDotImageView: ImageView
    private lateinit var dbStatusTextLabel: TextView
    private lateinit var qbImageView: ImageView
    private lateinit var qbStatusTextLabel: TextView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var loadingDialog: AlertDialog
    private lateinit var loadingText: TextView
    private lateinit var truckButton: Button
    private lateinit var ownerButton: Button
    private lateinit var transactorsButton: Button
    private val binding get() = _binding!!
    private var dbStatus = false
    private var qbStatus = false

    var dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
    var db = dbHelper?.writableDatabase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPettyCashBinding.inflate(inflater, container, false)
        toolbar = binding.pettyCashToolbar
        viewPager = binding.viewPager
        dotIndicator = binding.wormDotsIndicator
        dbDotImageView = binding.dbDotImageView
        dbStatusTextLabel = binding.dbStatusLabel
        qbImageView = binding.qbDotImageView
        qbStatusTextLabel = binding.qbStatusLabel
        nestedScrollView = binding.nestedScrollView
        floatingActionButton = binding.floatingActionButton
        truckButton = binding.trucksButton
        ownerButton = binding.ownersButton
        transactorsButton = binding.transactorsButton

        dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
        db = dbHelper?.writableDatabase

        val cards = listOf(R.layout.card_mpesa_petty_cash)

        val adapter = ViewPagerAdapter(cards)
        viewPager.adapter = adapter



        // Bind the ViewPager to the WormDotsIndicator
        dotIndicator.attachTo(viewPager)


        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Petty Cash"
        }

        // Delay accessing ImageView until the layout is inflated
        viewPager.post {
            val dotMenuButtonImage: ImageView = requireView().findViewById(R.id.menuIcon)
            // Set click listener on ImageView
            dotMenuButtonImage.setOnClickListener {
                // Show a toast message
                showPopupMenu(it)
            }
            cashImage = viewPager.rootView.findViewById<ImageView>(R.id.cashImage)
            cashLabel = viewPager.rootView.findViewById<TextView>(R.id.cashLabel)
            mpesaImage = viewPager.rootView.findViewById<ImageView>(R.id.mpesaImage)
            updateMpesaCardDetails()
        }


        checkDbStatus()
        checkQbStatus()

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY -> // the delay of the extension of the FAB is set for 12 items
            if (scrollY > oldScrollY + 12 && floatingActionButton.isExtended) {
                floatingActionButton.shrink()
            }

            // the delay of the extension of the FAB is set for 12 items
            if (scrollY < oldScrollY - 12 && !floatingActionButton.isExtended) {
                floatingActionButton.extend()
            }

            // if the nestedScrollView is at the first item of the list then the
            // extended floating action should be in extended state
            if (scrollY == 0) {
                floatingActionButton.extend()
            }
        })

        floatingActionButton.setOnClickListener {
            addNewPettyCash()
        }

        syncMainPettyCashValues()
        initializeButtons()






        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeButtons() {
        truckButton.setOnClickListener {
            trucksButtonAction()
        }
        ownerButton.setOnClickListener {
            ownersButtonAction()
        }
        transactorsButton.setOnClickListener {
            transactorsButtonAction()
        }

    }

    private fun transactorsButtonAction() {
        val intent = Intent(activity, TransactorsActivity::class.java)

        intent.putExtra("key", "value")

        startActivity(intent)
    }

    private fun ownersButtonAction() {
        val intent = Intent(activity, OwnersActivity::class.java)

        // Optionally, you can pass data to the second activity using extras
        intent.putExtra("key", "value")

        // Start the second activity
        startActivity(intent)
    }

    private fun trucksButtonAction() {
        val intent = Intent(activity, TrucksActivity::class.java)

        // Optionally, you can pass data to the second activity using extras
        intent.putExtra("key", "value")

        // Start the second activity
        startActivity(intent)

    }

    private fun syncMainPettyCashValues() {
        val loadingDialog = createLoadingDialog()
        loadingDialog.show()

        GlobalScope.launch(Dispatchers.Main) {
            fetchAndInsertOwners()
            fetchAndInsertTrucks()
            loadingDialog.dismiss()
        }
    }

    private suspend fun fetchAndInsertOwners() {
            val remoteOwners = fetchRemoteOwners()
            remoteOwners?.let { newOwners ->
                val localOwners = dbHelper?.getAllOwners()
                val ownersToInsert = newOwners.filterNot { newOwner ->
                    localOwners?.any { it.id == newOwner.id } == true
                }
                if (db?.isOpen == true) {
                    dbHelper?.insertOwners(ownersToInsert)
                }else {
                    dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
                    db = dbHelper?.writableDatabase
                    dbHelper?.insertOwners(ownersToInsert)
                }
            }

    }

    private suspend fun fetchAndInsertTrucks() {
        val remoteTrucks = fetchRemoteTrucks()
        remoteTrucks?.let { newTrucks ->
            val localTrucks = getLocalTrucks()
            val trucksToInsert = newTrucks.filterNot { newTruck ->
                localTrucks.any { it == newTruck }
            }
            if (db?.isOpen == true) {
                dbHelper?.insertTrucks(trucksToInsert)
            }else{
                dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
                db = dbHelper?.writableDatabase
                dbHelper?.insertTrucks(trucksToInsert)
            }
        }
    }

    private suspend fun fetchRemoteTrucks(): List<Truck>? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("operation", "getalltrucks")
                .build()

            val request = Request.Builder()
                .url("http://$SERVER_IP/api/index.php")
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string() ?: "")
                        val trucks = mutableListOf<Truck>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val truckId = jsonObject.getInt("id")
                            val truckNo = jsonObject.getString("truck_no")
                            val make = jsonObject.getString("make")
                            val ownerCode = jsonObject.getString("owner")
                            val activeStatusString = jsonObject.getString("active_status")

                            val activeStatus = activeStatusString == "active"
                            var owner: Owner? = null

                            if (db?.isOpen == true) {
                                owner = dbHelper?.getOwnerByCode(ownerCode)
                            }else{
                                dbHelper = DbHelper(requireContext())
                                db = dbHelper?.writableDatabase
                                owner = dbHelper?.getOwnerByCode(ownerCode)
                            }


                            // Parse other truck details...
                            trucks.add(Truck(truckId, truckNo, make, owner, activeStatus))
                        }
                        trucks
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    private suspend fun fetchRemoteOwners(): List<Owner>? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .add("operation", "getallowners")
                .build()

            val request = Request.Builder()
                .url("http://$SERVER_IP/api/index.php")
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonArray = JSONArray(response.body?.string() ?: "")
                        val owners = mutableListOf<Owner>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val ownerId = jsonObject.getInt("id")
                            val ownerName = jsonObject.getString("name")
                            val ownerCode = jsonObject.getString("owner_code")
                            // Parse other owner details...
                            owners.add(Owner(ownerId, ownerName, ownerCode))
                        }
                        owners
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    private fun getLocalTrucks(): List<Truck> {
        if (db?.isOpen == true) {
            return dbHelper?.getLocalTrucks()!!
        }else {
            dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
            db = dbHelper?.writableDatabase
            return dbHelper?.getLocalTrucks()!!
        }
    }

    private fun insertNewTrucks(trucks: List<Truck>) {
        if (db?.isOpen == true) {
            dbHelper?.insertTrucks(trucks)
        }else {
            dbHelper = this.activity?.applicationContext?.let { DbHelper(it) }
            db = dbHelper?.writableDatabase
            dbHelper?.insertTrucks(trucks)
        }
    }

    private fun addNewPettyCash() {
        val dialogFragment = AddPettyCashFragment()
        dialogFragment.show(requireActivity().supportFragmentManager, "fragment_add_petty_cash")

    }

    private fun checkQbStatus() {
        qbStatus = true
        updateStatus(qbStatus, qbImageView, qbStatusTextLabel)

        // Create a Handler
        val handler = android.os.Handler()

        // Define a Runnable to change the boolean value
        val changeBooleanRunnable = Runnable {
            // Change the boolean value after 5 seconds
            qbStatus = false
            updateStatus(qbStatus, qbImageView, qbStatusTextLabel)

        }

        // Post the Runnable with a delay of 5 seconds
        handler.postDelayed(changeBooleanRunnable, 10000)
    }

    private fun checkDbStatus() {
        dbStatus = true
        updateStatus(dbStatus, dbDotImageView, dbStatusTextLabel)

        // Create a Handler
        val handler = android.os.Handler()

        // Define a Runnable to change the boolean value
        val changeBooleanRunnable = Runnable {
            // Change the boolean value after 5 seconds
            dbStatus = false
            updateStatus(dbStatus, dbDotImageView, dbStatusTextLabel)

        }

        // Post the Runnable with a delay of 5 seconds
        handler.postDelayed(changeBooleanRunnable, 5000)

    }

    @SuppressLint("ResourceAsColor")
    private fun updateStatus(status: Boolean, imageView: ImageView, statusLabel: TextView) {
        if (status) {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.new_green_color
                ), PorterDuff.Mode.SRC_IN
            )
            statusLabel.text = "Online"
            addFadeInAndOutAnimation(imageView)

        } else {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
            imageView.clearAnimation()
            statusLabel.text = "Offline"
        }
    }

    private fun addFadeInAndOutAnimation(imageView: ImageView){

        val fadeInOut = AlphaAnimation(0f, 1f)
        fadeInOut.duration = 1000 // Duration for each fade (in milliseconds)
        fadeInOut.repeatMode = Animation.REVERSE // Reverse the animation
        fadeInOut.repeatCount = Animation.INFINITE // Repeat the animation indefinitely

        val handler = android.os.Handler()
        val delay = 2000 // Delay in milliseconds before starting the animation

        handler.postDelayed({
            imageView.startAnimation(fadeInOut)
        }, delay.toLong())

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, GravityCompat.END, 0, R.style.PopupMenuStyle)
        popupMenu.menuInflater.inflate(R.menu.petty_cash_card_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_cash -> {
                    updateCashCardDetails()
                    true
                }
                R.id.menu_mpesa -> {
                    updateMpesaCardDetails()
                    true
                }
                else -> false
            }
        }
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    private fun updateMpesaCardDetails() {
        cashImage.visibility = View.GONE
        cashLabel.visibility = View.GONE
        mpesaImage.visibility = View.VISIBLE

    }

    private fun updateCashCardDetails() {
        cashImage.visibility = View.VISIBLE
        cashLabel.visibility = View.VISIBLE
        mpesaImage.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun createLoadingDialog(): AlertDialog {
        // Create a custom view for the loading dialog
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.loading_dialog, null)
        loadingText = customView.findViewById(R.id.loading_text)
        loadingText.text = "Syncing... Please Wait"

        // Show loading dialog
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(customView)
            .setCancelable(false)
            .create()

        return loadingDialog
    }

    companion object {
        const val SERVER_IP = "10.0.2.2"
    }
}