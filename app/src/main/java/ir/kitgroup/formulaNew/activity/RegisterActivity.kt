package ir.kitgroup.formulaNew.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ir.kitgroup.formulaNew.R
import ir.kitgroup.formulaNew.network.ApiClient
import ir.kitgroup.formulaNew.network.ApiService
import ir.kitgroup.formulaNew.core.SharedPrefManager
import ir.kitgroup.formulaNew.databinding.ActivityRegisterBinding
import ir.kitgroup.formulaNew.network.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.tieNameFamily.text.toString()
            val mobile = binding.tieMobile.text.toString()

            if (name.isEmpty() || mobile.isEmpty()) {
                Toast.makeText(this, R.string.error_all_fields_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendRegistrationToServer(name, mobile)
        }
    }

    private fun sendRegistrationToServer(name: String, phone: String) {
        val api = ApiClient.getClient().create(ApiService::class.java)
        val call = api.registerUser(name, phone)

        call.enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(
                call: Call<RegisterResponse>,
                response: Response<RegisterResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    SharedPrefManager.setUserRegistered(this@RegisterActivity, true)
                    Toast.makeText(
                        this@RegisterActivity,
                        "ثبت‌نام موفقیت‌آمیز بود",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "خطا در ثبت‌نام", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "ارتباط برقرار نشد", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}
