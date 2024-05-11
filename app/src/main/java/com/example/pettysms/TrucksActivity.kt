package com.example.pettysms

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pettysms.databinding.ActivityTrucksBinding
import com.example.pettysms.databinding.ActivityViewAllTransactionsBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.search.SearchBar
import java.io.Serializable

class TrucksActivity : AppCompatActivity(), SortAndFilterTrucks.OnApplyClickListener {
    private lateinit var trucksRecyclerView: RecyclerView
    private lateinit var binding: ActivityTrucksBinding
    private lateinit var truckAdapter: TruckAdapter
    private lateinit var nestedScrollingView: NestedScrollView
    private lateinit var floatingActionButton: ExtendedFloatingActionButton
    private lateinit var searchBar: SearchBar
    private lateinit var appBarLayout: AppBarLayout

    private var keyValueMapToFilterAndSortFragment = mutableMapOf<String, MutableList<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrucksBinding.inflate(layoutInflater)
        trucksRecyclerView = binding.trucksRecyclerView
        nestedScrollingView = binding.nestedScrollView3
        floatingActionButton = binding.floatingActionButton
        appBarLayout = binding.appbar
        searchBar = binding.searchBar


        trucksRecyclerView.layoutManager = LinearLayoutManager(this)
        // Query database to get truck data
        val dbHelper = DbHelper(this)
        val trucks = dbHelper.getLocalTrucks() // Assuming getAllTrucks() retrieves all trucks from the database

        truckAdapter = TruckAdapter(trucks)
        trucksRecyclerView.adapter = truckAdapter


        nestedScrollingView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY -> // the delay of the extension of the FAB is set for 12 items
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

        val searchBarHeight = searchBar.height
        val slideUpDistance = -searchBarHeight.toFloat()
        val slideDownDistance = 0f

        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val slideDistance = if (verticalOffset < 0) slideUpDistance else slideDownDistance
            searchBar.animate()
                .translationY(slideDistance)
                .setDuration(200)
                .start()
        })

        floatingActionButton.setOnClickListener {

        }

        searchBar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_bar_options -> {
                    // Handle the "Settings" menu item click
                    showDialog(keyValueMapToFilterAndSortFragment)
                    //showSecondaryMenu(menuItem)
                    true
                }
                // Add more cases for other menu items if needed
                else -> false
            }
        }

        setContentView(binding.root)
    }

    fun showDialog(mapToSend: MutableMap<String, MutableList<String>>) {
        val dialog = SortAndFilterTrucks()

        // Create a Bundle and add the MutableMap as an argument
        if(!keyValueMapToFilterAndSortFragment.isNullOrEmpty()){
            val args = Bundle()
            args.putSerializable("yourMapKey", mapToSend as Serializable)
            dialog.arguments = args
        }

        dialog.setOnApplyClickListener(this)
        dialog.show(supportFragmentManager, "SortAndFilterTrucks")

    }

    override fun onApplyClick(keyValueMap: Map<String, List<String>>) {

    }


}