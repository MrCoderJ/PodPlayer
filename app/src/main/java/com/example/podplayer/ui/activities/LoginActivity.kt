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
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        callbackManager = CallbackManager.Factory.create()

        val iconFace = Typeface.createFromAsset(assets, "fonts/unicons.ttf")
        googleBtn.typeface = iconFace
        facebookBtn.typeface = iconFace
        twitterBtn.typeface = iconFace
        button.setOnClickListener { doLogin()

        }
        googleBtn.setOnClickListener { signIn()     }
        no_account.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
       // val loginButton: LoginButton = findViewById(R.id.facebookBtn)
       // loginButton.setPermissions("email", "public_profile")
      /**  loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                Log.d(TAG, "facebook:onSuccess:$result")
                handleFacebookAccessToken(result!!.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
                // [START_EXCLUDE]
                updateUI(null)
                // [END_EXCLUDE]
            }

            override fun onError(error: FacebookException?) {
                Log.d(TAG, "facebook:onError", error)
                // [START_EXCLUDE]
                updateUI(null)
                // [END_EXCLUDE]
            }

        })
      **/

    }

    override fun onStart() {
        super.onStart()
        val currentUser: FirebaseUser? = auth.currentUser
        updateUI(currentUser)
    }

    private fun showProgress() {
        progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress_bar.visibility = View.GONE
    }

    private fun doLogin() {
        val email = findViewById<EditText>(R.id.editTextTextEmailAddress)
        val password = findViewById<EditText>(R.id.editTextTextPassword)
        if (email.text.toString().isNotEmpty()&&password.text.toString().isNotEmpty()){
            auth.signInWithEmailAndPassword(email.text.toString(), password
                .text.toString()).addOnCompleteListener(this) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)

            }
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken){
        Log.d(TAG, "handleFacebookAccessToken: $token")
        showProgress()
        val credential =FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential).addOnCompleteListener(this){task ->
            if (task.isSuccessful){
                val user = auth.currentUser
                updateUI(user)
            }else{
                Toast.makeText(this, "Signin with Facebook failed", Toast.LENGTH_LONG).show()
                updateUI(null)
            }
            hideProgress()
        }
    }


    // Login with gmail
    private fun firebaseAuthWithGoogle(idToken: String) {
        showProgress()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) {task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Successful Signed in")
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
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }




    companion object {
        const val TAG = "GoogleActivity"
        const val RC_SIGN_IN = 9001
    }

}