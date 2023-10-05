// Copyright (c) 2020 iProov Ltd. All rights reserved.
package com.iproov.example

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.json.jsonDeserializer
import com.github.kittinunf.fuel.toolbox.HttpClient
import com.iproov.androidapiclient.AssuranceType
import com.iproov.androidapiclient.ClaimType
import com.iproov.androidapiclient.DemonstrationPurposesOnly
import com.iproov.androidapiclient.kotlinfuel.ApiClientFuel
import com.iproov.example.databinding.ActivityMainBinding
import com.iproov.sdk.IProov
import com.iproov.sdk.IProovCallbackLauncher
import com.iproov.sdk.core.exception.IProovException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class MainActivityCallback : AppCompatActivity() {

    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var binding: ActivityMainBinding
    private val iProovCallbackLauncher: IProovCallbackLauncher = IProovCallbackLauncher()
    private var userId = ""
    private var token = ""
    private val listener = object : IProovCallbackLauncher.Listener {

        override fun onConnecting() {
            binding.progressBar.isIndeterminate = true
        }

        override fun onConnected() {
            binding.progressBar.isIndeterminate = false
        }

        override fun onSuccess(result: IProov.SuccessResult) {
            //onResult(getString(R.string.success), "")
            println(result)
            val apiClientFuel = ApiClientFuel(
                this@MainActivityCallback,
                Constants.FUEL_URL,
                Constants.API_KEY,
                Constants.SECRET
            )
//            val apiClientFuel = ApiClientFuel(
//                this,
//                Constants.FUEL_URL,
//                Constants.API_KEY,
//                Constants.SECRET
//            )
//            uiScope.launch(Dispatchers.IO) {
//                try {
//                    val token = apiClientFuel.getToken(
//                        assuranceType,
//                        claimType,
//                        username
//                    )
//                    println(token)
//
//                    try {
////                        iProovCallbackLauncher.launch(
////                            this@MainActivityCallback,
////                            Constants.BASE_URL,
////                            token
////                        )
//                        onResult(getString(R.string.success), "")
//                    } catch (ex: Exception) {
//                        withContext(Dispatchers.Main) {
//                            onResult(getString(R.string.error), ex.localizedMessage)
//                        }
//                    }
//                } catch (ex: Exception) {
//                    withContext(Dispatchers.Main) {
//                        ex.printStackTrace()
//                        if (ex is FuelError) {
//                            val json = jsonDeserializer().deserialize(ex.response)
//                            val description = json.obj().getString("error_description")
//                            onResult(getString(R.string.error), description)
//                        } else {
//                            onResult(getString(R.string.error), getString(R.string.failed_to_get_token))
//                        }
//                    }
//                }
//            }
        }


        override fun onFailure(result: IProov.FailureResult) {
            println(result.reason)
            println(userId)
            onResult(result.reason.feedbackCode, getString(result.reason.description))
            // val randomString = generateRandomString()
            //  launchIProov(ClaimType.ENROL, randomString,  AssuranceType.GENUINE_PRESENCE)
        }


        override fun onProcessing(progress: Double, message: String?) {
            binding.progressBar.progress = progress.times(100).toInt()
        }

        override fun onError(exception: IProovException) =
            onResult(getString(R.string.error), exception.localizedMessage)

        override fun onCanceled(canceler: IProov.Canceler) =
            onResult(getString(R.string.canceled), "Canceled by $canceler")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (Constants.API_KEY.isEmpty() || Constants.SECRET.isEmpty()) {
            throw IllegalStateException("You must set the API_KEY and SECRET values in the Constants.kt file!")
        }

        arrayOf(
            binding.enrolGpaButton,
            binding.verifyLaButton,
            binding.verifyGpaButton
        ).forEach { it ->
            it.setOnClickListener {
                //val username = binding.usernameEditText.text.toString()
                userId = binding.usernameEditText.text.toString()
               //  userId = generateRandomString()

                if (userId.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.user_id_cannot_be_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val claimType = when (it) {
                        binding.enrolGpaButton -> ClaimType.ENROL
                        binding.verifyGpaButton -> ClaimType.VERIFY
                        binding.verifyLaButton -> ClaimType.VERIFY
                        else -> throw NotImplementedError()
                    }

                    val assuranceType = when (it) {
                        binding.enrolGpaButton -> AssuranceType.GENUINE_PRESENCE
                        binding.verifyGpaButton -> AssuranceType.GENUINE_PRESENCE
                        binding.verifyLaButton -> AssuranceType.LIVENESS
                        else -> throw NotImplementedError()
                    }
                    println(claimType)
                    println(userId)
                    println(assuranceType)
                    launchIProov(claimType, userId, assuranceType)
                }
            }
        }

        binding.versionTextView.text =
            getString(R.string.kotlin_version_format, iProovCallbackLauncher.sdkVersion)

        iProovCallbackLauncher.listener = listener
    }

    fun generateRandomString(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val chars2 = "abcdefghijklmnopqrstuvwxyz"
        val random = Random.Default
        val mailBuilder = StringBuilder()


        for (i in 1..10) {
            val randomIndex = random.nextInt(chars.length)
            val randomChar = chars[randomIndex]
            mailBuilder.append(randomChar)
        }
        mailBuilder.append("@")
        for (i in 1..5) {
            val randomIndex2 = random.nextInt(chars2.length)
            val randomChar2 = chars2[randomIndex2]
            mailBuilder.append(randomChar2)
        }
        mailBuilder.append(".com")
        return mailBuilder.toString()
    }

    override fun onDestroy() {
        iProovCallbackLauncher.listener = null
        super.onDestroy()
    }

    @DemonstrationPurposesOnly
    private fun launchIProov(claimType: ClaimType, username: String, assuranceType: AssuranceType) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = true

        val apiClientFuel = ApiClientFuel(
            this,
            Constants.FUEL_URL,
            Constants.API_KEY,
            Constants.SECRET
        )

        uiScope.launch(Dispatchers.IO) {
            try {
               token = apiClientFuel.getToken(
                    assuranceType,
                    claimType,
                    username
                )
                println(token)

                try {
                    iProovCallbackLauncher.launch(
                        this@MainActivityCallback,
                        Constants.BASE_URL,
                        token
                    )
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(getString(R.string.error), ex.localizedMessage)
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    ex.printStackTrace()
                    if (ex is FuelError) {
                        val json = jsonDeserializer().deserialize(ex.response)
                        val description = json.obj().getString("error_description")
                        onResult(getString(R.string.error), description)
                    } else {
                        onResult(getString(R.string.error), getString(R.string.failed_to_get_token))
                    }
                }
            }
        }
    }

    private fun onResult(title: String?, resultMessage: String?) {
        binding.progressBar.progress = 0
        binding.progressBar.visibility = View.GONE

        AlertDialog.Builder(this@MainActivityCallback)
            .setTitle(title)
            .setMessage(resultMessage)
            .setPositiveButton("Try again") { dialog, _ ->
                val randomString = generateRandomString()
                launchIProov(ClaimType.ENROL, randomString, AssuranceType.GENUINE_PRESENCE)
            }
            .setNegativeButton("cancle") { dialog, _ -> dialog.cancel() }
            .show()
    }


