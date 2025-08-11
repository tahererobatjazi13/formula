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
import ir.kitgroup.formula.core.Util.formatDateShamsi
import ir.kitgroup.formula.adapter.PackagingUsageDetailAdapter
import ir.kitgroup.formula.core.Util
import ir.kitgroup.formula.core.Util.formatQuantity
import ir.kitgroup.formula.database.entity.Packaging
import ir.kitgroup.formula.databinding.FragmentPackagingUsageDetailsBinding
import ir.kitgroup.formula.viewmodel.PackagingViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackagingUsageDetailsFragment : Fragment() {

    private var _binding: FragmentPackagingUsageDetailsBinding? = null
    private val binding get() = _binding!!

    private val packagingViewModel: PackagingViewModel by viewModels()
    private lateinit var packagingUsageDetailAdapter: PackagingUsageDetailAdapter
    private val args: PackagingUsageDetailsFragmentArgs by navArgs()
    private val selectedPackagings = mutableListOf<Packaging>()

    // Data vars
    private var productId: Int = 0
    private var productUsageId: Long = 0
    private var productName: String = ""
    private var productDate: Long = 0
    private var qty: Double = 0.0
    private var packagePrice: Double = 0.0
    private var productNamePdf: String = ""
    private var displayDateTime: String = ""
    private var isEditMode: Boolean = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideUIElements()
        _binding = FragmentPackagingUsageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initArgs()
        initUI()
        initAdapter()
        rxBinding()
        setupObservers()
        loadExistingData()


    }

    private fun initArgs() {
        productId = args.packageId
        productUsageId = args.id
        productName = args.packageName
        productDate = args.packageDate
        qty = args.formattedQty.toDoubleOrNull() ?: 0.0
        packagePrice = args.packagePrice.toDoubleOrNull() ?: 0.0

        val jalaliDate = JalaliCalendar()
        val dateFormatted = "%02d-%02d-%04d".format(
            jalaliDate.day, jalaliDate.month, jalaliDate.year
        )
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        displayDateTime = "$dateFormatted ، $time"
        productNamePdf = "${productName}_$displayDateTime"
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun initUI() = binding.apply {
        tvProductName.text = productName
        tvProductDate.text = formatDateShamsi(productDate)
        tvProductAmount.text = "${formatQuantity(qty)} گرم"
        tvProductPrice.text = "${Util.priceFormatter.format(packagePrice)} ریال"
        tvRemaining.text = "${formatQuantity(qty)} گرم"
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

        packagingUsageDetailAdapter.onQuantityChanged = { updateTotalPriceUI() }

        binding.rvPackagingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packagingUsageDetailAdapter
        }
    }

    private fun rxBinding() = binding.apply {

        ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnEditSave.setOnClickListener {
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


    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        packagingViewModel.totalUsedWeight.observe(viewLifecycleOwner) { usedWeight ->
            val remaining = qty - usedWeight
            binding.tvRemaining.text = "${formatQuantity(remaining)} گرم"
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
            binding.btnEditSave.text =
                getString(if (isEdit) R.string.label_edit else R.string.label_save)
        }
    }

    private fun loadExistingData() {
        packagingViewModel.loadUsagesForProduct(productUsageId)
        updateTotalPriceUI()
    }

    private fun updateUIWithProducts(packagings: List<Packaging>) {
        packagingUsageDetailAdapter.submitList(packagings)
    }

    private fun updateTotalPriceUI() {
        val totalPrice = selectedPackagings.sumOf { it.quantity * it.price }
        binding.tvTotalPrice.text = Util.priceFormatter.format(totalPrice)

        // به‌روزرسانی قیمت کل محصول + بسته‌بندی
        updateTotalProductWithPackagingPrice()
    }


    @SuppressLint("SetTextI18n")
    private fun updateTotalProductWithPackagingPrice() {
        val packagingTotalPrice = selectedPackagings.sumOf { it.quantity * it.price }
        val total = packagingTotalPrice + packagePrice
        binding.tvTotalPriceProductPackaging.text = "${Util.priceFormatter.format(total)} ریال"
    }

    private fun hideUIElements() {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility =
            View.GONE
        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}