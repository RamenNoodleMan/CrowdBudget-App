package com.crowdbudget.app.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crowdbudget.app.MainActivity
import com.crowdbudget.app.R
import com.crowdbudget.app.databinding.ActivityLoginBinding
import com.crowdbudget.app.utils.PriceCalculator
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Clear previous errors
            binding.etEmail.error = null
            binding.etPassword.error = null

            // Validate inputs
            if (!PriceCalculator.isValidEmail(email)) {
                binding.etEmail.error = getString(R.string.error_email_invalid)
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.etPassword.error = getString(R.string.error_password_short)
                return@setOnClickListener
            }

            // Show loading
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.success_login, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)

                    val errorMessage = when {
                        e.message?.contains("user not found") == true -> getString(R.string.error_email_invalid)
                        e.message?.contains("password is invalid") == true -> "Incorrect password"
                        e.message?.contains("network error") == true -> getString(R.string.error_network)
                        else -> "Login failed: ${e.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
        }

        // Register hyperlink
        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
          //  showForgotPasswordDialog()
            binding.tvForgotPassword.visibility = View.GONE
        }
    }




    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etForgotEmail)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Email") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (PriceCalculator.isValidEmail(email)) {
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}