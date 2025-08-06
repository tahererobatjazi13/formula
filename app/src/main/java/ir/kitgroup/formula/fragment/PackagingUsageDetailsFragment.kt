package ir.kitgroup.formula.fragment


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import ir.huri.jcal.JalaliCalendar
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.adapter.PackagingUsageDetailAdapter
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.databinding.FragmentPackagingUsageDetailsBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackagingUsageDetailsFragment : Fragment() {

    private var _binding: FragmentPackagingUsageDetailsBinding? = null
    private val packagingViewModel: PackagingViewModel by viewModels()
    private lateinit var packagingUsageDetailAdapter: PackagingUsageDetailAdapter
    private val args: PackagingUsageDetailsFragmentArgs by navArgs()
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var productDate: Long = 0
    private var productId: Int = 0
    private var productUsageId: Long = 0
    private var productName: String = ""
    private var qty: Double = 0.0
    private var isEditMode: Boolean = true
    private var packagePrice: Double = 0.0
    private val binding get() = _binding!!
    private val formatterQuantity = DecimalFormat("###,##0.###")
    private val selectedPackagings = mutableListOf<Packaging>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)).apply {
            visibility = View.GONE
        }
        (requireActivity().findViewById<Toolbar>(R.id.toolbar)).apply {
            visibility = View.GONE
        }
        _binding = FragmentPackagingUsageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        initAdapter()
        rxBinding()
        loadExistingData()
        setupObservers()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun init() {
        productId = args.packageId
        productUsageId = args.id
        productName = args.packageName
        productDate = args.packageDate

        val jalaliDate = JalaliCalendar()
        val dateFormatted =
            String.format("%02d-%02d-%04d", jalaliDate.day, jalaliDate.month, jalaliDate.year)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val time = timeFormat.format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"

        binding.tvProductName.text = productName
        binding.tvProductDate.text = formatDateShamsi(productDate)

        qty = args.formattedQty.toDoubleOrNull() ?: 0.0
        packagePrice = args.packagePrice.toDoubleOrNull() ?: 0.0

        binding.tvAmount.text = "${formatterQuantity.format(qty)} گرم"
        binding.tvPrice.text = "${formatterQuantity.format(packagePrice)} ریال"

        binding.tvRemaining.text = "${formatterQuantity.format(qty)} گرم"
        packagingViewModel.setTotalProductWeight(qty)
    }

    private fun initAdapter() {

        packagingUsageDetailAdapter =
            PackagingUsageDetailAdapter(packagingViewModel) { packaging ->
                if (!selectedPackagings.contains(packaging)) {
                    selectedPackagings.add(packaging)
                } else {
                    selectedPackagings.find { it.packagingId == packaging.packagingId }?.quantity =
                        packaging.quantity
                }

                updateTotalPriceUI()
            }

        packagingUsageDetailAdapter.onQuantityChanged = {
            updateTotalPriceUI()
        }

        binding.rvMaterials.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaterials.adapter = packagingUsageDetailAdapter
    }

    private fun rxBinding() {

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            if (selectedPackagings.isEmpty()) {
                Toast.makeText(context, R.string.error_no_packaging_selected, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    packagingViewModel.saveOrUpdateUsages(
                        productId,
                        productUsageId.toInt(),
                        selectedPackagings,
                        isEditMode
                    )

                    Toast.makeText(
                        context,
                        if (isEditMode)
                            R.string.msg_packaging_consumption_edit
                        else
                            R.string.msg_packaging_consumption_saved,
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ذخیره‌سازی!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadExistingData() {
        packagingViewModel.loadUsagesForProduct(productUsageId)
        updateTotalPriceUI()
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        packagingViewModel.totalUsedWeight.observe(viewLifecycleOwner) { usedWeight ->
            val remaining = qty - usedWeight
            binding.tvRemaining.text = "${formatterQuantity.format(remaining)} گرم"
        }
        packagingViewModel.allPackagings.observe(viewLifecycleOwner) { packagings ->
            updateUIWithProducts(packagings)
            packagingViewModel._packagingUsages.value.let { usages ->
                selectedPackagings.clear()
                usages.forEach { usage ->
                    if (usage.usedWeight > 0) {
                        val packaging = packagings.find { it.packagingId == usage.packagingId }
                        packaging?.let {
                            it.quantity = usage.usedWeight
                            selectedPackagings.add(it)
                        }
                    }
                }

                updateTotalPriceUI()
            }
        }

        //  وضعیت ویرایش
        packagingViewModel.isEditMode.observe(viewLifecycleOwner) { isEdit ->
            isEditMode = isEdit
            binding.btnSave.text =
                getString(if (isEdit) R.string.label_edit else R.string.label_save)
        }
    }

    private fun updateTotalPriceUI() {
        val totalPrice = selectedPackagings.sumOf { it.quantity * it.price }
        binding.tvTotalPrice.text = formatterQuantity.format(totalPrice)

        // به‌روزرسانی قیمت کل محصول + بسته‌بندی
        updateTotalProductWithPackagingPrice()
    }

    private fun updateUIWithProducts(packagings: List<Packaging>) {
        packagingUsageDetailAdapter.submitList(packagings)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTotalProductWithPackagingPrice() {
        val packagingTotalPrice = selectedPackagings.sumOf { it.quantity * it.price }
        val total = packagingTotalPrice + packagePrice
        val formattedTotal = formatterQuantity.format(total)
        binding.tvTotalPriceProductPackaging.text = "$formattedTotal ریال"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}