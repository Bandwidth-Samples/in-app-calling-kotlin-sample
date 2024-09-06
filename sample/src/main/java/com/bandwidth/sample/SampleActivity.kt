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
import com.bandwidth.webrtc.useragent.AuthorizationRequest
import com.bandwidth.webrtc.useragent.BandwidthUA
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashMap


/**
 * Represents the main screen where users can interact with the Bandwidth services.
 */
class SampleActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySampleBinding
    private lateinit var bandwidthSession: BandwidthSession
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
    }

    /**
     * Handler for managing UI changes based on different states.
     */
    private val uiHandler by lazy {
        SampleUIHandler(this, binding, suspend {
            makeCall(null)
        }, ::terminateCall)
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
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        uiHandler.idleState()
        requestMicrophonePermission()
        askNotificationPermission()
        checkIfOpenedFromNotification(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIfOpenedFromNotification(intent)
    }

    private fun checkIfOpenedFromNotification(intent: Intent?) {
        if (intent?.extras != null) {
            val incomingPacketModel = IncomingPacketModel(
                intent.extras?.getString("accountId", ""),
                intent.extras?.getString("applicationId", ""),
                intent.extras?.getString("fromNo", ""),
                intent.extras?.getString("toNo", ""),
                intent.extras?.getString("token", "")
            )

            if (intent.extras?.getBoolean(Constants.IS_DIRECT_CALL) != null) {
                binding.phoneNumberBox.text = incomingPacketModel.fromNo
                uiHandler.callNumber = incomingPacketModel.fromNo
                CoroutineScope(Dispatchers.IO).launch { makeCall(incomingPacketModel) }
            } else {
                val mapData = HashMap<String?, String?>()
                mapData["accountId"] = incomingPacketModel.accountId
                mapData["applicationId"] = incomingPacketModel.applicationId
                mapData["fromNo"] = incomingPacketModel.fromNo
                mapData["toNo"] = incomingPacketModel.toNo
                mapData["token"] = incomingPacketModel.token

                val incomingCallIntent = Intent(Intent.ACTION_VIEW)
                incomingCallIntent.setClassName(packageName, IncomingCallActivity::class.java.name)
                incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                incomingCallIntent.putExtra(Constants.KEY_EXTRA_DATA, Util.mapToJson(mapData))
                startActivity(incomingCallIntent)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            firebaseHelper.fetchAndUpdateFCMToken(getString(R.string.default_user_id))
        } else {
            Toast.makeText(
                this,
                "Notification permission required to send/receive the notifications",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                firebaseHelper.fetchAndUpdateFCMToken(getString(R.string.default_user_id))
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Initiates a call to a specified remote contact using the Bandwidth services.
     */
    private fun makeCall(packetModel: IncomingPacketModel?) {
        try {
            // Check if the user has provided a call number
            if (uiHandler.callNumber.isEmpty()) {
                throw Exception(getString(R.string.must_not_be_empty))
            }

            // Update the UI to show ringing state
            uiHandler.ringingState(block = true)

            // Set the configuration for Bandwidth user agent
            setUserAgentConfig(packetModel)

            // Update user agent settings before login
            bandwidthUA.setAllowHeader(null)
            bandwidthUA.setVerifyServer(true)

            // Log in with the Bandwidth user agent
            bandwidthUA.login(this)

            // Create a new remote contact instance using the call number from UI handler
            val remoteContact = RemoteContact(domain = "+${uiHandler.callNumber}")
            remoteContact.scheme = ""

            // Initiate the call using the Bandwidth user agent
            bandwidthSession = bandwidthUA.call(callTo = remoteContact, context = this)

            // Add a listener to monitor the state and progress of the active call
            bandwidthSession.addSessionEventListener(object : BandwidthSessionEventListener {
                override fun callTerminated(session: BandwidthSession?, info: TerminationInfo?) {
                    terminateCall()
                    firebaseHelper.setStatus(
                        getString(R.string.default_user_id),
                        "Idle"
                    ) {
                        Log.d(localClassName, "Status updated")
                    }
                }

                override fun callProgress(session: BandwidthSession?) {
                    firebaseHelper.updateStatus(
                        getString(R.string.default_user_id),
                        session?.callState?.name
                    ) {
                        Log.d(localClassName, "Status updated")
                    }
                    session?.let {
                        // Update the UI based on the current call state
                        when (session.callState) {
                            CallState.CONNECTED -> {
                                uiHandler.connectedState()
                                firebaseHelper.updateStatus(
                                    getString(R.string.default_user_id),
                                    "In-Call"
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

        } catch (e: Exception) {
            e.printStackTrace()
            // Show an error message in a Snackbar if any exception occurs
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_LONG).show()

            // Reset the UI to its idle state in case of an error
            uiHandler.idleState()

            firebaseHelper.updateStatus(
                getString(R.string.default_user_id),
                "Idle"
            ) {
                Log.d(localClassName, "Status updated")
            }
        }
    }

    /**
     * Configures the user agent settings for interactions with Bandwidth services.
     *
     * This function sets the log level for the user agent, initializes the logger, and
     * configures the server details including proxy address, port, domain, transport type,
     * OAuth token URL, headers for authentication, and user account details.
     * The configuration details are fetched using utility methods.
     */
    private fun setUserAgentConfig(packetModel: IncomingPacketModel?) {
        bandwidthUA.setLogLevel(LogLevel.VERBOSE)
        bandwidthUA.setLogger(SampleLogger())
        var account = AccountUA(
            username = Util.getString("account.username", this),
            displayName = Util.getString("account.display-name", this),
            password = Util.getString("account.password", this)
        )
        if (packetModel != null) {
            account = AccountUA(
                username = "+${packetModel.toNo}",
                displayName = packetModel.toNo,
                password = packetModel.toNo
            )
            bandwidthUA.setServerConfig(
                proxyAddress = Util.getString("connection.domain", this),
                port = Util.getInt("connection.port", this),
                domain = Util.getString("connection.domain", this),
                transport = Transport.TLS,
                account = account,
                authorizationRequest = AuthorizationRequest(
                    tokenUrl = Util.getString(
                        "connection.token",
                        this
                    ),
                    user = Util.getString("connection.header.user", this),
                    pass = Util.getString("connection.header.pass", this),
                    headers = HashMap()
                )
            )
            bandwidthUA.setOauthToken(packetModel.token)
            account = AccountUA(
                username = "+${packetModel.fromNo}",
                displayName = packetModel.fromNo,
                password = packetModel.fromNo
            )
        } else {
            bandwidthUA.setServerConfig(
                proxyAddress = Util.getString("connection.domain", this),
                port = Util.getInt("connection.port", this),
                domain = Util.getString("connection.domain", this),
                transport = Transport.TLS,
                account = account,
                authorizationRequest = AuthorizationRequest(
                    tokenUrl = Util.getString(
                        "connection.token",
                        this
                    ),
                    user = Util.getString("connection.header.user", this),
                    pass = Util.getString("connection.header.pass", this),
                    headers = HashMap()
                )
            )
        }

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
