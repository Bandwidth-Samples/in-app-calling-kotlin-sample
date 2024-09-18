package com.bandwidth.sample

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bandwidth.sample.databinding.ActivitySampleBinding
import com.bandwidth.sample.firebase.FirebaseHelper
import com.bandwidth.sample.incoming_call.IncomingCallActivity
import com.bandwidth.sample.incoming_call.model.IncomingPacketModel
import com.bandwidth.sample.model.AuthTokenResponse
import com.bandwidth.sample.notification.Constants
import com.bandwidth.webrtc.log.LogLevel
import com.bandwidth.webrtc.session.BandwidthSession
import com.bandwidth.webrtc.session.BandwidthSessionEventListener
import com.bandwidth.webrtc.session.CallState
import com.bandwidth.webrtc.session.NotifyEvent
import com.bandwidth.webrtc.session.RemoteContact
import com.bandwidth.webrtc.session.TerminationInfo
import com.bandwidth.webrtc.sip.enums.Transport
import com.bandwidth.webrtc.useragent.AccountUA
import com.bandwidth.webrtc.useragent.BandwidthUA
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.util.concurrent.CompletableFuture

/**
 * Represents the main screen where users can interact with the Bandwidth services.
 */
class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding
    private lateinit var bandwidthSession: BandwidthSession
    private lateinit var userId: String
    private lateinit var displayname: String
    private val bandwidthUA = BandwidthUA()
    private val firebaseHelper = FirebaseHelper()

    /**
     * Companion object containing constants related to permission requests.
     */
    companion object {
        /**
         * Request code used to identify the microphone permission request.
         * This constant is used when requesting the RECORD_AUDIO permission
         * and in the subsequent callback to check the result of the request.
         */
        const val RECORD_AUDIO_REQUEST_CODE = 1
        const val TAG = "SampleActivity"
    }

    /**
     * Handler for managing UI changes based on different states.
     */
    private val uiHandler by lazy {
        SampleUIHandler(this, binding, ::makeCall, ::terminateCall)
    }

    /**
     * Initializes the activity, sets up the UI, and requests microphone permission.
     *
     * This method is invoked when the activity starts. It sets up the view binding,
     * content view, and toolbar. The UI state is set to 'idle' by default. Additionally,
     * this method triggers a request for microphone permission.
     *
     * @param savedInstanceState If the activity is being re-initialized after being shut
     * down previously, this contains the data most recently supplied in onSaveInstanceState(Bundle).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = Util.getString("account.username", this)
        displayname = Util.getString("account.display-name", this)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        uiHandler.idleState()
        requestMicrophonePermission()
        askNotificationPermission()
        setUserAgentConfig()
        checkIfOpenedFromNotification(intent)
    }

    /**
     * Fetching the authorization token for
    * */
    private fun getOAuthTokenFromUrl(
        authUrl: String, authUser: String, authPass: String,
    ): AuthTokenResponse? {
        val client = OkHttpClient.Builder().build()

        // Empty RequestBody for POST request without body
        val requestBody = FormBody.Builder().add("grant_type", "client_credentials").build()

        val request = Request.Builder().url(authUrl).addHeader(
            "Authorization", Credentials.basic(authUser, authPass)
        ).addHeader("Content-Type", "application/x-www-form-urlencoded").post(requestBody).build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val authToken =
                    GsonBuilder().create().fromJson(responseBody, AuthTokenResponse::class.java)
                authToken
            } else {
                Log.e(
                    "Bandwidth",
                    "Failed to get auth token from $authUrl ${response.code} ${response.body}"
                )
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIfOpenedFromNotification(intent)
    }

    /**
     * this method perform following actions when notification received
     * 1. Open the [IncomingCallActivity] when app is in foreground
     * 2. make the call if [SampleActivity] opened using [onNewIntent] from [IncomingCallActivity]
     * */
    private fun checkIfOpenedFromNotification(intent: Intent?) {
        if (intent?.extras != null) {
            val incomingPacketModel = IncomingPacketModel(
                intent.extras?.getString("accountId", ""),
                intent.extras?.getString("applicationId", ""),
                intent.extras?.getString("fromNo", ""),
                intent.extras?.getString("toNo", "")
            )

            if (intent.extras?.getBoolean(Constants.IS_DIRECT_CALL) != null) {
                binding.phoneNumberBox.text = incomingPacketModel.fromNo.replace("+", "")
                uiHandler.callNumber = incomingPacketModel.fromNo.replace("+", "")
                makeCall()
            } else {
                val mapData = HashMap<String?, String?>()
                mapData["accountId"] = incomingPacketModel.accountId
                mapData["applicationId"] = incomingPacketModel.applicationId
                mapData["fromNo"] = incomingPacketModel.fromNo
                mapData["toNo"] = incomingPacketModel.toNo

                val incomingCallIntent = Intent(Intent.ACTION_VIEW)
                incomingCallIntent.setClassName(packageName, IncomingCallActivity::class.java.name)
                incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                incomingCallIntent.putExtra(Constants.KEY_EXTRA_DATA, Util.mapToJson(mapData))
                startActivity(incomingCallIntent)
            }
        }
    }

    /**
     * permission handler for push notification permission
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            firebaseHelper.fetchAndUpdateFCMToken(userId)
        } else {
            Toast.makeText(
                this,
                "Notification permission required to send/receive the notifications",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ask the push notification permission above Android 12
     * we are using FCM here to get the notification when a inbound call initiated
     */
    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                firebaseHelper.fetchAndUpdateFCMToken(userId)
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            firebaseHelper.fetchAndUpdateFCMToken(userId)
        }
    }

    /**
     * Initiates a call to a specified remote contact using the Bandwidth services.
     */
    private fun makeCall() {
        try {
            // Check if the user has provided a call number
            if (uiHandler.callNumber.isEmpty()) {
                throw Exception(getString(R.string.must_not_be_empty))
            }

            // Update the UI to show ringing state
            uiHandler.ringingState(block = true)

            // Create a new remote contact instance using the call number from UI handler
            val remoteContact =
                RemoteContact(
                    displayName = "+${uiHandler.callNumber}",
                    username = "+${uiHandler.callNumber}"
                )
            remoteContact.scheme = ""

            //Fetch the authorization token first before setting up the
            // do not forgot to refresh the token as per the expiry
            val future: CompletableFuture<AuthTokenResponse> = CompletableFuture.supplyAsync {
                getOAuthTokenFromUrl(
                    Util.getString(
                        "connection.auth.url", this
                    ), Util.getString(
                        "connection.auth.header.user", this
                    ), Util.getString(
                        "connection.auth.header.pass", this
                    )
                )
            }
            val authTokenResponse = future.get()
            if (authTokenResponse != null) {
                // Initiate the call using the Bandwidth user agent
                bandwidthSession = bandwidthUA.call(
                    callTo = remoteContact,
                    context = this,
                    authToken = authTokenResponse.accessToken
                )

                // Add a listener to monitor the state and progress of the active call
                bandwidthSession.addSessionEventListener(object : BandwidthSessionEventListener {
                    override fun callTerminated(
                        session: BandwidthSession?, info: TerminationInfo?
                    ) {
                        terminateCall()
                        firebaseHelper.setStatus(
                            userId, "Idle"
                        ) {
                            Log.d(localClassName, "Status updated")
                        }
                    }

                    override fun callProgress(session: BandwidthSession?) {
                        firebaseHelper.updateStatus(
                            userId, session?.callState?.name
                        ) {
                            Log.d(localClassName, "Status updated")
                        }
                        session?.let {
                            // Update the UI based on the current call state
                            when (session.callState) {
                                CallState.CONNECTED -> {
                                    uiHandler.connectedState()
                                    firebaseHelper.updateStatus(
                                        userId, "In-Call"
                                    ) {
                                        Log.d(localClassName, "Status updated")
                                    }
                                }

                                CallState.CONNECTING -> uiHandler.connectingState(session)
                                CallState.HOLD -> uiHandler.holdState(session)
                                CallState.RINGING -> {
                                    uiHandler.ringingState()
                                }

                                else -> {} // Handle other call states if needed
                            }
                        }
                    }

                    override fun incomingNotify(event: NotifyEvent?, dtmfValue: String?) {}
                })
            } else {
                terminateCall()
                // Show an error message in a Snackbar if any exception occurs
                Snackbar.make(
                    binding.root, getString(R.string.invalid_auth_token), Snackbar.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Show an error message in a Snackbar if any exception occurs
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_LONG).show()

            // Reset the UI to its idle state in case of an error
            uiHandler.idleState()

            firebaseHelper.updateStatus(
                userId, "Idle"
            ) {
                Log.d(localClassName, "Status updated")
            }
        }
    }

    /**
     * Configures the user agent settings for interactions with Bandwidth services.
     *
     * This function sets the log level for the user agent, initializes the logger, and
     * configures the server details including proxy address, port,
     * domain, transport type, user account details.
     * The configuration details are fetched using utility methods.
     */
    private fun setUserAgentConfig() {
        bandwidthUA.setLogLevel(LogLevel.VERBOSE)
        bandwidthUA.setLogger(SampleLogger())
        val account = AccountUA(
            username = Util.getString("account.username", this),
            displayName = Util.getString("account.display-name", this),
            password = Util.getString("account.password", this)
        )
        val body = HashMap<String, String>()
        body["grant_type"] = "client_credentials"
        bandwidthUA.setServerConfig(
            proxyAddress = Util.getString("connection.domain", this),
            port = Util.getInt("connection.port", this),
            domain = Util.getString("connection.domain", this),
            transport = Transport.TLS,
            account = account,
        )
        // Log in with the Bandwidth user agent
        bandwidthUA.login(this)
    }

    /**
     * Terminates the active call session.
     *
     * Attempts to terminate the current `bandwidthSession`. If any exception occurs during
     * the termination process, it displays the error message in a `Snackbar`. Regardless
     * of whether an exception occurs, it always updates the UI to the end call state using
     * the `uiHandler`.
     */
    private fun terminateCall() {
        try {
            bandwidthSession.terminate()
        } catch (e: Exception) {
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_LONG).show()
        }
        uiHandler.endCallState()
    }

    /**
     * Requests the microphone permission from the user.
     *
     * If the permission is not already granted, this function prompts the user with
     * a dialog to request the `RECORD_AUDIO` permission. The result of this request
     * can be captured in the `onRequestPermissionsResult` callback using the
     * `RECORD_AUDIO_REQUEST_CODE`.
     */
    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE
            )
        }
    }
}