//    fun makeHttpPostRequest() {
//        // Define the URL for your API endpoint
//        val apiUrl = "https://api.example.com/post_endpoint"
//
//        // Create an HttpClient
//        val httpClient: HttpClient = DefaultHttpClient()
//
//        // Create an HttpPost object
//        val httpPost = HttpPost(apiUrl)
//
//        // Define the JSON payload for your POST request
//        val jsonPayload = "{\"key1\": \"value1\", \"key2\": \"value2\"}"
//
//        // Set the JSON payload as the request entity
//        val entity = StringEntity(jsonPayload, "UTF-8")
//        entity.setContentType("application/json")
//        httpPost.entity = entity
//
//        try {
//            // Execute the POST request
//            val response: HttpResponse = httpClient.execute(httpPost)
//
//            // Get the response entity
//            val inputStream = response.entity.content
//
//            // Read the response content
//            val reader = BufferedReader(InputStreamReader(inputStream))
//            val stringBuilder = StringBuilder()
//            var line: String?
//
//            while (reader.readLine().also { line = it } != null) {
//                stringBuilder.append(line).append("\n")
//            }
//
//            val responseBody = stringBuilder.toString()
//
//            // Handle the response data as needed
//            println("Response: $responseBody")
//
//            // Close the input stream and release resources
//            inputStream.close()
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }



}
