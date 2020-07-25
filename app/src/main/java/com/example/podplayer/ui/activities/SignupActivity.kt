package com.example.podplayer.ui.activities

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.example.podplayer.R
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.facebookBtn
import kotlinx.android.synthetic.main.activity_login.googleBtn
import kotlinx.android.synthetic.main.activity_login.twitterBtn
import kotlinx.android.synthetic.main.activity_signup.*

class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

        auth = FirebaseAuth.getInstance()
        val iconFace = Typeface.createFromAsset(assets, "fonts/unicons.ttf")
        googleBtn.typeface = iconFace
        facebookBtn.typeface = iconFace
        twitterBtn.typeface = iconFace
        button_signup.setOnClickListener {
            signUp()
            }
        already_have_account.setOnClickListener {startActivity(Intent(this, LoginActivity::class.java)) }
        googleBtn.setOnClickListener { signIn() }
    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = auth.currentUser
        updateUI(currentUser)
    }

    //Firebase email&password signup
    private fun signUp() {
        val email = findViewById<EditText>(R.id.editTextTextEmailAddress_signup)
        val password = findViewById<EditText>(R.id.editTextTextPassword_signup)
        if (email.text.toString().isNotEmpty()&&password.text.toString().isNotEmpty()){
            auth.signInWithEmailAndPassword(email.text.toString(), password.text.toString()).addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    updateUI(user)
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_LONG).show()
                    updateUI(null)
                }
                hideProgress()
            }

        }else{
            if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
                email.error = "Please enter a valid email address"
                email.requestFocus()
            }
            email.error = "Please enter your email Address"
            email.requestFocus()
            password.error = "Please enter your password"
            password.requestFocus()
        }

    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun showProgress() {
        progress_bar_signup.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress_bar_signup.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LoginActivity.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(LoginActivity.TAG, "firebaseAuthWithGoogle" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(LoginActivity.TAG, "Google sign in failed", e)

            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgress()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) {task ->
            if (task.isSuccessful) {
                Log.d(LoginActivity.TAG, "Successful Signed in")
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(
                    this
                    , "Authentication failed", Toast.LENGTH_LONG
                ).show()
            }
            hideProgress()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, LoginActivity.RC_SIGN_IN)
    }
    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}