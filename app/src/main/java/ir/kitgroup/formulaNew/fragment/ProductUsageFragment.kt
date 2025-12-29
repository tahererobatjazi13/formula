package ir.kitgroup.formulaNew.fragment

import android.annotation.SuppressLint
import ir.kitgroup.formulaNew.databinding.FragmentProductUsageBinding
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.core.Util.calculatePricePerKg
import ir.kitgroup.formulaNew.core.Util.formatDateShamsi
import ir.kitgroup.formulaNew.core.Util.formatQuantity
import ir.kitgroup.formulaNew.core.Util.getTotalPriceForProduct
import ir.kitgroup.formulaNew.core.Util.getTotalQuantityForProduct
import ir.kitgroup.formulaNew.adapter.ProductUsageAdapter
import ir.kitgroup.formulaNew.core.Util
import ir.kitgroup.formulaNew.dialog.ConfirmDeleteDialog
import ir.kitgroup.formulaNew.viewmodel.ProductViewModel
import kotlin.math.roundToInt

class ProductUsageFragment : Fragment() {

    private var _binding: FragmentProductUsageBinding? = null
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productUsageAdapter: ProductUsageAdapter
    private val args: ProductUsageFragmentArgs by navArgs()
    private var pricePerKg: Double = 0.0
    private var totalPrice: Double = 0.0
    private var totalQuantity: Double = 0.0
    private var formatTotalQuantity: String = ""
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideUIElements()
        _binding = FragmentProductUsageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupObservers()
        setupListeners()

        binding.tvProductName.text = args.productName
        binding.tvProductDate.text = formatDateShamsi(args.productDate)

        viewModel.getProductDetails(args.productId).observe(viewLifecycleOwner) { details ->
            totalQuantity = getTotalQuantityForProduct(details)
            totalPrice = getTotalPriceForProduct(details)

            formatTotalQuantity = formatQuantity(totalQuantity)
            binding.tvProductAmount.text = "$formatTotalQuantity گرم"
            binding.tvProductPrice.text = Util.priceFormatter.format(totalPrice) + " ریال "
            pricePerKg = calculatePricePerKg(totalQuantity, totalPrice)
        }

        viewModel.loadHistory(args.productId)
    }

    private fun setupRecyclerView() {
        productUsageAdapter = ProductUsageAdapter(onClick = { product, formattedQty, totalPrice ->
            val action =
                ProductUsageFragmentDirections.actionProductUsageFragmentToProductUsageDetailsFragment(
                    1,
                    product.id,
                    product.productId,
                    args.productName,
                    args.productDate, formattedQty, totalPrice

                )
            findNavController().navigate(action)
        },
            onDelete = { productHistory ->
                val dialog = ConfirmDeleteDialog {
                    viewModel.deleteProductHistory(productHistory)
                }
                dialog.show(childFragmentManager, "ConfirmDeleteDialog")
            })
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productUsageAdapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.price.observe(viewLifecycleOwner) { price ->
            binding.tvPrice.text = "${Util.priceFormatter.format(price)} ریال"
        }
        viewModel.history.observe(viewLifecycleOwner) { list ->
            productUsageAdapter.submitList(list)
            binding.tvNoItem.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.lastInsertedId.observe(viewLifecycleOwner) { id ->
            if (id != null) {
                productUsageAdapter.setLastInsertedId(id)
                viewModel.resetLastInsertedId()
            }
        }
    }

    private fun setupListeners() {

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCalculate.setOnClickListener {
            handleCalculateButton()
        }
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            @SuppressLint("DefaultLocale")
            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s.isNullOrEmpty()) return

                isEditing = true

                val cleanString = s.toString().replace(",", "")

                try {
                    val parsed = cleanString.toDoubleOrNull()
                    if (parsed != null) {
                        val formatted = String.format("%,.0f", parsed)
                        binding.etQuantity.setText(formatted)
                        binding.etQuantity.setSelection(formatted.length)

                        // نمایش مقدار به کیلوگرم اگر بیشتر از 1000 گرم بود
                        if (parsed >= 1000) {
                            val kiloValue = parsed / 1000
                            binding.tvKiloEquivalent.visibility = View.VISIBLE

                            val displayValue =
                                if (kiloValue % 1 == 0.0)
                                    kiloValue.toInt().toString()
                                else
                                    String.format("%.1f", kiloValue)
                            binding.tvKiloEquivalent.text = "معادل $displayValue کیلوگرم"
                        } else {
                            binding.tvKiloEquivalent.visibility = View.GONE
                        }

                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                isEditing = false
            }

        })

    }

    @SuppressLint("DefaultLocale")
    private fun handleCalculateButton() {
        val quantityInput = binding.etQuantity.text.toString().replace(",", "").toDoubleOrNull()

        //val quantityInput = binding.etQuantity.text.toString().toDoubleOrNull()
        if (quantityInput == null) {
            binding.etQuantity.error = "عدد معتبر وارد کنید"
            return
        }

        val quantityInGrams = quantityInput
        val remainder = quantityInGrams % totalQuantity
        val correctedQuantity = if (remainder == 0.0) {
            quantityInGrams
        } else {
            (quantityInGrams / totalQuantity).roundToInt() * totalQuantity
        }

        if (correctedQuantity != quantityInGrams) {
            val formattedQty = formatQuantity(totalQuantity)
            Toast.makeText(
                context,
                "مقدار به ${correctedQuantity.toInt()} اصلاح شد چون باید ضریبی از $formattedQty باشد.",
                Toast.LENGTH_LONG
            ).show()

            val value = correctedQuantity
            binding.etQuantity.setText(formatQuantity(value))
        }
        viewModel.calculateAndSave(args.productId, correctedQuantity / 1000, pricePerKg)
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
