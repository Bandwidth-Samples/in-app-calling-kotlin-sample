package com.bandwidth.sample

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.bandwidth.sample.databinding.ActivitySampleBinding
import com.bandwidth.webrtc.session.*
import kotlinx.coroutines.*

/**
 * UI handler for managing and updating the UI elements of the sample activity based on different call states.
 *
 * @param activity Reference to the [SampleActivity].
 * @param binding Data binding instance for the sample activity layout.
 * @param makeCallAction Callback to be invoked to initiate a call.
 * @param terminateCallAction Callback to be invoked to terminate an ongoing call.
 */
class SampleUIHandler(
    private val activity: SampleActivity,
    private val binding: ActivitySampleBinding,
    private val makeCallAction: suspend () -> Unit,
    private val terminateCallAction: () -> Unit
) {
    var callNumber: String = ""
    private var connectedTimeElapsed: Int = 0

    /**
     * To set the call number manually
     * */

    fun callNumber(number: String) {
        callNumber = number
    }

    /**
     * Timer for tracking the elapsed call duration.
     * It utilizes the CountDownTimer, which ticks every second, to keep track of
     * the duration since the call has been connected. The timer counts indefinitely
     * because it's set with the maximum long value.
     */
    private var connectedTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {

        /**
         * This method is called for every tick of the timer.
         * @param millisUntilFinished The amount of time until the timer finishes.
         * In this case, it's not significant as the timer is set to count indefinitely.
         */
        override fun onTick(millisUntilFinished: Long) {
            // Increment the time elapsed since the call has been connected.
            connectedTimeElapsed++

            // Convert the elapsed time to a formatted string showing minutes and seconds.
            val timeString =
                String.format("%02d:%02d", connectedTimeElapsed / 60, connectedTimeElapsed % 60)

            // Display the formatted elapsed time in the UI.
            binding.timerLabel.text = timeString
        }

        /**
         * This method is called when the timer finishes.
         * It's currently empty as the timer is set to count indefinitely, so it won't practically finish.
         */
        override fun onFinish() {}
    }

    /**
     * Sets listeners to dial pad buttons for sending DTMF signals during an active call.
     *
     * @param session Active [BandwidthSession] instance.
     */
    private fun setSendDTMFListener(session: BandwidthSession) {
        // Create a mapping of buttons to their corresponding DTMF values
        val dtmfMapping = mapOf(
            binding.dial1 to DTMF.ONE,
            binding.dial2 to DTMF.TWO,
            binding.dial3 to DTMF.THREE,
            binding.dial4 to DTMF.FOUR,
            binding.dial5 to DTMF.FIVE,
            binding.dial6 to DTMF.SIX,
            binding.dial7 to DTMF.SEVEN,
            binding.dial8 to DTMF.EIGHT,
            binding.dial9 to DTMF.NINE,
            binding.dial0 to DTMF.ZERO,
            binding.dialStar to DTMF.STAR,
            binding.dialPound to DTMF.POUND
        )

        // Iterate through the mapping to assign the sendDTMF action as the click listener for each button
        for ((button, dtmfValue) in dtmfMapping) {
            button.setOnClickListener { session.sendDTMF(dtmfValue) }
        }

        binding.backspace.setOnClickListener { } // Empty block
    }


    /**
     * Sets listeners to dial pad buttons to construct the phone number.
     */
    private fun setDialNumberListener() {
        callNumber = ""

        // Mapping of buttons to their corresponding number values
        val buttonMapping = mapOf(
            binding.dial1 to 1,
            binding.dial2 to 2,
            binding.dial3 to 3,
            binding.dial4 to 4,
            binding.dial5 to 5,
            binding.dial6 to 6,
            binding.dial7 to 7,
            binding.dial8 to 8,
            binding.dial9 to 9,
            binding.dial0 to 0
        )

        // Set click listeners for numbered buttons
        for ((button, value) in buttonMapping) {
            button.setOnClickListener { addNumber(value) }
        }

        // Other buttons' click listeners
        binding.dialStar.setOnClickListener { }  // Empty block
        binding.dialPound.setOnClickListener { }  // Empty block
        binding.backspace.setOnClickListener { backspace() }
    }


    /**
     * Appends the provided number to the current phone number string.
     * This method is typically used in dial-pad implementations where
     * users tap on a number, and it gets appended to the displayed phone number.
     *
     * @param number Integer to be added to the phone number.
     */
    private fun addNumber(number: Int) {
        // Append the provided number to the existing callNumber string.
        callNumber += number

        // Update the UI to reflect the newly appended number.
        binding.phoneNumberBox.text = callNumber
    }

    /**
     * Removes the last digit from the phone number string.
     * This method is typically used in dial-pad implementations to allow
     * users to correct their input by removing the most recently entered digit.
     */
    private fun backspace() {
        // Check if the callNumber string is not empty.
        if (callNumber.isNotEmpty()) {
            // Remove the last character from the callNumber string.
            callNumber = callNumber.dropLast(1)

            // Update the UI to reflect the removed number.
            binding.phoneNumberBox.text = callNumber
        }
    }

    /**
     * Updates UI for the idle state, indicating no ongoing calls.
     */
    fun idleState() {
        CoroutineScope(Dispatchers.Main).launch {
            // Update textual components
            binding.apply {
                callingStatus.text = activity.getString(R.string.idle)
                statusLabel.text = activity.getString(R.string.status_idle)
                phoneNumberBox.text = activity.getString(R.string.phone_number)
                holdButton.text = activity.getString(R.string.hold)
                muteButton.text = activity.getString(R.string.mute)

                // Adjust component visibility
                phoneNumberBox.visibility = View.VISIBLE
                phoneNumberHelp.visibility = View.VISIBLE
                backspace.visibility = View.VISIBLE
                phoneNumberLabel.visibility = View.INVISIBLE

                // Change button color to green
                callButton.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.green))

                // Assign behaviors
                setDialNumberListener()
                setDisableButton(muteButton)
                setDisableButton(holdButton)
                callButton.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch { makeCallAction() }
                }
            }
        }
    }

    /**
     * Updates UI for the ringing state, indicating an outgoing call has been initiated.
     */
    fun ringingState(block: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            // Update textual components
            binding.apply {
                callingStatus.text = activity.getString(R.string.ringing)
                statusLabel.text = activity.getString(R.string.status_ringing)
                phoneNumberLabel.text = "+$callNumber"

                // Adjust component visibility
                phoneNumberLabel.visibility = View.VISIBLE
                phoneNumberBox.visibility = View.INVISIBLE
                phoneNumberHelp.visibility = View.INVISIBLE
                backspace.visibility = View.INVISIBLE

                if (block) {
                    // Set call button color to red
                    callButton.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.gray))

                    // Clear any previous onClick behavior from call button
                    callButton.setOnClickListener { }
                }
            }
        }
    }

    /**
     * Updates UI for the connecting state, indicating a connection attempt is in progress.
     *
     * @param session Current [BandwidthSession] instance representing the call.
     */
    fun connectingState(session: BandwidthSession) {
        CoroutineScope(Dispatchers.Main).launch {
            // Initialize DTMF listeners and reset timer
            setSendDTMFListener(session)
            connectedTimeElapsed = 0
            connectedTimer.start()

            binding.apply {
                // Adjust visibility
                timerLabel.visibility = View.VISIBLE
                statusLabel.visibility = View.INVISIBLE

                // Assign behaviors
                setEnableButton(muteButton) { mutedState(session) }
                setEnableButton(holdButton) { session.hold(true) }
            }
        }
    }

    /**
     * Updates the UI to indicate that the call has been successfully connected.
     */
    fun connectedState() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.apply {
                // Update the calling status text
                callingStatus.text = activity.getString(R.string.connected)

                // Set call button color to red
                callButton.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.red))

                // Assign behaviors
                callButton.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        terminateCallAction()
                    }
                }
            }
        }
    }

    /**
     * Updates UI to show that the remote party is on hold.
     *
     * @param session Active [BandwidthSession] instance representing the ongoing call.
     */
    fun holdState(session: BandwidthSession) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.apply {
                // Update the status and button text
                callingStatus.text = activity.getString(R.string.on_hold)
                statusLabel.text = activity.getString(R.string.on_hold)
                holdButton.text = activity.getString(R.string.un_hold)

                // Adjust component visibility
                timerLabel.visibility = View.INVISIBLE
                statusLabel.visibility = View.VISIBLE

                // Modify the behavior based on hold state
                session.hold(true)
                setEnableButton(holdButton) { unHoldState(session) }
            }
        }
    }

    /**
     * Updates the UI to indicate that the local audio has been muted.
     *
     * @param session Active [BandwidthSession] instance representing the ongoing call.
     */
    private fun mutedState(session: BandwidthSession) {
        binding.apply {
            // Update the status and button text
            callingStatus.text = activity.getString(R.string.on_mute)
            statusLabel.text = activity.getString(R.string.on_mute)
            muteButton.text = activity.getString(R.string.un_mute)

            // Adjust component visibility
            timerLabel.visibility = View.INVISIBLE
            statusLabel.visibility = View.VISIBLE

            // Modify behavior to mute the audio and adjust the button to toggle the mute state
            session.muteAudio(true)
            setEnableButton(muteButton) { unMutedState(session) }
        }
    }

    /**
     * Updates the UI to indicate that the remote party has been taken off hold.
     *
     * @param session Active [BandwidthSession] instance representing the ongoing call.
     */
    private fun unHoldState(session: BandwidthSession) {
        binding.apply {
            // Update the hold button text to allow the call to be put on hold again
            holdButton.text = activity.getString(R.string.hold)

            // Adjust component visibility to show the timer and hide the status label
            timerLabel.visibility = View.VISIBLE
            statusLabel.visibility = View.INVISIBLE

            // Modify behavior to take the call off hold and adjust the button to toggle the hold state
            session.hold(false)
            setEnableButton(holdButton) { holdState(session) }

            // Return to the default connected state
            connectedState()
        }
    }

    /**
     * Updates the UI to indicate that the local audio has been reactivated after being muted.
     *
     * @param session Active [BandwidthSession] instance representing the ongoing call.
     */
    private fun unMutedState(session: BandwidthSession) {
        binding.apply {
            // Update the mute button text to allow it to be muted again
            muteButton.text = activity.getString(R.string.mute)

            // Adjust component visibility to show the timer and hide the status label
            timerLabel.visibility = View.VISIBLE
            statusLabel.visibility = View.INVISIBLE

            // Modify behavior to unmute the audio and adjust the button to toggle the mute state
            session.muteAudio(false)
            setEnableButton(muteButton) { mutedState(session) }

            // Return to the default connected state
            connectedState()
        }
    }

    /**
     * Updates UI post-call, resetting the interface to its initial state.
     */
    fun endCallState() {
        CoroutineScope(Dispatchers.Main).launch {
            // Update textual components
            binding.apply {
                statusLabel.text = activity.getString(R.string.status_end)
                callingStatus.text = activity.getString(R.string.hang_up)

                // Adjust visibility
                timerLabel.visibility = View.INVISIBLE
                statusLabel.visibility = View.VISIBLE

                // Set call button color to gray
                callButton.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(activity.resources, R.color.grayText, activity.theme)
                )

                // Assign behaviors
                setDialNumberListener()
                setDisableButton(muteButton)
                setDisableButton(holdButton)
                connectedTimer.cancel()
                delay(2000)
                idleState()
            }
        }
    }

    /**
     * Disables the specified button, adjusting its appearance to indicate its disabled state.
     *
     * @param button The [Button] instance that needs to be disabled.
     */
    private fun setDisableButton(button: Button) {
        setEnableButton(button, null)
    }

    /**
     * Enables or disables the specified button, adjusting its appearance to indicate its active or inactive state.
     * When provided with an onClick function, the button will be enabled, otherwise it will be disabled.
     *
     * @param button The [Button] instance to be modified.
     * @param onClick An optional lambda function to be executed on button click.
     */
    private fun setEnableButton(button: Button, onClick: (() -> Unit)? = null) {
        // Get custom colors from colors.xml file
        val (textColor, drawableColor) = if (onClick != null) {
            Pair(
                ContextCompat.getColor(activity, R.color.bandwidthBlue),
                ContextCompat.getColor(activity, R.color.bandwidthBlue)
            )
        } else {
            Pair(
                ContextCompat.getColor(activity, R.color.grayText),
                ContextCompat.getColor(activity, R.color.grayText)
            )
        }

        // Apply the colors to the button's text and icon
        button.setTextColor(textColor)
        TextViewCompat.setCompoundDrawableTintMode(button, PorterDuff.Mode.SRC_ATOP)
        TextViewCompat.setCompoundDrawableTintList(button, ColorStateList.valueOf(drawableColor))

        // Enable or disable the button based on the presence of the onClick function
        button.isEnabled = onClick != null

        // Assign the onClick function if provided
        button.setOnClickListener { onClick?.invoke() }
        // Update the view layout
        button.requestLayout()
    }
}
