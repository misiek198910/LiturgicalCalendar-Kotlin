package mivs.liturgicalcalendar.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mivs.liturgicalcalendar.R

class ReadingsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_readings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleText: TextView = view.findViewById(R.id.titleText)
        val contentText: TextView = view.findViewById(R.id.contentText)

        // Pobieramy dane z argumentów
        val sigla = arguments?.getString(ARG_SIGLA)
        val content = arguments?.getString(ARG_CONTENT)

        android.util.Log.e("BottomSheet", "Sigla: $sigla")
        android.util.Log.e("BottomSheet", "Content: $content")

        titleText.text = sigla ?: "Brak tytułu"
        contentText.text = content ?: "Brak treści"
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    companion object {
        private const val ARG_SIGLA = "arg_sigla"
        private const val ARG_CONTENT = "arg_content"

        fun newInstance(sigla: String, content: String): ReadingsBottomSheet {
            val fragment = ReadingsBottomSheet()
            val args = Bundle()
            args.putString(ARG_SIGLA, sigla)
            args.putString(ARG_CONTENT, content)
            fragment.arguments = args
            return fragment
        }
    }
}