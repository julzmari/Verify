package com.mobdeve.s18.verify.controller

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobdeve.s18.verify.R

class PhotoSubmissionSheet(
    private val photoBitmap: Bitmap,
    private val onSubmit: (status: String, note: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_photo_submission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val previewImage = view.findViewById<ImageView>(R.id.previewImage)
        val statusGroup = view.findViewById<RadioGroup>(R.id.statusRadioGroup)
        val noteInput = view.findViewById<EditText>(R.id.noteInput)
        val submitBtn = view.findViewById<Button>(R.id.submitBtn)
        val cancelBtn = view.findViewById<Button>(R.id.cancelBtn)

        previewImage.setImageBitmap(photoBitmap)

        submitBtn.setOnClickListener {
            val selectedId = statusGroup.checkedRadioButtonId
            val status = when (selectedId) {
                R.id.statusDelivery -> "Delivery"
                R.id.statusInTransit -> "In-Transit"
                R.id.statusUnexpected -> "Unexpected Stop"
                else -> "Unknown"
            }
            val note = noteInput.text.toString().trim()
            onSubmit(status, note)
            dismiss()
        }

        cancelBtn.setOnClickListener {
            dismiss()
        }
    }
}
