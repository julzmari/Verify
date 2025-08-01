package com.mobdeve.s18.verify.controller

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mobdeve.s18.verify.R
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap

class PhotoSubmissionSheet(
    private val photoBitmap: Bitmap,
    private val onSubmit: (status: String, fullSizeBitmap: Bitmap) -> Unit
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val previewImage = view.findViewById<ImageView>(R.id.previewImage)
        val statusGroup = view.findViewById<RadioGroup>(R.id.statusRadioGroup)
        val submitBtn = view.findViewById<Button>(R.id.submitBtn)
        val cancelBtn = view.findViewById<Button>(R.id.cancelBtn)

        try {
            val previewBitmap = create3x4Preview(photoBitmap)
            previewImage.setImageBitmap(previewBitmap)
        } catch (e: Exception) {
            AppLogger.e("PhotoSubmission", "Failed to generate preview")
            dismiss()
        }


        submitBtn.setOnClickListener {
            val selectedId = statusGroup.checkedRadioButtonId
            val status = when (selectedId) {
                R.id.statusDelivery -> "Delivery"
                R.id.statusInTransit -> "In-transit"
                R.id.statusUnexpected -> "Unexpected Stop"
                else -> "Unknown"
            }

            onSubmit(status, photoBitmap)
            dismiss()
        }

        cancelBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun create3x4Preview(source: Bitmap): Bitmap {
        // Determine target dimensions (3:4 aspect ratio)
        val targetWidth = (resources.displayMetrics.widthPixels * 0.8).toInt()
        val targetHeight = (targetWidth * 4) / 3

        // Create transformation matrix
        val matrix = Matrix().apply {
            // Calculate scale factors
            val scaleX = targetWidth.toFloat() / source.width
            val scaleY = targetHeight.toFloat() / source.height
            val scale = scaleX.coerceAtMost(scaleY) // Maintain aspect ratio

            // Apply uniform scale
            postScale(scale, scale)

            // Center the image
            postTranslate(
                ((targetWidth - source.width * scale) / 2f),
                ((targetHeight - source.height * scale) / 2f)
            )
        }

        // Create new bitmap with exact 3:4 dimensions
        val previewBitmap: Bitmap = createBitmap(targetWidth, targetHeight)

        val canvas: android.graphics.Canvas = android.graphics.Canvas(previewBitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
        canvas.drawBitmap(source, matrix, null)

        return previewBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the preview bitmap if needed
        // (The original photoBitmap will be managed by the caller)
    }
}