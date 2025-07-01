package ir.kitgroup.formula.fragment

import android.annotation.SuppressLint
import ir.kitgroup.formula.databinding.FragmentProductUsageBinding
import android.os.Bundle
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
import ir.kitgroup.formula.R
import ir.kitgroup.formula.Util.calculatePricePerKg
import ir.kitgroup.formula.Util.formatDateShamsi
import ir.kitgroup.formula.Util.formatQuantity
import ir.kitgroup.formula.adapter.ProductUsageAdapter
import ir.kitgroup.formula.adapter.getTotalPriceForProduct
import ir.kitgroup.formula.adapter.getTotalQuantityForProduct
import ir.kitgroup.formula.viewmodel.ProductViewModel
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ProductUsageFragment : Fragment() {

    private var _binding: FragmentProductUsageBinding? = null
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productUsageAdapter: ProductUsageAdapter
    private val args: ProductUsageFragmentArgs by navArgs()
    private val formatter = DecimalFormat("#,###,###,###")
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
            binding.tvProductPrice.text = formatter.format(totalPrice) + " ریال "
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
        })
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productUsageAdapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupObservers() {
        viewModel.price.observe(viewLifecycleOwner) { price ->
            binding.tvPrice.text = "${formatter.format(price)} ریال"
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
    }

    @SuppressLint("DefaultLocale")
    private fun handleCalculateButton() {
        val quantityInput = binding.etQuantity.text.toString().toDoubleOrNull()
        if (quantityInput == null) {
            binding.etQuantity.error = "عدد معتبر وارد کنید"
            return
        }

        val quantityInGrams = quantityInput * 1000
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

            val formatter = DecimalFormat("0.####") // حداکثر 4 رقم اعشار، صفرهای اضافه نمایش داده نمی‌شوند
            val value = correctedQuantity / 1000.0
            binding.etQuantity.setText(formatter.format(value))
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
