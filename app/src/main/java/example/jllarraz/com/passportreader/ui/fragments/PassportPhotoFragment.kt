package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.databinding.FragmentPhotoBinding

class PassportPhotoFragment : Fragment(R.layout.fragment_photo) {

    private var passportPhotoFragmentListener: PassportPhotoFragmentListener? = null

    private var bitmap: Bitmap? = null

    private lateinit var binding: FragmentPhotoBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPhotoBinding.bind(view)
        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_IMAGE)) {
            bitmap = arguments.getParcelable<Bitmap>(IntentData.KEY_IMAGE)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData(bitmap)
    }

    private fun refreshData(bitmap: Bitmap?) {
        if (bitmap == null) {
            return
        }
        binding.image.setImageBitmap(bitmap)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is PassportPhotoFragmentListener) {
            passportPhotoFragmentListener = activity
        }
    }

    override fun onDetach() {
        passportPhotoFragmentListener = null
        super.onDetach()

    }

    interface PassportPhotoFragmentListener

    companion object {

        fun newInstance(bitmap: Bitmap): PassportPhotoFragment {
            val myFragment = PassportPhotoFragment()
            val args = Bundle()
            args.putParcelable(IntentData.KEY_IMAGE, bitmap)
            myFragment.arguments = args
            return myFragment
        }
    }

}
