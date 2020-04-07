package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.common.IntentData
import example.jllarraz.com.passportreader.ui.views.TouchImageView
import kotlinx.android.synthetic.main.fragment_photo.*

class PassportPhotoFragment : androidx.fragment.app.Fragment() {

    private var passportPhotoFragmentListener: PassportPhotoFragmentListener? = null

    private var bitmap: Bitmap? = null



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val inflatedView = inflater.inflate(R.layout.fragment_photo, container, false)



        return inflatedView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments!!.containsKey(IntentData.KEY_IMAGE)) {
            bitmap = arguments.getParcelable<Bitmap>(IntentData.KEY_IMAGE)
        } else {
            //error
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
        image?.setImageBitmap(bitmap)
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
