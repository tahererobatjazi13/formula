package ir.kitgroup.formula

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import ir.huri.jcal.JalaliCalendar
import ir.kitgroup.formula.adapter.ChangeLogAdapter
import ir.kitgroup.formula.database.entity.MaterialChangeLog
import ir.kitgroup.formula.databinding.FragmentChangeLogBinding
import ir.kitgroup.formula.viewmodel.MaterialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChangeLogFragment : Fragment() {

    private var _binding: FragmentChangeLogBinding? = null
    private lateinit var materialAdapter: ChangeLogAdapter

    private lateinit var allMaterialChangeLog: List<MaterialChangeLog>

    private val binding get() = _binding!!
    private val materialViewModel: MaterialViewModel by viewModels()
    private var displayDateTime: String = ""
    private val args: ChangeLogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangeLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        allMaterialChangeLog = listOf()

        materialAdapter = ChangeLogAdapter()

        binding.rvChangeLog.adapter = materialAdapter
        binding.rvChangeLog.layoutManager = LinearLayoutManager(requireContext())

        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.GONE
        }

        materialViewModel.getChangeLogsForMaterialByType(args.materialId, args.changeType)
            .observe(viewLifecycleOwner) { materials ->
                allMaterialChangeLog = materials
                materialAdapter.submitList(materials)

                val isEmpty = materials.isEmpty()
                binding.tvNoItem.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
    }

    @SuppressLint("DefaultLocale")
    private fun init() {
        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.VISIBLE
        }
    }
}