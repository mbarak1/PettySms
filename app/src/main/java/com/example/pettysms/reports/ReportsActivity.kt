package com.example.pettysms.reports

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.pettysms.R
import com.example.pettysms.databinding.ActivityReportsBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ReportsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportsBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var generateReportFab: ExtendedFloatingActionButton
    
    private val TAG = "ReportsActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPager()
        setupFab()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.reports)
    }
    
    private fun setupViewPager() {
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        
        // Set up the pager adapter with fragments
        val pagerAdapter = ReportsPagerAdapter(this)
        viewPager.apply {
            adapter = pagerAdapter
            // Keep more pages in memory to prevent reloading
            offscreenPageLimit = 4
            // Disable swiping to prevent accidental fragment switches
            isUserInputEnabled = true
            // Set initial page to 0 (M-Pesa Statements)
            setCurrentItem(0, false)
        }
        
        // Connect the TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "M-Pesa Statements"
                1 -> "PettyCash Statements"
                2 -> "PettyCash Copies"
                3 -> "PettyCash Schedule"
                4 -> "VAT Report"
                else -> "Unknown"
            }
        }.attach()
        
        // Listen for page changes to update the FAB text
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateFabForPage(position)
            }
        })
    }
    
    private fun setupFab() {
        generateReportFab = binding.fabGenerateReport
        
        // Initial state - set for first tab
        updateFabForPage(0)
        
        // Set up FAB click listener
        generateReportFab.setOnClickListener {
            val currentPosition = viewPager.currentItem
            when (currentPosition) {
                0 -> generateMpesaStatement()
                1 -> generatePettyCashStatement()
                2 -> generatePettyCashCopy()
                3 -> generatePettyCashSchedule()
                4 -> generateVatReport()
            }
        }
    }
    
    private fun updateFabForPage(position: Int) {
        generateReportFab.text = when (position) {
            0 -> getString(R.string.generate_mpesa_statement)
            1 -> getString(R.string.generate_petty_cash_statement)
            2 -> getString(R.string.generate_petty_cash_copy)
            3 -> getString(R.string.generate_schedule)
            4 -> "Generate VAT Report"
            else -> getString(R.string.generate_report)
        }
        
        // Update icon based on report type
        generateReportFab.setIconResource(
            when (position) {
                2 -> R.drawable.content_copy_24px // For PettyCash Copies
                3 -> R.drawable.calendar_month_24px // For Schedule
                4 -> R.drawable.receipt_long_24px // For VAT Report
                else -> R.drawable.description_24px // Default for statements
            }
        )
    }
    
    private fun generateMpesaStatement() {
        Log.d(TAG, "Generating M-Pesa Statement")
        // Will be implemented to trigger report generation in the M-Pesa fragment
        val fragment = supportFragmentManager.findFragmentByTag("f0") as? MpesaStatementFragment
        fragment?.generateReport()
    }
    
    private fun generatePettyCashStatement() {
        Log.d(TAG, "Generating PettyCash Statement")
        // Will be implemented to trigger report generation in the PettyCash Statement fragment
        val fragment = supportFragmentManager.findFragmentByTag("f1") as? PettyCashStatementFragment
        fragment?.generateReport()
    }
    
    private fun generatePettyCashCopy() {
        Log.d(TAG, "Generating PettyCash Copy")
        // Will be implemented to trigger report generation in the PettyCash Copies fragment
        val fragment = supportFragmentManager.findFragmentByTag("f2") as? PettyCashCopiesFragment
        fragment?.generateReport()
    }
    
    private fun generatePettyCashSchedule() {
        Log.d(TAG, "Generating PettyCash Schedule")
        // Will be implemented to trigger report generation in the PettyCash Schedule fragment
        val fragment = supportFragmentManager.findFragmentByTag("f3") as? PettyCashScheduleFragment
        fragment?.generateReport()
    }
    
    private fun generateVatReport() {
        Log.d(TAG, "Generating VAT Report")
        // Trigger report generation in the VAT Report fragment
        val fragment = supportFragmentManager.findFragmentByTag("f4") as? VatReportFragment
        fragment?.generateReport()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // ViewPager adapter for the report fragments
    private inner class ReportsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 5
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MpesaStatementFragment()
                1 -> PettyCashStatementFragment()
                2 -> PettyCashCopiesFragment()
                3 -> PettyCashScheduleFragment()
                4 -> VatReportFragment()
                else -> throw IllegalStateException("Unexpected position $position")
            }
        }
    }
} 