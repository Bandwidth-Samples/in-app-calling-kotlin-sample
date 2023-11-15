package com.bandwidth.sample

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bandwidth.sample.databinding.ActivitySampleBinding
import com.bandwidth.webrtc.log.LogLevel
import com.bandwidth.webrtc.session.*
import com.bandwidth.webrtc.sip.enums.Transport
import com.bandwidth.webrtc.useragent.*
import com.google.android.material.snackbar.Snackbar

/**
 * Represents the main screen where users can interact with the Bandwidth services.
 */
class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding
    private lateinit var bandwidthSession: BandwidthSession
    private val bandwidthUA = BandwidthUA()

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
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        uiHandler.idleState()

        requestMicrophonePermission()
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

            // Set the configuration for Bandwidth user agent
            setUserAgentConfig()

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
                }

                override fun callProgress(session: BandwidthSession?) {
                    session?.let {
                        // Update the UI based on the current call state
                        when (session.callState) {
                            CallState.CONNECTED -> uiHandler.connectedState()
                            CallState.CONNECTING -> uiHandler.connectingState(session)
                            CallState.HOLD -> uiHandler.holdState(session)
                            CallState.RINGING -> uiHandler.ringingState()
                            else -> {} // Handle other call states if needed
                        }
                    }
                }

                override fun incomingNotify(event: NotifyEvent?, dtmfValue: String?) {}
            })

        } catch (e: Exception) {
            // Show an error message in a Snackbar if any exception occurs
            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_LONG).show()

            // Reset the UI to its idle state in case of an error
            uiHandler.idleState()
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
    private fun setUserAgentConfig() {
        bandwidthUA.setLogLevel(LogLevel.VERBOSE)
        bandwidthUA.setLogger(SampleLogger())
        bandwidthUA.setServerConfig(
            proxyAddress = Util.getString("connection.domain", this),
            port = Util.getInt("connection.port", this),
            domain = Util.getString("connection.domain", this),
            transport = Transport.TLS,
            oAuthTokenUrl = Util.getString("connection.token", this),
            authHeaderUser = Util.getString("connection.header.user", this),
            authHeaderPassword = Util.getString("connection.header.pass", this),
            account = AccountUA(
                username = Util.getString("account.username", this),
                displayName =  Util.getString("account.display-name", this),
                password =  Util.getString("account.password", this)
            )
        )
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        }
    }
}
