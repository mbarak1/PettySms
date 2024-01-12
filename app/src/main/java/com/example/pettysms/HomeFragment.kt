package com.example.pettysms

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import com.example.pettysms.databinding.FragmentHomeBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        _binding!!.textviewFirst.setText(this.id.toString())
        val actionbar = binding.root.findViewById<Toolbar>(R.id.toolbar1)
        actionbar.setTitle("Hi" + " Mbarak")
        val buttonDropTables = binding.buttonDropTables
        var db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
        var db = db_helper?.writableDatabase
        buttonDropTables.setOnClickListener {
            if (db != null) {
                DbHelper.dropAllTables(db!!)
                activity?.finishAffinity()
            } else {
                var db_helper = this.activity?.applicationContext?.let { DbHelper(it) }
                var db = db_helper?.writableDatabase
                DbHelper.dropAllTables(db!!)
                activity?.finishAffinity()
            }
        }

        (activity as AppCompatActivity).setSupportActionBar(actionbar)
        return binding.root

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        activity?.menuInflater?.inflate(R.menu.menu_home, menu)

        //super.onPrepareOptionsMenu(menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.MpesaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}