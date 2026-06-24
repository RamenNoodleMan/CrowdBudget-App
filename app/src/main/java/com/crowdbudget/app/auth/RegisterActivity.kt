package com.crowdbudget.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crowdbudget.app.R
import com.crowdbudget.app.databinding.ActivityRegisterBinding
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Login link - takes user back to login screen
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Clear previous errors
            binding.etName.error = null
            binding.etEmail.error = null
            binding.etPassword.error = null

            // Validate all inputs
            var hasError = false

            if (!PriceCalculator.isValidName(name)) {
                binding.etName.error = getString(R.string.error_name_short)
                hasError = true
            }

            if (!PriceCalculator.isValidEmail(email)) {
                binding.etEmail.error = getString(R.string.error_email_invalid)
                hasError = true
            }

            if (!PriceCalculator.isValidPassword(password)) {
                binding.etPassword.error = getString(R.string.error_password_weak)
                hasError = true
            }

            if (hasError) return@setOnClickListener

            // Show loading
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Creating account..."
            binding.progressBar.visibility = android.view.View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val user = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = getString(R.string.register)
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, R.string.success_register, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = getString(R.string.register)
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = getString(R.string.register)
                    binding.progressBar.visibility = android.view.View.GONE

                    val errorMessage = when {
                        e.message?.contains("email already in use") == true -> getString(R.string.error_email_exists)
                        e.message?.contains("weak password") == true -> getString(R.string.error_password_weak)
                        e.message?.contains("network error") == true -> getString(R.string.error_network)
                        else -> "Registration failed: ${e.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
        }
    }
}